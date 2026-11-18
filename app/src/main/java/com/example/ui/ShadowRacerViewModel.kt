package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.AchievementEntity
import com.example.data.AppDatabase
import com.example.data.CarStateEntity
import com.example.data.GameRepository
import com.example.data.PlayerProfileEntity
import com.example.data.RaceCompletionOutcome
import com.example.data.RaceHistoryEntity
import com.example.data.SettingsEntity
import com.example.engine3d.GameGLRenderer
import com.example.engine3d.RaceHudSnapshot
import com.example.engine3d.RaceSimulation
import com.example.engine3d.RacerInput
import com.example.engine3d.SceneMode
import com.example.engine3d.ShowcaseConfig
import com.example.model.AppLanguage
import com.example.model.CameraPerspective
import com.example.model.CarSpec
import com.example.model.GameCatalog
import com.example.model.GameMode
import com.example.model.GraphicsQuality
import com.example.model.HeroAnimation
import com.example.model.HeroViewAngle
import com.example.model.Localization
import com.example.model.LocalizedLabels
import com.example.model.ScoringRules
import com.example.model.SpoilerStyle
import com.example.model.SteeringMode
import com.example.model.TrackSpec
import com.example.model.WheelStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Splash : Screen
    data object Home : Screen
    data object ModeSelect : Screen
    data object CareerLadder : Screen
    data object TrackSelect : Screen
    data object CarSelect : Screen
    data object Garage : Screen
    data object HeroProfile : Screen
    data object DailyRewards : Screen
    data object Leaderboard : Screen
    data object Settings : Screen
    data object Race : Screen
    data object Results : Screen
}

data class ChampionshipStanding(
    val name: String,
    val isPlayer: Boolean,
    val points: Int
)

data class ChampionshipSession(
    val raceIndex: Int = 0, // 0..3 (4 races)
    val trackIds: List<String> = listOf("futuristic_city", "mountain_highway", "desert_speedway", "cyberpunk_night"),
    val standings: Map<String, Int> = emptyMap(),
    val finished: Boolean = false
)

data class UiAppState(
    val profile: PlayerProfileEntity = PlayerProfileEntity(),
    val cars: List<CarStateEntity> = emptyList(),
    val achievements: List<AchievementEntity> = emptyList(),
    val recentRaces: List<RaceHistoryEntity> = emptyList(),
    val bestTimes: Map<String, Long> = emptyMap(),
    val settings: SettingsEntity = SettingsEntity(),
    val labels: LocalizedLabels = Localization.get(AppLanguage.ENGLISH)
)

class ShadowRacerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val repository = GameRepository(db)
    val soundEngine = SoundEngine(application)
    val tiltController = TiltSteeringController(application)
    val glRenderer = GameGLRenderer()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedMode = MutableStateFlow(GameMode.QUICK_RACE)
    val selectedMode: StateFlow<GameMode> = _selectedMode.asStateFlow()

    private val _selectedTrack = MutableStateFlow(GameCatalog.tracks[0])
    val selectedTrack: StateFlow<TrackSpec> = _selectedTrack.asStateFlow()

    private val _selectedCareerStage = MutableStateFlow<Int?>(null)
    val selectedCareerStage: StateFlow<Int?> = _selectedCareerStage.asStateFlow()

    private val _championship = MutableStateFlow<ChampionshipSession?>(null)
    val championship: StateFlow<ChampionshipSession?> = _championship.asStateFlow()

    private val _hudSnapshot = MutableStateFlow(RaceHudSnapshot())
    val hudSnapshot: StateFlow<RaceHudSnapshot> = _hudSnapshot.asStateFlow()

    private val _raceOutcome = MutableStateFlow<RaceCompletionOutcome?>(null)
    val raceOutcome: StateFlow<RaceCompletionOutcome?> = _raceOutcome.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _heroStudioAnim = MutableStateFlow(HeroAnimation.IDLE)
    val heroStudioAnim: StateFlow<HeroAnimation> = _heroStudioAnim.asStateFlow()

    private val _heroStudioView = MutableStateFlow(HeroViewAngle.FRONT)
    val heroStudioView: StateFlow<HeroViewAngle> = _heroStudioView.asStateFlow()

    private val _garageFocusCarId = MutableStateFlow("shadow_fang")
    val garageFocusCarId: StateFlow<String> = _garageFocusCarId.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var activeInput = RacerInput()
    private var activeSimulation: RaceSimulation? = null

    val appState: StateFlow<UiAppState> = combine(
        repository.profileFlow,
        repository.carsFlow,
        repository.achievementsFlow,
        repository.recentRacesFlow,
        combine(repository.bestTimesFlow, repository.settingsFlow) { bt, s -> bt to s }
    ) { profile, cars, achievements, races, pair ->
        val lang = AppLanguage.entries.find { it.code == pair.second.languageCode } ?: AppLanguage.ENGLISH
        UiAppState(
            profile = profile,
            cars = cars,
            achievements = achievements,
            recentRaces = races,
            bestTimes = pair.first,
            settings = pair.second,
            labels = Localization.get(lang)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiAppState())

    init {
        soundEngine.start()
        viewModelScope.launch {
            repository.ensureInitialized()
        }
        viewModelScope.launch {
            appState.collect { state ->
                syncRendererAndAudio(state)
            }
        }
        glRenderer.onHudUpdate = { snap ->
            _hudSnapshot.value = snap
        }
        glRenderer.onRaceEngineSound = { speedNorm, drifting, nitro, accel ->
            soundEngine.speedNorm = speedNorm
            soundEngine.isDrifting = drifting
            soundEngine.isNitroActive = nitro
            soundEngine.isAccelerating = accel
        }
    }

    private fun syncRendererAndAudio(state: UiAppState) {
        val settings = state.settings
        soundEngine.musicVolume = settings.musicVolume
        soundEngine.sfxVolume = settings.sfxVolume
        soundEngine.vibrationEnabled = settings.vibrationEnabled

        val cam = CameraPerspective.entries.find { it.id == settings.cameraPerspectiveId } ?: CameraPerspective.CHASE
        glRenderer.cameraPerspective = cam

        val quality = GraphicsQuality.entries.find { it.id == settings.graphicsQualityId } ?: GraphicsQuality.HIGH
        val screen = _currentScreen.value
        val displayCarId = if (screen == Screen.Garage) _garageFocusCarId.value else state.profile.selectedCarId
        val carSpec = GameCatalog.cars.find { it.id == displayCarId } ?: GameCatalog.cars[0]
        val carState = state.cars.find { it.carId == displayCarId }

        val outfit = GameCatalog.outfits.find { it.id == state.profile.selectedOutfitId } ?: GameCatalog.outfits[0]
        val acc = GameCatalog.accessories.find { it.id == state.profile.selectedAccessoryId } ?: GameCatalog.accessories[0]
        val aura = GameCatalog.auras.find { it.id == state.profile.selectedAuraId } ?: GameCatalog.auras[0]

        val heroAnim = when (screen) {
            Screen.HeroProfile -> _heroStudioAnim.value
            Screen.Results -> {
                val pos = _hudSnapshot.value.position
                val failed = _hudSnapshot.value.timeTrialFailed
                when {
                    failed -> HeroAnimation.LOSE
                    pos == 1 -> HeroAnimation.VICTORY
                    pos <= 3 -> HeroAnimation.CELEBRATION
                    else -> HeroAnimation.LOSE
                }
            }
            else -> HeroAnimation.IDLE
        }

        glRenderer.updateShowcase(
            ShowcaseConfig(
                carSpec = carSpec,
                bodyColorHex = carState?.paintColorHex ?: carSpec.defaultBodyColor,
                neonColorHex = carState?.neonColorHex ?: carSpec.defaultNeonColor,
                wheelStyleId = carState?.wheelStyleId ?: 0,
                spoilerStyleId = carState?.spoilerStyleId ?: 0,
                jacketColorHex = outfit.jacketColor,
                armorColorHex = outfit.armorColor,
                trimColorHex = outfit.trimColor,
                accessoryStyle = acc.styleIndex,
                auraColorHex = aura.colorHex,
                heroAnimation = heroAnim,
                heroViewAngle = _heroStudioView.value,
                quality = quality
            )
        )
    }

    fun navigateTo(screen: Screen) {
        soundEngine.playUiClick()
        _currentScreen.value = screen
        when (screen) {
            Screen.Home -> {
                soundEngine.isRacing = false
                glRenderer.sceneMode = SceneMode.HOME_SHOWCASE
                tiltController.stop()
            }
            Screen.Garage -> {
                soundEngine.isRacing = false
                _garageFocusCarId.value = appState.value.profile.selectedCarId
                glRenderer.dragYawOffsetDeg = 0f
                glRenderer.sceneMode = SceneMode.GARAGE_CAR
                syncRendererAndAudio(appState.value)
            }
            Screen.HeroProfile -> {
                soundEngine.isRacing = false
                glRenderer.dragYawOffsetDeg = 0f
                glRenderer.sceneMode = SceneMode.HERO_STUDIO
                syncRendererAndAudio(appState.value)
            }
            Screen.Results -> {
                soundEngine.isRacing = false
                glRenderer.sceneMode = SceneMode.RESULTS_PODIUM
                tiltController.stop()
                syncRendererAndAudio(appState.value)
            }
            Screen.Race -> {
                glRenderer.sceneMode = SceneMode.RACE
            }
            else -> {
                soundEngine.isRacing = false
                tiltController.stop()
            }
        }
    }

    fun startModeFlow(mode: GameMode) {
        _selectedMode.value = mode
        _selectedCareerStage.value = null
        when (mode) {
            GameMode.CAREER -> navigateTo(Screen.CareerLadder)
            GameMode.CHAMPIONSHIP -> {
                val initMap = (listOf(appState.value.profile.username) + GameCatalog.aiRivalNames).associateWith { 0 }
                _championship.value = ChampionshipSession(
                    raceIndex = 0,
                    standings = initMap,
                    finished = false
                )
                _selectedTrack.value = GameCatalog.tracks.first()
                navigateTo(Screen.CarSelect)
            }
            else -> navigateTo(Screen.TrackSelect)
        }
    }

    fun selectCareerStage(stage: Int) {
        val spec = GameCatalog.careerStages.find { it.stage == stage } ?: return
        _selectedMode.value = GameMode.CAREER
        _selectedCareerStage.value = stage
        _selectedTrack.value = GameCatalog.tracks.find { it.id == spec.trackId } ?: GameCatalog.tracks[0]
        navigateTo(Screen.CarSelect)
    }

    fun selectTrack(track: TrackSpec) {
        _selectedTrack.value = track
        navigateTo(Screen.CarSelect)
    }

    fun launchRace() {
        val state = appState.value
        val mode = _selectedMode.value
        val track = if (mode == GameMode.CHAMPIONSHIP) {
            val champ = _championship.value
            val tId = champ?.trackIds?.getOrNull(champ.raceIndex) ?: "futuristic_city"
            GameCatalog.tracks.find { it.id == tId } ?: _selectedTrack.value
        } else {
            _selectedTrack.value
        }
        _selectedTrack.value = track

        val carId = state.profile.selectedCarId
        val carSpec = GameCatalog.cars.find { it.id == carId } ?: GameCatalog.cars[0]
        val carState = state.cars.find { it.carId == carId }

        val careerStage = _selectedCareerStage.value
        val aiDiff = when {
            careerStage != null -> GameCatalog.careerStages.find { it.stage == careerStage }?.aiDifficulty ?: 0.78f
            mode == GameMode.CHAMPIONSHIP -> 0.84f + (_championship.value?.raceIndex ?: 0) * 0.04f
            else -> 0.80f
        }

        val sim = RaceSimulation(
            trackSpec = track,
            mode = mode,
            playerCarSpec = carSpec,
            playerBodyColor = carState?.paintColorHex ?: carSpec.defaultBodyColor,
            playerNeonColor = carState?.neonColorHex ?: carSpec.defaultNeonColor,
            playerWheelStyle = carState?.wheelStyleId ?: 0,
            playerSpoilerStyle = carState?.spoilerStyleId ?: 0,
            speedUpgradeLvl = carState?.speedLevel ?: 0,
            accelUpgradeLvl = carState?.accelLevel ?: 0,
            handlingUpgradeLvl = carState?.handlingLevel ?: 0,
            brakingUpgradeLvl = carState?.brakingLevel ?: 0,
            nitroUpgradeLvl = carState?.nitroLevel ?: 0,
            aiDifficulty = aiDiff,
            playerName = state.profile.username
        )

        sim.onCountdownTick = { isGo ->
            soundEngine.playCountdownBeep(isGo)
        }
        sim.onCollisionSfx = { heavy ->
            soundEngine.playCollision(heavy)
        }
        sim.onRaceCompleted = { finalSnap ->
            handleRaceFinished(sim, finalSnap)
        }

        activeSimulation = sim
        activeInput = RacerInput(
            autoAccelerate = state.settings.autoAccelerate,
            sensitivity = state.settings.controlSensitivity
        )
        glRenderer.currentInput = activeInput
        glRenderer.attachRaceSimulation(sim)

        _isPaused.value = false
        _raceOutcome.value = null
        _hudSnapshot.value = sim.buildHudSnapshot()
        soundEngine.isRacing = true

        if (state.settings.steeringModeId == SteeringMode.TILT.id) {
            tiltController.start()
        } else {
            tiltController.stop()
        }
        navigateTo(Screen.Race)
    }

    private fun handleRaceFinished(sim: RaceSimulation, snap: RaceHudSnapshot) {
        soundEngine.isRacing = false
        tiltController.stop()
        if (snap.position <= 3 && !snap.timeTrialFailed) {
            soundEngine.playVictoryFanfare()
        }

        viewModelScope.launch {
            var wonChampOverall = false
            val mode = sim.mode
            if (mode == GameMode.CHAMPIONSHIP) {
                val curChamp = _championship.value
                if (curChamp != null) {
                    val standings = curChamp.standings.toMutableMap()
                    val order = sim.getStandingsOrder()
                    order.forEachIndexed { idx, racer ->
                        val pts = ScoringRules.pointsForPosition(idx + 1)
                        standings[racer.name] = (standings[racer.name] ?: 0) + pts
                    }
                    val isLastRace = curChamp.raceIndex >= curChamp.trackIds.size - 1
                    if (isLastRace) {
                        val topName = standings.maxByOrNull { it.value }?.key
                        wonChampOverall = topName == appState.value.profile.username
                    }
                    _championship.value = curChamp.copy(
                        standings = standings,
                        finished = isLastRace
                    )
                }
            }

            val outcome = repository.recordCompletedRace(
                raceSessionId = snap.raceSessionId,
                mode = mode,
                trackId = sim.trackSpec.id,
                carId = sim.playerCarSpec.id,
                position = snap.position,
                raceTimeMs = snap.raceTimeMs,
                careerStagePlayed = _selectedCareerStage.value,
                wonChampionshipOverall = wonChampOverall
            )
            _raceOutcome.value = outcome
            navigateTo(Screen.Results)
        }
    }

    fun updateRaceControls(
        accel: Boolean? = null,
        brake: Boolean? = null,
        left: Boolean? = null,
        right: Boolean? = null,
        nitro: Boolean? = null,
        drift: Boolean? = null
    ) {
        val settings = appState.value.settings
        val tilt = if (settings.steeringModeId == SteeringMode.TILT.id) tiltController.tiltSteerValue else 0f
        activeInput = activeInput.copy(
            accelerate = accel ?: activeInput.accelerate,
            brake = brake ?: activeInput.brake,
            steerLeft = left ?: activeInput.steerLeft,
            steerRight = right ?: activeInput.steerRight,
            nitro = nitro ?: activeInput.nitro,
            drift = drift ?: activeInput.drift,
            tiltSteer = tilt,
            autoAccelerate = settings.autoAccelerate,
            sensitivity = settings.controlSensitivity
        )
        glRenderer.currentInput = activeInput
    }

    fun refreshTiltSample() {
        val settings = appState.value.settings
        if (settings.steeringModeId == SteeringMode.TILT.id) {
            activeInput = activeInput.copy(tiltSteer = tiltController.tiltSteerValue)
            glRenderer.currentInput = activeInput
        }
    }

    fun togglePause(pause: Boolean) {
        _isPaused.value = pause
        activeSimulation?.isPaused = pause
        soundEngine.isRacing = !pause
    }

    fun cycleCameraPerspective() {
        val all = CameraPerspective.entries
        val cur = CameraPerspective.entries.find { it.id == appState.value.settings.cameraPerspectiveId } ?: CameraPerspective.CHASE
        val next = all[(cur.ordinal + 1) % all.size]
        viewModelScope.launch {
            repository.updateSettings { it.copy(cameraPerspectiveId = next.id) }
        }
    }

    fun advanceNextRaceOrStage() {
        val mode = _selectedMode.value
        if (mode == GameMode.CAREER) {
            val cur = _selectedCareerStage.value ?: 1
            val next = (cur + 1).coerceAtMost(GameCatalog.careerStages.size)
            val unlocked = appState.value.profile.careerStageUnlocked
            if (next <= unlocked) {
                selectCareerStage(next)
                launchRace()
            } else {
                navigateTo(Screen.CareerLadder)
            }
        } else if (mode == GameMode.CHAMPIONSHIP) {
            val champ = _championship.value
            if (champ != null && !champ.finished) {
                _championship.value = champ.copy(raceIndex = champ.raceIndex + 1)
                launchRace()
            } else {
                startModeFlow(GameMode.CHAMPIONSHIP)
            }
        } else {
            val curIdx = _selectedTrack.value.index
            _selectedTrack.value = GameCatalog.tracks[(curIdx + 1) % GameCatalog.tracks.size]
            launchRace()
        }
    }

    // Garage & Hero Studio actions
    fun focusGarageCar(carId: String) {
        _garageFocusCarId.value = carId
        syncRendererAndAudio(appState.value)
        viewModelScope.launch {
            val car = appState.value.cars.find { it.carId == carId }
            if (car?.unlocked == true) {
                repository.selectCar(carId)
            }
        }
    }

    fun unlockOrSelectCar(carSpec: CarSpec) {
        viewModelScope.launch {
            val ok = repository.unlockCar(carSpec.id)
            showToast(if (ok) "${carSpec.name} ready!" else "Need ${carSpec.unlockCost} CR to unlock")
        }
    }

    fun setCarPaint(carId: String, bodyHex: Long, neonHex: Long) {
        viewModelScope.launch { repository.updateCarPaint(carId, bodyHex, neonHex) }
    }

    fun setCarWheel(carId: String, style: WheelStyle) {
        viewModelScope.launch {
            val ok = repository.updateCarWheel(carId, style)
            if (!ok) showToast("Need ${style.cost} CR for ${style.label}")
        }
    }

    fun setCarSpoiler(carId: String, style: SpoilerStyle) {
        viewModelScope.launch {
            val ok = repository.updateCarSpoiler(carId, style)
            if (!ok) showToast("Need ${style.cost} CR for ${style.label}")
        }
    }

    fun upgradeStat(carId: String, statKey: String) {
        viewModelScope.launch {
            val ok = repository.upgradeCarStat(carId, statKey)
            if (!ok) showToast("Insufficient credits or max level reached")
        }
    }

    fun setHeroStudioView(angle: HeroViewAngle) {
        _heroStudioView.value = angle
        glRenderer.dragYawOffsetDeg = 0f
        syncRendererAndAudio(appState.value)
    }

    fun setHeroStudioAnim(anim: HeroAnimation) {
        _heroStudioAnim.value = anim
        syncRendererAndAudio(appState.value)
    }

    fun selectOrUnlockOutfit(id: String) {
        viewModelScope.launch {
            if (!repository.selectOrUnlockHeroOutfit(id)) showToast("Insufficient credits")
        }
    }

    fun selectOrUnlockAccessory(id: String) {
        viewModelScope.launch {
            if (!repository.selectOrUnlockHeroAccessory(id)) showToast("Insufficient credits")
        }
    }

    fun selectOrUnlockAura(id: String) {
        viewModelScope.launch {
            if (!repository.selectOrUnlockHeroAura(id)) showToast("Insufficient credits")
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val reward = repository.claimDailyReward()
            showToast(if (reward > 0) "+$reward PTS & CR Claimed!" else "Already claimed today — come back tomorrow!")
        }
    }

    fun updateUsername(name: String) {
        viewModelScope.launch {
            repository.updateUsername(name)
            showToast("Driver tag updated")
        }
    }

    fun updateSettings(transform: (SettingsEntity) -> SettingsEntity) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        tiltController.stop()
        soundEngine.release()
    }
}

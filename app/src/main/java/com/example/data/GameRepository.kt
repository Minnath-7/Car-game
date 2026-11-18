package com.example.data

import com.example.model.AchievementSpec
import com.example.model.GameCatalog
import com.example.model.GameMode
import com.example.model.ScoringRules
import com.example.model.SpoilerStyle
import com.example.model.WheelStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

data class RaceCompletionOutcome(
    val pointsEarned: Int,
    val bonusPoints: Int,
    val totalScoreAfter: Int,
    val newlyUnlockedAchievements: List<AchievementSpec>,
    val duplicateBlocked: Boolean,
    val isNewBestTime: Boolean
)

class GameRepository(private val db: AppDatabase) {
    private val profileDao = db.playerProfileDao()
    private val carDao = db.carStateDao()
    private val achievementDao = db.achievementDao()
    private val historyDao = db.raceHistoryDao()
    private val settingsDao = db.settingsDao()

    val profileFlow: Flow<PlayerProfileEntity> = profileDao.observeProfile().map {
        it ?: PlayerProfileEntity()
    }

    val carsFlow: Flow<List<CarStateEntity>> = carDao.observeCars()
    val achievementsFlow: Flow<List<AchievementEntity>> = achievementDao.observeAchievements()
    val recentRacesFlow: Flow<List<RaceHistoryEntity>> = historyDao.observeRecentRaces()
    val bestTimesFlow: Flow<Map<String, Long>> = historyDao.observeBestTimesPerTrack().map { list ->
        list.associate { it.trackId to it.bestTimeMs }
    }
    val settingsFlow: Flow<SettingsEntity> = settingsDao.observeSettings().map {
        it ?: SettingsEntity()
    }

    suspend fun ensureInitialized() {
        if (profileDao.getProfileOnce() == null) {
            profileDao.upsertProfile(PlayerProfileEntity())
        }
        val initialCars = GameCatalog.cars.mapIndexed { idx, spec ->
            CarStateEntity(
                carId = spec.id,
                unlocked = idx == 0,
                paintColorHex = spec.defaultBodyColor,
                neonColorHex = spec.defaultNeonColor
            )
        }
        carDao.insertAllIgnore(initialCars)

        val initialAchievements = GameCatalog.achievements.map {
            AchievementEntity(id = it.id, unlocked = false, unlockedAtMs = 0L)
        }
        achievementDao.insertAllIgnore(initialAchievements)

        if (settingsDao.getSettingsOnce() == null) {
            settingsDao.upsertSettings(SettingsEntity())
        }
    }

    suspend fun updateUsername(newName: String) {
        val cleaned = newName.trim().take(20)
        if (cleaned.isEmpty()) return
        val current = profileDao.getProfileOnce() ?: PlayerProfileEntity()
        profileDao.upsertProfile(current.copy(username = cleaned))
    }

    suspend fun selectCar(carId: String) {
        val car = carDao.getCarOnce(carId) ?: return
        if (!car.unlocked) return
        val current = profileDao.getProfileOnce() ?: return
        profileDao.upsertProfile(current.copy(selectedCarId = carId))
    }

    suspend fun unlockCar(carId: String): Boolean {
        val spec = GameCatalog.cars.find { it.id == carId } ?: return false
        val car = carDao.getCarOnce(carId) ?: return false
        if (car.unlocked) return true
        val profile = profileDao.getProfileOnce() ?: return false
        if (profile.credits < spec.unlockCost) return false

        carDao.upsertCar(car.copy(unlocked = true))
        profileDao.upsertProfile(
            profile.copy(
                credits = profile.credits - spec.unlockCost,
                selectedCarId = carId
            )
        )
        return true
    }

    suspend fun updateCarPaint(carId: String, bodyColorHex: Long, neonColorHex: Long) {
        val car = carDao.getCarOnce(carId) ?: return
        carDao.upsertCar(car.copy(paintColorHex = bodyColorHex, neonColorHex = neonColorHex))
    }

    suspend fun updateCarWheel(carId: String, wheelStyle: WheelStyle): Boolean {
        val car = carDao.getCarOnce(carId) ?: return false
        val profile = profileDao.getProfileOnce() ?: return false
        if (car.wheelStyleId != wheelStyle.id && wheelStyle.cost > 0) {
            if (profile.credits < wheelStyle.cost) return false
            profileDao.upsertProfile(profile.copy(credits = profile.credits - wheelStyle.cost))
        }
        carDao.upsertCar(car.copy(wheelStyleId = wheelStyle.id))
        return true
    }

    suspend fun updateCarSpoiler(carId: String, spoilerStyle: SpoilerStyle): Boolean {
        val car = carDao.getCarOnce(carId) ?: return false
        val profile = profileDao.getProfileOnce() ?: return false
        if (car.spoilerStyleId != spoilerStyle.id && spoilerStyle.cost > 0) {
            if (profile.credits < spoilerStyle.cost) return false
            profileDao.upsertProfile(profile.copy(credits = profile.credits - spoilerStyle.cost))
        }
        carDao.upsertCar(car.copy(spoilerStyleId = spoilerStyle.id))
        return true
    }

    suspend fun upgradeCarStat(carId: String, statKey: String): Boolean {
        val car = carDao.getCarOnce(carId) ?: return false
        val profile = profileDao.getProfileOnce() ?: return false
        val currentLvl = when (statKey) {
            "speed" -> car.speedLevel
            "accel" -> car.accelLevel
            "handling" -> car.handlingLevel
            "braking" -> car.brakingLevel
            "nitro" -> car.nitroLevel
            else -> return false
        }
        if (currentLvl >= 5) return false
        val cost = (currentLvl + 1) * 100
        if (profile.credits < cost) return false

        val updatedCar = when (statKey) {
            "speed" -> car.copy(speedLevel = currentLvl + 1)
            "accel" -> car.copy(accelLevel = currentLvl + 1)
            "handling" -> car.copy(handlingLevel = currentLvl + 1)
            "braking" -> car.copy(brakingLevel = currentLvl + 1)
            "nitro" -> car.copy(nitroLevel = currentLvl + 1)
            else -> car
        }
        carDao.upsertCar(updatedCar)
        profileDao.upsertProfile(profile.copy(credits = profile.credits - cost))
        return true
    }

    suspend fun selectOrUnlockHeroOutfit(outfitId: String): Boolean {
        val spec = GameCatalog.outfits.find { it.id == outfitId } ?: return false
        val profile = profileDao.getProfileOnce() ?: return false
        val unlocked = profile.unlockedOutfitsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (!unlocked.contains(outfitId)) {
            if (profile.credits < spec.cost) return false
            unlocked.add(outfitId)
            profileDao.upsertProfile(
                profile.copy(
                    credits = profile.credits - spec.cost,
                    unlockedOutfitsCsv = unlocked.joinToString(","),
                    selectedOutfitId = outfitId
                )
            )
        } else {
            profileDao.upsertProfile(profile.copy(selectedOutfitId = outfitId))
        }
        return true
    }

    suspend fun selectOrUnlockHeroAccessory(accessoryId: String): Boolean {
        val spec = GameCatalog.accessories.find { it.id == accessoryId } ?: return false
        val profile = profileDao.getProfileOnce() ?: return false
        val unlocked = profile.unlockedAccessoriesCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (!unlocked.contains(accessoryId)) {
            if (profile.credits < spec.cost) return false
            unlocked.add(accessoryId)
            profileDao.upsertProfile(
                profile.copy(
                    credits = profile.credits - spec.cost,
                    unlockedAccessoriesCsv = unlocked.joinToString(","),
                    selectedAccessoryId = accessoryId
                )
            )
        } else {
            profileDao.upsertProfile(profile.copy(selectedAccessoryId = accessoryId))
        }
        return true
    }

    suspend fun selectOrUnlockHeroAura(auraId: String): Boolean {
        val spec = GameCatalog.auras.find { it.id == auraId } ?: return false
        val profile = profileDao.getProfileOnce() ?: return false
        val unlocked = profile.unlockedAurasCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (!unlocked.contains(auraId)) {
            if (profile.credits < spec.cost) return false
            unlocked.add(auraId)
            profileDao.upsertProfile(
                profile.copy(
                    credits = profile.credits - spec.cost,
                    unlockedAurasCsv = unlocked.joinToString(","),
                    selectedAuraId = auraId
                )
            )
        } else {
            profileDao.upsertProfile(profile.copy(selectedAuraId = auraId))
        }
        return true
    }

    suspend fun claimDailyReward(): Int {
        val profile = profileDao.getProfileOnce() ?: return 0
        val todayEpoch = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        if (profile.lastDailyClaimEpochDay >= todayEpoch) return 0

        val nextStreak = if (profile.lastDailyClaimEpochDay == todayEpoch - 1L) {
            (profile.dailyStreak % 7) + 1
        } else {
            1
        }
        val reward = 100 + (nextStreak - 1) * 50
        profileDao.upsertProfile(
            profile.copy(
                totalPoints = profile.totalPoints + reward,
                credits = profile.credits + reward,
                lastDailyClaimEpochDay = todayEpoch,
                dailyStreak = nextStreak
            )
        )
        return reward
    }

    suspend fun recordCompletedRace(
        raceSessionId: String,
        mode: GameMode,
        trackId: String,
        carId: String,
        position: Int,
        raceTimeMs: Long,
        careerStagePlayed: Int?,
        wonChampionshipOverall: Boolean
    ): RaceCompletionOutcome {
        // Strict anti-duplicate reward guard keyed by unique raceSessionId
        val existing = historyDao.getBySessionId(raceSessionId)
        val profile = profileDao.getProfileOnce() ?: PlayerProfileEntity()
        if (existing != null) {
            return RaceCompletionOutcome(
                pointsEarned = existing.pointsEarned,
                bonusPoints = 0,
                totalScoreAfter = profile.totalPoints,
                newlyUnlockedAchievements = emptyList(),
                duplicateBlocked = true,
                isNewBestTime = false
            )
        }

        val basePoints = if (mode == GameMode.TIME_TRIAL) {
            val targetMs = (GameCatalog.tracks.find { it.id == trackId }?.targetTimeSec ?: 105) * 1000L
            if (raceTimeMs <= targetMs) 200 else 100
        } else {
            ScoringRules.pointsForPosition(position)
        }

        var bonusPoints = 0
        var newCareerStageUnlocked = profile.careerStageUnlocked
        if (mode == GameMode.CAREER && careerStagePlayed != null) {
            val stageSpec = GameCatalog.careerStages.find { it.stage == careerStagePlayed }
            if (stageSpec != null && position <= stageSpec.requiredPosition) {
                bonusPoints += stageSpec.bonusPoints
                if (careerStagePlayed >= profile.careerStageUnlocked && careerStagePlayed < GameCatalog.careerStages.size) {
                    newCareerStageUnlocked = careerStagePlayed + 1
                }
            }
        }
        if (wonChampionshipOverall) {
            bonusPoints += 300
        }

        val prevBestTrack = historyDao.getBestTimeForTrack(trackId)
        val isNewBestTime = raceTimeMs > 0L && (prevBestTrack == null || raceTimeMs < prevBestTrack)

        historyDao.insertRaceIgnore(
            RaceHistoryEntity(
                raceSessionId = raceSessionId,
                modeId = mode.id,
                trackId = trackId,
                carId = carId,
                position = position,
                raceTimeMs = raceTimeMs,
                pointsEarned = basePoints + bonusPoints,
                timestampMs = System.currentTimeMillis()
            )
        )

        val newTotalRaces = profile.totalRaces + 1
        val isWin = position == 1
        val newConsecutiveWins = if (isWin) profile.consecutiveWins + 1 else 0
        val newFirsts = profile.firstPlaces + if (position == 1) 1 else 0
        val newSeconds = profile.secondPlaces + if (position == 2) 1 else 0
        val newThirds = profile.thirdPlaces + if (position == 3) 1 else 0
        val newBestOverall = if (raceTimeMs > 0 && (profile.bestRaceTimeMs == 0L || raceTimeMs < profile.bestRaceTimeMs)) {
            raceTimeMs
        } else {
            profile.bestRaceTimeMs
        }

        // Check achievements
        val currentAch = achievementDao.getAchievementsOnce().associateBy { it.id }
        val newlyUnlocked = mutableListOf<AchievementSpec>()
        var achievementBonus = 0
        val now = System.currentTimeMillis()

        suspend fun checkAndUnlock(id: String, condition: Boolean) {
            if (condition && currentAch[id]?.unlocked != true) {
                val spec = GameCatalog.achievements.find { it.id == id } ?: return
                achievementDao.upsertAchievement(AchievementEntity(id = id, unlocked = true, unlockedAtMs = now))
                newlyUnlocked.add(spec)
                achievementBonus += spec.rewardPoints
            }
        }

        checkAndUnlock("first_race", newTotalRaces >= 1)
        checkAndUnlock("three_streak", newConsecutiveWins >= 3)
        checkAndUnlock("five_races", newTotalRaces >= 5)
        checkAndUnlock("ten_races", newTotalRaces >= 10)
        checkAndUnlock("championship_winner", wonChampionshipOverall)

        val totalAdded = basePoints + bonusPoints + achievementBonus
        val updatedProfile = profile.copy(
            totalPoints = profile.totalPoints + totalAdded,
            credits = profile.credits + totalAdded,
            totalRaces = newTotalRaces,
            firstPlaces = newFirsts,
            secondPlaces = newSeconds,
            thirdPlaces = newThirds,
            totalVictories = newFirsts,
            consecutiveWins = newConsecutiveWins,
            bestRaceTimeMs = newBestOverall,
            careerStageUnlocked = newCareerStageUnlocked,
            championshipWins = profile.championshipWins + if (wonChampionshipOverall) 1 else 0
        )
        profileDao.upsertProfile(updatedProfile)

        return RaceCompletionOutcome(
            pointsEarned = basePoints,
            bonusPoints = bonusPoints + achievementBonus,
            totalScoreAfter = updatedProfile.totalPoints,
            newlyUnlockedAchievements = newlyUnlocked,
            duplicateBlocked = false,
            isNewBestTime = isNewBestTime
        )
    }

    suspend fun updateSettings(transform: (SettingsEntity) -> SettingsEntity) {
        val current = settingsDao.getSettingsOnce() ?: SettingsEntity()
        settingsDao.upsertSettings(transform(current))
    }
}

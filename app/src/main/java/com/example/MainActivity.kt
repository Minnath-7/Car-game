package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.GameMode
import com.example.ui.Screen
import com.example.ui.ShadowRacerViewModel
import com.example.ui.screens.CarSelectScreen
import com.example.ui.screens.CareerLadderScreen
import com.example.ui.screens.DailyRewardsScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.HeroProfileScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.ModeSelectScreen
import com.example.ui.screens.RaceResultsScreen
import com.example.ui.screens.RaceScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TrackSelectScreen
import com.example.ui.theme.ShadowRacerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ShadowRacerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ShadowRacerTheme {
                val screen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val appState by viewModel.appState.collectAsStateWithLifecycle()
                val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
                val selectedTrack by viewModel.selectedTrack.collectAsStateWithLifecycle()
                val hud by viewModel.hudSnapshot.collectAsStateWithLifecycle()
                val outcome by viewModel.raceOutcome.collectAsStateWithLifecycle()
                val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
                val championship by viewModel.championship.collectAsStateWithLifecycle()
                val focusCarId by viewModel.garageFocusCarId.collectAsStateWithLifecycle()
                val heroView by viewModel.heroStudioView.collectAsStateWithLifecycle()
                val heroAnim by viewModel.heroStudioAnim.collectAsStateWithLifecycle()
                val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(screen) {
                    val isLandscapeScreen = screen == Screen.Race || screen == Screen.Results
                    requestedOrientation = if (isLandscapeScreen) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    if (screen == Screen.Race) {
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        controller.show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                LaunchedEffect(toastMsg) {
                    val msg = toastMsg
                    if (msg != null) {
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearToast()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (screen) {
                        Screen.Splash -> SplashScreen(
                            onFinished = { viewModel.navigateTo(Screen.Home) }
                        )

                        Screen.Home -> HomeScreen(
                            state = appState,
                            renderer = viewModel.glRenderer,
                            onNavigate = { target -> viewModel.navigateTo(target) }
                        )

                        Screen.ModeSelect -> ModeSelectScreen(
                            state = appState,
                            onSelectMode = { mode -> viewModel.startModeFlow(mode) },
                            onBack = { viewModel.navigateTo(Screen.Home) }
                        )

                        Screen.CareerLadder -> CareerLadderScreen(
                            state = appState,
                            onSelectStage = { stage -> viewModel.selectCareerStage(stage) },
                            onBack = { viewModel.navigateTo(Screen.Home) }
                        )

                        Screen.TrackSelect -> TrackSelectScreen(
                            state = appState,
                            selectedTrack = selectedTrack,
                            onSelectTrack = { track -> viewModel.selectTrack(track) },
                            onBack = { viewModel.navigateTo(Screen.ModeSelect) }
                        )

                        Screen.CarSelect -> CarSelectScreen(
                            state = appState,
                            track = selectedTrack,
                            onSelectCar = { carId -> viewModel.focusGarageCar(carId) },
                            onOpenGarage = { viewModel.navigateTo(Screen.Garage) },
                            onStartRace = { viewModel.launchRace() },
                            onBack = {
                                val backTarget = when (selectedMode) {
                                    GameMode.CAREER -> Screen.CareerLadder
                                    GameMode.CHAMPIONSHIP -> Screen.ModeSelect
                                    else -> Screen.TrackSelect
                                }
                                viewModel.navigateTo(backTarget)
                            }
                        )

                        Screen.Garage -> GarageScreen(
                            state = appState,
                            focusCarId = focusCarId,
                            renderer = viewModel.glRenderer,
                            onFocusCar = { id -> viewModel.focusGarageCar(id) },
                            onUnlockCar = { spec -> viewModel.unlockOrSelectCar(spec) },
                            onPaintChange = { id, body, neon -> viewModel.setCarPaint(id, body, neon) },
                            onWheelChange = { id, ws -> viewModel.setCarWheel(id, ws) },
                            onSpoilerChange = { id, sp -> viewModel.setCarSpoiler(id, sp) },
                            onUpgradeStat = { id, key -> viewModel.upgradeStat(id, key) },
                            onBack = { viewModel.navigateTo(Screen.Home) }
                        )

                        Screen.HeroProfile -> HeroProfileScreen(
                            state = appState,
                            renderer = viewModel.glRenderer,
                            currentView = heroView,
                            currentAnim = heroAnim,
                            onSelectView = { v -> viewModel.setHeroStudioView(v) },
                            onSelectAnim = { a -> viewModel.setHeroStudioAnim(a) },
                            onSelectOutfit = { id -> viewModel.selectOrUnlockOutfit(id) },
                            onSelectAccessory = { id -> viewModel.selectOrUnlockAccessory(id) },
                            onSelectAura = { id -> viewModel.selectOrUnlockAura(id) },
                            onUpdateUsername = { name -> viewModel.updateUsername(name) },
                            onBack = { viewModel.navigateTo(Screen.Home) }
                        )

                        Screen.DailyRewards -> DailyRewardsScreen(
                            state = appState,
                            onClaim = { viewModel.claimDailyReward() },
                            onBack = { viewModel.navigateTo(Screen.Home) }
                        )

                        Screen.Leaderboard -> LeaderboardScreen(
                            state = appState,
                            onBack = { viewModel.navigateTo(Screen.Home) }
                        )

                        Screen.Settings -> SettingsScreen(
                            state = appState,
                            onUpdateSettings = { t -> viewModel.updateSettings(t) },
                            onBack = { viewModel.navigateTo(Screen.Home) }
                        )

                        Screen.Race -> RaceScreen(
                            state = appState,
                            hud = hud,
                            isPaused = isPaused,
                            renderer = viewModel.glRenderer,
                            onControlsChange = { a, b, l, r, n, d ->
                                viewModel.updateRaceControls(a, b, l, r, n, d)
                            },
                            onRefreshTilt = { viewModel.refreshTiltSample() },
                            onCycleCamera = { viewModel.cycleCameraPerspective() },
                            onPauseToggle = { p -> viewModel.togglePause(p) },
                            onRestart = { viewModel.launchRace() },
                            onQuitToMenu = { viewModel.navigateTo(Screen.Home) }
                        )

                        Screen.Results -> RaceResultsScreen(
                            state = appState,
                            hud = hud,
                            outcome = outcome,
                            championship = championship,
                            renderer = viewModel.glRenderer,
                            onRetry = { viewModel.launchRace() },
                            onNextRace = { viewModel.advanceNextRaceOrStage() },
                            onHome = { viewModel.navigateTo(Screen.Home) },
                            onGarage = { viewModel.navigateTo(Screen.Garage) }
                        )
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.soundEngine.pause()
        viewModel.tiltController.stop()
        if (viewModel.currentScreen.value == Screen.Race) {
            viewModel.togglePause(true)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.soundEngine.resume()
    }
}

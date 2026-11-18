package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.RaceCompletionOutcome
import com.example.engine3d.Game3DViewport
import com.example.engine3d.GameGLRenderer
import com.example.engine3d.RaceHudSnapshot
import com.example.model.GameMode
import com.example.model.ScoringRules
import com.example.model.SteeringMode
import com.example.ui.ChampionshipSession
import com.example.ui.UiAppState
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.CrimsonBoost
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassPanel
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeonCobalt
import com.example.ui.theme.SilverAccent
import com.example.ui.theme.SolarGold
import kotlinx.coroutines.delay

@Composable
fun RaceScreen(
    state: UiAppState,
    hud: RaceHudSnapshot,
    isPaused: Boolean,
    renderer: GameGLRenderer,
    onControlsChange: (accel: Boolean?, brake: Boolean?, left: Boolean?, right: Boolean?, nitro: Boolean?, drift: Boolean?) -> Unit,
    onRefreshTilt: () -> Unit,
    onCycleCamera: () -> Unit,
    onPauseToggle: (Boolean) -> Unit,
    onRestart: () -> Unit,
    onQuitToMenu: () -> Unit
) {
    BackHandler {
        onPauseToggle(!isPaused)
    }

    val isTiltMode = state.settings.steeringModeId == SteeringMode.TILT.id
    LaunchedEffect(isTiltMode, isPaused) {
        while (isTiltMode && !isPaused) {
            onRefreshTilt()
            delay(32)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        // Fullscreen 3D OpenGL ES Racing View
        Game3DViewport(
            renderer = renderer,
            modifier = Modifier.fillMaxSize(),
            allowTouchOrbit = false
        )

        // Subtle Motion/Nitro Edge Vignette when Nitro Active
        if (hud.isNitroActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 4.dp,
                        brush = Brush.radialGradient(listOf(Color.Transparent, CyberCyan.copy(alpha = 0.55f))),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
            )
        }

        // Clean Non-Obstructive Racing HUD
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // TOP LEFT: Lap, Total Laps, Time, Checkpoints, Mini-Map
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeonPanel(borderColor = BorderGlow) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Text(state.labels.lap, style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                                Text("${hud.currentLap}/${hud.totalLaps}", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                            }
                            Column {
                                val timeLabel = if (hud.mode == GameMode.TIME_TRIAL) "TIME LEFT" else "RACE TIME"
                                val shownMs = if (hud.mode == GameMode.TIME_TRIAL) hud.timeTrialRemainingMs else hud.raceTimeMs
                                Text(timeLabel, style = MaterialTheme.typography.labelSmall, color = SilverAccent)
                                Text(
                                    text = formatRaceTime(shownMs),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (hud.mode == GameMode.TIME_TRIAL && shownMs < 15000L) CrimsonBoost else Color.White
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        // Checkpoint progress indicators (CP 1..3)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("CP", style = MaterialTheme.typography.labelSmall, color = SilverAccent)
                            val reached = when (hud.checkpointIndex) {
                                1 -> 0
                                2 -> 1
                                3 -> 2
                                else -> 3
                            }
                            for (c in 1..3) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 18.dp, height = 5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (c <= reached) EmeraldSuccess else Color(0xFF28344E))
                                )
                            }
                        }
                    }
                }

                // Mini-Map of Track + Racers
                if (hud.miniMapOutline.isNotEmpty()) {
                    NeonPanel(
                        modifier = Modifier.size(96.dp),
                        borderColor = ElectricBlue.copy(alpha = 0.45f)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            val path = Path()
                            hud.miniMapOutline.forEachIndexed { idx, (nx, nz) ->
                                val px = nx * size.width
                                val py = nz * size.height
                                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }
                            path.close()
                            drawPath(path, color = SilverAccent.copy(alpha = 0.55f), style = Stroke(width = 4f, cap = StrokeCap.Round))
                            hud.miniMapDots.forEach { dot ->
                                drawCircle(
                                    color = if (dot.isPlayer) CyberCyan else CrimsonBoost,
                                    radius = if (dot.isPlayer) 6f else 4f,
                                    center = Offset(dot.normX * size.width, dot.normZ * size.height)
                                )
                            }
                        }
                    }
                }
            }

            // TOP CENTER: Live Racing Position (e.g. 1/6) + Nitro Meter
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NeonPanel(borderColor = CyberCyan.copy(alpha = 0.7f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("POS", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                            Text(
                                text = "${hud.position}/${hud.totalRacers}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = if (hud.position == 1) SolarGold else Color.White
                            )
                        }
                        // Nitro Energy Meter
                        Column(modifier = Modifier.width(120.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("NITRO", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                                Text("${hud.nitroPercent.toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1A2540))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((hud.nitroPercent / 100f).coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(NeonCobalt, CyberCyan)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // TOP RIGHT: Speedometer KM/H + Camera Toggle + Pause
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeonPanel(borderColor = BorderGlow) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${hud.speedKmh}",
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 26.sp),
                            color = if (hud.isNitroActive) CyberCyan else Color.White
                        )
                        Text(state.labels.speedUnit, style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                    }
                }
                IconButton(
                    onClick = onCycleCamera,
                    modifier = Modifier
                        .testTag("race_camera_button")
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassPanel)
                        .border(1.dp, BorderGlow, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.Cameraswitch, contentDescription = "Switch Camera", tint = CyberCyan)
                }
                IconButton(
                    onClick = { onPauseToggle(true) },
                    modifier = Modifier
                        .testTag("race_pause_button")
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassPanel)
                        .border(1.dp, BorderGlow, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = "Pause Race", tint = Color.White)
                }
            }

            // BOTTOM LEFT: Touch Steering Controls (or Tilt Steering indicator)
            if (isTiltMode) {
                NeonPanel(
                    modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp),
                    borderColor = CyberCyan.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "TILT STEERING ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.align(Alignment.BottomStart),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HoldControlPad(
                        label = "",
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        accent = ElectricBlue,
                        modifier = Modifier.size(width = 86.dp, height = 74.dp).testTag("steer_left_btn"),
                        onPressedChange = { pressed -> onControlsChange(null, null, pressed, null, null, null) }
                    )
                    HoldControlPad(
                        label = "",
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        accent = ElectricBlue,
                        modifier = Modifier.size(width = 86.dp, height = 74.dp).testTag("steer_right_btn"),
                        onPressedChange = { pressed -> onControlsChange(null, null, null, pressed, null, null) }
                    )
                }
            }

            // BOTTOM RIGHT: Drift, Nitro, Brake, Accelerator
            Row(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HoldControlPad(
                        label = "NITRO",
                        icon = Icons.Filled.Bolt,
                        accent = CyberCyan,
                        modifier = Modifier.size(width = 76.dp, height = 56.dp).testTag("nitro_btn"),
                        onPressedChange = { pressed -> onControlsChange(null, null, null, null, pressed, null) }
                    )
                    HoldControlPad(
                        label = "DRIFT",
                        icon = null,
                        accent = SolarGold,
                        modifier = Modifier.size(width = 76.dp, height = 56.dp).testTag("drift_btn"),
                        onPressedChange = { pressed -> onControlsChange(null, null, null, null, null, pressed) }
                    )
                }
                HoldControlPad(
                    label = "BRAKE",
                    icon = null,
                    accent = CrimsonBoost,
                    modifier = Modifier.size(width = 76.dp, height = 72.dp).testTag("brake_btn"),
                    onPressedChange = { pressed -> onControlsChange(null, pressed, null, null, null, null) }
                )
                HoldControlPad(
                    label = if (state.settings.autoAccelerate) "AUTO" else "GAS",
                    icon = null,
                    accent = EmeraldSuccess,
                    modifier = Modifier.size(width = 92.dp, height = 98.dp).testTag("gas_btn"),
                    onPressedChange = { pressed -> onControlsChange(pressed, null, null, null, null, null) }
                )
            }
        }

        // Countdown Overlay: 3, 2, 1, GO!
        if (hud.countdownValue >= 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val cdText = if (hud.countdownValue == 0) "GO!" else "${hud.countdownValue}"
                val cdColor = if (hud.countdownValue == 0) EmeraldSuccess else CyberCyan
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(DeepObsidian.copy(alpha = 0.72f))
                        .border(2.dp, cdColor, RoundedCornerShape(24.dp))
                        .padding(horizontal = 42.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = cdText,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 68.sp),
                        color = cdColor
                    )
                }
            }
        }

        // Pause Overlay
        AnimatedVisibility(
            visible = isPaused,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepObsidian.copy(alpha = 0.84f)),
                contentAlignment = Alignment.Center
            ) {
                NeonPanel(
                    modifier = Modifier.width(340.dp),
                    borderColor = CyberCyan
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(state.labels.paused, style = MaterialTheme.typography.headlineLarge, color = Color.White)
                        PrimaryNeonButton(
                            text = state.labels.resume,
                            icon = Icons.Filled.PlayArrow,
                            testTag = "resume_race_button",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onPauseToggle(false) }
                        )
                        SecondaryMenuButton(
                            text = "SWITCH CAMERA",
                            icon = Icons.Filled.Cameraswitch,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onCycleCamera
                        )
                        SecondaryMenuButton(
                            text = "RESTART RACE",
                            icon = Icons.Filled.Refresh,
                            accentColor = SolarGold,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onRestart
                        )
                        SecondaryMenuButton(
                            text = "EXIT TO MENU",
                            icon = Icons.Filled.Home,
                            accentColor = CrimsonBoost,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onQuitToMenu
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldControlPad(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    accent: Color,
    modifier: Modifier = Modifier,
    onPressedChange: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (pressed) accent.copy(alpha = 0.42f) else GlassPanel.copy(alpha = 0.68f))
            .border(if (pressed) 2.dp else 1.2.dp, accent.copy(alpha = if (pressed) 1f else 0.7f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onPressedChange(true)
                    waitForUpOrCancellation()
                    pressed = false
                    onPressedChange(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(icon, contentDescription = label.ifEmpty { "Control" }, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            if (label.isNotEmpty()) {
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
        }
    }
}

@Composable
fun RaceResultsScreen(
    state: UiAppState,
    hud: RaceHudSnapshot,
    outcome: RaceCompletionOutcome?,
    championship: ChampionshipSession?,
    renderer: GameGLRenderer,
    onRetry: () -> Unit,
    onNextRace: () -> Unit,
    onHome: () -> Unit,
    onGarage: () -> Unit
) {
    BackHandler(onBack = onHome)

    val pos = hud.position
    val headline = if (hud.timeTrialFailed) {
        "TIME EXPIRED – TRY AGAIN"
    } else if (hud.mode == GameMode.TIME_TRIAL) {
        "TIME TRIAL COMPLETE • ${outcome?.pointsEarned ?: 0} PTS"
    } else {
        ScoringRules.rankHeadline(pos)
    }
    val bannerColor = when {
        hud.timeTrialFailed -> CrimsonBoost
        pos == 1 -> SolarGold
        pos <= 3 -> CyberCyan
        else -> SilverAccent
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        // Live 3D Podium with Hero Kael Celebrating beside the Player's Car!
        Game3DViewport(
            renderer = renderer,
            modifier = Modifier.fillMaxSize(),
            allowTouchOrbit = true
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DeepObsidian.copy(alpha = 0.72f),
                            DeepObsidian.copy(alpha = 0.25f),
                            DeepObsidian.copy(alpha = 0.90f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Result Banner + Hero Portrait Badge
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(bannerColor.copy(alpha = 0.18f))
                        .border(1.5.dp, bannerColor, RoundedCornerShape(14.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.headlineLarge,
                        color = bannerColor
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${hud.trackName.uppercase()} • ${hud.currentLap}/${hud.totalLaps} LAPS COMPLETED",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }

            // Middle-Bottom Telemetry & Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pos <= 3 && !hud.timeTrialFailed) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_victory),
                            contentDescription = "Hero Victory Celebration",
                            modifier = Modifier
                                .size(width = 86.dp, height = 114.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, SolarGold, RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    NeonPanel(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("FINAL POSITION", style = MaterialTheme.typography.bodyMedium, color = SilverAccent)
                                Text("$pos / ${hud.totalRacers}", style = MaterialTheme.typography.titleMedium, color = bannerColor)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL TIME", style = MaterialTheme.typography.bodyMedium, color = SilverAccent)
                                Text(formatRaceTime(hud.raceTimeMs), style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("POINTS EARNED", style = MaterialTheme.typography.bodyMedium, color = SilverAccent)
                                val earned = (outcome?.pointsEarned ?: 0) + (outcome?.bonusPoints ?: 0)
                                Text("+$earned PTS", style = MaterialTheme.typography.titleMedium, color = EmeraldSuccess)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL ACCUMULATED", style = MaterialTheme.typography.bodyMedium, color = SilverAccent)
                                Text("${state.profile.totalPoints} PTS", style = MaterialTheme.typography.titleMedium, color = SolarGold)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOP SPEED / DRIFT", style = MaterialTheme.typography.bodyMedium, color = SilverAccent)
                                Text("${hud.topSpeedKmh} KM/H • ${hud.driftScore} DRIFT", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                            }
                        }
                    }
                }

                // Newly unlocked achievements
                if (outcome != null && outcome.newlyUnlockedAchievements.isNotEmpty()) {
                    NeonPanel(borderColor = SolarGold, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = SolarGold)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "ACHIEVEMENT UNLOCKED: ${outcome.newlyUnlockedAchievements.joinToString { it.title }}",
                                style = MaterialTheme.typography.titleMedium,
                                color = SolarGold
                            )
                        }
                    }
                }

                // Championship Standings Summary when playing Championship Mode
                if (hud.mode == GameMode.CHAMPIONSHIP && championship != null) {
                    NeonPanel(borderColor = ElectricBlue, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = if (championship.finished) "FINAL CHAMPIONSHIP STANDINGS" else "CHAMPIONSHIP RACE ${championship.raceIndex + 1}/4 STANDINGS",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberCyan
                            )
                            Spacer(Modifier.height(6.dp))
                            val sorted = championship.standings.entries.sortedByDescending { it.value }.take(4)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                sorted.forEachIndexed { idx, entry ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MidnightSurface)
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("#${idx + 1} ${entry.key}", style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
                                        Text("${entry.value} PTS", style = MaterialTheme.typography.labelSmall, color = SolarGold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4 Required Action Buttons: RETRY, NEXT RACE, HOME, GARAGE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryMenuButton(
                        text = state.labels.retry,
                        icon = Icons.Filled.Refresh,
                        testTag = "results_retry_button",
                        modifier = Modifier.weight(1f),
                        onClick = onRetry
                    )
                    PrimaryNeonButton(
                        text = state.labels.nextRace,
                        icon = Icons.Filled.SkipNext,
                        testTag = "results_next_button",
                        modifier = Modifier.weight(1.2f),
                        onClick = onNextRace
                    )
                    SecondaryMenuButton(
                        text = state.labels.home,
                        icon = Icons.Filled.Home,
                        testTag = "results_home_button",
                        accentColor = SilverAccent,
                        modifier = Modifier.weight(1f),
                        onClick = onHome
                    )
                    SecondaryMenuButton(
                        text = state.labels.garage,
                        icon = Icons.Filled.DirectionsCar,
                        testTag = "results_garage_button",
                        accentColor = SolarGold,
                        modifier = Modifier.weight(1f),
                        onClick = onGarage
                    )
                }
            }
        }
    }
}

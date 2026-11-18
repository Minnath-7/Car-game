package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.GameCatalog
import com.example.model.GameMode
import com.example.model.TrackSpec
import com.example.ui.UiAppState
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.CrimsonBoost
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.NeonCobalt
import com.example.ui.theme.SilverAccent
import com.example.ui.theme.SolarGold

@Composable
fun ModeSelectScreen(
    state: UiAppState,
    onSelectMode: (GameMode) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepObsidian, MidnightSurface)))
            .navigationBarsPadding()
    ) {
        TopHeaderBar(
            title = state.labels.selectMode,
            onBack = onBack,
            credits = state.profile.credits,
            totalPoints = state.profile.totalPoints
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(GameMode.entries) { mode ->
                val (icon, accent) = when (mode) {
                    GameMode.QUICK_RACE -> Icons.Filled.Speed to ElectricBlue
                    GameMode.CAREER -> Icons.Filled.EmojiEvents to SolarGold
                    GameMode.TIME_TRIAL -> Icons.Filled.Timer to EmeraldSuccess
                    GameMode.CHAMPIONSHIP -> Icons.Filled.MilitaryTech to CrimsonBoost
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mode_card_${mode.id}"),
                    shape = RoundedCornerShape(18.dp),
                    color = ElevatedCard,
                    border = BorderStroke(1.2.dp, accent.copy(alpha = 0.55f)),
                    onClick = { onSelectMode(mode) }
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accent.copy(alpha = 0.16f))
                                .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = mode.title, tint = accent, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text(mode.subtitle, style = MaterialTheme.typography.bodyMedium, color = SilverAccent)
                        }
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Select ${mode.title}",
                            tint = accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CareerLadderScreen(
    state: UiAppState,
    onSelectStage: (Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val unlockedStage = state.profile.careerStageUnlocked

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepObsidian, MidnightSurface)))
            .navigationBarsPadding()
    ) {
        TopHeaderBar(
            title = "CAREER MODE",
            onBack = onBack,
            credits = state.profile.credits,
            totalPoints = state.profile.totalPoints
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(GameCatalog.careerStages) { stage ->
                val isUnlocked = stage.stage <= unlockedStage
                val isCleared = stage.stage < unlockedStage
                val track = GameCatalog.tracks.find { it.id == stage.trackId } ?: GameCatalog.tracks[0]
                val borderCol = when {
                    isCleared -> EmeraldSuccess.copy(alpha = 0.6f)
                    isUnlocked -> SolarGold.copy(alpha = 0.7f)
                    else -> Color(0xFF28324B)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("career_stage_${stage.stage}"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isUnlocked) ElevatedCard else MidnightSurface.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, borderCol),
                    enabled = isUnlocked,
                    onClick = { onSelectStage(stage.stage) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCleared) EmeraldSuccess.copy(alpha = 0.2f)
                                    else if (isUnlocked) SolarGold.copy(alpha = 0.2f)
                                    else Color(0xFF1A2236)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isUnlocked) {
                                Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = MutedSteel)
                            } else if (isCleared) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Cleared", tint = EmeraldSuccess)
                            } else {
                                Text("${stage.stage}", style = MaterialTheme.typography.headlineLarge, color = SolarGold)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STAGE ${stage.stage}: ${stage.title.uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isUnlocked) Color.White else MutedSteel
                            )
                            Text(
                                text = "${track.name} • Top ${stage.requiredPosition} Finish • +${stage.bonusPoints} Bonus",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUnlocked) ElectricBlue else MutedSteel
                            )
                            Text(
                                text = stage.unlockDescription,
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverAccent.copy(alpha = if (isUnlocked) 0.85f else 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackSelectScreen(
    state: UiAppState,
    selectedTrack: TrackSpec,
    onSelectTrack: (TrackSpec) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepObsidian, MidnightSurface)))
            .navigationBarsPadding()
    ) {
        TopHeaderBar(
            title = state.labels.selectTrack,
            onBack = onBack,
            credits = state.profile.credits,
            totalPoints = state.profile.totalPoints
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(GameCatalog.tracks) { track ->
                val accent = Color(track.accentHex)
                val skyTop = Color(track.skyTopColor)
                val skyBot = Color(track.skyHorizonColor)
                val bestMs = state.bestTimes[track.id] ?: 0L
                val isSelected = track.id == selectedTrack.id

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("track_card_${track.id}"),
                    shape = RoundedCornerShape(18.dp),
                    color = ElevatedCard,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) CyberCyan else accent.copy(alpha = 0.5f)
                    ),
                    onClick = { onSelectTrack(track) }
                ) {
                    Row(
                        modifier = Modifier
                            .background(Brush.horizontalGradient(listOf(skyTop.copy(alpha = 0.65f), ElevatedCard)))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Track Minimap & Environment Swatch
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.verticalGradient(listOf(skyTop, skyBot.copy(alpha = 0.7f))))
                                .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            TrackOutlineCanvas(track = track, lineColor = accent, modifier = Modifier.size(66.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TRACK ${track.index + 1}: ${track.name.uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = track.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = accent
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "BEST: ${formatRaceTime(bestMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarGold
                                )
                                Text(
                                    text = "TARGET: ${track.targetTimeSec}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SilverAccent
                                )
                            }
                        }
                        Icon(Icons.Filled.Flag, contentDescription = "Race ${track.name}", tint = accent)
                    }
                }
            }
        }
    }
}

@Composable
fun TrackOutlineCanvas(track: TrackSpec, lineColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val pts = track.controlPoints
        if (pts.isEmpty()) return@Canvas
        val minX = pts.minOf { it.x }
        val maxX = pts.maxOf { it.x }
        val minZ = pts.minOf { it.z }
        val maxZ = pts.maxOf { it.z }
        val span = maxOf(maxX - minX, maxZ - minZ).coerceAtLeast(1f)
        val cx = (minX + maxX) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        val pad = size.minDimension * 0.14f
        val scale = size.minDimension - pad * 2f

        val path = Path()
        pts.forEachIndexed { idx, p ->
            val sx = size.width * 0.5f + ((p.x - cx) / span) * scale
            val sy = size.height * 0.5f + ((p.z - cz) / span) * scale
            if (idx == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
        }
        path.close()
        drawPath(path, color = lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
        val startX = size.width * 0.5f + ((pts[0].x - cx) / span) * scale
        val startY = size.height * 0.5f + ((pts[0].z - cz) / span) * scale
        drawCircle(Color.White, radius = 5f, center = Offset(startX, startY))
    }
}

@Composable
fun CarSelectScreen(
    state: UiAppState,
    track: TrackSpec,
    onSelectCar: (String) -> Unit,
    onOpenGarage: () -> Unit,
    onStartRace: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val selectedId = state.profile.selectedCarId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepObsidian, MidnightSurface)))
            .navigationBarsPadding()
    ) {
        TopHeaderBar(
            title = state.labels.selectCar,
            onBack = onBack,
            credits = state.profile.credits,
            totalPoints = state.profile.totalPoints
        )

        Text(
            text = "CIRCUIT: ${track.name.uppercase()} • 3 LAPS",
            style = MaterialTheme.typography.labelLarge,
            color = ElectricBlue,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(GameCatalog.cars) { spec ->
                val carState = state.cars.find { it.carId == spec.id }
                val unlocked = carState?.unlocked == true
                val isSelected = spec.id == selectedId
                val neonCol = Color(carState?.neonColorHex ?: spec.defaultNeonColor)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("car_select_${spec.id}"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0xFF172748) else ElevatedCard,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) CyberCyan else if (unlocked) BorderGlow else Color(0xFF28324B)
                    ),
                    onClick = {
                        if (unlocked) onSelectCar(spec.id) else onOpenGarage()
                    }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(spec.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
                                Text(spec.tagline, style = MaterialTheme.typography.bodyMedium, color = neonCol)
                            }
                            if (!unlocked) {
                                CurrencyChip(label = "LOCKED • ${spec.unlockCost} CR", color = SolarGold)
                            } else if (isSelected) {
                                CurrencyChip(label = "SELECTED", color = CyberCyan)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        val spd = spec.baseMaxSpeed + (carState?.speedLevel ?: 0) * 11f
                        val acc = spec.baseAcceleration + (carState?.accelLevel ?: 0) * 0.045f
                        val hnd = spec.baseHandling + (carState?.handlingLevel ?: 0) * 0.045f
                        StatProgressBar("TOP SPEED", spd / 410f, "${spd.toInt()} KM/H", neonCol)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) {
                                StatProgressBar("ACCEL", acc.coerceAtMost(1.2f) / 1.2f, "${(acc * 100).toInt()}")
                            }
                            Box(Modifier.weight(1f)) {
                                StatProgressBar("HANDLING", hnd.coerceAtMost(1.2f) / 1.2f, "${(hnd * 100).toInt()}")
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecondaryMenuButton(
                text = "GARAGE",
                icon = Icons.Filled.PlayArrow,
                onClick = onOpenGarage,
                modifier = Modifier.weight(0.42f)
            )
            PrimaryNeonButton(
                text = state.labels.startRace,
                icon = Icons.Filled.Flag,
                testTag = "start_race_button",
                modifier = Modifier.weight(0.58f),
                onClick = onStartRace
            )
        }
    }
}

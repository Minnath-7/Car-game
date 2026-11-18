package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.SettingsEntity
import com.example.model.AppLanguage
import com.example.model.CameraPerspective
import com.example.model.GameCatalog
import com.example.model.GraphicsQuality
import com.example.model.ScoringRules
import com.example.model.SteeringMode
import com.example.ui.UiAppState
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.SilverAccent
import com.example.ui.theme.SolarGold
import java.util.concurrent.TimeUnit

@Composable
fun DailyRewardsScreen(
    state: UiAppState,
    onClaim: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val profile = state.profile
    val todayEpoch = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
    val canClaim = profile.lastDailyClaimEpochDay < todayEpoch
    val currentDay = if (canClaim) (profile.dailyStreak % 7) + 1 else profile.dailyStreak.coerceIn(1, 7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepObsidian, MidnightSurface)))
            .navigationBarsPadding()
    ) {
        TopHeaderBar(
            title = state.labels.dailyRewards,
            onBack = onBack,
            credits = profile.credits,
            totalPoints = profile.totalPoints
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "STREAK: ${profile.dailyStreak} DAYS • LOG IN DAILY FOR BONUS PTS & CR",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan
                )
                Spacer(Modifier.height(6.dp))
            }

            items((1..7).toList()) { day ->
                val reward = 100 + (day - 1) * 50
                val isClaimed = day < currentDay || (!canClaim && day == currentDay)
                val isToday = canClaim && day == currentDay
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isToday) Color(0xFF142D4C) else ElevatedCard,
                    border = BorderStroke(
                        if (isToday) 2.dp else 1.dp,
                        when {
                            isToday -> CyberCyan
                            isClaimed -> EmeraldSuccess.copy(alpha = 0.6f)
                            else -> BorderGlow
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isClaimed) Icons.Filled.CheckCircle else Icons.Filled.CardGiftcard,
                                contentDescription = null,
                                tint = if (isClaimed) EmeraldSuccess else if (isToday) CyberCyan else SolarGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("DAY $day REWARD", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("+$reward PTS & +$reward CR", style = MaterialTheme.typography.bodyMedium, color = SolarGold)
                            }
                        }
                        CurrencyChip(
                            label = if (isClaimed) "CLAIMED" else if (isToday) "READY" else "LOCKED",
                            color = if (isClaimed) EmeraldSuccess else if (isToday) CyberCyan else SilverAccent
                        )
                    }
                }
            }
        }

        Box(Modifier.padding(16.dp)) {
            PrimaryNeonButton(
                text = if (canClaim) "CLAIM TODAY'S REWARD" else "COME BACK TOMORROW",
                icon = Icons.Filled.CardGiftcard,
                enabled = canClaim,
                testTag = "claim_daily_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = onClaim
            )
        }
    }
}

@Composable
fun LeaderboardScreen(
    state: UiAppState,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val profile = state.profile

    val rivals = listOf(
        Triple("Vex Kuro", 1950, "Obsidian Valkyrie"),
        Triple("Nova Takahashi", 1420, "Solaris Apex"),
        Triple("Zane Vance", 1080, "Cobalt Tempest"),
        Triple("Lyra Chen", 740, "Phantom R-Spec"),
        Triple("Ryker Volt", 420, "Vortex V8"),
        Triple("${profile.username} (YOU)", profile.totalPoints, GameCatalog.cars.find { it.id == profile.selectedCarId }?.name ?: "Shadow Fang GT")
    ).sortedByDescending { it.second }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepObsidian, MidnightSurface)))
            .navigationBarsPadding()
    ) {
        TopHeaderBar(
            title = state.labels.leaderboard,
            onBack = onBack,
            credits = profile.credits,
            totalPoints = profile.totalPoints
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("WORLD CHAMPIONSHIP STANDINGS", style = MaterialTheme.typography.titleLarge, color = CyberCyan)
            }
            itemsIndexed(rivals) { idx, (name, pts, car) ->
                val isYou = name.endsWith("(YOU)")
                val rank = idx + 1
                val col = when (rank) {
                    1 -> SolarGold
                    2 -> SilverAccent
                    3 -> Color(0xFFCD7F32)
                    else -> ElectricBlue
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isYou) Color(0xFF142E52) else ElevatedCard,
                    border = BorderStroke(if (isYou) 2.dp else 1.dp, if (isYou) CyberCyan else col.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#$rank", style = MaterialTheme.typography.headlineMedium, color = col, modifier = Modifier.width(38.dp))
                            Column {
                                Text(name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("$car • LVL ${ScoringRules.levelForPoints(pts)}", style = MaterialTheme.typography.bodyMedium, color = SilverAccent)
                            }
                        }
                        CurrencyChip(label = "$pts PTS", color = col)
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("PERSONAL TRACK RECORDS", style = MaterialTheme.typography.titleLarge, color = CyberCyan)
            }
            items(GameCatalog.tracks) { track ->
                val best = state.bestTimes[track.id] ?: 0L
                NeonPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(track.accentHex), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(track.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                        Text(formatRaceTime(best), style = MaterialTheme.typography.labelLarge, color = SolarGold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: UiAppState,
    onUpdateSettings: ((SettingsEntity) -> SettingsEntity) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val s = state.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepObsidian, MidnightSurface)))
            .navigationBarsPadding()
    ) {
        TopHeaderBar(
            title = state.labels.settings,
            onBack = onBack,
            credits = state.profile.credits,
            totalPoints = state.profile.totalPoints
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                NeonPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("GRAPHICS QUALITY", style = MaterialTheme.typography.titleMedium, color = CyberCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GraphicsQuality.entries.forEach { q ->
                                val active = s.graphicsQualityId == q.id
                                Surface(
                                    modifier = Modifier.weight(1f).testTag("graphics_${q.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) ElectricBlue else ElevatedCard,
                                    onClick = { onUpdateSettings { it.copy(graphicsQualityId = q.id) } }
                                ) {
                                    Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                        Text(q.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (active) DeepObsidian else Color.White)
                                    }
                                }
                            }
                        }

                        Text("CAMERA PERSPECTIVE", style = MaterialTheme.typography.titleMedium, color = CyberCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CameraPerspective.entries.forEach { cam ->
                                val active = s.cameraPerspectiveId == cam.id
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) ElectricBlue else ElevatedCard,
                                    onClick = { onUpdateSettings { it.copy(cameraPerspectiveId = cam.id) } }
                                ) {
                                    Box(Modifier.padding(vertical = 10.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(cam.label, style = MaterialTheme.typography.labelSmall, color = if (active) DeepObsidian else Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                NeonPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AUDIO MIXER", style = MaterialTheme.typography.titleMedium, color = CyberCyan)

                        Text("SFX VOLUME: ${(s.sfxVolume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Slider(
                            value = s.sfxVolume,
                            onValueChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } },
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = ElectricBlue)
                        )

                        Text("MUSIC VOLUME: ${(s.musicVolume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Slider(
                            value = s.musicVolume,
                            onValueChange = { v -> onUpdateSettings { it.copy(musicVolume = v) } },
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = ElectricBlue)
                        )
                    }
                }
            }

            item {
                NeonPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("DRIVING CONTROLS", style = MaterialTheme.typography.titleMedium, color = CyberCyan)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SteeringMode.entries.forEach { mode ->
                                val active = s.steeringModeId == mode.id
                                Surface(
                                    modifier = Modifier.weight(1f).testTag("steering_${mode.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) ElectricBlue else ElevatedCard,
                                    onClick = { onUpdateSettings { it.copy(steeringModeId = mode.id) } }
                                ) {
                                    Box(Modifier.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                                        Text(mode.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (active) DeepObsidian else Color.White)
                                    }
                                }
                            }
                        }

                        Text("STEERING SENSITIVITY: ${(s.controlSensitivity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Slider(
                            value = s.controlSensitivity,
                            onValueChange = { v -> onUpdateSettings { it.copy(controlSensitivity = v) } },
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = ElectricBlue)
                        )

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("AUTO ACCELERATE", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("Gas engages automatically during race", style = MaterialTheme.typography.bodyMedium, color = SilverAccent)
                            }
                            Switch(
                                checked = s.autoAccelerate,
                                onCheckedChange = { c -> onUpdateSettings { it.copy(autoAccelerate = c) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = DeepObsidian, checkedTrackColor = CyberCyan)
                            )
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("HAPTIC VIBRATION", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("Tactile feedback on impacts & finish", style = MaterialTheme.typography.bodyMedium, color = SilverAccent)
                            }
                            Switch(
                                checked = s.vibrationEnabled,
                                onCheckedChange = { c -> onUpdateSettings { it.copy(vibrationEnabled = c) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = DeepObsidian, checkedTrackColor = CyberCyan)
                            )
                        }
                    }
                }
            }

            item {
                NeonPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("LANGUAGE", style = MaterialTheme.typography.titleMedium, color = CyberCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                val active = s.languageCode == lang.code
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) ElectricBlue else ElevatedCard,
                                    onClick = { onUpdateSettings { it.copy(languageCode = lang.code) } }
                                ) {
                                    Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                        Text(lang.displayName, style = MaterialTheme.typography.labelSmall, color = if (active) DeepObsidian else Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

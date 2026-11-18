package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine3d.Game3DViewport
import com.example.engine3d.GameGLRenderer
import com.example.model.GameCatalog
import com.example.model.ScoringRules
import com.example.ui.Screen
import com.example.ui.UiAppState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassPanel
import com.example.ui.theme.NeonCobalt
import com.example.ui.theme.SilverAccent
import com.example.ui.theme.SolarGold
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var targetProgress by remember { mutableFloatStateOf(0.05f) }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1500),
        label = "splash_progress"
    )

    LaunchedEffect(Unit) {
        delay(120)
        targetProgress = 1f
        delay(1750)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        Image(
            painter = painterResource(id = R.drawable.img_splash_keyart),
            contentDescription = "Shadow Racer Key Art",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DeepObsidian.copy(alpha = 0.72f),
                            Color.Transparent,
                            DeepObsidian.copy(alpha = 0.94f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SHADOW RACER",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 34.sp),
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ULTIMATE SPEED",
                    style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 5.sp),
                    color = CyberCyan
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "INITIALIZING 3D ENGINE & GARAGE...",
                    style = MaterialTheme.typography.labelSmall,
                    color = SilverAccent
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ElectricBlue,
                    trackColor = Color(0xFF1A2540)
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HomeScreen(
    state: UiAppState,
    renderer: GameGLRenderer,
    onNavigate: (Screen) -> Unit
) {
    val profile = state.profile
    val labels = state.labels
    val level = ScoringRules.levelForPoints(profile.totalPoints)
    val selectedCar = GameCatalog.cars.find { it.id == profile.selectedCarId } ?: GameCatalog.cars[0]
    val todayEpoch = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
    val dailyAvailable = profile.lastDailyClaimEpochDay < todayEpoch

    Box(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        // Real-time 3D OpenGL ES Showcase: Kael standing beside his Signature Car in Futuristic City!
        Game3DViewport(
            renderer = renderer,
            modifier = Modifier.fillMaxSize(),
            allowTouchOrbit = true
        )

        // Subtle atmospheric gradient scrims so HUD & buttons stay crystal clear
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DeepObsidian.copy(alpha = 0.78f),
                            Color.Transparent,
                            Color.Transparent,
                            DeepObsidian.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Hero Avatar + Title + Points & Level
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassPanel)
                            .border(1.dp, ElectricBlue.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                            .clickable { onNavigate(Screen.HeroProfile) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_portrait),
                            contentDescription = "Hero Portrait",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, CyberCyan, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = profile.username,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "LVL $level • ${profile.totalVictories} WINS",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        CurrencyChip(label = "${profile.totalPoints} PTS", color = SolarGold)
                        Spacer(Modifier.height(4.dp))
                        CurrencyChip(label = "${profile.credits} CR", color = ElectricBlue)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SHADOW RACER",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                    Text(
                        text = "ULTIMATE SPEED • ${selectedCar.name.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue
                    )
                }
            }

            // Bottom Navigation Menu: All 7 Required Buttons!
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Large & Prominent PLAY button
                PrimaryNeonButton(
                    text = labels.play,
                    icon = Icons.Filled.PlayArrow,
                    testTag = "home_play_button",
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    onClick = { onNavigate(Screen.ModeSelect) }
                )

                // 2. GARAGE & 3. CAREER MODE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryMenuButton(
                        text = labels.garage,
                        icon = Icons.Filled.DirectionsCar,
                        testTag = "home_garage_button",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Garage) }
                    )
                    SecondaryMenuButton(
                        text = labels.careerMode,
                        icon = Icons.Filled.EmojiEvents,
                        testTag = "home_career_button",
                        accentColor = SolarGold,
                        badgeText = "STG ${profile.careerStageUnlocked}",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.CareerLadder) }
                    )
                }

                // 4. DAILY REWARDS & 5. LEADERBOARD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryMenuButton(
                        text = labels.dailyRewards,
                        icon = Icons.Filled.CardGiftcard,
                        testTag = "home_daily_button",
                        accentColor = EmeraldSuccess,
                        badgeText = if (dailyAvailable) "NEW" else null,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.DailyRewards) }
                    )
                    SecondaryMenuButton(
                        text = labels.leaderboard,
                        icon = Icons.Filled.Leaderboard,
                        testTag = "home_leaderboard_button",
                        accentColor = NeonCobalt,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Leaderboard) }
                    )
                }

                // 6. SETTINGS & 7. PLAYER PROFILE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryMenuButton(
                        text = labels.settings,
                        icon = Icons.Filled.Settings,
                        testTag = "home_settings_button",
                        accentColor = SilverAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Settings) }
                    )
                    SecondaryMenuButton(
                        text = labels.playerProfile,
                        icon = Icons.Filled.Person,
                        testTag = "home_profile_button",
                        accentColor = CyberCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.HeroProfile) }
                    )
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine3d.Game3DViewport
import com.example.engine3d.GameGLRenderer
import com.example.model.GameCatalog
import com.example.model.HeroAnimation
import com.example.model.HeroViewAngle
import com.example.model.ScoringRules
import com.example.ui.UiAppState
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.SilverAccent
import com.example.ui.theme.SolarGold

@Composable
fun HeroProfileScreen(
    state: UiAppState,
    renderer: GameGLRenderer,
    currentView: HeroViewAngle,
    currentAnim: HeroAnimation,
    onSelectView: (HeroViewAngle) -> Unit,
    onSelectAnim: (HeroAnimation) -> Unit,
    onSelectOutfit: (String) -> Unit,
    onSelectAccessory: (String) -> Unit,
    onSelectAura: (String) -> Unit,
    onUpdateUsername: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val profile = state.profile
    val level = ScoringRules.levelForPoints(profile.totalPoints)
    val levelPct = ScoringRules.pointsIntoLevel(profile.totalPoints) / 350f
    val unlockedCarsCount = state.cars.count { it.unlocked }
    val unlockedOutfits = profile.unlockedOutfitsCsv.split(",").toSet()
    val unlockedAcc = profile.unlockedAccessoriesCsv.split(",").toSet()
    val unlockedAuras = profile.unlockedAurasCsv.split(",").toSet()

    var showRenameDialog by remember { mutableStateOf(false) }
    var draftName by remember(profile.username) { mutableStateOf(profile.username) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("EDIT DRIVER TAG") },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { if (it.length <= 20) draftName = it },
                    singleLine = true,
                    label = { Text("Username") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateUsername(draftName)
                    showRenameDialog = false
                }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("CANCEL") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepObsidian, MidnightSurface)))
            .navigationBarsPadding()
    ) {
        TopHeaderBar(
            title = "HERO & PROFILE",
            onBack = onBack,
            credits = profile.credits,
            totalPoints = profile.totalPoints
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live 3D Hero Character Studio (Front / Back / Side / Orbit + 6 Animations!)
            item {
                NeonPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("3D HERO STUDIO • KAEL", style = MaterialTheme.typography.titleLarge, color = CyberCyan)
                            Text("DRAG TO ROTATE", style = MaterialTheme.typography.labelSmall, color = SilverAccent)
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(235.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, BorderGlow, RoundedCornerShape(14.dp))
                        ) {
                            Game3DViewport(
                                renderer = renderer,
                                modifier = Modifier.fillMaxSize(),
                                allowTouchOrbit = true
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        // Front / Side / Back / Orbit Views
                        Text("CAMERA VIEW", style = MaterialTheme.typography.labelSmall, color = SilverAccent)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeroViewAngle.entries.forEach { view ->
                                val active = view == currentView
                                Surface(
                                    modifier = Modifier.weight(1f).testTag("hero_view_${view.name}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) ElectricBlue else ElevatedCard,
                                    onClick = { onSelectView(view) }
                                ) {
                                    Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = view.label.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (active) DeepObsidian else Color.White
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        // Idle, Walk, Run, Victory, Celebration, Lose Animations
                        Text("HERO ANIMATION", style = MaterialTheme.typography.labelSmall, color = SilverAccent)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(HeroAnimation.entries) { anim ->
                                val active = anim == currentAnim
                                Surface(
                                    modifier = Modifier.testTag("hero_anim_${anim.name}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) CyberCyan else ElevatedCard,
                                    border = BorderStroke(1.dp, if (active) CyberCyan else BorderGlow),
                                    onClick = { onSelectAnim(anim) }
                                ) {
                                    Text(
                                        text = anim.label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) DeepObsidian else Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Hero Customization (Outfits, Shoulder Gear, Visual Effects)
            item {
                NeonPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("HERO CUSTOMIZATION", style = MaterialTheme.typography.titleLarge, color = Color.White)

                        Text("OUTFITS", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(GameCatalog.outfits) { item ->
                                val isUnlocked = unlockedOutfits.contains(item.id)
                                val isEquipped = profile.selectedOutfitId == item.id
                                CustomizationChip(
                                    title = item.name,
                                    cost = item.cost,
                                    unlocked = isUnlocked,
                                    equipped = isEquipped,
                                    onClick = { onSelectOutfit(item.id) }
                                )
                            }
                        }

                        Text("SHOULDER ACCESSORIES", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(GameCatalog.accessories) { item ->
                                val isUnlocked = unlockedAcc.contains(item.id)
                                val isEquipped = profile.selectedAccessoryId == item.id
                                CustomizationChip(
                                    title = item.name,
                                    cost = item.cost,
                                    unlocked = isUnlocked,
                                    equipped = isEquipped,
                                    onClick = { onSelectAccessory(item.id) }
                                )
                            }
                        }

                        Text("AURA EFFECTS", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(GameCatalog.auras) { item ->
                                val isUnlocked = unlockedAuras.contains(item.id)
                                val isEquipped = profile.selectedAuraId == item.id
                                CustomizationChip(
                                    title = item.name,
                                    cost = item.cost,
                                    unlocked = isUnlocked,
                                    equipped = isEquipped,
                                    onClick = { onSelectAura(item.id) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Player Profile & Career Telemetry
            item {
                NeonPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hero_portrait),
                                contentDescription = "Hero Portrait",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, CyberCyan, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(profile.username, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                                    IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Rename", tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                    }
                                }
                                StatProgressBar("DRIVER LEVEL $level", levelPct, "${profile.totalPoints} PTS", SolarGold)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox("RACES", "${profile.totalRaces}", Modifier.weight(1f))
                            StatBox("1ST PLACE", "${profile.firstPlaces}", Modifier.weight(1f), SolarGold)
                            StatBox("2ND PLACE", "${profile.secondPlaces}", Modifier.weight(1f), SilverAccent)
                            StatBox("3RD PLACE", "${profile.thirdPlaces}", Modifier.weight(1f), Color(0xFFCD7F32))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox("VICTORIES", "${profile.totalVictories}", Modifier.weight(1f), EmeraldSuccess)
                            StatBox("BEST TIME", formatRaceTime(profile.bestRaceTimeMs), Modifier.weight(1f), CyberCyan)
                            StatBox("CARS", "$unlockedCarsCount/${GameCatalog.cars.size}", Modifier.weight(1f), ElectricBlue)
                        }
                    }
                }
            }

            // 4. Achievements
            item {
                Text("ACHIEVEMENTS", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }
            items(GameCatalog.achievements) { ach ->
                val unlocked = state.achievements.find { it.id == ach.id }?.unlocked == true
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (unlocked) Color(0xFF142B3E) else ElevatedCard,
                    border = BorderStroke(1.dp, if (unlocked) EmeraldSuccess.copy(alpha = 0.7f) else Color(0xFF26304A))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (unlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = if (unlocked) EmeraldSuccess else MutedSteel,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ach.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(ach.description, style = MaterialTheme.typography.bodyMedium, color = SilverAccent)
                        }
                        CurrencyChip(
                            label = if (unlocked) "UNLOCKED" else "+${ach.rewardPoints} PTS",
                            color = if (unlocked) EmeraldSuccess else SolarGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizationChip(
    title: String,
    cost: Int,
    unlocked: Boolean,
    equipped: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (equipped) Color(0xFF13335A) else ElevatedCard,
        border = BorderStroke(1.dp, if (equipped) CyberCyan else if (unlocked) BorderGlow else Color(0xFF2B3550)),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(
                text = when {
                    equipped -> "EQUIPPED"
                    unlocked -> "OWNED"
                    else -> "$cost CR"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    equipped -> CyberCyan
                    unlocked -> EmeraldSuccess
                    else -> SolarGold
                }
            )
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MidnightSurface)
            .border(1.dp, Color(0xFF253252), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedSteel)
    }
}

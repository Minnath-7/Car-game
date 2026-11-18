package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine3d.Game3DViewport
import com.example.engine3d.GameGLRenderer
import com.example.model.CarSpec
import com.example.model.GameCatalog
import com.example.model.SpoilerStyle
import com.example.model.WheelStyle
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

@Composable
fun GarageScreen(
    state: UiAppState,
    focusCarId: String,
    renderer: GameGLRenderer,
    onFocusCar: (String) -> Unit,
    onUnlockCar: (CarSpec) -> Unit,
    onPaintChange: (String, Long, Long) -> Unit,
    onWheelChange: (String, WheelStyle) -> Unit,
    onSpoilerChange: (String, SpoilerStyle) -> Unit,
    onUpgradeStat: (String, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val spec = GameCatalog.cars.find { it.id == focusCarId } ?: GameCatalog.cars[0]
    val carState = state.cars.find { it.carId == focusCarId }
    val unlocked = carState?.unlocked == true
    val isEquipped = state.profile.selectedCarId == focusCarId
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Paint & Aero, 1 = Upgrades

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepObsidian, MidnightSurface)))
            .navigationBarsPadding()
    ) {
        TopHeaderBar(
            title = "3D GARAGE",
            onBack = onBack,
            credits = state.profile.credits,
            totalPoints = state.profile.totalPoints
        )

        // Interactive 3D Rotating Turntable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, BorderGlow, RoundedCornerShape(18.dp))
        ) {
            Game3DViewport(
                renderer = renderer,
                modifier = Modifier.fillMaxSize(),
                allowTouchOrbit = true
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(spec.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(spec.tagline, style = MaterialTheme.typography.labelSmall, color = CyberCyan)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            ) {
                if (!unlocked) {
                    PrimaryNeonButton(
                        text = "UNLOCK ${spec.unlockCost} CR",
                        icon = Icons.Filled.Lock,
                        testTag = "unlock_car_button",
                        onClick = { onUnlockCar(spec) }
                    )
                } else {
                    CurrencyChip(
                        label = if (isEquipped) "ACTIVE RIDE" else "OWNED",
                        color = if (isEquipped) EmeraldSuccess else ElectricBlue
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Car Collection Strip
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(GameCatalog.cars) { item ->
                val itemUnlocked = state.cars.find { it.carId == item.id }?.unlocked == true
                val focused = item.id == focusCarId
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (focused) Color(0xFF172C52) else ElevatedCard,
                    border = BorderStroke(
                        if (focused) 1.5.dp else 1.dp,
                        if (focused) CyberCyan else if (itemUnlocked) BorderGlow else Color(0xFF28324B)
                    ),
                    onClick = { onFocusCar(item.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!itemUnlocked) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = SolarGold, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (focused) Color.White else SilverAccent
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MidnightSurface,
            contentColor = ElectricBlue
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("PAINT & AERO", style = MaterialTheme.typography.labelSmall) },
                icon = { Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("UPGRADES & STATS", style = MaterialTheme.typography.labelSmall) },
                icon = { Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedTab == 0) {
                item {
                    val curBody = carState?.paintColorHex ?: spec.defaultBodyColor
                    val curNeon = carState?.neonColorHex ?: spec.defaultNeonColor

                    NeonPanel(modifier = Modifier.fillMaxWidth().padding(0.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("EXTERIOR PAINT", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GameCatalog.paintSwatches.forEach { (name, hex) ->
                                    val selected = curBody == hex
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(hex))
                                            .border(
                                                if (selected) 2.5.dp else 1.dp,
                                                if (selected) CyberCyan else Color.White.copy(alpha = 0.35f),
                                                CircleShape
                                            )
                                            .clickable(enabled = unlocked) {
                                                onPaintChange(spec.id, hex, curNeon)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selected) Icon(Icons.Filled.Check, contentDescription = name, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            Text("NEON HIGHLIGHT TRIM", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GameCatalog.neonSwatches.forEach { (name, hex) ->
                                    val selected = curNeon == hex
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(hex))
                                            .border(
                                                if (selected) 2.5.dp else 1.dp,
                                                if (selected) Color.White else Color.Transparent,
                                                CircleShape
                                            )
                                            .clickable(enabled = unlocked) {
                                                onPaintChange(spec.id, curBody, hex)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selected) Icon(Icons.Filled.Check, contentDescription = name, tint = DeepObsidian, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    NeonPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("ALLOY WHEELS", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            WheelStyle.entries.forEach { ws ->
                                val active = (carState?.wheelStyleId ?: 0) == ws.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) ElectricBlue.copy(alpha = 0.18f) else Color.Transparent)
                                        .clickable(enabled = unlocked) { onWheelChange(spec.id, ws) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ws.label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                    Text(
                                        text = if (active) "EQUIPPED" else if (ws.cost == 0) "FREE" else "${ws.cost} CR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) EmeraldSuccess else SolarGold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    NeonPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("RACING SPOILER", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            SpoilerStyle.entries.forEach { sp ->
                                val active = (carState?.spoilerStyleId ?: 0) == sp.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) ElectricBlue.copy(alpha = 0.18f) else Color.Transparent)
                                        .clickable(enabled = unlocked) { onSpoilerChange(spec.id, sp) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sp.label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                    Text(
                                        text = if (active) "EQUIPPED" else if (sp.cost == 0) "FREE" else "${sp.cost} CR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) EmeraldSuccess else SolarGold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val stats = listOf(
                    Triple("speed", "MAX SPEED (ENGINE)", (carState?.speedLevel ?: 0) to (spec.baseMaxSpeed + (carState?.speedLevel ?: 0) * 11f) / 410f),
                    Triple("accel", "ACCELERATION", (carState?.accelLevel ?: 0) to (spec.baseAcceleration + (carState?.accelLevel ?: 0) * 0.045f) / 1.2f),
                    Triple("handling", "HANDLING & GRIP", (carState?.handlingLevel ?: 0) to (spec.baseHandling + (carState?.handlingLevel ?: 0) * 0.045f) / 1.2f),
                    Triple("braking", "BRAKING POWER", (carState?.brakingLevel ?: 0) to (spec.baseBraking + (carState?.brakingLevel ?: 0) * 0.05f) / 1.2f),
                    Triple("nitro", "NITRO CAPACITY", (carState?.nitroLevel ?: 0) to (spec.baseNitro + (carState?.nitroLevel ?: 0) * 0.05f) / 1.25f)
                )
                items(stats) { (key, title, pair) ->
                    val (lvl, norm) = pair
                    val cost = (lvl + 1) * 100
                    NeonPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                StatProgressBar(
                                    label = "$title (LV $lvl/5)",
                                    valueNorm = norm,
                                    displayValue = "${(norm * 100).toInt()}%"
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            PrimaryNeonButton(
                                text = if (lvl >= 5) "MAX" else "+LV ($cost)",
                                enabled = unlocked && lvl < 5 && state.profile.credits >= cost,
                                testTag = "upgrade_$key",
                                onClick = { onUpgradeStat(spec.id, key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

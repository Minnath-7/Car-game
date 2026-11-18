package com.example.model

enum class GameMode(val id: String, val title: String, val subtitle: String) {
    QUICK_RACE("quick", "QUICK RACE", "Instant 3-lap battle against 5 AI rivals"),
    CAREER("career", "CAREER MODE", "10 progressive stages, unlock tracks & hypercars"),
    TIME_TRIAL("time_trial", "TIME TRIAL", "Beat the target clock and set track records"),
    CHAMPIONSHIP("championship", "CHAMPIONSHIP", "4-race tournament for the Ultimate Trophy")
}

enum class CameraPerspective(val id: String, val label: String) {
    CHASE("chase", "Chase Cam"),
    CLOSE_REAR("close", "Close Rear"),
    HOOD("hood", "Hood Cam"),
    CINEMATIC("cinematic", "Cinematic")
}

enum class SteeringMode(val id: String, val label: String) {
    TOUCH("touch", "Touch Buttons"),
    TILT("tilt", "Gyroscope Tilt")
}

enum class GraphicsQuality(val id: String, val label: String) {
    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High")
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    JAPANESE("ja", "日本語")
}

enum class HeroViewAngle(val label: String, val yawDeg: Float) {
    FRONT("Front", 0f),
    SIDE("Side", 90f),
    BACK("Back", 180f),
    ORBIT("Orbit", -1f)
}

enum class HeroAnimation(val label: String) {
    IDLE("Idle"),
    WALK("Walk"),
    RUN("Run"),
    VICTORY("Victory"),
    CELEBRATION("Celebrate"),
    LOSE("Defeat")
}

enum class WheelStyle(val id: Int, val label: String, val cost: Int) {
    ALLOY_BLADE(0, "Silver Turbine", 0),
    CYBER_RING(1, "Neon Halo", 120),
    CARBON_FIVE(2, "Carbon 5-Spoke", 200),
    PHANTOM_DISC(3, "Aero Disc", 280)
}

enum class SpoilerStyle(val id: Int, val label: String, val cost: Int) {
    STOCK_LIP(0, "Stealth Lip", 0),
    GT_WING(1, "Carbon GT Wing", 150),
    TWIN_BLADE(2, "Cyber Twin-Blade", 250),
    HYPER_FIN(3, "Valkyrie Split Fin", 350)
}

enum class CarSilhouette {
    SHADOW_FANG,
    VORTEX_WEDGE,
    PHANTOM_PROTO,
    COBALT_DRIFT,
    SOLARIS_EXOTIC,
    VALKYRIE_HYPER
}

data class CarSpec(
    val id: String,
    val name: String,
    val tagline: String,
    val silhouette: CarSilhouette,
    val unlockCost: Int,
    val defaultBodyColor: Long,
    val defaultNeonColor: Long,
    val baseMaxSpeed: Float,    // km/h scale: 260..360
    val baseAcceleration: Float,// 0.55..1.0
    val baseHandling: Float,    // 0.55..1.0
    val baseBraking: Float,     // 0.55..1.0
    val baseNitro: Float        // 0.55..1.0
)

enum class TrackTheme {
    FUTURISTIC_CITY,
    MOUNTAIN_HIGHWAY,
    DESERT_SPEEDWAY,
    COASTAL_HIGHWAY,
    CYBERPUNK_NIGHT
}

data class TrackWaypoint(
    val x: Float,
    val y: Float,
    val z: Float
)

data class TrackSpec(
    val id: String,
    val index: Int,
    val name: String,
    val subtitle: String,
    val theme: TrackTheme,
    val roadWidth: Float,
    val targetTimeSec: Int,
    val unlockCareerStage: Int,
    val skyTopColor: Long,
    val skyHorizonColor: Long,
    val accentHex: Long,
    val isNight: Boolean,
    val isWetRoad: Boolean,
    val controlPoints: List<TrackWaypoint>
)

data class CareerStageSpec(
    val stage: Int,
    val title: String,
    val trackId: String,
    val requiredPosition: Int,
    val aiDifficulty: Float,
    val bonusPoints: Int,
    val unlockDescription: String
)

data class HeroOutfitSpec(
    val id: String,
    val name: String,
    val cost: Int,
    val jacketColor: Long,
    val armorColor: Long,
    val trimColor: Long
)

data class HeroAccessorySpec(
    val id: String,
    val name: String,
    val cost: Int,
    val styleIndex: Int
)

data class HeroAuraSpec(
    val id: String,
    val name: String,
    val cost: Int,
    val colorHex: Long
)

data class AchievementSpec(
    val id: String,
    val title: String,
    val description: String,
    val rewardPoints: Int
)

object ScoringRules {
    fun pointsForPosition(position: Int): Int = when (position) {
        1 -> 200
        2 -> 150
        3 -> 100
        4 -> 50
        5 -> 25
        6 -> 10
        else -> 10
    }

    fun rankHeadline(position: Int): String = when (position) {
        1 -> "1ST PLACE: 200 POINTS – CHAMPION"
        2 -> "2ND PLACE: 150 POINTS – RUNNER-UP"
        3 -> "3RD PLACE: 100 POINTS – THIRD PLACE"
        4 -> "4TH PLACE: 50 POINTS"
        5 -> "5TH PLACE: 25 POINTS"
        else -> "6TH PLACE: 10 POINTS"
    }

    fun levelForPoints(totalPoints: Int): Int = (totalPoints / 350) + 1
    fun pointsIntoLevel(totalPoints: Int): Int = totalPoints % 350
}

object GameCatalog {
    val cars: List<CarSpec> = listOf(
        CarSpec(
            id = "shadow_fang",
            name = "Shadow Fang GT",
            tagline = "Signature Obsidian Hypercar",
            silhouette = CarSilhouette.SHADOW_FANG,
            unlockCost = 0,
            defaultBodyColor = 0xFF101522,
            defaultNeonColor = 0xFF00D2FF,
            baseMaxSpeed = 285f,
            baseAcceleration = 0.72f,
            baseHandling = 0.76f,
            baseBraking = 0.74f,
            baseNitro = 0.75f
        ),
        CarSpec(
            id = "vortex_v8",
            name = "Vortex V8",
            tagline = "Razor Wedge Interceptor",
            silhouette = CarSilhouette.VORTEX_WEDGE,
            unlockCost = 300,
            defaultBodyColor = 0xFF1F2A44,
            defaultNeonColor = 0xFF00F0FF,
            baseMaxSpeed = 300f,
            baseAcceleration = 0.78f,
            baseHandling = 0.74f,
            baseBraking = 0.76f,
            baseNitro = 0.78f
        ),
        CarSpec(
            id = "phantom_rspec",
            name = "Phantom R-Spec",
            tagline = "Aerodynamic Endurance Prototype",
            silhouette = CarSilhouette.PHANTOM_PROTO,
            unlockCost = 600,
            defaultBodyColor = 0xFFC8D2E6,
            defaultNeonColor = 0xFF2D6BFF,
            baseMaxSpeed = 315f,
            baseAcceleration = 0.80f,
            baseHandling = 0.82f,
            baseBraking = 0.80f,
            baseNitro = 0.80f
        ),
        CarSpec(
            id = "cobalt_tempest",
            name = "Cobalt Tempest",
            tagline = "Twin-Turbo Street Drift Beast",
            silhouette = CarSilhouette.COBALT_DRIFT,
            unlockCost = 900,
            defaultBodyColor = 0xFF0D2E6E,
            defaultNeonColor = 0xFF00E5FF,
            baseMaxSpeed = 322f,
            baseAcceleration = 0.86f,
            baseHandling = 0.88f,
            baseBraking = 0.82f,
            baseNitro = 0.85f
        ),
        CarSpec(
            id = "solaris_apex",
            name = "Solaris Apex",
            tagline = "Carbon Mid-Engine Track Weapon",
            silhouette = CarSilhouette.SOLARIS_EXOTIC,
            unlockCost = 1300,
            defaultBodyColor = 0xFF8A1030,
            defaultNeonColor = 0xFFFFB800,
            baseMaxSpeed = 338f,
            baseAcceleration = 0.90f,
            baseHandling = 0.86f,
            baseBraking = 0.88f,
            baseNitro = 0.88f
        ),
        CarSpec(
            id = "obsidian_valkyrie",
            name = "Obsidian Valkyrie",
            tagline = "Ultimate Quantum-Drive Hypercar",
            silhouette = CarSilhouette.VALKYRIE_HYPER,
            unlockCost = 1800,
            defaultBodyColor = 0xFF0A0D16,
            defaultNeonColor = 0xFF00F0FF,
            baseMaxSpeed = 355f,
            baseAcceleration = 0.95f,
            baseHandling = 0.94f,
            baseBraking = 0.93f,
            baseNitro = 0.96f
        )
    )

    val paintSwatches: List<Pair<String, Long>> = listOf(
        "Obsidian Black" to 0xFF101522,
        "Phantom Silver" to 0xFFC8D4E8,
        "Cobalt Surge" to 0xFF1348B8,
        "Crimson Pulse" to 0xFFB0183A,
        "Emerald Venom" to 0xFF0D7A52,
        "Solar Gold" to 0xFFD99416,
        "Stealth Graphite" to 0xFF2D3546
    )

    val neonSwatches: List<Pair<String, Long>> = listOf(
        "Electric Blue" to 0xFF00D2FF,
        "Hyper Cyan" to 0xFF00FFEA,
        "Crimson Laser" to 0xFFFF2E63,
        "Solar Flare" to 0xFFFFC400,
        "Violet Plasma" to 0xFFAA44FF
    )

    val tracks: List<TrackSpec> = listOf(
        TrackSpec(
            id = "futuristic_city",
            index = 0,
            name = "Futuristic City",
            subtitle = "Neon Metropolis • Night Skyscrapers",
            theme = TrackTheme.FUTURISTIC_CITY,
            roadWidth = 22f,
            targetTimeSec = 105,
            unlockCareerStage = 1,
            skyTopColor = 0xFF040818,
            skyHorizonColor = 0xFF0B2248,
            accentHex = 0xFF00D2FF,
            isNight = true,
            isWetRoad = false,
            controlPoints = listOf(
                TrackWaypoint(0f, 0f, 0f),
                TrackWaypoint(0f, 0f, -135f),
                TrackWaypoint(65f, 2f, -220f),
                TrackWaypoint(165f, 4f, -210f),
                TrackWaypoint(215f, 2f, -120f),
                TrackWaypoint(175f, 0f, -20f),
                TrackWaypoint(225f, 3f, 85f),
                TrackWaypoint(170f, 5f, 180f),
                TrackWaypoint(60f, 2f, 195f),
                TrackWaypoint(-10f, 0f, 120f)
            )
        ),
        TrackSpec(
            id = "mountain_highway",
            index = 1,
            name = "Mountain Highway",
            subtitle = "Alpine Passes • Bridges & Tunnels",
            theme = TrackTheme.MOUNTAIN_HIGHWAY,
            roadWidth = 20f,
            targetTimeSec = 112,
            unlockCareerStage = 1,
            skyTopColor = 0xFF2874C8,
            skyHorizonColor = 0xFFA6D8FF,
            accentHex = 0xFF00E676,
            isNight = false,
            isWetRoad = false,
            controlPoints = listOf(
                TrackWaypoint(0f, 0f, 0f),
                TrackWaypoint(0f, 6f, -140f),
                TrackWaypoint(85f, 16f, -230f),
                TrackWaypoint(195f, 22f, -195f),
                TrackWaypoint(245f, 14f, -85f),
                TrackWaypoint(180f, 8f, 15f),
                TrackWaypoint(230f, 12f, 125f),
                TrackWaypoint(140f, 6f, 215f),
                TrackWaypoint(35f, 2f, 180f),
                TrackWaypoint(-25f, 0f, 100f)
            )
        ),
        TrackSpec(
            id = "desert_speedway",
            index = 2,
            name = "Desert Speedway",
            subtitle = "Golden Dunes • Sunset Canyon",
            theme = TrackTheme.DESERT_SPEEDWAY,
            roadWidth = 24f,
            targetTimeSec = 100,
            unlockCareerStage = 2,
            skyTopColor = 0xFF421C52,
            skyHorizonColor = 0xFFFF8C42,
            accentHex = 0xFFFFB800,
            isNight = false,
            isWetRoad = false,
            controlPoints = listOf(
                TrackWaypoint(0f, 0f, 0f),
                TrackWaypoint(10f, 1f, -155f),
                TrackWaypoint(105f, 5f, -245f),
                TrackWaypoint(235f, 3f, -210f),
                TrackWaypoint(265f, 0f, -85f),
                TrackWaypoint(205f, 4f, 35f),
                TrackWaypoint(245f, 7f, 155f),
                TrackWaypoint(140f, 3f, 230f),
                TrackWaypoint(25f, 1f, 175f),
                TrackWaypoint(-15f, 0f, 95f)
            )
        ),
        TrackSpec(
            id = "coastal_highway",
            index = 3,
            name = "Coastal Highway",
            subtitle = "Ocean Breeze • Palms & Beaches",
            theme = TrackTheme.COASTAL_HIGHWAY,
            roadWidth = 22f,
            targetTimeSec = 108,
            unlockCareerStage = 4,
            skyTopColor = 0xFF1789FC,
            skyHorizonColor = 0xFFC5F2FF,
            accentHex = 0xFF00E5FF,
            isNight = false,
            isWetRoad = false,
            controlPoints = listOf(
                TrackWaypoint(0f, 1f, 0f),
                TrackWaypoint(-15f, 2f, -150f),
                TrackWaypoint(65f, 3f, -255f),
                TrackWaypoint(190f, 2f, -240f),
                TrackWaypoint(255f, 1f, -125f),
                TrackWaypoint(220f, 2f, 5f),
                TrackWaypoint(245f, 4f, 130f),
                TrackWaypoint(155f, 2f, 225f),
                TrackWaypoint(45f, 1f, 200f),
                TrackWaypoint(-5f, 1f, 105f)
            )
        ),
        TrackSpec(
            id = "cyberpunk_night",
            index = 4,
            name = "Cyberpunk Night City",
            subtitle = "Wet Reflective Asphalt • Neon Purple",
            theme = TrackTheme.CYBERPUNK_NIGHT,
            roadWidth = 21f,
            targetTimeSec = 110,
            unlockCareerStage = 6,
            skyTopColor = 0xFF0B041A,
            skyHorizonColor = 0xFF310D59,
            accentHex = 0xFFAA44FF,
            isNight = true,
            isWetRoad = true,
            controlPoints = listOf(
                TrackWaypoint(0f, 0f, 0f),
                TrackWaypoint(5f, 2f, -145f),
                TrackWaypoint(90f, 6f, -235f),
                TrackWaypoint(210f, 4f, -190f),
                TrackWaypoint(230f, 1f, -75f),
                TrackWaypoint(165f, 3f, 25f),
                TrackWaypoint(220f, 6f, 135f),
                TrackWaypoint(130f, 4f, 225f),
                TrackWaypoint(30f, 1f, 185f),
                TrackWaypoint(-20f, 0f, 100f)
            )
        )
    )

    val careerStages: List<CareerStageSpec> = listOf(
        CareerStageSpec(1, "Neon Awakening", "futuristic_city", 3, 0.64f, 50, "Unlocks Stage 2 & Desert Circuit Access"),
        CareerStageSpec(2, "Alpine Surge", "mountain_highway", 3, 0.68f, 60, "Unlocks Desert Speedway"),
        CareerStageSpec(3, "Solar Drift", "desert_speedway", 2, 0.73f, 80, "Unlocks Stage 4"),
        CareerStageSpec(4, "Skyline Duel", "futuristic_city", 2, 0.77f, 100, "Unlocks Coastal Highway"),
        CareerStageSpec(5, "Azure Velocity", "coastal_highway", 2, 0.81f, 120, "Unlocks Stage 6"),
        CareerStageSpec(6, "Midnight Rain", "cyberpunk_night", 2, 0.85f, 150, "Unlocks Cyberpunk Night City"),
        CareerStageSpec(7, "Canyon Apex", "desert_speedway", 1, 0.89f, 180, "Master the Sunset Straight"),
        CareerStageSpec(8, "Summit Assault", "mountain_highway", 1, 0.93f, 200, "Conquer the Alpine Tunnel"),
        CareerStageSpec(9, "Pacific Overdrive", "coastal_highway", 1, 0.97f, 240, "Outrun Elite Rivals"),
        CareerStageSpec(10, "Shadow Legend", "cyberpunk_night", 1, 1.02f, 350, "Ultimate Career Mastery")
    )

    val outfits: List<HeroOutfitSpec> = listOf(
        HeroOutfitSpec("shadow_armor", "Shadow Armored Jacket", 0, 0xFF141824, 0xFFB8C6DE, 0xFF00D2FF),
        HeroOutfitSpec("chrome_phantom", "Chrome Phantom Coat", 150, 0xFF1E2638, 0xFFE6EEF9, 0xFF00F0FF),
        HeroOutfitSpec("cyber_ronin", "Cyber Ronin Tactical", 260, 0xFF161028, 0xFF99AACC, 0xFFAA44FF),
        HeroOutfitSpec("crimson_apex", "Apex Strike Suit", 360, 0xFF28121B, 0xFFD8E0EC, 0xFFFF3366)
    )

    val accessories: List<HeroAccessorySpec> = listOf(
        HeroAccessorySpec("pulse_shoulders", "Blue Pulse Pauldrons", 0, 0),
        HeroAccessorySpec("plasma_wings", "Plasma Shoulder Fins", 140, 1),
        HeroAccessorySpec("aero_blades", "Silver Aero Blades", 220, 2),
        HeroAccessorySpec("zero_halo", "Quantum Holo-Ring", 320, 3)
    )

    val auras: List<HeroAuraSpec> = listOf(
        HeroAuraSpec("electric_blue", "Electric Blue Surge", 0, 0xFF00D2FF),
        HeroAuraSpec("hyper_cyan", "Hyper Cyan Volt", 120, 0xFF00FFEA),
        HeroAuraSpec("crimson_nova", "Crimson Overdrive", 220, 0xFFFF3366),
        HeroAuraSpec("solar_gold", "Champion Gold Aura", 350, 0xFFFFB800)
    )

    val achievements: List<AchievementSpec> = listOf(
        AchievementSpec("first_race", "First Race Completed", "Finish your very first Shadow Racer event", 50),
        AchievementSpec("three_streak", "Three Consecutive Victories", "Win 1st place in 3 races in a row", 200),
        AchievementSpec("five_races", "Five Races Completed", "Complete 5 official races", 100),
        AchievementSpec("ten_races", "Ten Races Completed", "Complete 10 official races", 180),
        AchievementSpec("championship_winner", "Championship Winner", "Win 1st overall in Championship Mode", 300)
    )

    val aiRivalNames: List<String> = listOf(
        "Vex Kuro",
        "Nova Takahashi",
        "Zane Vance",
        "Lyra Chen",
        "Ryker Volt"
    )
}

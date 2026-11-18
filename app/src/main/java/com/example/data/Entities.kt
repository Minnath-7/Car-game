package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val username: String = "Kael Shadow",
    val totalPoints: Int = 250,
    val credits: Int = 350,
    val totalRaces: Int = 0,
    val firstPlaces: Int = 0,
    val secondPlaces: Int = 0,
    val thirdPlaces: Int = 0,
    val totalVictories: Int = 0,
    val consecutiveWins: Int = 0,
    val bestRaceTimeMs: Long = 0L,
    val selectedCarId: String = "shadow_fang",
    val selectedOutfitId: String = "shadow_armor",
    val selectedAccessoryId: String = "pulse_shoulders",
    val selectedAuraId: String = "electric_blue",
    val unlockedOutfitsCsv: String = "shadow_armor",
    val unlockedAccessoriesCsv: String = "pulse_shoulders",
    val unlockedAurasCsv: String = "electric_blue",
    val careerStageUnlocked: Int = 1,
    val lastDailyClaimEpochDay: Long = -1L,
    val dailyStreak: Int = 0,
    val championshipWins: Int = 0
)

@Entity(tableName = "car_states")
data class CarStateEntity(
    @PrimaryKey val carId: String,
    val unlocked: Boolean,
    val paintColorHex: Long,
    val neonColorHex: Long,
    val wheelStyleId: Int = 0,
    val spoilerStyleId: Int = 0,
    val speedLevel: Int = 0,
    val accelLevel: Int = 0,
    val handlingLevel: Int = 0,
    val brakingLevel: Int = 0,
    val nitroLevel: Int = 0
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlocked: Boolean = false,
    val unlockedAtMs: Long = 0L
)

@Entity(tableName = "race_history")
data class RaceHistoryEntity(
    @PrimaryKey val raceSessionId: String,
    val modeId: String,
    val trackId: String,
    val carId: String,
    val position: Int,
    val raceTimeMs: Long,
    val pointsEarned: Int,
    val timestampMs: Long
)

@Entity(tableName = "game_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val graphicsQualityId: String = "high",
    val sfxVolume: Float = 0.85f,
    val musicVolume: Float = 0.65f,
    val controlSensitivity: Float = 0.75f,
    val steeringModeId: String = "touch",
    val autoAccelerate: Boolean = false,
    val cameraPerspectiveId: String = "chase",
    val vibrationEnabled: Boolean = true,
    val languageCode: String = "en"
)

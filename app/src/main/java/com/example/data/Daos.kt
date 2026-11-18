package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProfileDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)
}

@Dao
interface CarStateDao {
    @Query("SELECT * FROM car_states")
    fun observeCars(): Flow<List<CarStateEntity>>

    @Query("SELECT * FROM car_states")
    suspend fun getCarsOnce(): List<CarStateEntity>

    @Query("SELECT * FROM car_states WHERE carId = :carId")
    suspend fun getCarOnce(carId: String): CarStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCar(car: CarStateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(cars: List<CarStateEntity>)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAchievementsOnce(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievement(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(achievements: List<AchievementEntity>)
}

@Dao
interface RaceHistoryDao {
    @Query("SELECT * FROM race_history ORDER BY timestampMs DESC LIMIT 50")
    fun observeRecentRaces(): Flow<List<RaceHistoryEntity>>

    @Query("SELECT * FROM race_history WHERE raceSessionId = :sessionId LIMIT 1")
    suspend fun getBySessionId(sessionId: String): RaceHistoryEntity?

    @Query("SELECT MIN(raceTimeMs) FROM race_history WHERE trackId = :trackId AND raceTimeMs > 0")
    suspend fun getBestTimeForTrack(trackId: String): Long?

    @Query("SELECT trackId, MIN(raceTimeMs) as bestTimeMs FROM race_history WHERE raceTimeMs > 0 GROUP BY trackId")
    fun observeBestTimesPerTrack(): Flow<List<TrackBestTimeTuple>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRaceIgnore(race: RaceHistoryEntity): Long
}

data class TrackBestTimeTuple(
    val trackId: String,
    val bestTimeMs: Long
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM game_settings WHERE id = 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM game_settings WHERE id = 1")
    suspend fun getSettingsOnce(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SettingsEntity)
}

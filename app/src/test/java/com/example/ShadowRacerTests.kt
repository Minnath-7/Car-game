package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.engine3d.ParticlePool3D
import com.example.engine3d.RaceSimulation
import com.example.engine3d.RacerInput
import com.example.engine3d.TrackSpline
import com.example.model.GameCatalog
import com.example.model.GameMode
import com.example.model.ScoringRules
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShadowRacerTests {

    @Test
    fun `scoring table matches exact position rewards`() {
        assertEquals(200, ScoringRules.pointsForPosition(1))
        assertEquals(150, ScoringRules.pointsForPosition(2))
        assertEquals(100, ScoringRules.pointsForPosition(3))
        assertEquals(50, ScoringRules.pointsForPosition(4))
        assertEquals(25, ScoringRules.pointsForPosition(5))
        assertEquals(10, ScoringRules.pointsForPosition(6))
    }

    @Test
    fun `all 5 tracks build valid closed splines`() {
        assertEquals(5, GameCatalog.tracks.size)
        for (track in GameCatalog.tracks) {
            val spline = TrackSpline(track)
            assertTrue("Track ${track.name} length > 500", spline.totalLength > 500f)
            assertTrue("Track ${track.name} samples > 100", spline.samples.size > 100)
        }
    }

    @Test
    fun `AI opponents navigate track and complete 3 laps`() {
        val track = GameCatalog.tracks[0]
        val car = GameCatalog.cars[0]
        val sim = RaceSimulation(
            trackSpec = track,
            mode = GameMode.QUICK_RACE,
            playerCarSpec = car,
            playerBodyColor = car.defaultBodyColor,
            playerNeonColor = car.defaultNeonColor,
            playerWheelStyle = 0,
            playerSpoilerStyle = 0,
            speedUpgradeLvl = 0,
            accelUpgradeLvl = 0,
            handlingUpgradeLvl = 0,
            brakingUpgradeLvl = 0,
            nitroUpgradeLvl = 0,
            aiDifficulty = 0.95f
        )
        val particles = ParticlePool3D(64)
        // Step ~110 seconds of simulated 3-lap race at 30Hz
        val dt = 1f / 30f
        for (step in 0 until (110 * 30)) {
            sim.step(dt, RacerInput(accelerate = true), particles)
            if (sim.racers[1].finished) break
        }
        assertEquals(6, sim.racers.size)
        assertTrue("AI racer #1 should finish 3 laps", sim.racers[1].finished || sim.racers[1].currentLap >= 3)
    }

    @Test
    fun `repository saves race points once and blocks duplicate reward`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Shadow Racer", context.getString(R.string.app_name))

        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = GameRepository(db)
            repo.ensureInitialized()

            val initialProfile = db.playerProfileDao().getProfileOnce()!!
            val sessionId = "test-race-session-001"

            val firstOutcome = repo.recordCompletedRace(
                raceSessionId = sessionId,
                mode = GameMode.QUICK_RACE,
                trackId = "futuristic_city",
                carId = "shadow_fang",
                position = 1,
                raceTimeMs = 82_500L,
                careerStagePlayed = null,
                wonChampionshipOverall = false
            )
            assertFalse(firstOutcome.duplicateBlocked)
            assertEquals(200, firstOutcome.pointsEarned)
            assertTrue(firstOutcome.newlyUnlockedAchievements.any { it.id == "first_race" })

            val afterFirst = db.playerProfileDao().getProfileOnce()!!
            assertEquals(initialProfile.totalPoints + 250, afterFirst.totalPoints) // 200 + 50 first_race achievement
            assertEquals(1, afterFirst.totalRaces)
            assertEquals(1, afterFirst.firstPlaces)

            // Attempt duplicate claim with identical sessionId
            val dupOutcome = repo.recordCompletedRace(
                raceSessionId = sessionId,
                mode = GameMode.QUICK_RACE,
                trackId = "futuristic_city",
                carId = "shadow_fang",
                position = 1,
                raceTimeMs = 82_500L,
                careerStagePlayed = null,
                wonChampionshipOverall = false
            )
            assertTrue(dupOutcome.duplicateBlocked)
            val afterDup = db.playerProfileDao().getProfileOnce()!!
            assertEquals(afterFirst.totalPoints, afterDup.totalPoints)
            assertEquals(1, afterDup.totalRaces)
        } finally {
            db.close()
        }
    }
}

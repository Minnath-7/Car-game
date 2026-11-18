package com.example.engine3d

import com.example.model.CarSpec
import com.example.model.GameCatalog
import com.example.model.GameMode
import com.example.model.TrackSpec
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class RacerInput(
    val accelerate: Boolean = false,
    val brake: Boolean = false,
    val steerLeft: Boolean = false,
    val steerRight: Boolean = false,
    val tiltSteer: Float = 0f, // -1..1 from gyroscope/accelerometer
    val nitro: Boolean = false,
    val drift: Boolean = false,
    val autoAccelerate: Boolean = false,
    val sensitivity: Float = 0.75f
)

data class RacerState(
    val index: Int,
    val name: String,
    val isPlayer: Boolean,
    val carSpec: CarSpec,
    val bodyColorHex: Long,
    val neonColorHex: Long,
    val wheelStyleId: Int,
    val spoilerStyleId: Int,
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var yawRad: Float = 0f,
    var speed: Float = 0f,         // world units / sec (1 unit ~ 3.6 km/h display scale)
    var driftAngleRad: Float = 0f,
    var rollDeg: Float = 0f,
    var steerVisualDeg: Float = 0f,
    var wheelSpinDeg: Float = 0f,
    var nitroFuel: Float = 100f,   // 0..100
    var nitroActive: Boolean = false,
    var isDrifting: Boolean = false,
    var isBraking: Boolean = false,
    var closestSampleIdx: Int = 0,
    var currentLap: Int = 1,
    var nextCheckpoint: Int = 1,   // 1, 2, 3, then 0 (finish)
    var lapProgressS: Float = 0f,
    var totalProgress: Float = 0f,
    var finished: Boolean = false,
    var finishTimeMs: Long = 0L,
    // AI parameters
    var aiTargetLane: Float = 0f,
    var aiSkillFactor: Float = 0.8f,
    var aiLaneTimer: Float = 0f
)

data class MiniMapDot(
    val normX: Float,
    val normZ: Float,
    val isPlayer: Boolean
)

data class RaceHudSnapshot(
    val raceSessionId: String = "",
    val mode: GameMode = GameMode.QUICK_RACE,
    val trackName: String = "",
    val countdownValue: Int = 3, // 3, 2, 1, 0 (GO!), -1 (Racing)
    val currentLap: Int = 1,
    val totalLaps: Int = 3,
    val position: Int = 1,
    val totalRacers: Int = 6,
    val speedKmh: Int = 0,
    val nitroPercent: Float = 100f,
    val raceTimeMs: Long = 0L,
    val timeTrialRemainingMs: Long = 0L,
    val checkpointIndex: Int = 0,
    val isDrifting: Boolean = false,
    val isNitroActive: Boolean = false,
    val topSpeedKmh: Int = 0,
    val driftScore: Int = 0,
    val finished: Boolean = false,
    val timeTrialFailed: Boolean = false,
    val miniMapOutline: List<Pair<Float, Float>> = emptyList(),
    val miniMapDots: List<MiniMapDot> = emptyList()
)

class RaceSimulation(
    val trackSpec: TrackSpec,
    val mode: GameMode,
    val playerCarSpec: CarSpec,
    val playerBodyColor: Long,
    val playerNeonColor: Long,
    val playerWheelStyle: Int,
    val playerSpoilerStyle: Int,
    val speedUpgradeLvl: Int,
    val accelUpgradeLvl: Int,
    val handlingUpgradeLvl: Int,
    val brakingUpgradeLvl: Int,
    val nitroUpgradeLvl: Int,
    val aiDifficulty: Float = 0.78f,
    val playerName: String = "Kael Shadow"
) {
    val raceSessionId: String = UUID.randomUUID().toString()
    val spline: TrackSpline = TrackSpline(trackSpec)
    val totalLaps: Int = 3
    val racers: List<RacerState>

    var phaseCountdownTimer: Float = 3.6f // 3.6s -> 3, 2, 1, GO!
        private set
    var raceElapsedMs: Long = 0L
        private set
    var isPaused: Boolean = false
    var topSpeedRecordedKmh: Int = 0
        private set
    var driftScore: Int = 0
        private set
    var raceFinished: Boolean = false
        private set
    var timeTrialExpired: Boolean = false
        private set

    private var lastBeepSecond: Int = 4
    private var minX = Float.MAX_VALUE
    private var maxX = -Float.MAX_VALUE
    private var minZ = Float.MAX_VALUE
    private var maxZ = -Float.MAX_VALUE
    private val mapOutline: List<Pair<Float, Float>>

    var onCountdownTick: ((isGo: Boolean) -> Unit)? = null
    var onCollisionSfx: ((heavy: Boolean) -> Unit)? = null
    var onRaceCompleted: ((RaceHudSnapshot) -> Unit)? = null

    init {
        for (sp in spline.samples) {
            if (sp.x < minX) minX = sp.x
            if (sp.x > maxX) maxX = sp.x
            if (sp.z < minZ) minZ = sp.z
            if (sp.z > maxZ) maxZ = sp.z
        }
        val spanX = (maxX - minX).coerceAtLeast(1f)
        val spanZ = (maxZ - minZ).coerceAtLeast(1f)
        val span = maxOf(spanX, spanZ)
        val cx = (minX + maxX) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        mapOutline = spline.samples.filterIndexed { idx, _ -> idx % 5 == 0 }.map { sp ->
            ((sp.x - cx) / span + 0.5f) to ((sp.z - cz) / span + 0.5f)
        }

        val list = mutableListOf<RacerState>()
        val isSolo = mode == GameMode.TIME_TRIAL
        val gridCount = if (isSolo) 1 else 6

        // Spawn grid just behind start line
        for (i in 0 until gridCount) {
            val isPlayer = i == 0
            val gridSlot = if (isSolo) 0 else (gridCount - 1 - i) // Player starts P6 in race for exciting overtakes!
            val backDist = 8f + (gridSlot / 2) * 8.5f
            val laneOffset = if (gridSlot % 2 == 0) -3.4f else 3.4f
            val spawnSample = spline.sampleAtDistance(spline.totalLength - backDist)
            val spawnIdx = spline.findClosestIndex(spawnSample.x, spawnSample.z)

            val carSpec = if (isPlayer) {
                playerCarSpec
            } else {
                val aiIdx = (i + trackSpec.index) % GameCatalog.cars.size
                GameCatalog.cars[aiIdx]
            }

            list.add(
                RacerState(
                    index = i,
                    name = if (isPlayer) playerName else GameCatalog.aiRivalNames[(i - 1) % GameCatalog.aiRivalNames.size],
                    isPlayer = isPlayer,
                    carSpec = carSpec,
                    bodyColorHex = if (isPlayer) playerBodyColor else carSpec.defaultBodyColor,
                    neonColorHex = if (isPlayer) playerNeonColor else carSpec.defaultNeonColor,
                    wheelStyleId = if (isPlayer) playerWheelStyle else (i % 4),
                    spoilerStyleId = if (isPlayer) playerSpoilerStyle else (i % 4),
                    x = spawnSample.x + spawnSample.rx * laneOffset,
                    y = spawnSample.y,
                    z = spawnSample.z + spawnSample.rz * laneOffset,
                    yawRad = spawnSample.yawRad,
                    closestSampleIdx = spawnIdx,
                    currentLap = 1,
                    nextCheckpoint = 1,
                    lapProgressS = -backDist,
                    totalProgress = -backDist,
                    aiTargetLane = if (i % 2 == 0) -3.2f else 3.2f,
                    aiSkillFactor = (aiDifficulty + (5 - i) * 0.032f).coerceIn(0.58f, 1.05f),
                    aiLaneTimer = 1.5f + i * 0.6f
                )
            )
        }
        racers = list
    }

    fun step(dtRaw: Float, input: RacerInput, particles: ParticlePool3D) {
        if (isPaused || raceFinished) return
        val dt = dtRaw.coerceIn(0.001f, 0.05f)

        // Countdown phase
        if (phaseCountdownTimer > 0f) {
            phaseCountdownTimer -= dt
            val sec = when {
                phaseCountdownTimer > 2.6f -> 3
                phaseCountdownTimer > 1.6f -> 2
                phaseCountdownTimer > 0.6f -> 1
                phaseCountdownTimer > 0f -> 0 // GO!
                else -> -1
            }
            if (sec in 0..3 && sec < lastBeepSecond) {
                lastBeepSecond = sec
                onCountdownTick?.invoke(sec == 0)
            }
            if (phaseCountdownTimer > 0.6f) {
                return // Hold cars on grid during 3-2-1
            }
        }

        raceElapsedMs += (dt * 1000f).toLong()

        // Time Trial expiration check
        if (mode == GameMode.TIME_TRIAL) {
            val limitMs = trackSpec.targetTimeSec * 1000L
            if (raceElapsedMs >= limitMs && !raceFinished) {
                timeTrialExpired = true
                raceFinished = true
                onRaceCompleted?.invoke(buildHudSnapshot())
                return
            }
        }

        // Update each racer
        for (r in racers) {
            if (r.finished) {
                // Coast smoothly after crossing finish
                r.speed *= 0.96f
                moveCarOnTrack(r, dt)
                continue
            }
            if (r.isPlayer) {
                updatePlayerRacer(r, dt, input, particles)
            } else {
                updateAiRacer(r, dt, particles)
            }
            updateLapAndCheckpoints(r)
        }

        // Car-to-car collision resolution
        resolveCollisions(particles)

        // Check if player finished
        val player = racers[0]
        if (player.finished && !raceFinished) {
            raceFinished = true
            // Assign finish times to any remaining AI racers based on gap
            for (r in racers) {
                if (!r.finished) {
                    val remDist = (totalLaps * spline.totalLength - r.totalProgress).coerceAtLeast(10f)
                    r.finishTimeMs = raceElapsedMs + (remDist / 45f * 1000f).toLong()
                    r.finished = true
                }
            }
            onRaceCompleted?.invoke(buildHudSnapshot())
        }
    }

    private fun updatePlayerRacer(r: RacerState, dt: Float, input: RacerInput, particles: ParticlePool3D) {
        val maxSpeedUnits = (r.carSpec.baseMaxSpeed + speedUpgradeLvl * 11f) / 3.4f
        val accelStat = r.carSpec.baseAcceleration + accelUpgradeLvl * 0.045f
        val handlingStat = r.carSpec.baseHandling + handlingUpgradeLvl * 0.045f
        val brakingStat = r.carSpec.baseBraking + brakingUpgradeLvl * 0.05f
        val nitroCapBonus = 1f + nitroUpgradeLvl * 0.12f

        val wantsAccel = input.accelerate || input.autoAccelerate
        val wantsBrake = input.brake
        val buttonSteer = (if (input.steerLeft) 1f else 0f) + (if (input.steerRight) -1f else 0f)
        val rawSteer = (buttonSteer + input.tiltSteer).coerceIn(-1f, 1f) * (0.65f + input.sensitivity * 0.65f)
        val steerInput = rawSteer.coerceIn(-1.15f, 1.15f)

        val canNitro = input.nitro && r.nitroFuel > 2f && wantsAccel
        r.nitroActive = canNitro
        if (canNitro) {
            r.nitroFuel = (r.nitroFuel - (26f / nitroCapBonus) * dt).coerceAtLeast(0f)
        } else {
            r.nitroFuel = (r.nitroFuel + 4.5f * dt).coerceAtMost(100f)
        }

        val effectiveTopSpeed = maxSpeedUnits * (if (canNitro) 1.24f else 1.0f)
        val accelForce = (34f + accelStat * 28f) * (if (canNitro) 1.55f else 1.0f)

        r.isBraking = wantsBrake && r.speed > 2f
        if (wantsAccel && !wantsBrake) {
            r.speed += accelForce * dt
        } else if (wantsBrake) {
            r.speed -= (45f + brakingStat * 35f) * dt
        } else {
            r.speed -= 12f * dt
        }
        r.speed = r.speed.coerceIn(-18f, effectiveTopSpeed)

        val speedRatio = (abs(r.speed) / maxSpeedUnits).coerceIn(0f, 1.2f)
        val drifting = input.drift && abs(steerInput) > 0.15f && r.speed > 18f
        r.isDrifting = drifting

        val turnRate = (1.55f + handlingStat * 0.85f) * (0.35f + 0.65f * speedRatio.coerceAtMost(1f)) * (if (drifting) 1.32f else 1.0f)
        val dirSign = if (r.speed >= -0.5f) 1f else -1f
        r.yawRad = TrackSpline.wrapAngleRad(r.yawRad + steerInput * turnRate * dirSign * dt)

        val targetDriftAngle = if (drifting) steerInput * 0.34f else 0f
        r.driftAngleRad += (targetDriftAngle - r.driftAngleRad) * (8f * dt).coerceAtMost(1f)
        r.steerVisualDeg += (steerInput * 26f - r.steerVisualDeg) * (12f * dt).coerceAtMost(1f)
        r.rollDeg += (-steerInput * speedRatio * 4.5f - r.rollDeg) * (8f * dt).coerceAtMost(1f)

        if (drifting) {
            r.nitroFuel = (r.nitroFuel + 12f * dt).coerceAtMost(100f)
            driftScore += (speedRatio * 40f * dt).toInt().coerceAtLeast(1)
            particles.emitTireSmoke(r.x - cos(r.yawRad) * 0.9f, r.y, r.z + sin(r.yawRad) * 0.9f)
            particles.emitTireSmoke(r.x + cos(r.yawRad) * 0.9f, r.y, r.z - sin(r.yawRad) * 0.9f)
        }

        if (canNitro) {
            val backX = -sin(r.yawRad)
            val backZ = -cos(r.yawRad)
            particles.emitNitroFlame(r.x + backX * 2.3f, r.y + 0.35f, r.z + backZ * 2.3f, backX, backZ)
        }

        moveCarOnTrack(r, dt)

        val kmh = abs(r.speed * 3.4f).toInt()
        if (kmh > topSpeedRecordedKmh) topSpeedRecordedKmh = kmh
    }

    private fun updateAiRacer(r: RacerState, dt: Float, particles: ParticlePool3D) {
        val sampleCount = spline.samples.size
        val lookAheadIdx = (r.closestSampleIdx + 10) % sampleCount
        val farAheadIdx = (r.closestSampleIdx + 22) % sampleCount
        val targetSp = spline.samples[lookAheadIdx]
        val farSp = spline.samples[farAheadIdx]

        // Periodic lane shifting & overtaking avoidance
        r.aiLaneTimer -= dt
        val halfRoad = trackSpec.roadWidth * 0.38f
        if (r.aiLaneTimer <= 0f) {
            r.aiLaneTimer = 1.6f + Random.nextFloat() * 2.2f
            r.aiTargetLane = (Random.nextFloat() * 2f - 1f) * halfRoad
        }

        // Check if another car is directly ahead -> shift lane to overtake!
        for (other in racers) {
            if (other.index == r.index) continue
            val dz = other.x - r.x
            val dw = other.z - r.z
            val distSq = dz * dz + dw * dw
            if (distSq < 180f && other.totalProgress > r.totalProgress) {
                r.aiTargetLane = if (r.aiTargetLane >= 0f) -halfRoad * 0.85f else halfRoad * 0.85f
                break
            }
        }

        val tx = targetSp.x + targetSp.rx * r.aiTargetLane
        val tz = targetSp.z + targetSp.rz * r.aiTargetLane
        val desiredYaw = atan2(tx - r.x, tz - r.z)
        val yawErr = TrackSpline.wrapAngleRad(desiredYaw - r.yawRad)

        val maxAiSpeed = ((r.carSpec.baseMaxSpeed * 0.96f) / 3.4f) * r.aiSkillFactor
        // Corner slow-down based on upcoming curvature
        val curveErr = abs(TrackSpline.wrapAngleRad(farSp.yawRad - targetSp.yawRad))
        val cornerFactor = (1f - curveErr * 0.45f).coerceIn(0.62f, 1.0f)

        val useNitro = curveErr < 0.14f && r.nitroFuel > 35f && r.aiSkillFactor > 0.75f
        r.nitroActive = useNitro
        if (useNitro) {
            r.nitroFuel = (r.nitroFuel - 20f * dt).coerceAtLeast(0f)
            val backX = -sin(r.yawRad)
            val backZ = -cos(r.yawRad)
            particles.emitNitroFlame(r.x + backX * 2.3f, r.y + 0.35f, r.z + backZ * 2.3f, backX, backZ)
        } else {
            r.nitroFuel = (r.nitroFuel + 5f * dt).coerceAtMost(100f)
        }

        val targetSpeed = maxAiSpeed * cornerFactor * (if (useNitro) 1.15f else 1.0f)
        if (r.speed < targetSpeed) {
            r.speed += (38f + r.carSpec.baseAcceleration * 20f) * dt
            r.isBraking = false
        } else {
            r.speed -= 28f * dt
            r.isBraking = r.speed - targetSpeed > 8f
        }
        r.speed = r.speed.coerceIn(0f, maxAiSpeed * 1.18f)

        val steerCmd = (yawErr * 2.4f).coerceIn(-1f, 1f)
        r.yawRad = TrackSpline.wrapAngleRad(r.yawRad + steerCmd * 2.6f * dt)
        r.steerVisualDeg = steerCmd * 22f
        r.rollDeg = -steerCmd * 3.5f

        moveCarOnTrack(r, dt)
    }

    private fun moveCarOnTrack(r: RacerState, dt: Float) {
        val moveHeading = r.yawRad + r.driftAngleRad * 0.35f
        r.x += sin(moveHeading) * r.speed * dt
        r.z += cos(moveHeading) * r.speed * dt
        r.wheelSpinDeg = (r.wheelSpinDeg + r.speed * 58f * dt) % 360f

        // Project onto spline for elevation Y & guardrail wall containment
        val closestIdx = spline.findClosestIndex(r.x, r.z, r.closestSampleIdx)
        r.closestSampleIdx = closestIdx
        val sp = spline.samples[closestIdx]
        r.y = sp.y

        // Lateral distance from centerline
        val dx = r.x - sp.x
        val dz = r.z - sp.z
        val lat = dx * sp.rx + dz * sp.rz
        val maxLat = trackSpec.roadWidth * 0.5f - 0.6f
        if (abs(lat) > maxLat) {
            val clamped = lat.coerceIn(-maxLat, maxLat)
            val corr = clamped - lat
            r.x += sp.rx * corr
            r.z += sp.rz * corr
            r.speed *= 0.88f
            // Gently align heading with track tangent on wall scrape
            val yawDiff = TrackSpline.wrapAngleRad(sp.yawRad - r.yawRad)
            r.yawRad = TrackSpline.wrapAngleRad(r.yawRad + yawDiff * 0.25f)
            if (r.isPlayer && abs(corr) > 0.15f) {
                onCollisionSfx?.invoke(false)
            }
        }
    }

    private fun updateLapAndCheckpoints(r: RacerState) {
        val count = spline.samples.size
        val frac = r.closestSampleIdx.toFloat() / count
        val s = spline.samples[r.closestSampleIdx].s

        // 4 sequential checkpoints: 0.25, 0.50, 0.75, 0.00 (finish)
        when (r.nextCheckpoint) {
            1 -> if (frac in 0.20f..0.42f) r.nextCheckpoint = 2
            2 -> if (frac in 0.45f..0.68f) r.nextCheckpoint = 3
            3 -> if (frac in 0.70f..0.92f) r.nextCheckpoint = 0
            0 -> if (frac < 0.15f) {
                r.currentLap++
                r.nextCheckpoint = 1
                if (r.currentLap > totalLaps) {
                    r.finished = true
                    r.finishTimeMs = raceElapsedMs
                }
            }
        }

        // Smooth total progress for live 1..6 ranking
        val effectiveLap = (r.currentLap - 1).coerceAtLeast(0)
        // Handle pre-start grid negative progress
        val adjustedS = if (r.currentLap == 1 && r.nextCheckpoint == 1 && frac > 0.85f) {
            s - spline.totalLength
        } else {
            s
        }
        r.lapProgressS = adjustedS
        r.totalProgress = effectiveLap * spline.totalLength + adjustedS
    }

    private fun resolveCollisions(particles: ParticlePool3D) {
        val minSep = 2.4f
        val minSepSq = minSep * minSep
        for (i in 0 until racers.size) {
            val a = racers[i]
            for (j in i + 1 until racers.size) {
                val b = racers[j]
                val dx = b.x - a.x
                val dz = b.z - a.z
                val dSq = dx * dx + dz * dz
                if (dSq in 0.0001f..minSepSq) {
                    val dist = sqrt(dSq)
                    val nx = dx / dist
                    val nz = dz / dist
                    val push = (minSep - dist) * 0.52f
                    a.x -= nx * push
                    a.z -= nz * push
                    b.x += nx * push
                    b.z += nz * push
                    a.speed *= 0.93f
                    b.speed *= 0.93f
                    particles.emitSparks((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f, (a.z + b.z) * 0.5f, 8)
                    if (a.isPlayer || b.isPlayer) {
                        onCollisionSfx?.invoke(true)
                    }
                }
            }
        }
    }

    fun getPlayerPosition(): Int {
        val p = racers[0]
        var pos = 1
        for (i in 1 until racers.size) {
            val o = racers[i]
            val oAhead = when {
                o.finished && !p.finished -> true
                !o.finished && p.finished -> false
                o.finished && p.finished -> o.finishTimeMs < p.finishTimeMs
                else -> o.totalProgress > p.totalProgress
            }
            if (oAhead) pos++
        }
        return pos
    }

    fun getStandingsOrder(): List<RacerState> {
        return racers.sortedWith(compareBy<RacerState> { !it.finished }
            .thenBy { if (it.finished) it.finishTimeMs else Long.MAX_VALUE }
            .thenByDescending { it.totalProgress })
    }

    fun buildHudSnapshot(): RaceHudSnapshot {
        val player = racers[0]
        val countdownDisplay = when {
            phaseCountdownTimer > 2.6f -> 3
            phaseCountdownTimer > 1.6f -> 2
            phaseCountdownTimer > 0.6f -> 1
            phaseCountdownTimer > 0f -> 0
            else -> -1
        }
        val spanX = (maxX - minX).coerceAtLeast(1f)
        val spanZ = (maxZ - minZ).coerceAtLeast(1f)
        val span = maxOf(spanX, spanZ)
        val cx = (minX + maxX) * 0.5f
        val cz = (minZ + maxZ) * 0.5f

        val dots = racers.map { r ->
            MiniMapDot(
                normX = (r.x - cx) / span + 0.5f,
                normZ = (r.z - cz) / span + 0.5f,
                isPlayer = r.isPlayer
            )
        }

        val limitMs = trackSpec.targetTimeSec * 1000L
        val remMs = (limitMs - raceElapsedMs).coerceAtLeast(0L)

        return RaceHudSnapshot(
            raceSessionId = raceSessionId,
            mode = mode,
            trackName = trackSpec.name,
            countdownValue = countdownDisplay,
            currentLap = player.currentLap.coerceAtMost(totalLaps),
            totalLaps = totalLaps,
            position = getPlayerPosition(),
            totalRacers = racers.size,
            speedKmh = abs(player.speed * 3.4f).toInt(),
            nitroPercent = player.nitroFuel,
            raceTimeMs = raceElapsedMs,
            timeTrialRemainingMs = remMs,
            checkpointIndex = player.nextCheckpoint,
            isDrifting = player.isDrifting,
            isNitroActive = player.nitroActive,
            topSpeedKmh = topSpeedRecordedKmh,
            driftScore = driftScore,
            finished = raceFinished,
            timeTrialFailed = timeTrialExpired,
            miniMapOutline = mapOutline,
            miniMapDots = dots
        )
    }
}

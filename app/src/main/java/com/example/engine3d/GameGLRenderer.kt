package com.example.engine3d

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.model.CameraPerspective
import com.example.model.CarSpec
import com.example.model.GameCatalog
import com.example.model.GraphicsQuality
import com.example.model.HeroAnimation
import com.example.model.HeroViewAngle
import com.example.model.TrackSpec
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

enum class SceneMode {
    HOME_SHOWCASE,
    GARAGE_CAR,
    HERO_STUDIO,
    RACE,
    RESULTS_PODIUM
}

data class ShowcaseConfig(
    val carSpec: CarSpec = GameCatalog.cars[0],
    val bodyColorHex: Long = GameCatalog.cars[0].defaultBodyColor,
    val neonColorHex: Long = GameCatalog.cars[0].defaultNeonColor,
    val wheelStyleId: Int = 0,
    val spoilerStyleId: Int = 0,
    val jacketColorHex: Long = GameCatalog.outfits[0].jacketColor,
    val armorColorHex: Long = GameCatalog.outfits[0].armorColor,
    val trimColorHex: Long = GameCatalog.outfits[0].trimColor,
    val accessoryStyle: Int = 0,
    val auraColorHex: Long = GameCatalog.auras[0].colorHex,
    val heroAnimation: HeroAnimation = HeroAnimation.IDLE,
    val heroViewAngle: HeroViewAngle = HeroViewAngle.FRONT,
    val quality: GraphicsQuality = GraphicsQuality.HIGH
)

class GameGLRenderer : GLSurfaceView.Renderer {
    @Volatile var sceneMode: SceneMode = SceneMode.HOME_SHOWCASE
    @Volatile var cameraPerspective: CameraPerspective = CameraPerspective.CHASE
    @Volatile var dragYawOffsetDeg: Float = 0f

    @Volatile var raceSimulation: RaceSimulation? = null
    @Volatile var currentInput: RacerInput = RacerInput()
    @Volatile var onHudUpdate: ((RaceHudSnapshot) -> Unit)? = null
    @Volatile var onRaceEngineSound: ((speedNorm: Float, drifting: Boolean, nitro: Boolean, accel: Boolean) -> Unit)? = null

    private var showcaseConfig: ShowcaseConfig = ShowcaseConfig()
    private var showcaseDirty = true
    private var trackDirty = true
    private var activeTrackSpec: TrackSpec = GameCatalog.tracks[0]
    private var activeQuality: GraphicsQuality = GraphicsQuality.HIGH

    private lateinit var shader: ShaderProgram
    private var showcaseCarRig: CarRig3D? = null
    private var showcaseHeroRig: HeroRig3D? = null
    private var showcaseStageMesh: Mesh? = null
    private var trackScene: TrackScene3D? = null
    private var raceCarRigs: List<CarRig3D> = emptyList()
    private var raceRigsBuiltForSession: String = ""

    private val particles = ParticlePool3D(280)

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelScratch = FloatArray(16)
    private val wheelScratch = FloatArray(16)
    private val mvpScratch = FloatArray(16)

    private var aspectRatio = 1.6f
    private var lastFrameNs = System.nanoTime()
    private var totalTimeSec = 0f
    private var hudPushTimer = 0f

    // Smooth camera state
    private var camX = 0f
    private var camY = 5f
    private var camZ = 10f
    private var lookX = 0f
    private var lookY = 1f
    private var lookZ = 0f

    fun updateShowcase(config: ShowcaseConfig) {
        if (config != showcaseConfig) {
            showcaseConfig = config
            showcaseDirty = true
            if (config.quality != activeQuality) {
                activeQuality = config.quality
                trackDirty = true
            }
        }
    }

    fun attachRaceSimulation(sim: RaceSimulation) {
        raceSimulation = sim
        activeTrackSpec = sim.trackSpec
        trackDirty = true
        particles.clear()
        val p = sim.racers[0]
        camX = p.x - sin(p.yawRad) * 10f
        camY = p.y + 4.5f
        camZ = p.z - cos(p.yawRad) * 10f
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        shader = ShaderProgram()
        buildShowcaseStageMesh()
        showcaseDirty = true
        trackDirty = true
        raceRigsBuiltForSession = ""
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspectRatio = width.toFloat() / height.coerceAtLeast(1).toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNs = nowNs
        totalTimeSec += dt

        if (showcaseDirty) {
            rebuildShowcaseAssets()
            showcaseDirty = false
        }

        val sim = raceSimulation
        if (sceneMode == SceneMode.RACE && sim != null) {
            if (trackDirty || raceRigsBuiltForSession != sim.raceSessionId) {
                trackScene = TrackMeshBuilder.buildTrackScene(sim.spline, activeQuality)
                raceCarRigs = sim.racers.map { r ->
                    CarMeshBuilder.buildCar(
                        r.carSpec.silhouette,
                        r.bodyColorHex,
                        r.neonColorHex,
                        r.wheelStyleId,
                        r.spoilerStyleId
                    )
                }
                raceRigsBuiltForSession = sim.raceSessionId
                trackDirty = false
            }
            sim.step(dt, currentInput, particles)
            particles.update(dt)
            renderRaceScene(sim, dt)

            hudPushTimer += dt
            if (hudPushTimer >= 0.032f) {
                hudPushTimer = 0f
                val snap = sim.buildHudSnapshot()
                onHudUpdate?.invoke(snap)
                val p = sim.racers[0]
                onRaceEngineSound?.invoke(
                    (p.speed / (p.carSpec.baseMaxSpeed / 3.4f)).coerceIn(0f, 1.3f),
                    p.isDrifting,
                    p.nitroActive,
                    currentInput.accelerate || currentInput.autoAccelerate
                )
            }
        } else {
            if (trackDirty) {
                val previewSpline = TrackSpline(GameCatalog.tracks[0], samplesPerSegment = 20)
                trackScene = TrackMeshBuilder.buildTrackScene(previewSpline, activeQuality)
                trackDirty = false
            }
            particles.update(dt)
            when (sceneMode) {
                SceneMode.HOME_SHOWCASE -> renderHomeShowcase()
                SceneMode.GARAGE_CAR -> renderGarageCar()
                SceneMode.HERO_STUDIO -> renderHeroStudio()
                SceneMode.RESULTS_PODIUM -> renderResultsPodium()
                SceneMode.RACE -> renderHomeShowcase()
            }
        }
    }

    private fun renderHomeShowcase() {
        setEnvironmentUniforms(GameCatalog.tracks[0], fogDensity = 0.0045f)
        val angle = totalTimeSec * 0.22f
        val eyeX = sin(angle) * 7.6f
        val eyeZ = cos(angle) * 7.6f
        val eyeY = 2.5f + sin(totalTimeSec * 0.5f) * 0.25f
        setupCamera(eyeX, eyeY, eyeZ, 0.2f, 1.1f, 0f, fov = 50f)

        // Draw futuristic city skyline & clouds in background
        trackScene?.let { ts ->
            Matrix.setIdentityM(modelScratch, 0)
            Matrix.translateM(modelScratch, 0, -25f, -3.5f, -35f)
            drawWithTransform(ts.sceneryMesh, shader, vpMatrix, modelScratch, mvpScratch, emissive = 0.1f)
            drawWithTransform(ts.emissivePropsMesh, shader, vpMatrix, modelScratch, mvpScratch, emissive = 1.2f)

            Matrix.setIdentityM(modelScratch, 0)
            Matrix.rotateM(modelScratch, 0, totalTimeSec * 1.4f, 0f, 1f, 0f)
            drawWithTransform(ts.cloudMesh, shader, vpMatrix, modelScratch, mvpScratch, emissive = 0.15f)
        }

        // Neon Showcase Platform
        showcaseStageMesh?.let {
            Matrix.setIdentityM(modelScratch, 0)
            drawWithTransform(it, shader, vpMatrix, modelScratch, mvpScratch, emissive = 0.35f, specStrength = 0.7f)
        }

        // Hero Kael standing beside his signature sports car
        showcaseHeroRig?.let {
            HeroMeshBuilder.drawHero(
                rig = it,
                shader = shader,
                vpMatrix = vpMatrix,
                worldX = -1.85f,
                worldY = 0f,
                worldZ = 0.85f,
                yawDeg = 18f,
                animation = HeroAnimation.IDLE,
                timeSec = totalTimeSec,
                scale = 1.15f
            )
        }

        showcaseCarRig?.let {
            CarMeshBuilder.drawCar(
                rig = it,
                shader = shader,
                vpMatrix = vpMatrix,
                worldX = 0.95f,
                worldY = 0f,
                worldZ = -0.25f,
                yawDeg = -22f,
                pitchDeg = 0f,
                rollDeg = 0f,
                steerVisualDeg = 14f,
                wheelSpinDeg = 0f,
                isBraking = false,
                scratchModel = modelScratch,
                scratchWheel = wheelScratch,
                scratchMvp = mvpScratch
            )
        }
    }

    private fun renderGarageCar() {
        setEnvironmentUniforms(GameCatalog.tracks[0], fogDensity = 0.003f)
        val angleRad = TrackSpline.degToRad(dragYawOffsetDeg) + totalTimeSec * 0.42f
        val eyeX = sin(angleRad) * 6.4f
        val eyeZ = cos(angleRad) * 6.4f
        setupCamera(eyeX, 2.2f, eyeZ, 0f, 0.75f, 0f, fov = 46f)

        showcaseStageMesh?.let {
            Matrix.setIdentityM(modelScratch, 0)
            drawWithTransform(it, shader, vpMatrix, modelScratch, mvpScratch, emissive = 0.4f, specStrength = 0.8f)
        }

        showcaseCarRig?.let {
            CarMeshBuilder.drawCar(
                rig = it,
                shader = shader,
                vpMatrix = vpMatrix,
                worldX = 0f,
                worldY = 0f,
                worldZ = 0f,
                yawDeg = 0f,
                pitchDeg = 0f,
                rollDeg = 0f,
                steerVisualDeg = 12f,
                wheelSpinDeg = 0f,
                isBraking = false,
                scratchModel = modelScratch,
                scratchWheel = wheelScratch,
                scratchMvp = mvpScratch
            )
        }
    }

    private fun renderHeroStudio() {
        setEnvironmentUniforms(GameCatalog.tracks[0], fogDensity = 0.002f)
        val viewAngle = showcaseConfig.heroViewAngle
        val yawRad = if (viewAngle == HeroViewAngle.ORBIT) {
            TrackSpline.degToRad(dragYawOffsetDeg) + totalTimeSec * 0.65f
        } else {
            TrackSpline.degToRad(viewAngle.yawDeg + dragYawOffsetDeg)
        }
        val eyeX = sin(yawRad) * 3.8f
        val eyeZ = cos(yawRad) * 3.8f
        setupCamera(eyeX, 1.55f, eyeZ, 0f, 1.05f, 0f, fov = 45f)

        showcaseStageMesh?.let {
            Matrix.setIdentityM(modelScratch, 0)
            Matrix.scaleM(modelScratch, 0, 0.65f, 1f, 0.65f)
            drawWithTransform(it, shader, vpMatrix, modelScratch, mvpScratch, emissive = 0.45f)
        }

        showcaseHeroRig?.let {
            HeroMeshBuilder.drawHero(
                rig = it,
                shader = shader,
                vpMatrix = vpMatrix,
                worldX = 0f,
                worldY = 0f,
                worldZ = 0f,
                yawDeg = 0f,
                animation = showcaseConfig.heroAnimation,
                timeSec = totalTimeSec,
                scale = 1.18f
            )
        }
    }

    private fun renderResultsPodium() {
        setEnvironmentUniforms(activeTrackSpec, fogDensity = 0.004f)
        val angle = sin(totalTimeSec * 0.4f) * 0.38f + 0.35f
        setupCamera(sin(angle) * 6.8f, 2.3f, cos(angle) * 6.8f, 0f, 1.0f, 0f, fov = 48f)

        showcaseStageMesh?.let {
            Matrix.setIdentityM(modelScratch, 0)
            drawWithTransform(it, shader, vpMatrix, modelScratch, mvpScratch, emissive = 0.45f)
        }

        showcaseHeroRig?.let {
            HeroMeshBuilder.drawHero(
                rig = it,
                shader = shader,
                vpMatrix = vpMatrix,
                worldX = -1.55f,
                worldY = 0f,
                worldZ = 1.05f,
                yawDeg = 15f,
                animation = showcaseConfig.heroAnimation,
                timeSec = totalTimeSec,
                scale = 1.18f
            )
        }

        showcaseCarRig?.let {
            CarMeshBuilder.drawCar(
                rig = it,
                shader = shader,
                vpMatrix = vpMatrix,
                worldX = 0.9f,
                worldY = 0f,
                worldZ = -0.35f,
                yawDeg = -25f,
                pitchDeg = 0f,
                rollDeg = 0f,
                steerVisualDeg = 10f,
                wheelSpinDeg = 0f,
                isBraking = false,
                scratchModel = modelScratch,
                scratchWheel = wheelScratch,
                scratchMvp = mvpScratch
            )
        }
    }

    private fun renderRaceScene(sim: RaceSimulation, dt: Float) {
        val ts = trackScene ?: return
        val spec = sim.trackSpec
        val fogDens = if (spec.isNight) 0.0038f else 0.0020f
        setEnvironmentUniforms(spec, fogDensity = fogDens)

        val player = sim.racers[0]
        val speedNorm = (player.speed / 95f).coerceIn(0f, 1.3f)

        // Cinematic Intro flyby during 3-2-1 countdown!
        if (sim.phaseCountdownTimer > 0.6f) {
            val orbit = (3.6f - sim.phaseCountdownTimer) * 0.75f
            val targetX = player.x + sin(player.yawRad + 2.4f - orbit) * 6.8f
            val targetZ = player.z + cos(player.yawRad + 2.4f - orbit) * 6.8f
            setupCamera(
                targetX, player.y + 2.1f, targetZ,
                player.x, player.y + 1.0f, player.z,
                fov = 52f
            )
        } else {
            val fx = sin(player.yawRad)
            val fz = cos(player.yawRad)
            val fovBoost = (if (player.nitroActive) 11f else 0f) + speedNorm * 8f

            val (desCamX, desCamY, desCamZ, desLookX, desLookY, desLookZ, fov) = when (cameraPerspective) {
                CameraPerspective.CHASE -> floatArrayOf(
                    player.x - fx * (8.4f + speedNorm * 1.6f),
                    player.y + 3.2f,
                    player.z - fz * (8.4f + speedNorm * 1.6f),
                    player.x + fx * 10f,
                    player.y + 1.1f,
                    player.z + fz * 10f,
                    58f + fovBoost
                )
                CameraPerspective.CLOSE_REAR -> floatArrayOf(
                    player.x - fx * 5.6f,
                    player.y + 2.0f,
                    player.z - fz * 5.6f,
                    player.x + fx * 12f,
                    player.y + 0.9f,
                    player.z + fz * 12f,
                    62f + fovBoost
                )
                CameraPerspective.HOOD -> floatArrayOf(
                    player.x + fx * 0.8f,
                    player.y + 1.35f,
                    player.z + fz * 0.8f,
                    player.x + fx * 24f,
                    player.y + 1.1f,
                    player.z + fz * 24f,
                    68f + fovBoost
                )
                CameraPerspective.CINEMATIC -> {
                    val side = sin(totalTimeSec * 0.65f) * 3.8f
                    floatArrayOf(
                        player.x - fx * 9.5f + cos(player.yawRad) * side,
                        player.y + 4.2f,
                        player.z - fz * 9.5f - sin(player.yawRad) * side,
                        player.x + fx * 8f,
                        player.y + 0.9f,
                        player.z + fz * 8f,
                        56f + fovBoost
                    )
                }
            }.let { a -> CameraTargets(a[0], a[1], a[2], a[3], a[4], a[5], a[6]) }

            val lerp = if (cameraPerspective == CameraPerspective.HOOD) 1f else (11f * dt).coerceIn(0.15f, 1f)
            camX += (desCamX - camX) * lerp
            camY += (desCamY - camY) * lerp
            camZ += (desCamZ - camZ) * lerp
            lookX += (desLookX - lookX) * lerp
            lookY += (desLookY - lookY) * lerp
            lookZ += (desLookZ - lookZ) * lerp

            setupCamera(camX, camY, camZ, lookX, lookY, lookZ, fov = fov)
        }

        // Draw Environment & Track
        Matrix.setIdentityM(modelScratch, 0)
        drawWithTransform(ts.groundMesh, shader, vpMatrix, modelScratch, mvpScratch, emissive = 0f, specStrength = if (spec.isWetRoad) 0.45f else 0.08f)
        val roadSpec = if (spec.isWetRoad) 0.95f else 0.20f
        drawWithTransform(ts.roadMesh, shader, vpMatrix, modelScratch, mvpScratch, emissive = 0f, specStrength = roadSpec, specPower = 40f)
        drawWithTransform(ts.guardrailMesh, shader, vpMatrix, modelScratch, mvpScratch, emissive = 0f, specStrength = 0.4f)
        drawWithTransform(ts.sceneryMesh, shader, vpMatrix, modelScratch, mvpScratch, emissive = 0f, specStrength = 0.25f)
        drawWithTransform(ts.emissivePropsMesh, shader, vpMatrix, modelScratch, mvpScratch, emissive = 1.25f, specStrength = 0.1f)

        // Moving clouds
        Matrix.setIdentityM(modelScratch, 0)
        Matrix.translateM(modelScratch, 0, sin(totalTimeSec * 0.05f) * 35f, 0f, cos(totalTimeSec * 0.04f) * 35f)
        drawWithTransform(ts.cloudMesh, shader, vpMatrix, modelScratch, mvpScratch, emissive = 0.12f, specStrength = 0f)

        // Draw Hero standing beside player car during 3-2-1 race intro!
        if (sim.phaseCountdownTimer > 0.8f) {
            showcaseHeroRig?.let {
                val rx = cos(player.yawRad)
                val rz = -sin(player.yawRad)
                HeroMeshBuilder.drawHero(
                    rig = it,
                    shader = shader,
                    vpMatrix = vpMatrix,
                    worldX = player.x - rx * 1.95f,
                    worldY = player.y,
                    worldZ = player.z - rz * 1.95f,
                    yawDeg = TrackSpline.radToDeg(player.yawRad) + 25f,
                    animation = HeroAnimation.IDLE,
                    timeSec = totalTimeSec,
                    scale = 1.12f
                )
            }
        }

        // Draw all 6 Racing Cars
        for (i in sim.racers.indices) {
            if (i >= raceCarRigs.size) break
            val r = sim.racers[i]
            val rig = raceCarRigs[i]
            val visualYaw = TrackSpline.radToDeg(r.yawRad + r.driftAngleRad)
            CarMeshBuilder.drawCar(
                rig = rig,
                shader = shader,
                vpMatrix = vpMatrix,
                worldX = r.x,
                worldY = r.y,
                worldZ = r.z,
                yawDeg = visualYaw,
                pitchDeg = 0f,
                rollDeg = r.rollDeg,
                steerVisualDeg = r.steerVisualDeg,
                wheelSpinDeg = r.wheelSpinDeg,
                isBraking = r.isBraking,
                scratchModel = modelScratch,
                scratchWheel = wheelScratch,
                scratchMvp = mvpScratch
            )
        }

        // Draw Pooled 3D Particles (Nitro flames, Tire smoke, Sparks)
        particles.drawAll(shader, vpMatrix)
    }

    private data class CameraTargets(
        val cx: Float, val cy: Float, val cz: Float,
        val lx: Float, val ly: Float, val lz: Float,
        val fov: Float
    )

    private fun setEnvironmentUniforms(spec: TrackSpec, fogDensity: Float) {
        val sky = TrackSpline.argbToRgba(spec.skyHorizonColor)
        val top = TrackSpline.argbToRgba(spec.skyTopColor)
        GLES20.glClearColor(top[0] * 0.6f + sky[0] * 0.4f, top[1] * 0.6f + sky[1] * 0.4f, top[2] * 0.6f + sky[2] * 0.4f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        shader.use()
        if (spec.isNight) {
            GLES20.glUniform3f(shader.uLightDirLoc, 0.4f, 0.85f, 0.35f)
            GLES20.glUniform3f(shader.uAmbientColorLoc, 0.36f, 0.42f, 0.58f)
        } else {
            GLES20.glUniform3f(shader.uLightDirLoc, 0.55f, 0.90f, 0.40f)
            GLES20.glUniform3f(shader.uAmbientColorLoc, 0.52f, 0.54f, 0.58f)
        }
        GLES20.glUniform3f(shader.uFogColorLoc, sky[0], sky[1], sky[2])
        GLES20.glUniform1f(shader.uFogDensityLoc, fogDensity)
    }

    private fun setupCamera(
        eyeX: Float, eyeY: Float, eyeZ: Float,
        centerX: Float, centerY: Float, centerZ: Float,
        fov: Float
    ) {
        GLES20.glUniform3f(shader.uCameraPosLoc, eyeX, eyeY, eyeZ)
        Matrix.perspectiveM(projMatrix, 0, fov, aspectRatio, 0.4f, 520f)
        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
    }

    private fun rebuildShowcaseAssets() {
        val cfg = showcaseConfig
        showcaseCarRig = CarMeshBuilder.buildCar(
            cfg.carSpec.silhouette,
            cfg.bodyColorHex,
            cfg.neonColorHex,
            cfg.wheelStyleId,
            cfg.spoilerStyleId
        )
        showcaseHeroRig = HeroMeshBuilder.buildHero(
            cfg.jacketColorHex,
            cfg.armorColorHex,
            cfg.trimColorHex,
            cfg.accessoryStyle,
            cfg.auraColorHex
        )
    }

    private fun buildShowcaseStageMesh() {
        val mb = MeshBuilder(1024)
        // Dark metallic turntable platform with glowing electric-blue ring
        mb.addCylinderY(0f, -0.35f, 0f, 4.4f, 4.2f, 0.35f, 28, 0.08f, 0.11f, 0.18f)
        mb.addCylinderY(0f, -0.04f, 0f, 4.26f, 4.26f, 0.06f, 28, 0.0f, 0.82f, 1.0f)
        showcaseStageMesh = mb.build()
    }
}

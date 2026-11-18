package com.example.engine3d

import com.example.model.GraphicsQuality
import com.example.model.TrackSpec
import com.example.model.TrackTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class TrackScene3D(
    val groundMesh: Mesh,
    val roadMesh: Mesh,
    val guardrailMesh: Mesh,
    val emissivePropsMesh: Mesh,
    val sceneryMesh: Mesh,
    val cloudMesh: Mesh
)

object TrackMeshBuilder {
    fun buildTrackScene(
        spline: TrackSpline,
        quality: GraphicsQuality
    ): TrackScene3D {
        val spec = spline.spec
        val samples = spline.samples
        val n = samples.size
        val halfW = spec.roadWidth * 0.5f

        val ground = MeshBuilder(2048)
        val road = MeshBuilder(16384)
        val guardrails = MeshBuilder(8192)
        val emissiveProps = MeshBuilder(8192)
        val scenery = MeshBuilder(16384)
        val clouds = MeshBuilder(2048)

        // 1. Ground Terrain / Ocean Plane
        val groundColor = when (spec.theme) {
            TrackTheme.FUTURISTIC_CITY -> floatArrayOf(0.04f, 0.06f, 0.12f)
            TrackTheme.MOUNTAIN_HIGHWAY -> floatArrayOf(0.12f, 0.34f, 0.16f)
            TrackTheme.DESERT_SPEEDWAY -> floatArrayOf(0.76f, 0.53f, 0.25f)
            TrackTheme.COASTAL_HIGHWAY -> floatArrayOf(0.05f, 0.45f, 0.68f) // Turquoise ocean!
            TrackTheme.CYBERPUNK_NIGHT -> floatArrayOf(0.06f, 0.03f, 0.12f)
        }
        ground.addQuad(
            -650f, -2.5f, -650f,
            650f, -2.5f, -650f,
            650f, -2.5f, 650f,
            -650f, -2.5f, 650f,
            groundColor[0], groundColor[1], groundColor[2]
        )

        // Coastal sandy beach strip on inner/left side
        if (spec.theme == TrackTheme.COASTAL_HIGHWAY) {
            ground.addQuad(
                -180f, -1.6f, -340f,
                160f, -1.6f, -340f,
                160f, -1.6f, 320f,
                -180f, -1.6f, 320f,
                0.86f, 0.78f, 0.54f
            )
        }

        // 2. Road Surface Ribbon + Curbs + Guardrails + Center Dashes
        val roadCol = when (spec.theme) {
            TrackTheme.FUTURISTIC_CITY -> floatArrayOf(0.10f, 0.12f, 0.18f)
            TrackTheme.MOUNTAIN_HIGHWAY -> floatArrayOf(0.22f, 0.24f, 0.27f)
            TrackTheme.DESERT_SPEEDWAY -> floatArrayOf(0.26f, 0.23f, 0.22f)
            TrackTheme.COASTAL_HIGHWAY -> floatArrayOf(0.25f, 0.27f, 0.30f)
            TrackTheme.CYBERPUNK_NIGHT -> floatArrayOf(0.08f, 0.09f, 0.16f) // Dark wet asphalt
        }
        val curbA = if (spec.isNight) floatArrayOf(0.0f, 0.85f, 1.0f) else floatArrayOf(0.92f, 0.18f, 0.22f)
        val curbB = if (spec.isNight) floatArrayOf(0.15f, 0.22f, 0.40f) else floatArrayOf(0.94f, 0.94f, 0.96f)
        val neonCol = TrackSpline.argbToRgba(spec.accentHex)

        for (i in 0 until n) {
            val a = samples[i]
            val b = samples[(i + 1) % n]

            val axL = a.x - a.rx * halfW; val azL = a.z - a.rz * halfW
            val axR = a.x + a.rx * halfW; val azR = a.z + a.rz * halfW
            val bxL = b.x - b.rx * halfW; val bzL = b.z - b.rz * halfW
            val bxR = b.x + b.rx * halfW; val bzR = b.z + b.rz * halfW

            // Main asphalt quad
            val shade = if (i % 2 == 0) 1.0f else 0.96f
            road.addQuad(
                axL, a.y + 0.01f, azL,
                axR, a.y + 0.01f, azR,
                bxR, b.y + 0.01f, bzR,
                bxL, b.y + 0.01f, bzL,
                roadCol[0] * shade, roadCol[1] * shade, roadCol[2] * shade
            )

            // Center dashed line
            if (i % 4 < 2) {
                val dashW = 0.28f
                emissiveProps.addQuad(
                    a.x - a.rx * dashW, a.y + 0.04f, a.z - a.rz * dashW,
                    a.x + a.rx * dashW, a.y + 0.04f, a.z + a.rz * dashW,
                    b.x + b.rx * dashW, b.y + 0.04f, b.z + b.rz * dashW,
                    b.x - b.rx * dashW, b.y + 0.04f, b.z - b.rz * dashW,
                    if (spec.isNight) neonCol[0] else 0.95f,
                    if (spec.isNight) neonCol[1] else 0.85f,
                    if (spec.isNight) neonCol[2] else 0.30f
                )
            }

            // Curbs (width 1.1m)
            val cCol = if ((i / 2) % 2 == 0) curbA else curbB
            val curbOuter = halfW + 1.1f
            val acxL = a.x - a.rx * curbOuter; val aczL = a.z - a.rz * curbOuter
            val bcxL = b.x - b.rx * curbOuter; val bczL = b.z - b.rz * curbOuter
            val acxR = a.x + a.rx * curbOuter; val aczR = a.z + a.rz * curbOuter
            val bcxR = b.x + b.rx * curbOuter; val bczR = b.z + b.rz * curbOuter

            road.addQuad(acxL, a.y + 0.03f, aczL, axL, a.y + 0.03f, azL, bxL, b.y + 0.03f, bzL, bcxL, b.y + 0.03f, bczL, cCol[0], cCol[1], cCol[2])
            road.addQuad(axR, a.y + 0.03f, azR, acxR, a.y + 0.03f, aczR, bcxR, b.y + 0.03f, bczR, bxR, b.y + 0.03f, bzR, cCol[0], cCol[1], cCol[2])

            // Guardrails
            val railDist = halfW + 1.4f
            val arxL = a.x - a.rx * railDist; val arzL = a.z - a.rz * railDist
            val brxL = b.x - b.rx * railDist; val brzL = b.z - b.rz * railDist
            val arxR = a.x + a.rx * railDist; val arzR = a.z + a.rz * railDist
            val brxR = b.x + b.rx * railDist; val brzR = b.z + b.rz * railDist

            guardrails.addQuad(arxL, a.y, arzL, arxL, a.y + 0.85f, arzL, brxL, b.y + 0.85f, brzL, brxL, b.y, brzL, 0.35f, 0.40f, 0.48f)
            guardrails.addQuad(arxR, a.y, arzR, brxR, b.y, brzR, brxR, b.y + 0.85f, brzR, arxR, a.y + 0.85f, arzR, 0.35f, 0.40f, 0.48f)

            if (spec.isNight && i % 2 == 0) {
                // Glowing neon rail strip
                emissiveProps.addQuad(
                    arxL, a.y + 0.65f, arzL,
                    arxL, a.y + 0.82f, arzL,
                    brxL, b.y + 0.82f, brzL,
                    brxL, b.y + 0.65f, brzL,
                    neonCol[0], neonCol[1], neonCol[2]
                )
                emissiveProps.addQuad(
                    arxR, a.y + 0.65f, arzR,
                    brxR, b.y + 0.65f, brzR,
                    brxR, b.y + 0.82f, brzR,
                    arxR, a.y + 0.82f, arzR,
                    neonCol[0], neonCol[1], neonCol[2]
                )
            }

            // Road support pillars when elevated (bridges!)
            if (a.y > 3.5f && i % 10 == 0) {
                scenery.addBox(a.x, (a.y - 2.5f) * 0.5f, a.z, spec.roadWidth * 0.65f, a.y + 2.5f, 2.2f, 0.32f, 0.35f, 0.40f)
            }
        }

        // 3. Start/Finish Overhead Gantry + 4 Checkpoint Arches
        val gantryIndices = listOf(0, n / 4, n / 2, (3 * n) / 4)
        for ((gIdx, sIdx) in gantryIndices.withIndex()) {
            val sp = samples[sIdx]
            val isFinish = gIdx == 0
            val pDist = halfW + 1.8f
            val lx = sp.x - sp.rx * pDist; val lz = sp.z - sp.rz * pDist
            val rx = sp.x + sp.rx * pDist; val rz = sp.z + sp.rz * pDist
            val topH = 8.2f
            scenery.addBox(lx, sp.y + topH * 0.5f, lz, 1.4f, topH, 1.4f, 0.16f, 0.20f, 0.28f)
            scenery.addBox(rx, sp.y + topH * 0.5f, rz, 1.4f, topH, 1.4f, 0.16f, 0.20f, 0.28f)
            // Crossbar
            val barCol = if (isFinish) floatArrayOf(0.0f, 0.90f, 1.0f) else floatArrayOf(neonCol[0], neonCol[1], neonCol[2])
            emissiveProps.addQuad(
                lx, sp.y + topH - 1.2f, lz,
                rx, sp.y + topH - 1.2f, rz,
                rx, sp.y + topH, rz,
                lx, sp.y + topH, lz,
                barCol[0], barCol[1], barCol[2]
            )
            // Start line checkered stripe on ground
            if (isFinish) {
                val cCount = 8
                for (c in 0 until cCount) {
                    val f0 = -1f + 2f * (c.toFloat() / cCount)
                    val f1 = -1f + 2f * ((c + 1).toFloat() / cCount)
                    val col = if (c % 2 == 0) 0.95f else 0.08f
                    emissiveProps.addQuad(
                        sp.x + sp.rx * halfW * f0, sp.y + 0.05f, sp.z + sp.rz * halfW * f0,
                        sp.x + sp.rx * halfW * f1, sp.y + 0.05f, sp.z + sp.rz * halfW * f1,
                        sp.x + sp.rx * halfW * f1 + sp.tx * 2.2f, sp.y + 0.05f, sp.z + sp.rz * halfW * f1 + sp.tz * 2.2f,
                        sp.x + sp.rx * halfW * f0 + sp.tx * 2.2f, sp.y + 0.05f, sp.z + sp.rz * halfW * f0 + sp.tz * 2.2f,
                        col, col, col
                    )
                }
            }
        }

        // 4. Mountain Highway Tunnel & Bridge
        if (spec.theme == TrackTheme.MOUNTAIN_HIGHWAY) {
            val tunnelStart = (n * 0.24f).toInt()
            val tunnelEnd = (n * 0.34f).toInt()
            for (i in tunnelStart until tunnelEnd) {
                val a = samples[i]
                val b = samples[(i + 1) % n]
                val tW = halfW + 2.2f
                val tH = 8.5f
                val axL = a.x - a.rx * tW; val azL = a.z - a.rz * tW
                val axR = a.x + a.rx * tW; val azR = a.z + a.rz * tW
                val bxL = b.x - b.rx * tW; val bzL = b.z - b.rz * tW
                val bxR = b.x + b.rx * tW; val bzR = b.z + b.rz * tW

                scenery.addQuad(axL, a.y, azL, axL, a.y + tH, azL, bxL, b.y + tH, bzL, bxL, b.y, bzL, 0.25f, 0.26f, 0.30f)
                scenery.addQuad(axR, a.y, azR, bxR, b.y, bzR, bxR, b.y + tH, bzR, axR, a.y + tH, azR, 0.25f, 0.26f, 0.30f)
                scenery.addQuad(axL, a.y + tH, azL, axR, a.y + tH, azR, bxR, b.y + tH, bzR, bxL, b.y + tH, bzL, 0.20f, 0.22f, 0.26f)
                if (i % 3 == 0) {
                    emissiveProps.addBox(a.x, a.y + tH - 0.3f, a.z, 2.2f, 0.25f, 1.2f, 1.0f, 0.88f, 0.55f)
                }
            }
        }

        // 5. Trackside 3D Environment Props (Scaled by GraphicsQuality)
        val rng = Random(spec.index * 919 + 42)
        val stepStride = when (quality) {
            GraphicsQuality.LOW -> 12
            GraphicsQuality.MEDIUM -> 7
            GraphicsQuality.HIGH -> 5
        }

        for (i in 0 until n step stepStride) {
            val sp = samples[i]
            val side = if (i % 2 == 0) 1f else -1f
            val distOut = halfW + 10f + rng.nextFloat() * 24f
            val px = sp.x + sp.rx * side * distOut
            val pz = sp.z + sp.rz * side * distOut

            when (spec.theme) {
                TrackTheme.FUTURISTIC_CITY -> {
                    val h = 24f + rng.nextFloat() * 58f
                    val w = 10f + rng.nextFloat() * 12f
                    scenery.addBox(px, sp.y + h * 0.5f - 2f, pz, w, h, w, 0.08f, 0.11f, 0.20f)
                    // Electric-blue neon window strips & rooftop beacons
                    emissiveProps.addBox(px, sp.y + h * 0.65f, pz, w + 0.3f, 1.4f, w + 0.3f, 0.0f, 0.82f, 1.0f)
                    if (i % (stepStride * 2) == 0) {
                        // Light pole on roadside
                        val lx = sp.x - sp.rx * side * (halfW + 2.5f)
                        val lz = sp.z - sp.rz * side * (halfW + 2.5f)
                        scenery.addBox(lx, sp.y + 4f, lz, 0.4f, 8f, 0.4f, 0.3f, 0.36f, 0.46f)
                        emissiveProps.addBox(lx, sp.y + 8f, lz, 1.4f, 0.35f, 0.6f, 0.0f, 0.88f, 1.0f)
                    }
                }
                TrackTheme.MOUNTAIN_HIGHWAY -> {
                    // Green mountain peaks & pine trees
                    val mh = 22f + rng.nextFloat() * 44f
                    val mw = 18f + rng.nextFloat() * 18f
                    scenery.addTaperedBox(px, -3f, pz, mw, mw, mh, 2.5f, 2.5f, 0f, 0f, 0.14f, 0.42f, 0.20f)
                    // Pine tree closer to road
                    val tx = sp.x - sp.rx * side * (halfW + 6f + rng.nextFloat() * 8f)
                    val tz = sp.z - sp.rz * side * (halfW + 6f + rng.nextFloat() * 8f)
                    scenery.addCylinderY(tx, sp.y - 1f, tz, 0.45f, 0.35f, 3.0f, 6, 0.36f, 0.22f, 0.12f)
                    scenery.addTaperedBox(tx, sp.y + 2f, tz, 3.8f, 3.8f, sp.y + 9.5f, 0.3f, 0.3f, 0f, 0f, 0.09f, 0.38f, 0.15f)
                }
                TrackTheme.DESERT_SPEEDWAY -> {
                    // Rocky desert mesas & golden sand dunes
                    val h = 14f + rng.nextFloat() * 34f
                    val w = 14f + rng.nextFloat() * 16f
                    if (i % (stepStride * 2) == 0) {
                        scenery.addTaperedBox(px, -3f, pz, w, w, h, w * 0.62f, w * 0.62f, 0f, 0f, 0.72f, 0.34f, 0.18f)
                    } else {
                        scenery.addTaperedBox(px, -3f, pz, w * 1.4f, w * 1.4f, h * 0.45f, 3f, 3f, 0f, 0f, 0.88f, 0.66f, 0.32f)
                    }
                }
                TrackTheme.COASTAL_HIGHWAY -> {
                    // Palm trees with curved trunks & green fronds!
                    val pxPalm = sp.x + sp.rx * side * (halfW + 5f + rng.nextFloat() * 8f)
                    val pzPalm = sp.z + sp.rz * side * (halfW + 5f + rng.nextFloat() * 8f)
                    scenery.addCylinderY(pxPalm, sp.y - 1f, pzPalm, 0.45f, 0.28f, 7.5f, 7, 0.48f, 0.34f, 0.22f)
                    scenery.addBox(pxPalm, sp.y + 6.7f, pzPalm, 5.2f, 0.7f, 1.4f, 0.14f, 0.62f, 0.24f)
                    scenery.addBox(pxPalm, sp.y + 6.7f, pzPalm, 1.4f, 0.7f, 5.2f, 0.16f, 0.68f, 0.26f)
                }
                TrackTheme.CYBERPUNK_NIGHT -> {
                    val h = 28f + rng.nextFloat() * 64f
                    val w = 11f + rng.nextFloat() * 12f
                    scenery.addBox(px, sp.y + h * 0.5f - 2f, pz, w, h, w, 0.06f, 0.05f, 0.14f)
                    val isPurple = i % (stepStride * 2) == 0
                    val rN = if (isPurple) 0.75f else 0.0f
                    val gN = if (isPurple) 0.22f else 0.90f
                    val bN = 1.0f
                    emissiveProps.addBox(px, sp.y + h * 0.45f, pz, w + 0.35f, 1.8f, w + 0.35f, rN, gN, bN)
                    emissiveProps.addBox(px, sp.y + h * 0.82f, pz, w * 0.6f, 3.0f, w * 0.6f, 1.0f, 0.18f, 0.55f)
                }
            }
        }

        // 6. Atmospheric Clouds in Sky
        val cloudCol = if (spec.isNight) floatArrayOf(0.14f, 0.20f, 0.36f) else floatArrayOf(0.95f, 0.97f, 1.0f)
        for (c in 0 until 14) {
            val angle = (c / 14f) * (2f * PI.toFloat())
            val rad = 240f + (c % 3) * 65f
            val cx = 110f + cos(angle) * rad
            val cz = sin(angle) * rad
            val cy = 85f + (c % 4) * 14f
            clouds.addBox(cx, cy, cz, 34f + (c % 3) * 12f, 6f, 18f, cloudCol[0], cloudCol[1], cloudCol[2], 0.75f)
            clouds.addBox(cx + 12f, cy + 3f, cz + 4f, 22f, 5f, 14f, cloudCol[0], cloudCol[1], cloudCol[2], 0.75f)
        }

        return TrackScene3D(
            groundMesh = ground.build(),
            roadMesh = road.build(),
            guardrailMesh = guardrails.build(),
            emissivePropsMesh = emissiveProps.build(),
            sceneryMesh = scenery.build(),
            cloudMesh = clouds.build()
        )
    }
}

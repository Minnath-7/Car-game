package com.example.engine3d

import android.opengl.Matrix
import com.example.model.CarSilhouette

data class CarRig3D(
    val bodyMesh: Mesh,
    val glassMesh: Mesh,
    val neonMesh: Mesh,
    val headlightMesh: Mesh,
    val taillightMesh: Mesh,
    val spoilerMesh: Mesh,
    val wheelMesh: Mesh,
    val shadowMesh: Mesh
)

object CarMeshBuilder {
    fun buildCar(
        silhouette: CarSilhouette,
        bodyColorHex: Long,
        neonColorHex: Long,
        wheelStyleId: Int,
        spoilerStyleId: Int
    ): CarRig3D {
        val bc = TrackSpline.argbToRgba(bodyColorHex)
        val nc = TrackSpline.argbToRgba(neonColorHex)
        val br = bc[0]; val bg = bc[1]; val bb = bc[2]
        val nr = nc[0]; val ng = nc[1]; val nb = nc[2]

        val body = MeshBuilder(4096)
        val glass = MeshBuilder(1024)
        val neon = MeshBuilder(1536)
        val headlights = MeshBuilder(512)
        val taillights = MeshBuilder(512)
        val spoiler = MeshBuilder(1024)
        val wheel = MeshBuilder(1024)
        val shadow = MeshBuilder(256)

        // Car dimensions vary subtly by silhouette for authentic variety
        val width = when (silhouette) {
            CarSilhouette.COBALT_DRIFT -> 2.18f
            CarSilhouette.VALKYRIE_HYPER -> 2.22f
            CarSilhouette.PHANTOM_PROTO -> 2.10f
            else -> 2.12f
        }
        val length = when (silhouette) {
            CarSilhouette.PHANTOM_PROTO -> 4.95f
            CarSilhouette.VALKYRIE_HYPER -> 4.85f
            CarSilhouette.VORTEX_WEDGE -> 4.55f
            else -> 4.70f
        }

        // 1. Lower Aero Skirt & Diffuser (dark carbon)
        body.addTaperedBox(
            0f, 0.22f, 0f,
            width * 1.02f, length * 0.98f,
            0.44f,
            width * 0.98f, length * 0.96f,
            0f, 0f,
            0.09f, 0.10f, 0.14f
        )

        // 2. Sculpted Main Chassis Body (+Z is forward)
        val hoodNoseDrop = if (silhouette == CarSilhouette.VORTEX_WEDGE || silhouette == CarSilhouette.VALKYRIE_HYPER) 0.56f else 0.64f
        body.addTaperedBox(
            0f, 0.42f, -0.05f,
            width * 0.98f, length * 0.92f,
            0.78f,
            width * 0.90f, length * 0.84f,
            0f, -0.10f,
            br, bg, bb
        )

        // Aggressive sloped front hood
        body.addTaperedBox(
            0f, 0.42f, length * 0.30f,
            width * 0.92f, length * 0.36f,
            hoodNoseDrop,
            width * 0.80f, length * 0.26f,
            0f, -0.10f,
            br * 1.05f, bg * 1.05f, bb * 1.05f
        )

        // Muscular rear fenders / haunches
        body.addTaperedBox(
            -width * 0.36f, 0.50f, -length * 0.22f,
            0.52f, length * 0.44f,
            0.88f,
            0.40f, length * 0.34f,
            0f, 0f,
            br * 0.92f, bg * 0.92f, bb * 0.92f
        )
        body.addTaperedBox(
            width * 0.36f, 0.50f, -length * 0.22f,
            0.52f, length * 0.44f,
            0.88f,
            0.40f, length * 0.34f,
            0f, 0f,
            br * 0.92f, bg * 0.92f, bb * 0.92f
        )

        // Sporty front grille & splitter
        body.addBox(0f, 0.32f, length * 0.47f, width * 0.72f, 0.16f, 0.14f, 0.05f, 0.06f, 0.09f)
        body.addBox(0f, 0.22f, length * 0.48f, width * 0.96f, 0.06f, 0.22f, 0.15f, 0.18f, 0.24f)

        // Silver/Metallic grille ribs & exhaust pipes
        body.addBox(-0.48f, 0.30f, -length * 0.48f, 0.24f, 0.14f, 0.20f, 0.65f, 0.70f, 0.78f)
        body.addBox(0.48f, 0.30f, -length * 0.48f, 0.24f, 0.14f, 0.20f, 0.65f, 0.70f, 0.78f)

        // 3. Reflective Glossy Glass Canopy Cabin
        val cabinLen = when (silhouette) {
            CarSilhouette.PHANTOM_PROTO -> 2.15f
            CarSilhouette.SOLARIS_EXOTIC -> 1.85f
            else -> 1.98f
        }
        glass.addTaperedBox(
            0f, 0.74f, -0.14f,
            width * 0.76f, cabinLen,
            1.22f,
            width * 0.56f, cabinLen * 0.56f,
            0f, -0.14f,
            0.14f, 0.22f, 0.36f, 0.95f
        )
        // Roof spine
        body.addTaperedBox(
            0f, 1.18f, -0.26f,
            width * 0.52f, cabinLen * 0.52f,
            1.26f,
            width * 0.46f, cabinLen * 0.46f,
            0f, 0f,
            br * 0.88f, bg * 0.88f, bb * 0.88f
        )

        // 4. Electric Neon Highlights (Side skirts, front intake, hood trim, underglow!)
        neon.addBox(-width * 0.50f, 0.28f, 0f, 0.05f, 0.05f, length * 0.78f, nr, ng, nb)
        neon.addBox(width * 0.50f, 0.28f, 0f, 0.05f, 0.05f, length * 0.78f, nr, ng, nb)
        neon.addBox(0f, 0.24f, length * 0.49f, width * 0.74f, 0.04f, 0.05f, nr, ng, nb)
        neon.addBox(0f, 0.79f, 0.55f, 0.06f, 0.03f, 1.35f, nr, ng, nb)
        // Subtle underglow ring on ground plane
        neon.addQuad(
            -width * 0.55f, 0.04f, -length * 0.48f,
            width * 0.55f, 0.04f, -length * 0.48f,
            width * 0.55f, 0.04f, length * 0.48f,
            -width * 0.55f, 0.04f, length * 0.48f,
            nr, ng, nb, 0.34f
        )

        // 5. LED Headlights (+Z front)
        headlights.addBox(-width * 0.34f, 0.50f, length * 0.45f, 0.36f, 0.10f, 0.14f, 0.85f, 0.97f, 1.0f)
        headlights.addBox(width * 0.34f, 0.50f, length * 0.45f, 0.36f, 0.10f, 0.14f, 0.85f, 0.97f, 1.0f)

        // 6. Rear Taillight Bar (-Z rear)
        taillights.addBox(-width * 0.32f, 0.62f, -length * 0.46f, 0.48f, 0.10f, 0.10f, 1.0f, 0.16f, 0.28f)
        taillights.addBox(width * 0.32f, 0.62f, -length * 0.46f, 0.48f, 0.10f, 0.10f, 1.0f, 0.16f, 0.28f)

        // 7. Customizable Racing Spoiler
        when (spoilerStyleId) {
            0 -> {
                // Stealth lip
                spoiler.addTaperedBox(
                    0f, 0.82f, -length * 0.44f,
                    width * 0.84f, 0.24f,
                    0.92f,
                    width * 0.80f, 0.18f,
                    0f, -0.05f,
                    0.12f, 0.14f, 0.20f
                )
            }
            1 -> {
                // Carbon GT Wing
                spoiler.addBox(-0.55f, 0.92f, -length * 0.41f, 0.07f, 0.28f, 0.20f, 0.15f, 0.17f, 0.22f)
                spoiler.addBox(0.55f, 0.92f, -length * 0.41f, 0.07f, 0.28f, 0.20f, 0.15f, 0.17f, 0.22f)
                spoiler.addBox(0f, 1.08f, -length * 0.43f, width * 1.02f, 0.06f, 0.34f, br * 0.9f, bg * 0.9f, bb * 0.9f)
                spoiler.addBox(-width * 0.51f, 1.08f, -length * 0.43f, 0.04f, 0.20f, 0.36f, nr, ng, nb)
                spoiler.addBox(width * 0.51f, 1.08f, -length * 0.43f, 0.04f, 0.20f, 0.36f, nr, ng, nb)
            }
            2 -> {
                // Cyber Twin-Blade
                spoiler.addBox(-0.60f, 0.96f, -length * 0.41f, 0.06f, 0.34f, 0.22f, nr, ng, nb)
                spoiler.addBox(0.60f, 0.96f, -length * 0.41f, 0.06f, 0.34f, 0.22f, nr, ng, nb)
                spoiler.addBox(0f, 1.14f, -length * 0.44f, width * 1.06f, 0.05f, 0.26f, 0.12f, 0.14f, 0.20f)
                spoiler.addBox(0f, 1.02f, -length * 0.40f, width * 0.96f, 0.05f, 0.20f, 0.12f, 0.14f, 0.20f)
            }
            else -> {
                // Valkyrie Split Aero Fin
                spoiler.addTaperedBox(-0.52f, 0.88f, -length * 0.38f, 0.10f, 0.50f, 1.24f, 0.05f, 0.26f, 0f, -0.14f, br, bg, bb)
                spoiler.addTaperedBox(0.52f, 0.88f, -length * 0.38f, 0.10f, 0.50f, 1.24f, 0.05f, 0.26f, 0f, -0.14f, br, bg, bb)
                spoiler.addBox(0f, 1.22f, -length * 0.44f, width * 1.08f, 0.06f, 0.30f, nr, ng, nb)
            }
        }

        // 8. Racing Wheel + Detailed Alloy Rim (centered at origin for easy axle rotation)
        val tireRadius = 0.38f
        val tireWidth = 0.30f
        wheel.addCylinderX(-tireWidth * 0.5f, 0f, 0f, tireRadius, tireWidth, 16, 0.07f, 0.07f, 0.09f)
        val rimColor = when (wheelStyleId) {
            1 -> floatArrayOf(nr, ng, nb) // Neon Halo
            2 -> floatArrayOf(0.20f, 0.24f, 0.30f) // Carbon 5-Spoke
            3 -> floatArrayOf(0.86f, 0.90f, 0.98f) // Aero Disc
            else -> floatArrayOf(0.75f, 0.82f, 0.92f) // Silver Turbine
        }
        wheel.addCylinderX(-tireWidth * 0.52f, 0f, 0f, tireRadius * 0.65f, tireWidth * 1.04f, 12, rimColor[0], rimColor[1], rimColor[2])
        // Alloy spokes / hub accent
        wheel.addBox(0f, 0f, 0f, tireWidth * 1.08f, tireRadius * 1.1f, 0.08f, 0.88f, 0.92f, 0.98f)
        wheel.addBox(0f, 0f, 0f, tireWidth * 1.08f, 0.08f, tireRadius * 1.1f, 0.88f, 0.92f, 0.98f)

        // 9. Soft Contact Shadow Quad
        shadow.addQuad(
            -width * 0.58f, 0.02f, -length * 0.52f,
            width * 0.58f, 0.02f, -length * 0.52f,
            width * 0.58f, 0.02f, length * 0.52f,
            -width * 0.58f, 0.02f, length * 0.52f,
            0.01f, 0.01f, 0.03f, 0.65f
        )

        return CarRig3D(
            bodyMesh = body.build(),
            glassMesh = glass.build(),
            neonMesh = neon.build(),
            headlightMesh = headlights.build(),
            taillightMesh = taillights.build(),
            spoilerMesh = spoiler.build(),
            wheelMesh = wheel.build(),
            shadowMesh = shadow.build()
        )
    }

    fun drawCar(
        rig: CarRig3D,
        shader: ShaderProgram,
        vpMatrix: FloatArray,
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        yawDeg: Float,
        pitchDeg: Float,
        rollDeg: Float,
        steerVisualDeg: Float,
        wheelSpinDeg: Float,
        isBraking: Boolean,
        scratchModel: FloatArray,
        scratchWheel: FloatArray,
        scratchMvp: FloatArray,
        scale: Float = 1f
    ) {
        Matrix.setIdentityM(scratchModel, 0)
        Matrix.translateM(scratchModel, 0, worldX, worldY, worldZ)
        Matrix.rotateM(scratchModel, 0, yawDeg, 0f, 1f, 0f)
        if (pitchDeg != 0f) Matrix.rotateM(scratchModel, 0, pitchDeg, 1f, 0f, 0f)
        if (rollDeg != 0f) Matrix.rotateM(scratchModel, 0, rollDeg, 0f, 0f, 1f)
        if (scale != 1f) Matrix.scaleM(scratchModel, 0, scale, scale, scale)

        // Shadow first
        drawWithTransform(rig.shadowMesh, shader, vpMatrix, scratchModel, scratchMvp, emissive = 0f, specStrength = 0f)

        // Glossy metallic chassis & spoiler
        drawWithTransform(rig.bodyMesh, shader, vpMatrix, scratchModel, scratchMvp, emissive = 0.04f, specStrength = 0.85f, specPower = 48f)
        drawWithTransform(rig.spoilerMesh, shader, vpMatrix, scratchModel, scratchMvp, emissive = 0.06f, specStrength = 0.70f, specPower = 36f)

        // Glass cabin reflections
        drawWithTransform(rig.glassMesh, shader, vpMatrix, scratchModel, scratchMvp, emissive = 0.10f, specStrength = 1.25f, specPower = 64f)

        // Glowing neon trim, headlights, brake lights
        drawWithTransform(rig.neonMesh, shader, vpMatrix, scratchModel, scratchMvp, emissive = 1.15f, specStrength = 0.2f)
        drawWithTransform(rig.headlightMesh, shader, vpMatrix, scratchModel, scratchMvp, emissive = 1.5f, specStrength = 0.3f)
        val brakeBoost = if (isBraking) 1.8f else 0.85f
        drawWithTransform(rig.taillightMesh, shader, vpMatrix, scratchModel, scratchMvp, emissive = brakeBoost, specStrength = 0.2f)

        // 4 Rotating & Steering Wheels
        val wheelPositions = arrayOf(
            floatArrayOf(-0.98f, 0.38f, 1.42f, 1f),   // Front Left
            floatArrayOf(0.98f, 0.38f, 1.42f, 1f),    // Front Right
            floatArrayOf(-1.00f, 0.38f, -1.42f, 0f),  // Rear Left
            floatArrayOf(1.00f, 0.38f, -1.42f, 0f)    // Rear Right
        )
        for (wp in wheelPositions) {
            Matrix.setIdentityM(scratchWheel, 0)
            Matrix.translateM(scratchWheel, 0, wp[0], wp[1], wp[2])
            if (wp[3] > 0.5f) {
                Matrix.rotateM(scratchWheel, 0, steerVisualDeg, 0f, 1f, 0f)
            }
            Matrix.rotateM(scratchWheel, 0, wheelSpinDeg, 1f, 0f, 0f)
            val combined = FloatArray(16)
            Matrix.multiplyMM(combined, 0, scratchModel, 0, scratchWheel, 0)
            drawWithTransform(rig.wheelMesh, shader, vpMatrix, combined, scratchMvp, emissive = 0.05f, specStrength = 0.55f, specPower = 32f)
        }
    }
}

package com.example.engine3d

import android.opengl.Matrix
import com.example.model.HeroAnimation
import kotlin.math.abs
import kotlin.math.sin

data class HeroRig3D(
    val headMesh: Mesh,
    val torsoMesh: Mesh,
    val leftShoulderMesh: Mesh,
    val rightShoulderMesh: Mesh,
    val leftArmMesh: Mesh,
    val rightArmMesh: Mesh,
    val leftLegMesh: Mesh,
    val rightLegMesh: Mesh,
    val auraMesh: Mesh
)

object HeroMeshBuilder {
    fun buildHero(
        jacketColorHex: Long,
        armorColorHex: Long,
        trimColorHex: Long,
        accessoryStyle: Int,
        auraColorHex: Long
    ): HeroRig3D {
        val jc = TrackSpline.argbToRgba(jacketColorHex)
        val ac = TrackSpline.argbToRgba(armorColorHex)
        val tc = TrackSpline.argbToRgba(trimColorHex)
        val auc = TrackSpline.argbToRgba(auraColorHex)

        // 1. Head: Stylized Anime Hero with messy spiky jet-black hair & sharp glowing blue eyes
        val head = MeshBuilder(2048)
        // Jaw & face
        head.addTaperedBox(0f, 0.02f, 0f, 0.22f, 0.24f, 0.30f, 0.26f, 0.28f, 0f, 0f, 0.94f, 0.80f, 0.72f)
        // Sharp expressive electric-blue eyes (+Z is front)
        head.addBox(-0.065f, 0.16f, 0.138f, 0.055f, 0.032f, 0.02f, tc[0], tc[1], tc[2])
        head.addBox(0.065f, 0.16f, 0.138f, 0.055f, 0.032f, 0.02f, tc[0], tc[1], tc[2])
        // Dark eyebrows
        head.addBox(-0.065f, 0.195f, 0.14f, 0.068f, 0.018f, 0.02f, 0.05f, 0.06f, 0.09f)
        head.addBox(0.065f, 0.195f, 0.14f, 0.068f, 0.018f, 0.02f, 0.05f, 0.06f, 0.09f)
        // Messy spiky jet-black anime hair crown + forward/side spikes
        head.addTaperedBox(0f, 0.23f, -0.02f, 0.30f, 0.31f, 0.36f, 0.22f, 0.24f, 0f, 0f, 0.05f, 0.06f, 0.09f)
        head.addBox(0f, 0.18f, -0.14f, 0.28f, 0.24f, 0.08f, 0.05f, 0.06f, 0.09f)
        // Signature spiky hair tufts
        val spikes = arrayOf(
            floatArrayOf(-0.11f, 0.32f, 0.11f, 0.07f, 0.16f, -0.05f, 0.08f),
            floatArrayOf(0.0f, 0.35f, 0.12f, 0.08f, 0.18f, 0.0f, 0.10f),
            floatArrayOf(0.11f, 0.31f, 0.10f, 0.07f, 0.15f, 0.06f, 0.08f),
            floatArrayOf(-0.14f, 0.28f, -0.02f, 0.08f, 0.17f, -0.10f, -0.04f),
            floatArrayOf(0.14f, 0.29f, -0.03f, 0.08f, 0.17f, 0.10f, -0.04f),
            floatArrayOf(-0.06f, 0.35f, -0.10f, 0.09f, 0.20f, -0.04f, -0.09f),
            floatArrayOf(0.07f, 0.34f, -0.11f, 0.09f, 0.19f, 0.05f, -0.10f)
        )
        for (sp in spikes) {
            head.addTaperedBox(sp[0], sp[1], sp[2], sp[3], sp[3], sp[1] + sp[4], 0.015f, 0.015f, sp[5], sp[6], 0.06f, 0.07f, 0.11f)
        }

        // 2. Torso: Premium black armored racing jacket with high collar, silver plates & electric-blue trim
        val torso = MeshBuilder(2048)
        // Waist to chest
        torso.addTaperedBox(0f, 0.0f, 0f, 0.36f, 0.22f, 0.56f, 0.46f, 0.26f, 0f, 0f, jc[0], jc[1], jc[2])
        // High futuristic jacket collar
        torso.addTaperedBox(0f, 0.54f, -0.02f, 0.26f, 0.22f, 0.68f, 0.30f, 0.24f, 0f, -0.02f, jc[0] * 0.9f, jc[1] * 0.9f, jc[2] * 0.9f)
        // Silver chest armor plates
        torso.addBox(-0.11f, 0.40f, 0.135f, 0.16f, 0.20f, 0.04f, ac[0], ac[1], ac[2])
        torso.addBox(0.11f, 0.40f, 0.135f, 0.16f, 0.20f, 0.04f, ac[0], ac[1], ac[2])
        // Silver back armor spine
        torso.addBox(0f, 0.34f, -0.135f, 0.18f, 0.38f, 0.04f, ac[0], ac[1], ac[2])
        // Electric-blue glowing chest core & zipper trim
        torso.addBox(0f, 0.32f, 0.145f, 0.045f, 0.42f, 0.03f, tc[0], tc[1], tc[2])
        torso.addBox(0f, 0.34f, -0.148f, 0.04f, 0.34f, 0.03f, tc[0], tc[1], tc[2])
        // Armored belt
        torso.addBox(0f, 0.05f, 0f, 0.38f, 0.08f, 0.25f, ac[0] * 0.85f, ac[1] * 0.85f, ac[2] * 0.85f)
        torso.addBox(0f, 0.05f, 0.13f, 0.09f, 0.06f, 0.03f, tc[0], tc[1], tc[2])

        // 3. Glowing Blue Shoulder Accessories (Left & Right)
        fun buildShoulder(sign: Float): Mesh {
            val mb = MeshBuilder(512)
            // Base pauldron
            mb.addTaperedBox(sign * 0.04f, -0.05f, 0f, 0.18f, 0.20f, 0.10f, 0.15f, 0.16f, sign * 0.03f, 0f, ac[0] * 0.7f, ac[1] * 0.7f, ac[2] * 0.7f)
            when (accessoryStyle) {
                1 -> { // Plasma Shoulder Fins
                    mb.addTaperedBox(sign * 0.08f, 0.04f, -0.02f, 0.06f, 0.18f, 0.28f, 0.02f, 0.06f, sign * 0.10f, -0.08f, tc[0], tc[1], tc[2])
                }
                2 -> { // Silver Aero Blades
                    mb.addBox(sign * 0.10f, 0.07f, 0f, 0.18f, 0.04f, 0.22f, ac[0], ac[1], ac[2])
                    mb.addBox(sign * 0.12f, 0.09f, 0f, 0.14f, 0.02f, 0.18f, tc[0], tc[1], tc[2])
                }
                3 -> { // Quantum Holo Ring
                    mb.addBox(sign * 0.08f, 0.12f, 0f, 0.16f, 0.03f, 0.16f, tc[0], tc[1], tc[2])
                }
                else -> { // Pulse Core Pauldrons
                    mb.addBox(sign * 0.08f, 0.04f, 0f, 0.13f, 0.06f, 0.15f, tc[0], tc[1], tc[2])
                }
            }
            return mb.build()
        }

        // 4. Articulated Arms (pivot at shoulder y = 0)
        fun buildArm(): Mesh {
            val mb = MeshBuilder(512)
            // Jacket sleeve + silver forearm gauntlet + glove
            mb.addTaperedBox(0f, -0.30f, 0f, 0.11f, 0.11f, 0.0f, 0.13f, 0.13f, 0f, 0f, jc[0], jc[1], jc[2])
            mb.addBox(0f, -0.38f, 0f, 0.115f, 0.18f, 0.115f, ac[0] * 0.85f, ac[1] * 0.85f, ac[2] * 0.85f)
            mb.addBox(0f, -0.45f, 0.03f, 0.04f, 0.10f, 0.08f, tc[0], tc[1], tc[2])
            mb.addBox(0f, -0.52f, 0f, 0.09f, 0.10f, 0.10f, 0.08f, 0.09f, 0.12f)
            return mb.build()
        }

        // 5. Articulated Legs (pivot at hip y = 0)
        fun buildLeg(): Mesh {
            val mb = MeshBuilder(512)
            mb.addTaperedBox(0f, -0.45f, 0f, 0.14f, 0.15f, 0.0f, 0.16f, 0.17f, 0f, 0f, jc[0] * 0.9f, jc[1] * 0.9f, jc[2] * 0.9f)
            // Silver knee guard + futuristic racing boot
            mb.addBox(0f, -0.42f, 0.08f, 0.12f, 0.10f, 0.05f, ac[0], ac[1], ac[2])
            mb.addTaperedBox(0f, -0.84f, 0.03f, 0.14f, 0.24f, -0.45f, 0.13f, 0.15f, 0f, -0.02f, 0.07f, 0.08f, 0.11f)
            mb.addBox(0f, -0.82f, 0.04f, 0.145f, 0.035f, 0.25f, tc[0], tc[1], tc[2])
            return mb.build()
        }

        // 6. Ground Energy Aura Ring
        val aura = MeshBuilder(512)
        aura.addCylinderY(0f, 0.02f, 0f, 0.72f, 0.78f, 0.02f, 20, auc[0], auc[1], auc[2], 0.45f)

        return HeroRig3D(
            headMesh = head.build(),
            torsoMesh = torso.build(),
            leftShoulderMesh = buildShoulder(-1f),
            rightShoulderMesh = buildShoulder(1f),
            leftArmMesh = buildArm(),
            rightArmMesh = buildArm(),
            leftLegMesh = buildLeg(),
            rightLegMesh = buildLeg(),
            auraMesh = aura.build()
        )
    }

    fun drawHero(
        rig: HeroRig3D,
        shader: ShaderProgram,
        vpMatrix: FloatArray,
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        yawDeg: Float,
        animation: HeroAnimation,
        timeSec: Float,
        scale: Float = 1.12f
    ) {
        val root = FloatArray(16)
        val part = FloatArray(16)
        val mvp = FloatArray(16)

        var bodyBob = 0f
        var bodyLean = 0f
        var bodyTurn = 0f
        var lArmX = 0f; var lArmZ = 8f
        var rArmX = 0f; var rArmZ = -8f
        var lLegX = 0f; var rLegX = 0f
        var headPitch = 0f; var headYaw = 0f

        when (animation) {
            HeroAnimation.IDLE -> {
                bodyBob = sin(timeSec * 2.2f) * 0.02f
                lArmX = sin(timeSec * 2.2f) * 4f
                rArmX = -sin(timeSec * 2.2f) * 4f
                headYaw = sin(timeSec * 0.9f) * 10f
            }
            HeroAnimation.WALK -> {
                val p = timeSec * 5.5f
                bodyBob = abs(sin(p)) * 0.04f
                lLegX = sin(p) * 28f
                rLegX = -sin(p) * 28f
                lArmX = -sin(p) * 22f
                rArmX = sin(p) * 22f
            }
            HeroAnimation.RUN -> {
                val p = timeSec * 10.5f
                bodyBob = abs(sin(p)) * 0.08f
                bodyLean = 14f
                lLegX = sin(p) * 52f
                rLegX = -sin(p) * 52f
                lArmX = -sin(p) * 48f
                rArmX = sin(p) * 48f
            }
            HeroAnimation.VICTORY -> {
                val p = timeSec * 6.0f
                bodyBob = abs(sin(p * 0.7f)) * 0.14f
                rArmX = -148f + sin(p) * 12f
                rArmZ = -18f
                lArmX = 18f
                lArmZ = 22f
                headPitch = -10f
            }
            HeroAnimation.CELEBRATION -> {
                val p = timeSec * 7.0f
                bodyBob = abs(sin(p)) * 0.22f
                bodyTurn = sin(timeSec * 2.4f) * 35f
                lArmX = -140f + sin(p) * 20f
                rArmX = -140f - sin(p) * 20f
                lArmZ = 28f
                rArmZ = -28f
                lLegX = sin(p) * 18f
                rLegX = -sin(p) * 18f
            }
            HeroAnimation.LOSE -> {
                bodyLean = 16f
                headPitch = 24f
                headYaw = sin(timeSec * 2.0f) * 14f
                lArmX = -12f; lArmZ = 6f
                rArmX = -12f; rArmZ = -6f
            }
        }

        // Aura on ground
        Matrix.setIdentityM(root, 0)
        Matrix.translateM(root, 0, worldX, worldY, worldZ)
        Matrix.scaleM(root, 0, scale, scale, scale)
        drawWithTransform(rig.auraMesh, shader, vpMatrix, root, mvp, emissive = 1.2f, specStrength = 0.1f)

        // Character Root (hips sit at y = 0.84)
        Matrix.setIdentityM(root, 0)
        Matrix.translateM(root, 0, worldX, worldY + bodyBob, worldZ)
        Matrix.rotateM(root, 0, yawDeg + bodyTurn, 0f, 1f, 0f)
        Matrix.scaleM(root, 0, scale, scale, scale)

        // Legs
        Matrix.setIdentityM(part, 0)
        Matrix.translateM(part, 0, -0.11f, 0.84f, 0f)
        Matrix.rotateM(part, 0, lLegX, 1f, 0f, 0f)
        val lLegWorld = FloatArray(16)
        Matrix.multiplyMM(lLegWorld, 0, root, 0, part, 0)
        drawWithTransform(rig.leftLegMesh, shader, vpMatrix, lLegWorld, mvp, emissive = 0.12f, specStrength = 0.45f)

        Matrix.setIdentityM(part, 0)
        Matrix.translateM(part, 0, 0.11f, 0.84f, 0f)
        Matrix.rotateM(part, 0, rLegX, 1f, 0f, 0f)
        val rLegWorld = FloatArray(16)
        Matrix.multiplyMM(rLegWorld, 0, root, 0, part, 0)
        drawWithTransform(rig.rightLegMesh, shader, vpMatrix, rLegWorld, mvp, emissive = 0.12f, specStrength = 0.45f)

        // Torso
        val torsoWorld = FloatArray(16)
        Matrix.setIdentityM(part, 0)
        Matrix.translateM(part, 0, 0f, 0.84f, 0f)
        Matrix.rotateM(part, 0, bodyLean, 1f, 0f, 0f)
        Matrix.multiplyMM(torsoWorld, 0, root, 0, part, 0)
        drawWithTransform(rig.torsoMesh, shader, vpMatrix, torsoWorld, mvp, emissive = 0.25f, specStrength = 0.65f)

        // Head
        Matrix.setIdentityM(part, 0)
        Matrix.translateM(part, 0, 0f, 0.64f, 0f)
        Matrix.rotateM(part, 0, headYaw, 0f, 1f, 0f)
        Matrix.rotateM(part, 0, headPitch, 1f, 0f, 0f)
        val headWorld = FloatArray(16)
        Matrix.multiplyMM(headWorld, 0, torsoWorld, 0, part, 0)
        drawWithTransform(rig.headMesh, shader, vpMatrix, headWorld, mvp, emissive = 0.18f, specStrength = 0.35f)

        // Glowing Shoulders + Arms
        Matrix.setIdentityM(part, 0)
        Matrix.translateM(part, 0, -0.27f, 0.50f, 0f)
        val lShWorld = FloatArray(16)
        Matrix.multiplyMM(lShWorld, 0, torsoWorld, 0, part, 0)
        drawWithTransform(rig.leftShoulderMesh, shader, vpMatrix, lShWorld, mvp, emissive = 0.95f, specStrength = 0.5f)

        Matrix.setIdentityM(part, 0)
        Matrix.translateM(part, 0, -0.29f, 0.48f, 0f)
        Matrix.rotateM(part, 0, lArmX, 1f, 0f, 0f)
        Matrix.rotateM(part, 0, lArmZ, 0f, 0f, 1f)
        val lArmWorld = FloatArray(16)
        Matrix.multiplyMM(lArmWorld, 0, torsoWorld, 0, part, 0)
        drawWithTransform(rig.leftArmMesh, shader, vpMatrix, lArmWorld, mvp, emissive = 0.18f, specStrength = 0.45f)

        Matrix.setIdentityM(part, 0)
        Matrix.translateM(part, 0, 0.27f, 0.50f, 0f)
        val rShWorld = FloatArray(16)
        Matrix.multiplyMM(rShWorld, 0, torsoWorld, 0, part, 0)
        drawWithTransform(rig.rightShoulderMesh, shader, vpMatrix, rShWorld, mvp, emissive = 0.95f, specStrength = 0.5f)

        Matrix.setIdentityM(part, 0)
        Matrix.translateM(part, 0, 0.29f, 0.48f, 0f)
        Matrix.rotateM(part, 0, rArmX, 1f, 0f, 0f)
        Matrix.rotateM(part, 0, rArmZ, 0f, 0f, 1f)
        val rArmWorld = FloatArray(16)
        Matrix.multiplyMM(rArmWorld, 0, torsoWorld, 0, part, 0)
        drawWithTransform(rig.rightArmMesh, shader, vpMatrix, rArmWorld, mvp, emissive = 0.18f, specStrength = 0.45f)
    }
}

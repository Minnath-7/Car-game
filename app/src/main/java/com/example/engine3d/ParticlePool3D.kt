package com.example.engine3d

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.random.Random

/**
 * Object-pooled 3D particle system with zero per-frame object allocations.
 * Supports nitro exhaust flames, drift tire smoke, and collision sparks.
 */
class ParticlePool3D(private val maxParticles: Int = 280) {
    private val active = BooleanArray(maxParticles)
    private val px = FloatArray(maxParticles)
    private val py = FloatArray(maxParticles)
    private val pz = FloatArray(maxParticles)
    private val vx = FloatArray(maxParticles)
    private val vy = FloatArray(maxParticles)
    private val vz = FloatArray(maxParticles)
    private val life = FloatArray(maxParticles)
    private val maxLife = FloatArray(maxParticles)
    private val size = FloatArray(maxParticles)
    private val pr = FloatArray(maxParticles)
    private val pg = FloatArray(maxParticles)
    private val pb = FloatArray(maxParticles)

    private var cursor = 0
    private val unitCubeMesh: Mesh
    private val modelScratch = FloatArray(16)
    private val mvpScratch = FloatArray(16)

    init {
        val mb = MeshBuilder(360)
        mb.addBox(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)
        unitCubeMesh = mb.build()
    }

    fun spawn(
        x: Float, y: Float, z: Float,
        velX: Float, velY: Float, velZ: Float,
        duration: Float,
        scale: Float,
        r: Float, g: Float, b: Float
    ) {
        val idx = cursor
        cursor = (cursor + 1) % maxParticles
        active[idx] = true
        px[idx] = x
        py[idx] = y
        pz[idx] = z
        vx[idx] = velX
        vy[idx] = velY
        vz[idx] = velZ
        life[idx] = duration
        maxLife[idx] = duration.coerceAtLeast(0.05f)
        size[idx] = scale
        pr[idx] = r
        pg[idx] = g
        pb[idx] = b
    }

    fun emitNitroFlame(x: Float, y: Float, z: Float, backX: Float, backZ: Float) {
        val jx = (Random.nextFloat() - 0.5f) * 2.2f
        val jy = (Random.nextFloat() - 0.3f) * 1.4f
        val jz = (Random.nextFloat() - 0.5f) * 2.2f
        spawn(
            x, y, z,
            backX * 14f + jx, jy, backZ * 14f + jz,
            duration = 0.24f + Random.nextFloat() * 0.12f,
            scale = 0.36f + Random.nextFloat() * 0.18f,
            r = 0.05f, g = 0.85f, b = 1.0f
        )
    }

    fun emitTireSmoke(x: Float, y: Float, z: Float) {
        spawn(
            x + (Random.nextFloat() - 0.5f) * 0.5f,
            y + 0.15f,
            z + (Random.nextFloat() - 0.5f) * 0.5f,
            (Random.nextFloat() - 0.5f) * 1.8f,
            1.4f + Random.nextFloat() * 1.2f,
            (Random.nextFloat() - 0.5f) * 1.8f,
            duration = 0.45f + Random.nextFloat() * 0.25f,
            scale = 0.48f + Random.nextFloat() * 0.28f,
            r = 0.78f, g = 0.84f, b = 0.92f
        )
    }

    fun emitSparks(x: Float, y: Float, z: Float, count: Int = 10) {
        for (i in 0 until count) {
            spawn(
                x, y + 0.45f, z,
                (Random.nextFloat() - 0.5f) * 16f,
                3f + Random.nextFloat() * 9f,
                (Random.nextFloat() - 0.5f) * 16f,
                duration = 0.32f + Random.nextFloat() * 0.20f,
                scale = 0.18f,
                r = 1.0f, g = 0.76f, b = 0.18f
            )
        }
    }

    fun update(dt: Float) {
        for (i in 0 until maxParticles) {
            if (!active[i]) continue
            life[i] -= dt
            if (life[i] <= 0f) {
                active[i] = false
                continue
            }
            px[i] += vx[i] * dt
            py[i] += vy[i] * dt
            pz[i] += vz[i] * dt
            vy[i] *= 0.96f
        }
    }

    fun drawAll(shader: ShaderProgram, vpMatrix: FloatArray) {
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        for (i in 0 until maxParticles) {
            if (!active[i]) continue
            val t = (life[i] / maxLife[i]).coerceIn(0f, 1f)
            val s = size[i] * (0.5f + 0.8f * t)
            Matrix.setIdentityM(modelScratch, 0)
            Matrix.translateM(modelScratch, 0, px[i], py[i], pz[i])
            Matrix.scaleM(modelScratch, 0, s, s, s)
            drawWithTransform(
                mesh = unitCubeMesh,
                shader = shader,
                vpMatrix = vpMatrix,
                modelMatrix = modelScratch,
                scratchMvp = mvpScratch,
                emissive = 1.1f,
                specStrength = 0f,
                tintR = pr[i],
                tintG = pg[i],
                tintB = pb[i],
                tintA = t * 0.85f
            )
        }
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    fun clear() {
        active.fill(false)
    }
}

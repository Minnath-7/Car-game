package com.example.engine3d

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Mesh(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / FLOATS_PER_VERTEX
    private val buffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }

    fun draw(
        shader: ShaderProgram,
        mvpMatrix: FloatArray,
        modelMatrix: FloatArray,
        emissive: Float = 0f,
        specStrength: Float = 0.35f,
        specPower: Float = 24f,
        tintR: Float = 1f,
        tintG: Float = 1f,
        tintB: Float = 1f,
        tintA: Float = 1f
    ) {
        if (vertexCount <= 0) return

        GLES20.glUniformMatrix4fv(shader.uMvpLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelLoc, 1, false, modelMatrix, 0)
        GLES20.glUniform1f(shader.uEmissiveLoc, emissive)
        GLES20.glUniform1f(shader.uSpecStrengthLoc, specStrength)
        GLES20.glUniform1f(shader.uSpecPowerLoc, specPower)
        GLES20.glUniform4f(shader.uColorTintLoc, tintR, tintG, tintB, tintA)

        buffer.position(0)
        GLES20.glEnableVertexAttribArray(shader.aPosLoc)
        GLES20.glVertexAttribPointer(shader.aPosLoc, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, buffer)

        buffer.position(3)
        GLES20.glEnableVertexAttribArray(shader.aNormalLoc)
        GLES20.glVertexAttribPointer(shader.aNormalLoc, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, buffer)

        buffer.position(6)
        GLES20.glEnableVertexAttribArray(shader.aColorLoc)
        GLES20.glVertexAttribPointer(shader.aColorLoc, 4, GLES20.GL_FLOAT, false, STRIDE_BYTES, buffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    }

    companion object {
        const val FLOATS_PER_VERTEX = 10 // x, y, z, nx, ny, nz, r, g, b, a
        const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4
    }
}

class MeshBuilder(initialCapacityFloats: Int = 4096) {
    private var data = FloatArray(initialCapacityFloats)
    private var size = 0

    private fun ensureCapacity(extra: Int) {
        if (size + extra > data.size) {
            var nextCap = data.size * 2
            while (nextCap < size + extra) nextCap *= 2
            data = data.copyOf(nextCap)
        }
    }

    fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        ensureCapacity(10)
        data[size++] = x
        data[size++] = y
        data[size++] = z
        data[size++] = nx
        data[size++] = ny
        data[size++] = nz
        data[size++] = r
        data[size++] = g
        data[size++] = b
        data[size++] = a
    }

    fun addTriangle(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val ux = x2 - x1
        val uy = y2 - y1
        val uz = z2 - z1
        val vx = x3 - x1
        val vy = y3 - y1
        val vz = z3 - z1
        var nx = uy * vz - uz * vy
        var ny = uz * vx - ux * vz
        var nz = ux * vy - uy * vx
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
        nx /= len; ny /= len; nz /= len
        addVertex(x1, y1, z1, nx, ny, nz, r, g, b, a)
        addVertex(x2, y2, z2, nx, ny, nz, r, g, b, a)
        addVertex(x3, y3, z3, nx, ny, nz, r, g, b, a)
    }

    fun addQuad(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        addTriangle(x1, y1, z1, x2, y2, z2, x3, y3, z3, r, g, b, a)
        addTriangle(x1, y1, z1, x3, y3, z3, x4, y4, z4, r, g, b, a)
    }

    /**
     * Adds an axis-aligned or tapered prism between bottom rectangle (cx, y0, cz, bw, bl)
     * and top rectangle (cx + topOffsetX, y1, cz + topOffsetZ, tw, tl).
     */
    fun addTaperedBox(
        cx: Float, y0: Float, cz: Float,
        bw: Float, bl: Float,
        y1: Float,
        tw: Float, tl: Float,
        topOffsetX: Float = 0f,
        topOffsetZ: Float = 0f,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val bx0 = cx - bw * 0.5f; val bx1 = cx + bw * 0.5f
        val bz0 = cz - bl * 0.5f; val bz1 = cz + bl * 0.5f

        val tx0 = cx + topOffsetX - tw * 0.5f; val tx1 = cx + topOffsetX + tw * 0.5f
        val tz0 = cz + topOffsetZ - tl * 0.5f; val tz1 = cz + topOffsetZ + tl * 0.5f

        // Top
        addQuad(tx0, y1, tz1, tx1, y1, tz1, tx1, y1, tz0, tx0, y1, tz0, r, g, b, a)
        // Bottom
        addQuad(bx0, y0, bz0, bx1, y0, bz0, bx1, y0, bz1, bx0, y0, bz1, r * 0.7f, g * 0.7f, b * 0.7f, a)
        // Front (+z)
        addQuad(bx0, y0, bz1, bx1, y0, bz1, tx1, y1, tz1, tx0, y1, tz1, r * 0.95f, g * 0.95f, b * 0.95f, a)
        // Back (-z)
        addQuad(bx1, y0, bz0, bx0, y0, bz0, tx0, y1, tz0, tx1, y1, tz0, r * 0.85f, g * 0.85f, b * 0.85f, a)
        // Left (-x)
        addQuad(bx0, y0, bz0, bx0, y0, bz1, tx0, y1, tz1, tx0, y1, tz0, r * 0.9f, g * 0.9f, b * 0.9f, a)
        // Right (+x)
        addQuad(bx1, y0, bz1, bx1, y0, bz0, tx1, y1, tz0, tx1, y1, tz1, r * 0.9f, g * 0.9f, b * 0.9f, a)
    }

    fun addBox(
        cx: Float, cy: Float, cz: Float,
        w: Float, h: Float, l: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        addTaperedBox(cx, cy - h * 0.5f, cz, w, l, cy + h * 0.5f, w, l, 0f, 0f, r, g, b, a)
    }

    fun addCylinderY(
        cx: Float, y0: Float, cz: Float,
        radiusBottom: Float, radiusTop: Float, height: Float,
        segments: Int = 12,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val y1 = y0 + height
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val x0b = cx + c0 * radiusBottom; val z0b = cz + s0 * radiusBottom
            val x1b = cx + c1 * radiusBottom; val z1b = cz + s1 * radiusBottom
            val x0t = cx + c0 * radiusTop; val z0t = cz + s0 * radiusTop
            val x1t = cx + c1 * radiusTop; val z1t = cz + s1 * radiusTop

            addQuad(x0b, y0, z0b, x1b, y0, z1b, x1t, y1, z1t, x0t, y1, z0t, r, g, b, a)
            addTriangle(cx, y1, cz, x0t, y1, z0t, x1t, y1, z1t, r * 1.05f, g * 1.05f, b * 1.05f, a)
            addTriangle(cx, y0, cz, x1b, y0, z1b, x0b, y0, z0b, r * 0.7f, g * 0.7f, b * 0.7f, a)
        }
    }

    fun addCylinderX(
        x0: Float, cy: Float, cz: Float,
        radius: Float, width: Float,
        segments: Int = 14,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val x1 = x0 + width
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val y0 = cy + cos(a0) * radius; val z0 = cz + sin(a0) * radius
            val y1 = cy + cos(a1) * radius; val z1 = cz + sin(a1) * radius
            addQuad(x0, y0, z0, x1, y0, z0, x1, y1, z1, x0, y1, z1, r, g, b, a)
            addTriangle(x1, cy, cz, x1, y0, z0, x1, y1, z1, r * 0.9f, g * 0.9f, b * 0.9f, a)
            addTriangle(x0, cy, cz, x0, y1, z1, x0, y0, z0, r * 0.9f, g * 0.9f, b * 0.9f, a)
        }
    }

    fun build(): Mesh = Mesh(data.copyOf(size))
}

fun drawWithTransform(
    mesh: Mesh,
    shader: ShaderProgram,
    vpMatrix: FloatArray,
    modelMatrix: FloatArray,
    scratchMvp: FloatArray,
    emissive: Float = 0f,
    specStrength: Float = 0.35f,
    specPower: Float = 24f,
    tintR: Float = 1f,
    tintG: Float = 1f,
    tintB: Float = 1f,
    tintA: Float = 1f
) {
    Matrix.multiplyMM(scratchMvp, 0, vpMatrix, 0, modelMatrix, 0)
    mesh.draw(
        shader = shader,
        mvpMatrix = scratchMvp,
        modelMatrix = modelMatrix,
        emissive = emissive,
        specStrength = specStrength,
        specPower = specPower,
        tintR = tintR,
        tintG = tintG,
        tintB = tintB,
        tintA = tintA
    )
}

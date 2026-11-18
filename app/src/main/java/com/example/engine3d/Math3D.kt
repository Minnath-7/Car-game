package com.example.engine3d

import com.example.model.TrackSpec
import com.example.model.TrackWaypoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float)

data class TrackSample(
    val s: Float,         // Cumulative arc length from start line
    val x: Float,
    val y: Float,
    val z: Float,
    val tx: Float,        // Forward unit tangent
    val ty: Float,
    val tz: Float,
    val rx: Float,        // Right unit vector (horizontal)
    val rz: Float,
    val yawRad: Float     // atan2(tx, tz)
)

class TrackSpline(val spec: TrackSpec, samplesPerSegment: Int = 36) {
    val samples: List<TrackSample>
    val totalLength: Float

    init {
        val pts = spec.controlPoints
        val n = pts.size
        val raw = ArrayList<Vec3>(n * samplesPerSegment)
        for (i in 0 until n) {
            val p0 = pts[(i - 1 + n) % n]
            val p1 = pts[i]
            val p2 = pts[(i + 1) % n]
            val p3 = pts[(i + 2) % n]
            for (step in 0 until samplesPerSegment) {
                val t = step.toFloat() / samplesPerSegment
                raw.add(catmullRom(p0, p1, p2, p3, t))
            }
        }

        val count = raw.size
        val built = ArrayList<TrackSample>(count)
        var accDist = 0f
        for (i in 0 until count) {
            val cur = raw[i]
            val next = raw[(i + 1) % count]
            val prev = raw[(i - 1 + count) % count]

            if (i > 0) {
                val dx = cur.x - prev.x
                val dy = cur.y - prev.y
                val dz = cur.z - prev.z
                accDist += sqrt(dx * dx + dy * dy + dz * dz)
            }

            var tx = next.x - prev.x
            var ty = next.y - prev.y
            var tz = next.z - prev.z
            val tLen = sqrt(tx * tx + ty * ty + tz * tz).coerceAtLeast(0.0001f)
            tx /= tLen
            ty /= tLen
            tz /= tLen

            // Right vector in XZ plane: tangent x up(0,1,0) => (-tz, 0, tx) or (tz, 0, -tx)
            // Forward = (tx, tz). Cross((tx,0,tz), (0,1,0)) = (-tz, 0, tx) -> right is (-tz, tx)
            val hLen = sqrt(tx * tx + tz * tz).coerceAtLeast(0.0001f)
            val rx = -tz / hLen
            val rz = tx / hLen
            val yaw = atan2(tx, tz)

            built.add(TrackSample(accDist, cur.x, cur.y, cur.z, tx, ty, tz, rx, rz, yaw))
        }
        val last = raw.last()
        val first = raw.first()
        val closeDx = first.x - last.x
        val closeDy = first.y - last.y
        val closeDz = first.z - last.z
        totalLength = accDist + sqrt(closeDx * closeDx + closeDy * closeDy + closeDz * closeDz)
        samples = built
    }

    fun sampleAtDistance(dist: Float): TrackSample {
        var d = dist % totalLength
        if (d < 0f) d += totalLength
        // Binary search on s
        var lo = 0
        var hi = samples.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (samples[mid].s <= d) lo = mid else hi = mid - 1
        }
        val a = samples[lo]
        val b = samples[(lo + 1) % samples.size]
        val segLen = if (lo == samples.size - 1) (totalLength - a.s) else (b.s - a.s)
        val f = if (segLen > 0.0001f) ((d - a.s) / segLen).coerceIn(0f, 1f) else 0f
        val tx = a.tx + (b.tx - a.tx) * f
        val ty = a.ty + (b.ty - a.ty) * f
        val tz = a.tz + (b.tz - a.tz) * f
        val tLen = sqrt(tx * tx + ty * ty + tz * tz).coerceAtLeast(0.0001f)
        val ntx = tx / tLen
        val nty = ty / tLen
        val ntz = tz / tLen
        val hLen = sqrt(ntx * ntx + ntz * ntz).coerceAtLeast(0.0001f)
        return TrackSample(
            s = d,
            x = a.x + (b.x - a.x) * f,
            y = a.y + (b.y - a.y) * f,
            z = a.z + (b.z - a.z) * f,
            tx = ntx,
            ty = nty,
            tz = ntz,
            rx = -ntz / hLen,
            rz = ntx / hLen,
            yawRad = atan2(ntx, ntz)
        )
    }

    fun findClosestIndex(x: Float, z: Float, hintIdx: Int = -1): Int {
        val count = samples.size
        var bestIdx = 0
        var bestDistSq = Float.MAX_VALUE

        if (hintIdx in 0 until count) {
            for (offset in -24..24) {
                val idx = (hintIdx + offset + count) % count
                val sp = samples[idx]
                val dx = x - sp.x
                val dz = z - sp.z
                val dSq = dx * dx + dz * dz
                if (dSq < bestDistSq) {
                    bestDistSq = dSq
                    bestIdx = idx
                }
            }
            if (bestDistSq < 1800f) return bestIdx
        }

        for (i in 0 until count) {
            val sp = samples[i]
            val dx = x - sp.x
            val dz = z - sp.z
            val dSq = dx * dx + dz * dz
            if (dSq < bestDistSq) {
                bestDistSq = dSq
                bestIdx = i
            }
        }
        return bestIdx
    }

    private fun catmullRom(
        p0: TrackWaypoint,
        p1: TrackWaypoint,
        p2: TrackWaypoint,
        p3: TrackWaypoint,
        t: Float
    ): Vec3 {
        val t2 = t * t
        val t3 = t2 * t
        fun interp(a: Float, b: Float, c: Float, d: Float): Float {
            return 0.5f * (
                (2f * b) +
                    (-a + c) * t +
                    (2f * a - 5f * b + 4f * c - d) * t2 +
                    (-a + 3f * b - 3f * c + d) * t3
                )
        }
        return Vec3(
            interp(p0.x, p1.x, p2.x, p3.x),
            interp(p0.y, p1.y, p2.y, p3.y),
            interp(p0.z, p1.z, p2.z, p3.z)
        )
    }

    companion object {
        fun wrapAngleRad(a: Float): Float {
            var r = a
            val twoPi = (2.0 * PI).toFloat()
            while (r > PI.toFloat()) r -= twoPi
            while (r < -PI.toFloat()) r += twoPi
            return r
        }

        fun degToRad(deg: Float): Float = deg * (PI.toFloat() / 180f)
        fun radToDeg(rad: Float): Float = rad * (180f / PI.toFloat())

        fun argbToRgba(hex: Long, alphaOverride: Float? = null): FloatArray {
            val a = alphaOverride ?: (((hex ushr 24) and 0xFF) / 255f)
            val r = ((hex ushr 16) and 0xFF) / 255f
            val g = ((hex ushr 8) and 0xFF) / 255f
            val b = (hex and 0xFF) / 255f
            return floatArrayOf(r, g, b, a)
        }
    }
}

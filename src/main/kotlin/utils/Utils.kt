package utils

import kotlin.experimental.and
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object Utils {

    fun findVertexAverage(vertices: Array<Vector2D>): Vector2D {
        var acc = Vector2D()
        vertices.forEach { acc += it }
        return acc / vertices.size.toDouble()
    }

    fun getPolygonCentroid(vertices: Array<Vector2D>): Vector2D {

        val n = vertices.size
        var signedArea = 0.0
        var ans = Vector2D()

        for (i in 0 until n) {

            val x0 = vertices[i].x
            val y0 = vertices[i].y
            val x1 = vertices[(i + 1) % n].x
            val y1 = vertices[(i + 1) % n].y

            val a = (x0 * y1) - (x1 * y0)

            signedArea += a
            ans += Vector2D((x0 + x1) * a, (y0 + y1) * a)
        }

        signedArea *= 0.5
        return Vector2D(ans.x / (6 * signedArea), ans.y / (6 * signedArea))
    }

    fun getShapeWithCentroidZero(verts: Array<Vector2D>): Array<Vector2D> {
        val centroid = Utils.getPolygonCentroid(verts)
        return Array(verts.size) {
            verts[it] - centroid
        }
    }

    fun rotatePointAroundPoint(point: Vector2D, center: Vector2D, angle: Double): Vector2D {
        var rVector = point
        val s = sin(-angle)
        val c = cos(-angle)
        rVector -= center
        rVector = Vector2D(rVector.x * c - rVector.y * s, rVector.x * s + rVector.y * c)
        return rVector + center
    }

    fun  getPolygonArea(verts: Array<Vector2D>): Double {
        var area = 0.0

        var j = verts.size - 1
        for (i in verts.indices) {
            area += (verts[j].x + verts[i].x) * (verts[j].y - verts[i].y)
            j = i
        }

        return abs(area / 2.0)
    }

    fun calculateMassAndInertia(verts: Array<Vector2D>, density: Double): Pair<Double, Double> {
        var c = Vector2D(0.0, 0.0)
        var area = 0.0
        var inert = 0.0
        val kInv3 = 1.0 / 3.0
        for (i in verts.indices) {
            val p1 = verts[i]
            val p2 = verts[(i + 1) % verts.size]
            val d = p1 cross p2
            val triangleArea = 0.5 * d
            area += triangleArea

            val weight = triangleArea * kInv3
            c += p1 * weight
            c += p2 * weight
            val intx2 = p1.x * p1.x + p2.x * p1.x + p2.x * p2.x
            val inty2 = p1.y * p1.y + p2.y * p1.y + p2.y * p2.y
            inert += 0.25f * kInv3 * d * (intx2 + inty2)
        }
        c *= (1.0f / area)

        val mass = abs(density * area)
        val inertia = abs(inert * density)
        return Pair(mass, inertia)
    }

    fun compareDouble(d1: Double, d2: Double, epsilon: Double = 0.01): Boolean {
        return abs(d1 - d2) < epsilon
    }

    fun byteArrayToInt(buffer: Array<Byte>): Int {
        return (buffer[3].toInt() shl 24) or
                (buffer[2].toInt() and 0xff shl 16) or
                (buffer[1].toInt() and 0xff shl 8) or
                (buffer[0].toInt() and 0xff)
    }

    fun byteArrayToLong(buffer: Array<Byte>): Long {
        return (buffer[7].toLong() shl 56) or
               (buffer[6].toLong() and 0xff shl 48) or
               (buffer[5].toLong() and 0xff shl 40) or
               (buffer[4].toLong() and 0xff shl 32) or
               (buffer[3].toLong() and 0xff shl 24) or
               (buffer[2].toLong() and 0xff shl 16) or
               (buffer[1].toLong() and 0xff shl 8) or
               (buffer[0].toLong() and 0xff)
    }

}

fun Int.toByteArray(): Array<Byte> {
    val buffer = Array<Byte>(4) {0}
    buffer[0] = (this shr 0).toByte()
    buffer[1] = (this shr 8).toByte()
    buffer[2] = (this shr 16).toByte()
    buffer[3] = (this shr 24).toByte()
    return buffer
}

fun Long.toByteArray(): Array<Byte> {
    val buffer = Array<Byte>(8) {0}
    buffer[0] = (this ushr 0).toByte()
    buffer[1] = (this ushr 8).toByte()
    buffer[2] = (this ushr 16).toByte()
    buffer[3] = (this ushr 24).toByte()
    buffer[4] = (this ushr 32).toByte()
    buffer[5] = (this ushr 40).toByte()
    buffer[6] = (this ushr 48).toByte()
    buffer[7] = (this ushr 56).toByte()
    return buffer
}

fun Double.toDeg() = this * 57.2958

fun Double.toRad() = this / 57.2958
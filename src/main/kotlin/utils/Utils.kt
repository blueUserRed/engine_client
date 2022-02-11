package utils

import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object Utils {

    /**
     * finds the average vertex for an array of vertices
     */
    fun findVertexAverage(vertices: Array<Vector2D>): Vector2D {
        var acc = Vector2D()
        vertices.forEach { acc += it }
        return acc / vertices.size.toDouble()
    }

    /**
     * finds the average vertex for a list of vertices
     */
    fun findVertexAverage(vertices: List<Vector2D>): Vector2D {
        var acc = Vector2D()
        vertices.forEach { acc += it }
        return acc / vertices.size.toDouble()
    }

    /**
     * calculates the [centroid][https://en.wikipedia.org/wiki/Centroid] of a polygon
     */
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

    /**
     * translates a polygon so that the centroid of the polygon is at the origin of the coordinate-system
     */
    fun getShapeWithCentroidZero(verts: Array<Vector2D>): Array<Vector2D> {
        val centroid = getPolygonCentroid(verts)
        return Array(verts.size) {
            verts[it] - centroid
        }
    }

    /**
     * rotates a point around a point
     * @param point the point that should be rotated
     * @param center the point around which the other should be rotated
     * @param angle the angle in rad that the point should be rotated by
     */
    fun rotatePointAroundPoint(point: Vector2D, center: Vector2D, angle: Double): Vector2D {
        var rVector = point
        val s = sin(-angle)
        val c = cos(-angle)
        rVector -= center
        rVector = Vector2D(rVector.x * c - rVector.y * s, rVector.x * s + rVector.y * c)
        return rVector + center
    }

    /**
     * @return the area of the polygon
     */
    fun getPolygonArea(verts: Array<Vector2D>): Double {
        var area = 0.0

        var j = verts.size - 1
        for (i in verts.indices) {
            area += (verts[j].x + verts[i].x) * (verts[j].y - verts[i].y)
            j = i
        }

        return abs(area / 2.0)
    }

    /**
     * calculates the mass and the inertia of a polygon
     * @param verts the polygon
     * @param density the density of the polygon
     * @return Pair(mass, inertia)
     */
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
            val x2 = p1.x * p1.x + p2.x * p1.x + p2.x * p2.x
            val y2 = p1.y * p1.y + p2.y * p1.y + p2.y * p2.y
            inert += 0.25f * kInv3 * d * (x2 + y2)
        }
        c *= (1.0f / area)

        val mass = abs(density * area)
        val inertia = abs(inert * density)
        return Pair(mass, inertia)
    }

    /**
     * compares to double with an epsilon
     * @param d1 the first double
     * @param d2 the second double
     * @param epsilon the epsilon that the doubles are allowed to deviate. Default = 0.01
     */
    fun compareDouble(d1: Double, d2: Double, epsilon: Double = 0.01): Boolean {
        return abs(d1 - d2) < epsilon
    }

    fun isConvex(verts: List<Vector2D>): Boolean {
        if (verts.size < 3) return false
        var res = 0
        for (i in verts.indices) {
            val tmp = verts[(i + 1) % verts.size]
            val v = Vector2D(tmp.x - verts[i].x, tmp.y - verts[i].y)
            val u = verts[(i + 2) % verts.size]

            if (i == 0) {
                res = (u.x * v.y - u.y * v.x + v.x * verts[i].y - v.y * verts[i].x).toInt()
            } else {
                val newRes = (u.x * v.y - u.y * v.x + v.x * verts[i].y - v.y * verts[i].x).toInt()
                if ( (newRes > 0 && res < 0) || (newRes < 0 && res > 0) ) return false
            }
        }
        return true
    }

    object AES {

        /**
         * the salt used for encryption
         */
        const val SALT: String = "l!?snf)h3-xcpni[=3jh"

        /**
         * the Iv used for encryption
         */
        val IV: ByteArray = byteArrayOf(13, 122, 29, -92, 101, -74, 27, -96,
            -85, -81, 24, -90, 22, 45, 32, -113)

        /**
         * encrypts a string using a key
         */
        fun encrypt(input: String, key: Long): String {
            val iv = IvParameterSpec(IV)

            val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyChars = Base64.getEncoder().encodeToString(key.toByteArray()).toCharArray()
            val keySpec = PBEKeySpec(keyChars, SALT.toByteArray(), 65536, 256)

            val secretKeySpec = SecretKeySpec(secretKeyFactory.generateSecret(keySpec).encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv)
            return Base64.getEncoder().encodeToString(
                cipher.doFinal(input.toByteArray(StandardCharsets.UTF_8))
            )
        }

        /**
         * decrypts a string using a key
         */
        fun decrypt(input: String, key: Long): String {
            val iv = IvParameterSpec(IV)

            val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyChars = Base64.getEncoder().encodeToString(key.toByteArray()).toCharArray()
            val keySpec = PBEKeySpec(keyChars, SALT.toByteArray(), 65536, 256)

            val secretKeySpec = SecretKeySpec(secretKeyFactory.generateSecret(keySpec).encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv)
            return String(cipher.doFinal(Base64.getDecoder().decode(input)))
        }

    }

}

fun Double.toDeg() = this * 57.2958

fun Double.toRad() = this / 57.2958

/**
 * compares to double with an epsilon
 * @receiver the first double
 * @param other the second double
 * @param epsilon the epsilon that the doubles are allowed to deviate. Default = 0.01
 */
fun Double.compare(other: Double, epsilon: Double = 0.01): Boolean {
    return Utils.compareDouble(this, other, epsilon)
}

/**
 * @return true if the char is hexadecimal
 */
fun Char.isHexadecimal(): Boolean {
    return this.code in 48..57 || this.code in 65..70 || this.code in 97..102
}

/**
 * same as [append][java.lang.StringBuilder.append]
 */
operator fun StringBuilder.plusAssign(other: String): Unit = run { this.append(other) }

/**
 * @return the highest double, -Double.MAX_VALUE if empty
 */
fun max(vararg doubles: Double): Double {
    var max = -Double.MAX_VALUE
    for (double in doubles) if (double > max) max = double
    return max
}

/**
 * @return the highest y-value, -Double.MAX_VALUE if empty
 */
fun maxY(vararg vectors: Vector2D): Double {
    var max = -Double.MAX_VALUE
    for (vector in vectors) if (vector.y > max) max = vector.y
    return max
}

/**
 * @return the lowest y-value, Double.MAX_VALUE if empty
 */
fun minY(vararg vectors: Vector2D): Double {
    var min = Double.MAX_VALUE
    for (vector in vectors) if (vector.y < min) min = vector.y
    return min
}

/**
 * the sum of all numbers in the list
 */
fun <T : Number> List<T>.sum(): Double {
    var sum = 0.0
    for (num in this) sum += num.toDouble()
    return sum
}

/**
 * the average of all numbers in the list
 */
fun <T : Number> List<T>.average(): Double {
    var sum = 0.0
    for (num in this) sum += num.toDouble()
    return sum / this.size
}

fun Long.toByteArray(): ByteArray {
    var long = this
    val result = ByteArray(8)
    for (i in 7 downTo 0) {
        result[i] = (long and 0xFF).toByte()
        long = long shl 8
    }
    return result
}
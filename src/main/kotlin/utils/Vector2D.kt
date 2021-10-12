package utils

import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.sqrt

class Vector2D (val x: Double, val y: Double) {

    constructor() : this(0.0, 0.0)
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
    constructor(n: Double) : this(n, n)
    constructor(n: Int) : this(n, n)

    val mag: Double
        get() = sqrt(this.x * this.x + this.y * this.y)

    val unit: Vector2D
        get() {
            if (this.mag == 0.0) return Vector2D(0.0, 0.0)
            return this / this.mag
        }

    val normal: Vector2D
        get() = Vector2D(-this.y, this.x)

    operator fun unaryMinus(): Vector2D {
        return Vector2D(-x, -y)
    }

    operator fun plus(other: Vector2D): Vector2D {
        return Vector2D(this.x + other.x, this.y + other.y)
    }

    operator fun minus(other: Vector2D): Vector2D {
        return Vector2D(this.x - other.x, this.y - other.y)
    }

    operator fun times(other: Double): Vector2D {
        return Vector2D(this.x * other, this.y * other)
    }

    operator fun div(other: Double): Vector2D {
        return Vector2D(this.x / other, this.y / other)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Vector2D) return false
        return Utils.compareDouble(this.x, other.x) && Utils.compareDouble(this.y, other.y)
    }

    fun getWithMag(mag: Double): Vector2D {
        return this.unit * mag
    }

    infix fun dot(other: Vector2D): Double {
        return this.x * other.x + this.y * other.y
    }

    infix fun cross(other: Vector2D): Double {
        return this.x * other.y - this.y * other.x
    }

    infix fun cross(other: Double): Vector2D {
        return Vector2D(other * this.y, -other * this.x)
    }

    override fun toString(): String {
        return "Vector($x, $y)"
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    fun serialize(output: DataOutputStream) {
        output.writeDouble(x)
        output.writeDouble(y)
    }

    companion object {

        fun deserializer(input: DataInputStream): Vector2D {
            val x = input.readDouble()
            val y = input.readDouble()
            return Vector2D(x, y)
        }

    }

}

infix fun Double.cross(other: Vector2D): Vector2D {
    return Vector2D(-this * other.y, this * other.x)
}
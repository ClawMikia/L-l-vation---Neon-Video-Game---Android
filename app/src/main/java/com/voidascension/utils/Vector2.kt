package com.voidascension.utils

import kotlin.math.*

data class Vector2(var x: Float = 0f, var y: Float = 0f) {

    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Vector2(x / scalar, y / scalar)

    fun length() = sqrt(x * x + y * y)
    fun lengthSquared() = x * x + y * y

    fun normalized(): Vector2 {
        val len = length()
        return if (len > 0.0001f) Vector2(x / len, y / len) else Vector2()
    }

    fun normalize(): Vector2 {
        val len = length()
        if (len > 0.0001f) { x /= len; y /= len }
        return this
    }

    fun distanceTo(other: Vector2) = (other - this).length()
    fun distanceSquaredTo(other: Vector2): Float {
        val dx = other.x - x; val dy = other.y - y
        return dx * dx + dy * dy
    }

    fun dot(other: Vector2) = x * other.x + y * other.y
    fun angle() = atan2(y, x)

    fun set(nx: Float, ny: Float): Vector2 { x = nx; y = ny; return this }
    fun set(other: Vector2): Vector2 { x = other.x; y = other.y; return this }

    fun lerp(target: Vector2, t: Float) = Vector2(
        x + (target.x - x) * t,
        y + (target.y - y) * t
    )

    companion object {
        fun fromAngle(angle: Float, length: Float = 1f) =
            Vector2(cos(angle) * length, sin(angle) * length)

        fun random(magnitude: Float = 1f): Vector2 {
            val angle = Math.random().toFloat() * 2f * PI.toFloat()
            return fromAngle(angle, magnitude)
        }

        val ZERO = Vector2(0f, 0f)
        val UP    = Vector2(0f, -1f)
        val DOWN  = Vector2(0f, 1f)
        val LEFT  = Vector2(-1f, 0f)
        val RIGHT = Vector2(1f, 0f)
    }
}

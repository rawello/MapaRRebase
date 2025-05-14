package com.rawello.mapar.data.quaternionExt

import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.inverse
import io.github.sceneview.collision.Vector3
import kotlin.math.sqrt

fun Quaternion.convert(quaternion2: Quaternion): Quaternion {
    return this * quaternion2
}

fun Quaternion.inverted(): Quaternion {
    return inverse(this)
}

fun Quaternion.Companion.lookRotation(forward: Vector3, up: Vector3 = Vector3.up()): Quaternion {
    val vector = Vector3(forward.x, forward.y, forward.z)
    val vector2 = Vector3.cross(up, vector).normalized()
    val vector3 = Vector3.cross(vector, vector2)
    val m00 = vector2.x
    val m01 = vector2.y
    val m02 = vector2.z
    val m10 = vector3.x
    val m11 = vector3.y
    val m12 = vector3.z
    val m20 = vector.x
    val m21 = vector.y
    val m22 = vector.z

    val num8 = (m00 + m11) + m22
    val quaternion = Quaternion()
    if (num8 > 0f) {
        var num = sqrt(num8 + 1f)
        quaternion.w = num * 0.5f
        num = 0.5f / num
        quaternion.x = (m12 - m21) * num
        quaternion.y = (m20 - m02) * num
        quaternion.z = (m01 - m10) * num
        return quaternion
    }
    if ((m00 >= m11) && (m00 >= m22)) {
        val num7 = sqrt(((1f + m00) - m11) - m22)
        val num4 = 0.5f / num7
        quaternion.x = 0.5f * num7
        quaternion.y = (m01 + m10) * num4
        quaternion.z = (m02 + m20) * num4
        quaternion.w = (m12 - m21) * num4
        return quaternion
    }
    if (m11 > m22) {
        val num6 = sqrt(((1f + m11) - m00) - m22)
        val num3 = 0.5f / num6
        quaternion.x = (m10 + m01) * num3
        quaternion.y = 0.5f * num6
        quaternion.z = (m21 + m12) * num3
        quaternion.w = (m20 - m02) * num3
        return quaternion
    }
    val num5 = sqrt(((1f + m22) - m00) - m11)
    val num2 = 0.5f / num5
    quaternion.x = (m20 + m02) * num2
    quaternion.y = (m21 + m12) * num2
    quaternion.z = 0.5f * num5
    quaternion.w = (m01 - m10) * num2

    return quaternion
}

private fun Quaternion.Companion.multiply(q1: Quaternion, q2: Quaternion): io.github.sceneview.collision.Quaternion {
    val w = q1.w * q2.w - q1.x * q2.x - q1.y * q2.y - q1.z * q2.z
    val x = q1.w * q2.x + q1.x * q2.w + q1.y * q2.z - q1.z * q2.y
    val y = q1.w * q2.y - q1.x * q2.z + q1.y * q2.w + q1.z * q2.x
    val z = q1.w * q2.z + q1.x * q2.y - q1.y * q2.x + q1.z * q2.w
    return io.github.sceneview.collision.Quaternion(w, x, y, z)
}

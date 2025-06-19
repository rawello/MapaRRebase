package com.rawello.mapar.presentation.helpers

import com.rawello.mapar.domain.pathfinder.Dijkstra
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.collision.Vector3
import io.github.sceneview.node.Node
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PathHelper {
    fun distance(from: FloatArray, to: FloatArray): Float {
        val dx = to[0] - from[0]
        val dy = to[1] - from[1]
        val dz = to[2] - from[2]
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }

    fun calculateOrientation(from: FloatArray, to: FloatArray): FloatArray {
        val direction = FloatArray(3) { to[it] - from[it] }
        val up = floatArrayOf(0f, 1f, 0f) // Вектор "вверх"

        val directionNormalized = Vector3(direction[0], direction[1], direction[2]).normalized()
        val upNormalized = Vector3(up[0], up[1], up[2]).normalized()

        val axis = Vector3.cross(directionNormalized, upNormalized).normalized()

        val angle = acos(Vector3.dot(directionNormalized, upNormalized))

        val quaternion = Quaternion(
            axis.x * sin(angle / 2),
            axis.y * sin(angle / 2),
            axis.z * sin(angle / 2),
            cos(angle / 2)
        ).toFloatArray()

        return quaternion
    }

    fun clearOldArrows(childNodes: MutableList<Node>) {
        childNodes.removeAll { it.name == "arrow" }
    }

    fun sumOfDistances(path: List<Dijkstra.Node>): Float {
        var totalDistance = 0f
        for (i in 0 until path.size - 1) {
            totalDistance += distance(path[i].position, path[i + 1].position)
        }
        return totalDistance
    }


    fun generateBezierPoints(p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray, steps: Int): List<FloatArray> {
        val points = mutableListOf<FloatArray>()
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val t2 = t * t
            val t3 = t2 * t
            val mt = 1 - t
            val mt2 = mt * mt
            val mt3 = mt2 * mt

            val x = mt3 * p0[0] + 3 * mt2 * t * p1[0] + 3 * mt * t2 * p2[0] + t3 * p3[0]
            val y = mt3 * p0[1] + 3 * mt2 * t * p1[1] + 3 * mt * t2 * p2[1] + t3 * p3[1]
            val z = mt3 * p0[2] + 3 * mt2 * t * p1[2] + 3 * mt * t2 * p2[2] + t3 * p3[2]
            points.add(floatArrayOf(x, y, z))
        }
        return points
    }

    fun generateBezierPath(path: List<Dijkstra.Node>): List<FloatArray> {
        val bezierPoints = mutableListOf<FloatArray>()
        for (i in 0 until path.size - 1) {
            val p0 = path[i].position
            val p3 = path[i + 1].position

            val controlPoint1 = floatArrayOf(
                p0[0] + (p3[0] - p0[0]) / 3,
                (p0[1] + p3[1]) / 2,
                p0[2] + (p3[2] - p0[2]) / 3
            )
            val controlPoint2 = floatArrayOf(
                p0[0] + 2 * (p3[0] - p0[0]) / 3,
                (p0[1] + p3[1]) / 2,
                p0[2] + 2 * (p3[2] - p0[2]) / 3
            )

            val segmentPoints = generateBezierPoints(p0, controlPoint1, controlPoint2, p3, 10)
            bezierPoints.addAll(segmentPoints)
        }
        return bezierPoints
    }

    fun calculateTurnDirection(currentPosition: FloatArray, nextPosition: FloatArray): String {
        val dx = nextPosition[0] - currentPosition[0]
        val dz = nextPosition[2] - currentPosition[2]

        val threshold = 0.2f

        return when {
            abs(dx) < threshold && abs(dz) < threshold -> "Вперед"
            dx > 0 && dz > 0 -> "Вправо"
            dx > 0 && dz < 0 -> "Влево"
            dx < 0 && dz > 0 -> "Влево"
            dx < 0 && dz < 0 -> "Вправо"
            dx > 0 -> "Вперед"
            dx < 0 -> "Назад"
            dz > 0 -> "Вправо"
            dz < 0 -> "Влево"
            else -> "Вперед"
        }
    }
}
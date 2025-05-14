package com.rawello.mapar.domain.pathfinder

class Dijkstra {
    data class Node(val id: String, val position: FloatArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Node

            if (id != other.id) return false
            if (!position.contentEquals(other.position)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + position.contentHashCode()
            return result
        }
    }

    data class Edge(val from: Node, val to: Node, val weight: Float = 1.0f)

    fun dijkstra(graph: Map<Node, List<Edge>>, start: Node, end: Node): List<Node> {
        val distances = mutableMapOf(start to 0f)
        val previousNodes = mutableMapOf<Node, Node>()
        val unvisitedNodes = graph.keys.toMutableSet()

        while (unvisitedNodes.isNotEmpty()) {
            val currentNode = unvisitedNodes.minBy { distances[it] ?: Float.POSITIVE_INFINITY }
            unvisitedNodes.remove(currentNode)

            val currentDistance = distances[currentNode] ?: continue

            for (edge in graph[currentNode] ?: continue) {
                val neighbor = edge.to
                val altDistance = currentDistance + edge.weight
                if (altDistance < (distances[neighbor] ?: Float.POSITIVE_INFINITY)) {
                    distances[neighbor] = altDistance
                    previousNodes[neighbor] = currentNode
                }
            }
        }

        val path = mutableListOf<Node>()
        var currentNode: Node? = end
        while (currentNode != null) {
            path.add(0, currentNode)
            currentNode = previousNodes[currentNode]
        }

        return path
    }
}
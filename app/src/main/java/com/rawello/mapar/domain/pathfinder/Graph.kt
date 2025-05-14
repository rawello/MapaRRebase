package com.rawello.mapar.domain.pathfinder

class Graph {
    private val graph = mutableMapOf<Dijkstra.Node, MutableList<Dijkstra.Edge>>()

    fun addNode(node: Dijkstra.Node) {
        graph[node] = mutableListOf()
    }

    fun addEdge(from: Dijkstra.Node, to: Dijkstra.Node, weight: Float = 1.0f) {
        graph[from]?.add(Dijkstra.Edge(from, to, weight))
    }

    fun getGraph(): Map<Dijkstra.Node, List<Dijkstra.Edge>> {
        return graph
    }
}
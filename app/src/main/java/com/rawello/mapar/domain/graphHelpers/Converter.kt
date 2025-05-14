package com.rawello.mapar.domain.graphHelpers

import android.util.Log
import com.rawello.mapar.data.models.NodeDb
import com.rawello.mapar.domain.pathfinder.Dijkstra
import com.rawello.mapar.domain.pathfinder.Graph

private fun convertNodesAndEdges(nodeDbs: List<NodeDb>): Pair<List<Dijkstra.Node>, List<Dijkstra.Edge>> {
    val nodes = nodeDbs.map { Dijkstra.Node(it.id.toString(), it.translation) }
    val edges = mutableListOf<Dijkstra.Edge>()

    try {
        nodeDbs.forEach { nodeDb ->
            if(nodeDb.neighbors.isNotEmpty()){
                val fromNode = nodes.find { it.id == nodeDb.id.toString() } ?: return@forEach
                val neighborIds = nodeDb.neighbors.split(",").map { it.toInt() }

                neighborIds.forEach { neighborId ->
                    val toNode = nodes.find { it.id == neighborId.toString() } ?: return@forEach
                    edges.add(Dijkstra.Edge(fromNode, toNode))
                }
            }
        }

    }
    catch (ex: Exception){
        Log.d("graph conv", ex.message!!)
    }

    return Pair(nodes, edges)
}

fun createGraph(nodeList: List<NodeDb>): Graph {
    val graph = Graph()

    nodeList.forEach { node ->
        graph.addNode(Dijkstra.Node(node.id.toString(), node.translation))
    }

    nodeList.forEach { node ->
        node.neighbors.split(",").forEach { neighborId ->
            val neighborNode = nodeList.find { it.id.toString() == neighborId }
            if (neighborNode != null) {
                graph.addEdge(Dijkstra.Node(node.id.toString(), node.translation), Dijkstra.Node(neighborNode.id.toString(), neighborNode.translation))
                graph.addEdge(Dijkstra.Node(neighborNode.id.toString(), neighborNode.translation), Dijkstra.Node(node.id.toString(), node.translation))
            }
        }
    }

    return graph
}

fun findPath(graph: Graph, startNode: Dijkstra.Node, endNode: Dijkstra.Node): List<Dijkstra.Node> {
    val dijkstra = Dijkstra()

    return dijkstra.dijkstra(graph.getGraph(), startNode, endNode)
}
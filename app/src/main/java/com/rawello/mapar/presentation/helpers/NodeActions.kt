package com.rawello.mapar.presentation.helpers

import android.util.Log
import com.rawello.mapar.data.database.NodeViewModel
import com.rawello.mapar.data.models.NodeDb
import io.github.sceneview.ar.arcore.quaternion
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.Node

class NodeActions {
    fun saveNode(viewModel: NodeViewModel, anchorNode: AnchorNode, type: String, title: String) {
        viewModel.insertNode(
            NodeDb(
                translation = anchorNode.pose.translation,
                quaternion = anchorNode.pose.quaternion.toFloatArray(),
                type = type,
                title = title,
                neighbors = "",
                buildingId = 1,
                nameNode = "node"
            )
        )
    }

    fun updateNodeNeighbors(insert: Boolean, node: NodeDb, neighborId: Int, viewModel: NodeViewModel, nodeList: List<NodeDb>) {
        try {
            val intListNeighbors = if (node.neighbors.isNotEmpty()) {
                node.neighbors.split(",").map { it.toInt() }
            } else {
                emptyList()
            }

            if (insert) {
                if (intListNeighbors.contains(neighborId)) {
                    throw Exception("уже есть, можно снекбар добавить")
                }
                val newListNeighbors = intListNeighbors + neighborId
                node.neighbors = newListNeighbors.joinToString(separator = ",") { it.toString() }

                nodeList.find { it.id == neighborId }?.let { neighborNode ->
                    val neighborNeighbors = if (neighborNode.neighbors.isNotEmpty()) {
                        neighborNode.neighbors.split(",").map { it.toInt() }
                    } else {
                        emptyList()
                    }
                    if (!neighborNeighbors.contains(node.id)) {
                        val newNeighborNeighbors = neighborNeighbors + node.id
                        neighborNode.neighbors = newNeighborNeighbors.joinToString(separator = ",") { it.toString() }
                        viewModel.updateNode(neighborNode)
                    }
                }
            } else {
                val newListNeighbors = intListNeighbors.filter { it != neighborId }
                node.neighbors = newListNeighbors.joinToString(separator = ",") { it.toString() }

                nodeList.find { it.id == neighborId }?.let { neighborNode ->
                    val neighborNeighbors = if (neighborNode.neighbors.isNotEmpty()) {
                        neighborNode.neighbors.split(",").map { it.toInt() }
                    } else {
                        emptyList()
                    }
                    if (neighborNeighbors.contains(node.id)) {
                        val newNeighborNeighbors = neighborNeighbors.filter { it != node.id }
                        neighborNode.neighbors = newNeighborNeighbors.joinToString(separator = ",") { it.toString() }
                        viewModel.updateNode(neighborNode)
                    }
                }
            }
            viewModel.updateNode(node)
        } catch (ex: Exception) {
            ex.message?.let { Log.d("exception", it) }
        }
    }

    fun deleteNode(nodeList: List<NodeDb>, selectedNode: Node, viewModel: NodeViewModel): List<NodeDb> {
        nodeList.find { it.id.toString() == selectedNode.name }
            ?.let { node -> viewModel.deleteNode(node) }

        nodeList.forEach { node ->
            try {
                updateNodeNeighbors(false, node, selectedNode.name!!.toInt(), viewModel, nodeList)
            } catch (_: Exception) {
            }
        }

        selectedNode.destroy()

        return nodeList
    }
}
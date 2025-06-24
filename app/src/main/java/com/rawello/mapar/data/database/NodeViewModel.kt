package com.rawello.mapar.data.database

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rawello.mapar.data.models.NodeDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

class NodeViewModel @Inject constructor(private val nodeDao: NodeDao) : ViewModel() {

    fun getAllNodes(): Flow<List<NodeDb>> {
        return nodeDao.getNodes()
    }

    suspend fun getNodesForBuilding(buildingId: Long): List<NodeDb> {
        Log.d("NodeViewModel", "Getting nodes for building $buildingId")
        val nodes = nodeDao.getNodesForBuilding(buildingId)
        Log.d("NodeViewModel", "Found ${nodes.size} nodes for building $buildingId")
        return nodes
    }

    fun insertNode(node: NodeDb) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("NodeViewModel", "Inserting node: id=${node.id}, type=${node.type}, title=${node.title}, buildingId=${node.buildingId}")
                nodeDao.insertNode(node)
            } catch (e: Exception) {
                Log.e("NodeViewModel", "Error inserting node: ${e.message}")
            }
        }
    }

    fun updateNode(node: NodeDb) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("NodeViewModel", "Updating node: id=${node.id}, type=${node.type}, title=${node.title}")
                nodeDao.updateNode(node)
            } catch (e: Exception) {
                Log.e("NodeViewModel", "Error updating node: ${e.message}")
            }
        }
    }

    fun deleteNode(node: NodeDb) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("NodeViewModel", "Deleting node: id=${node.id}")
                nodeDao.deleteNode(node)
            } catch (e: Exception) {
                Log.e("NodeViewModel", "Error deleting node: ${e.message}")
            }
        }
    }

    fun truncate() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("NodeViewModel", "Truncating nodes table")
                nodeDao.truncate()
            } catch (e: Exception) {
                Log.e("NodeViewModel", "Error truncating nodes table: ${e.message}")
            }
        }
    }

    suspend fun deleteNodesForBuilding(buildingId: Long) {
        try {
            Log.d("NodeViewModel", "Deleting all nodes for building $buildingId")
            nodeDao.deleteNodesForBuilding(buildingId)
        } catch (e: Exception) {
            Log.e("NodeViewModel", "Error deleting nodes for building $buildingId: ${e.message}")
        }
    }
}
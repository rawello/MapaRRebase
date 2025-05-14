package com.rawello.mapar.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rawello.mapar.data.models.NodeDb
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao{
    @Insert
    suspend fun insertNode(nodeDb: NodeDb)

    @Update
    suspend fun updateNode(nodeDb: NodeDb)

    @Delete
    suspend fun deleteNode(nodeDb: NodeDb)

    @Query("DELETE from Nodes")
    suspend fun truncate()

    @Query("select * from Nodes")
    fun getNodes(): Flow<List<NodeDb>>

    @Query("SELECT * FROM nodes WHERE buildingId = :buildingId")
    suspend fun getNodesForBuilding(buildingId: Long): List<NodeDb>
}
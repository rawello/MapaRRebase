package com.rawello.mapar.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rawello.mapar.data.models.Building

@Dao
interface BuildingDao {
    @Insert
    suspend fun insertBuilding(building: Building)

    @Query("SELECT * FROM buildings")
    suspend fun getAllBuildings(): List<Building>
}
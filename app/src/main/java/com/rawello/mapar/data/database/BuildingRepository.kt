package com.rawello.mapar.data.database

import android.content.Context
import com.rawello.mapar.data.models.Building

class BuildingRepository(context: Context) {
    private val buildingDao = AppDatabase.getDatabase(context).buildingDao()

    suspend fun getAllBuildings(): List<Building> = buildingDao.getAllBuildings()
    suspend fun insertBuilding(building: Building) = buildingDao.insertBuilding(building)
}

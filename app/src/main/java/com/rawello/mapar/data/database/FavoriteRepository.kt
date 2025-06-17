package com.rawello.mapar.data.database

import kotlinx.coroutines.flow.Flow

class FavoriteRepository(private val favoriteDao: FavoriteDao) {
    fun getAllFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
    suspend fun insertFavorite(favorite: FavoriteEntity) = favoriteDao.insertFavorite(favorite)
    suspend fun deleteFavorite(favorite: FavoriteEntity) = favoriteDao.deleteFavorite(favorite)
    suspend fun deleteByNodeId(nodeId: Int) = favoriteDao.deleteByNodeId(nodeId)
}

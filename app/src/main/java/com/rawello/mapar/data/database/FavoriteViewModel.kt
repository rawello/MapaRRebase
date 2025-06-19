package com.rawello.mapar.data.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FavoriteRepository
    val favorites: StateFlow<List<FavoriteEntity>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FavoriteRepository(db.favoriteDao())
        favorites = repository.getAllFavorites().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun addFavorite(buildingId: Int, buildingName: String) {
        viewModelScope.launch {
            repository.insertFavorite(FavoriteEntity(buildingId = buildingId, buildingName = buildingName))
        }
    }

    fun removeFavorite(buildingId: Int) {
        viewModelScope.launch {
            repository.deleteByBuildingId(buildingId)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FavoriteViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FavoriteViewModel(application) as T
            }
            throw IllegalArgumentException("Неизвестный класс ViewModel")
        }
    }
}

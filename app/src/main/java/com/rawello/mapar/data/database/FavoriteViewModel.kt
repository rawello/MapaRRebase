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

    fun addFavorite(nodeId: Int, nodeName: String) {
        viewModelScope.launch {
            repository.insertFavorite(FavoriteEntity(nodeId = nodeId, nodeName = nodeName))
        }
    }

    fun removeFavorite(nodeId: Int) {
        viewModelScope.launch {
            repository.deleteByNodeId(nodeId)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FavoriteViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FavoriteViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

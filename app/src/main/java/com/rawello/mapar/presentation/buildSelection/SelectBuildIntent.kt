package com.rawello.mapar.presentation.buildSelection

import com.rawello.mapar.data.models.Building

sealed class SelectBuildIntent {
    data object NavigateToNext : SelectBuildIntent()
    data object NavigateQr : SelectBuildIntent()
    data object NavigateText : SelectBuildIntent()
    data class SelectBuilding(val building: Building) : SelectBuildIntent()
    data class UpdateSearchQuery(val query: String) : SelectBuildIntent()
    data class ToggleFavorite(val buildingId: Long) : SelectBuildIntent()
    data class UpdateBuildings(val buildings: List<Building>) : SelectBuildIntent()
    data class UpdateErrorMessage(val message: String) : SelectBuildIntent()
    data class UpdateFavorites(val favorites: List<Long>) : SelectBuildIntent()
}

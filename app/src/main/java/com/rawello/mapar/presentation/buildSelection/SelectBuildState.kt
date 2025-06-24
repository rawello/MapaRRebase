package com.rawello.mapar.presentation.buildSelection

import com.rawello.mapar.data.models.Building

data class SelectBuildState(
    val buildings: List<Building> = emptyList(),
    val selectedBuilding: Building? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val favorites: List<Long> = emptyList()
)

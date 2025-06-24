package com.rawello.mapar.data.network

data class NodeResponse(
    val id: Int,
    val name: String,
    val translation: String,
    val quaternion: String,
    val modelPath: String,
    val title: String,
    val neighbors: String,
    val buildingId: Long
)
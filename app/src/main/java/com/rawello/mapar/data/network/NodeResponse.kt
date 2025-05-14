package com.rawello.mapar.data.network

data class NodeResponse(
    val id: Int,
    val name: Int,
    val translation: String,
    val quaternion: String,
    val modelPath: String,
    val title: String,
    val neighbors: String
)
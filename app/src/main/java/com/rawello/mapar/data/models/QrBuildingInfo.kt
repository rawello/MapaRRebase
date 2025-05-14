package com.rawello.mapar.data.models

import kotlinx.serialization.Serializable

@Serializable
data class QrBuildingInfo(
    val buildingId: Int,
    val buildingName: String,
    val buildingAddress: String,
    val nodeId: Int,
    val nodeName: String,
    val nodeTitle: String
) {
    fun toJson(): String {
        return """
        {
            "buildingId": $buildingId,
            "buildingName": "$buildingName",
            "buildingAddress": "$buildingAddress",
            "nodeId": $nodeId,
            "nodeName": "$nodeName",
            "nodeTitle": "$nodeTitle"
        }
        """.trimIndent()
    }
}

// Пример JSON для тестирования:
/*
{
    "buildingId": 4,
    "buildingName": "Building 1",
    "buildingAddress": "Address 1",
    "nodeId": 10,
    "nodeName": "19",
    "nodeTitle": "133"
}
*/
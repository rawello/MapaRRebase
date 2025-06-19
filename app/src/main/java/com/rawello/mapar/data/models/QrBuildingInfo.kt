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
            "type": "mapar_location",
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

package com.rawello.mapar.data.models

import com.google.gson.Gson

data class QrCodeData(
    val type: String = "mapar_location",
    val buildingId: Int,
    val buildingName: String,
    val buildingAddress: String,
    val nodeId: Int,
    val nodeName: String,
    val nodeTitle: String
) {
    companion object {
        private val gson = Gson()

        fun fromJson(json: String): QrCodeData? = runCatching {
            val data = gson.fromJson(json, QrCodeData::class.java)
            // Проверяем, что тип соответствует ожидаемому
            if (data.type == "mapar_location") {
                data
            } else {
                null
            }
        }.getOrNull()

        fun toJson(data: QrCodeData): String = gson.toJson(data)
    }
}
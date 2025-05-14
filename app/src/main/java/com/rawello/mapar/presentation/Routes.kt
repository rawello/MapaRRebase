package com.rawello.mapar.presentation

enum class Routes(val string: String) {
    First("first"),
    Second("second"),
    QrAlpha("qrAlpha"),
    Text("text"),
    ArScene("arScene/{buildingId}/{buildingName}"),
    ArSceneWithDestination("arScene/{buildingId}/{buildingName}/{destinationNodeId}")
}
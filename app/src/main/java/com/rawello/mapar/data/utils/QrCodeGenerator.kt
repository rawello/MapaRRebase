package com.rawello.mapar.data.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.rawello.mapar.data.models.QrCodeData
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

object QrCodeGenerator {
    private val writer = QRCodeWriter()
    private val hints = mapOf(EncodeHintType.MARGIN to 1)
    private const val QR_SIZE = 512

    fun generateQRCode(
        buildingId: Int, 
        buildingName: String, 
        buildingAddress: String,
        nodeId: Int,
        nodeName: String,
        nodeTitle: String
    ): Bitmap {
        val qrCodeData = QrCodeData(
            type = "mapar_location",
            buildingId = buildingId,
            buildingName = buildingName,
            buildingAddress = buildingAddress,
            nodeId = nodeId,
            nodeName = nodeName,
            nodeTitle = nodeTitle
        )
        
        val content = Gson().toJson(qrCodeData)
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints)
        val bitmap = createBitmap(QR_SIZE, QR_SIZE)
        
        for (x in 0 until QR_SIZE) {
            for (y in 0 until QR_SIZE) {
                bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        
        return bitmap
    }
}
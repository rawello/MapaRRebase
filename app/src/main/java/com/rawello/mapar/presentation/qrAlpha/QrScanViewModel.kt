package com.rawello.mapar.presentation.qrAlpha

import android.graphics.RectF
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rawello.mapar.data.models.QrCodeData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class QrScanViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<QrScanUIState> = MutableStateFlow(QrScanUIState())
    val uiState: StateFlow<QrScanUIState> = _uiState
    private var onNavigateToLocation: ((QrCodeData) -> Unit)? = null

    fun setNavigationCallback(callback: (QrCodeData) -> Unit) {
        onNavigateToLocation = callback
    }

    init {
        viewModelScope.launch {
            uiState.collect { state ->
                Log.d("QrScanViewModel", "State updated: showDialog=${state.showDialog}, qrData=${state.qrCodeData}")
            }
        }
    }

    fun onIntent(intent: QrScanIntent) {
        when (intent) {
            is QrScanIntent.QrCodeDetected -> onQrCodeDetected(intent.result, intent.boundingBox)
            is QrScanIntent.TargetPositioned -> onTargetPositioned(intent.rect)
            is QrScanIntent.DismissSnackbar -> dismissSnackbar()
            is QrScanIntent.ConfirmLocation -> confirmLocation()
            is QrScanIntent.DismissDialog -> dismissDialog()
        }
    }

    private fun onQrCodeDetected(result: String, boundingBox: RectF) {
        Log.d("QrScanViewModel", "QR Code detected: $result")
        viewModelScope.launch {
            val qrData = QrCodeData.fromJson(result)
            if (qrData != null) {
                Log.d("QrScanViewModel", "Valid QR data parsed: $qrData")
                _uiState.update { currentState ->
                    currentState.copy(
                        detectedQR = result,
                        showDialog = true,
                        qrCodeData = qrData,
                        boundingBox = boundingBox,
                        showSnackbar = false,
                        confirmed = false
                    )
                }
                // Прямой вызов диалога без ожидания LaunchedEffect
                _uiState.value.qrCodeData?.let { data ->
                    Log.d("QrScanViewModel", "Showing confirmation dialog for location: ${data.buildingName}")
                }
            } else {
                Log.e("QrScanViewModel", "Failed to parse QR data")
                _uiState.update { currentState ->
                    currentState.copy(
                        detectedQR = "Некорректный QR код",
                        showSnackbar = true,
                        boundingBox = boundingBox,
                        showDialog = false,
                        qrCodeData = null
                    )
                }
                delay(1500)
                _uiState.update { currentState -> 
                    currentState.copy(
                        showSnackbar = false,
                        boundingBox = null
                    )
                }
            }
        }
    }

    private fun onTargetPositioned(rect: Rect) {
        viewModelScope.launch {
            _uiState.update { it.copy(targetRect = rect) }
        }
    }

    private fun dismissSnackbar() {
        viewModelScope.launch {
            _uiState.update { it.copy(showSnackbar = false) }
        }
    }

    fun confirmLocation() {
        val currentQrData = _uiState.value.qrCodeData
        if (currentQrData != null) {
            onNavigateToLocation?.invoke(currentQrData)
            _uiState.update { it.copy(
                showDialog = false,
                qrCodeData = null
            ) }
        }
    }

    private fun dismissDialog() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                showDialog = false, 
                qrCodeData = null,
                boundingBox = null
            ) }
        }
    }
}

data class QrScanUIState(
    var loading: Boolean = false,
    var detectedQR: String = "",
    var targetRect: Rect = Rect.Zero,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    var showSnackbar: Boolean = false,
    var boundingBox: RectF? = null,
    var showDialog: Boolean = false,
    var confirmed: Boolean = false,
    var qrCodeData: QrCodeData? = null
)

sealed class QrScanIntent {
    data class QrCodeDetected(val result: String, val boundingBox: RectF) : QrScanIntent()
    data class TargetPositioned(val rect: Rect) : QrScanIntent()
    data object DismissSnackbar : QrScanIntent()
    data object ConfirmLocation : QrScanIntent()
    data object DismissDialog : QrScanIntent()
}
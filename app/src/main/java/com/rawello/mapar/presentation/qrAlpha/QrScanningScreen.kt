@file:OptIn(ExperimentalPermissionsApi::class)

package com.rawello.mapar.presentation.qrAlpha

import android.Manifest
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.rawello.mapar.data.ml.QrCodeAnalyzer
import com.rawello.mapar.presentation.Routes
import com.rawello.mapar.presentation.permission.FeatureThatRequiresCameraPermission
import com.rawello.mapar.presentation.permission.NeedCameraPermissionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class)
@Composable
@ExperimentalGetImage
fun QrScanningScreen(
    navController: NavController,
    viewModel: QrScanViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    
    // Эффект для освобождения ресурсов при уничтожении экрана
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                cameraProvider = null
            } catch (e: Exception) {
                Log.e("QrScanningScreen", "Error unbinding camera", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setNavigationCallback { qrCodeData ->
            // Освобождаем ресурсы камеры перед навигацией
            try {
                cameraProvider?.unbindAll()
                cameraProvider = null
            } catch (e: Exception) {
                Log.e("QrScanningScreen", "Error unbinding camera", e)
            }
            
            navController.navigate(Routes.ArScene.string
                .replace("{buildingId}", qrCodeData.buildingId.toString())
                .replace("{buildingName}", qrCodeData.buildingName)
                .replace("{destinationNodeId}", qrCodeData.nodeId.toString())
            )
        }
    }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    LaunchedEffect(uiState.confirmed) {
        if (uiState.confirmed && uiState.qrCodeData != null) {
            val qrData = uiState.qrCodeData!!
            delay(500) // Добавляем небольшую задержку
            navController.navigate(
                Routes.ArScene.string
                    .replace("{buildingId}", qrData.buildingId.toString())
                    .replace("{buildingName}", qrData.buildingName)
                    .replace("{destinationNodeId}", qrData.nodeId.toString())
            )
        }
    }

    DisposableEffect(
        key1 = lifecycleOwner,
        effect = {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    cameraPermissionState.launchPermissionRequest()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    )

    val previewView = remember { PreviewView(context) }

    val preview = Preview.Builder().build()
    val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    val targetRect by remember { derivedStateOf { uiState.targetRect } }

    LaunchedEffect(targetRect) {
        imageAnalysis.setAnalyzer(
            Dispatchers.Default.asExecutor(),
            QrCodeAnalyzer(
                targetRect = android.graphics.Rect(
                    targetRect.left.toInt(),
                    targetRect.top.toInt(),
                    targetRect.right.toInt(),
                    targetRect.bottom.toInt()
                ),
                previewView = previewView,
            ) { result, boundingBox ->
                viewModel.onIntent(QrScanIntent.QrCodeDetected(result, boundingBox))
            }
        )
    }

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(uiState.lensFacing)
        .build()
    var camera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(uiState.lensFacing) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider = provider
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )
        preview.surfaceProvider = previewView.surfaceProvider
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FeatureThatRequiresCameraPermission(
            deniedContent = { status ->
                NeedCameraPermissionScreen(
                    requestPermission = cameraPermissionState::launchPermissionRequest,
                    shouldShowRationale = status.shouldShowRationale
                )
            },
            grantedContent = {
                if (uiState.showDialog && uiState.qrCodeData != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.onIntent(QrScanIntent.DismissDialog) },
                        title = { Text("Точка найдена") },
                        text = {
                            Column {
                                Text("Здание: ${uiState.qrCodeData?.buildingName}")
                                Text("Адрес: ${uiState.qrCodeData?.buildingAddress}")
                                Text("Точка: ${uiState.qrCodeData?.nodeTitle}")
                                Text("Продолжить итерацию?")
                            }
                        },
                        confirmButton = {
                            Button(onClick = { viewModel.onIntent(QrScanIntent.ConfirmLocation) }) {
                                Text("Да")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.onIntent(QrScanIntent.DismissDialog) }) {
                                Text("Нет")
                            }
                        }
                    )
                }

                Scaffold(
                    snackbarHost = {
                        if (uiState.showSnackbar) {
                            Snackbar(
                                action = {
                                    TextButton(onClick = { viewModel.onIntent(QrScanIntent.DismissSnackbar) }) {
                                        Text("OK")
                                    }
                                }
                            ) {
                                Text(uiState.detectedQR)
                            }
                        }
                    }
                ) { paddingValues ->
                    Content(
                        modifier = Modifier.padding(paddingValues),
                        previewView = previewView,
                        onTargetPositioned = { rect -> viewModel.onIntent(QrScanIntent.TargetPositioned(rect)) },
                        boundingBox = uiState.boundingBox
                    )
                }
            }
        )

        if (uiState.showSnackbar) {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                action = {
                    TextButton(onClick = { viewModel.onIntent(QrScanIntent.DismissSnackbar) }) {
                        Text("OK")
                    }
                }
            ) {
                Text(uiState.detectedQR)
            }
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier,
    previewView: PreviewView,
    onTargetPositioned: (Rect) -> Unit,
    boundingBox: RectF?
) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )
        val widthInPx: Float
        val heightInPx: Float
        val radiusInPx: Float
        with(LocalDensity.current) {
            widthInPx = 250.dp.toPx()
            heightInPx = 250.dp.toPx()
            radiusInPx = 16.dp.toPx()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .5f)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .size(250.dp)
                    .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                    .onGloballyPositioned {
                        onTargetPositioned(it.boundsInRoot())
                    }
            ) {
                val offset = Offset(
                    x = (size.width - widthInPx) / 2,
                    y = (size.height - heightInPx) / 2,
                )
                val cutoutRect = Rect(offset, Size(widthInPx, heightInPx))
                drawRoundRect(
                    topLeft = cutoutRect.topLeft,
                    size = cutoutRect.size,
                    cornerRadius = CornerRadius(radiusInPx, radiusInPx),
                    color = Color.Transparent,
                    blendMode = BlendMode.Clear
                )

                boundingBox?.let { box ->
                    drawRoundRect(
                        topLeft = Offset(box.left, box.top),
                        size = Size(box.width(), box.height()),
                        cornerRadius = CornerRadius(radiusInPx, radiusInPx),
                        color = Color.Red.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
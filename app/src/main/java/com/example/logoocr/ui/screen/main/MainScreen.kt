package com.example.logoocr.ui.screen.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logoocr.core.camera.getCameraProvider
import com.example.logoocr.core.storage.ImageStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToConfirm: (Long) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForPermission(
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToConfirm.collect { id ->
            onNavigateToConfirm(id)
        }
    }

    val imageCaptureState = remember { mutableStateOf<ImageCapture?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RecoLens") },
                actions = {
                    androidx.compose.material3.TextButton(onClick = onNavigateToRegister) {
                        Text("ロゴ登録")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.TextButton(onClick = onNavigateToGallery) {
                        Text("ロゴ一覧")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.TextButton(onClick = onNavigateToHistory) {
                        Text("履歴")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val imageCapture = imageCaptureState.value
                    if (imageCapture != null && hasCameraPermission && !uiState.isProcessing) {
                        coroutineScope.launch {
                            val photoFile = try {
                                captureImage(context, imageCapture)
                            } catch (e: Exception) {
                                viewModel.resetMessage()
                                null
                            }
                            if (photoFile != null) {
                                viewModel.onImageCaptured(photoFile)
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text("撮影")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (hasCameraPermission) {
                CameraPreview(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    lifecycleOwner = lifecycleOwner,
                    onImageCaptureReady = { imageCaptureState.value = it }
                )
            } else {
                PermissionRequiredMessage(onRequest = { permissionLauncher() })
            }

            if (uiState.isProcessing) {
                RowCentered {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("解析中...")
                }
            }

            uiState.lastRecognition?.let { summary ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "直近の結果",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = summary.message,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "信頼度: ${(summary.confidence * 100f).toInt()}%"
                        )
                    }
                }
            }

            uiState.message?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!uiState.hasLogos) {
                Text(
                    text = "ロゴが未登録です。まず「ロゴ登録」から登録してください。",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PermissionRequiredMessage(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("カメラ権限が必要です。")
        Button(onClick = onRequest) {
            Text("権限を許可")
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    var currentImageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    LaunchedEffect(lifecycleOwner) {
        val cameraProvider = getCameraProvider(context)
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
        currentImageCapture = imageCapture
        onImageCaptureReady(imageCapture)
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

private suspend fun captureImage(
    context: Context,
    imageCapture: ImageCapture
): File? = suspendCancellableCoroutine { continuation ->
    val photoFile = ImageStorage.createImageFile(context, prefix = "capture")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                if (!continuation.isCompleted) {
                    continuation.resume(photoFile)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                if (!continuation.isCompleted) {
                    photoFile.delete()
                    continuation.resume(null)
                }
            }
        }
    )

    continuation.invokeOnCancellation {
        photoFile.delete()
    }
}

@Composable
private fun rememberLauncherForPermission(
    onResult: (Boolean) -> Unit
): () -> Unit {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = onResult
    )
    return { launcher.launch(Manifest.permission.CAMERA) }
}

@Composable
private fun RowCentered(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

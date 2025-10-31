package com.example.logoocr.ui.screen.register

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.logoocr.core.storage.ImageStorage
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterLogoScreen(
    onBack: () -> Unit,
    viewModel: RegisterLogoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var launchAfterPermission by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null) {
            viewModel.setImageFromCamera(file)
        } else {
            file?.delete()
        }
        pendingCameraFile = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.setImageFromGallery(it) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && launchAfterPermission) {
            val file = ImageStorage.createImageFile(context, prefix = "logo")
            pendingCameraFile = file
            val uri = ImageStorage.getUriForFile(context, file)
            cameraLauncher.launch(uri)
        } else if (!granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("カメラ権限が必要です")
            }
        }
        launchAfterPermission = false
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            snackbarHostState.showSnackbar("ロゴを登録しました")
            viewModel.consumeCompletion()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ロゴ登録") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("戻る") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(
                value = uiState.brandName,
                onValueChange = viewModel::onBrandNameChange,
                label = { Text("ブランド名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("備考 (任意)") },
                modifier = Modifier.fillMaxWidth()
            )

            AsyncImage(
                model = uiState.previewUri,
                contentDescription = "選択されたロゴ画像",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            RowButtons(
                onPickFromGallery = {
                    galleryLauncher.launch(arrayOf("image/*"))
                },
                onCaptureFromCamera = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        val file = ImageStorage.createImageFile(context, prefix = "logo")
                        pendingCameraFile = file
                        val uri = ImageStorage.getUriForFile(context, file)
                        cameraLauncher.launch(uri)
                    } else {
                        launchAfterPermission = true
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = viewModel::saveLogo,
                enabled = !uiState.isProcessing
            ) {
                Text("登録する")
            }
        }
    }
}

@Composable
private fun RowButtons(
    onPickFromGallery: () -> Unit,
    onCaptureFromCamera: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onPickFromGallery, modifier = Modifier.weight(1f)) {
            Text("ギャラリーから選択")
        }
        Button(onClick = onCaptureFromCamera, modifier = Modifier.weight(1f)) {
            Text("カメラで撮影")
        }
    }
}

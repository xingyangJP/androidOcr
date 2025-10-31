package com.example.logoocr.core.camera

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await

suspend fun getCameraProvider(context: Context): ProcessCameraProvider =
    ProcessCameraProvider.getInstance(context).await()

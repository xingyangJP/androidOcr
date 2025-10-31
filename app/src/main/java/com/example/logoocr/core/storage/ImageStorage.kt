package com.example.logoocr.core.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageStorage {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    fun createImageFile(context: Context, prefix: String = "IMG_"): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.filesDir
        return File.createTempFile(
            "${prefix}${timestamp}_",
            ".jpg",
            storageDir
        )
    }

    fun getUriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)

    fun copyToInternalStorage(context: Context, uri: Uri, fileNamePrefix: String): String? {
        val resolver: ContentResolver = context.contentResolver
        val destinationDir = File(context.filesDir, "logos").apply { mkdirs() }
        val file = File(destinationDir, "$fileNamePrefix-${System.currentTimeMillis()}.jpg")
        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        }.getOrNull()
    }
}

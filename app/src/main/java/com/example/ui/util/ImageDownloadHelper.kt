package com.example.ui.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageDownloadHelper {

    suspend fun saveBitmapToPictures(
        context: Context,
        bitmap: Bitmap,
        storyTitle: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val safePrefix = storyTitle
                .replace(Regex("[^a-zA-Z0-9\\u0D80-\\u0DFF_\\-]+"), "_")
                .trim('_')
                .ifBlank { "CeyloneseFuturism" }
                .take(28)
            val fileName = "${safePrefix}_${System.currentTimeMillis()}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/CeyloneseFuturism"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri: Uri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: throw IllegalStateException("MediaStore URI සෑදීමට නොහැකි විය.")

                try {
                    resolver.openOutputStream(uri)?.use { stream ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                            throw IllegalStateException("රූපය සුරැකීම අසාර්ථක විය.")
                        }
                    } ?: throw IllegalStateException("රූප ගොනුව විවෘත කිරීමට නොහැකි විය.")

                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    throw e
                }
                "Pictures/CeyloneseFuturism/$fileName"
            } else {
                val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: context.filesDir
                val targetDir = File(picturesDir, "CeyloneseFuturism").apply { mkdirs() }
                val targetFile = File(targetDir, fileName)
                FileOutputStream(targetFile).use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw IllegalStateException("රූපය සුරැකීම අසාර්ථක විය.")
                    }
                }
                targetFile.absolutePath
            }
        }
    }
}

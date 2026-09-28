package dev.dettmer.simplenotes.ui.editor

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

internal fun createCameraPhotoUri(context: Context): Uri {
    val directory = File(context.cacheDir, "camera_photos").apply { mkdirs() }
    val file = File(directory, "photo-${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

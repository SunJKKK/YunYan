package com.sunjk.sunjktool.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtil {

    /**
     * Copy a content URI to app-internal storage and return the internal file path.
     * Returns null if the copy fails.
     */
    fun copyToInternal(context: Context, uri: Uri): String? {
        return try {
            val ext = getExtension(context, uri) ?: "jpg"
            val fileName = "img_${UUID.randomUUID()}.$ext"
            val file = File(context.filesDir, "images/$fileName")
            file.parentFile?.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Delete an image file from internal storage.
     */
    fun deleteInternal(path: String?) {
        if (path != null) {
            File(path).delete()
        }
    }

    private fun getExtension(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                val name = cursor.getString(nameIndex)
                name.substringAfterLast('.', "")
            } else null
        } ?: uri.path?.substringAfterLast('.', "")
    }
}

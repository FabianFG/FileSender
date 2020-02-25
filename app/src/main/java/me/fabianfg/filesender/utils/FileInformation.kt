package me.fabianfg.filesender.utils

import android.annotation.TargetApi
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns


@TargetApi(Build.VERSION_CODES.KITKAT)
fun Uri.getDisplayName(context: Context): String? {
    var result: String? = null
    if (this.scheme.equals("content")) {
        context.contentResolver.query(this, null, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
    }
    if (result == null) {
        result = this.path!!.substringAfterLast('/')
    }
    return result
}

fun Uri.getSize(context: Context): String? {
    var fileSize: String? = null
    val cursor = context.contentResolver
        .query(this, null, null, null, null, null)
    if (cursor != null && cursor.moveToFirst()) { // get file size
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (!cursor.isNull(sizeIndex)) {
            fileSize = cursor.getString(sizeIndex)
        }
    }
    cursor?.close()
    return fileSize
}
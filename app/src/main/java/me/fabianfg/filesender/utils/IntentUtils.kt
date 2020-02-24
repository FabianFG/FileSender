@file:Suppress("DEPRECATION")
package me.fabianfg.filesender.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import me.fabianfg.filesender.R
import java.io.File

fun Context.openFile(file : File) {
    val fileUri = FileProvider.getUriForFile(this, "me.fabianfg.provider", file)
    val mimeMap = MimeTypeMap.getSingleton()
    var intent = Intent(Intent.ACTION_VIEW)
    val mimeType = mimeMap.getMimeTypeFromExtension(file.extension) ?: "*/*"
    if (mimeType == "application/vnd.android.package-archive") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    } else {
        intent.setDataAndType(fileUri, mimeType)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    try {
        startActivity(intent)
    } catch (e : ActivityNotFoundException) {
        intent.setDataAndType(fileUri, "*/*")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching { startActivity(intent) }
            .onFailure { Toast.makeText(this, getString(R.string.file_open_failed, file.name), Toast.LENGTH_LONG).show() }
    }
}
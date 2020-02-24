package me.fabianfg.filesender.frontend.ui.send

import android.content.Context
import android.net.Uri
import com.google.gson.annotations.Expose
import me.fabianfg.filesender.server.FileDescriptor
import me.fabianfg.filesender.utils.FileInformation
import java.io.IOException

class UriFileDescriptor(@Expose(serialize = false, deserialize = false) val context: Context, @Expose(serialize = false, deserialize = false) val uri: Uri) : FileDescriptor(FileInformation.getPath(context, uri)?.substringAfterLast('/')?: "Unknown Name", FileInformation.getSize(context, uri)?.toLongOrNull() ?: 0L) {
    override fun openInputStream() = context.contentResolver.openInputStream(uri) ?: throw IOException("Failed to open input stream on ${uri.path}")
    override fun toString() = fileName
}
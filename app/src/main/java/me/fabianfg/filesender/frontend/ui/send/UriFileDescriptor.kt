package me.fabianfg.filesender.frontend.ui.send

import android.content.Context
import android.net.Uri
import com.google.gson.annotations.Expose
import me.fabianfg.filesender.server.FileDescriptor
import me.fabianfg.filesender.utils.getDisplayName
import me.fabianfg.filesender.utils.getSize
import java.io.IOException

class UriFileDescriptor(@Expose(serialize = false, deserialize = false) val context: Context, @Expose(serialize = false, deserialize = false) val uri: Uri) : FileDescriptor(uri.getDisplayName(context)?: "Unknown Name", uri.getSize(context)?.toLongOrNull() ?: 0L) {
    override fun openInputStream() = context.contentResolver.openInputStream(uri) ?: throw IOException("Failed to open input stream on ${uri.path}")
    override fun toString() = fileName
}
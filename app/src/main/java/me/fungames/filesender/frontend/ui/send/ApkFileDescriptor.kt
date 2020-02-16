package me.fungames.filesender.frontend.ui.send

import com.google.gson.annotations.Expose
import me.fungames.filesender.server.FileDescriptor
import java.io.File

class ApkFileDescriptor(@Expose(serialize = false, deserialize = false) val file : File, @Expose(serialize = false, deserialize = false) val appName : String) : FileDescriptor(appName, file.length()) {
    override fun openInputStream() = file.inputStream()
}
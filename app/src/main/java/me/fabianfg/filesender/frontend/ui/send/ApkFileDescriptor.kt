package me.fabianfg.filesender.frontend.ui.send

import com.google.gson.annotations.Expose
import me.fabianfg.filesender.server.FileDescriptor
import java.io.File

class ApkFileDescriptor(@Expose(serialize = false, deserialize = false) val file : File, @Expose(serialize = false, deserialize = false) val appName : String) : FileDescriptor(appName, file.length()) {
    override fun openInputStream() = file.inputStream()
}
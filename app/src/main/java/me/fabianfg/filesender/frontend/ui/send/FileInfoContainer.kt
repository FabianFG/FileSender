package me.fabianfg.filesender.frontend.ui.send

import me.fabianfg.filesender.server.FileDescriptor

class FileInfoContainer(var id : Int, val descriptor: FileDescriptor) {
    override fun toString() = descriptor.toString()
}
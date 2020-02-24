package me.fungames.filesender.frontend.ui.send

import me.fungames.filesender.server.FileDescriptor

class FileInfoContainer(var id : Int, val descriptor: FileDescriptor) {
    override fun toString() = descriptor.toString()
}
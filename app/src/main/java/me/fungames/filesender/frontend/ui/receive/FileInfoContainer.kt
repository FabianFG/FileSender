package me.fungames.filesender.frontend.ui.receive

import me.fungames.filesender.server.BasicFileDescriptor

class FileInfoContainer(val id : Int, val descriptor: BasicFileDescriptor) {
    override fun toString() = descriptor.toString()
}
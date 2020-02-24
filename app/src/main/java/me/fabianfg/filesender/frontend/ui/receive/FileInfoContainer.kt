package me.fabianfg.filesender.frontend.ui.receive

import me.fabianfg.filesender.server.BasicFileDescriptor

class FileInfoContainer(val id : Int, val descriptor: BasicFileDescriptor) {
    override fun toString() = descriptor.toString()
}
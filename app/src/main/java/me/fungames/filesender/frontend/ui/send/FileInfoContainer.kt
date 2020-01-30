package me.fungames.filesender.frontend.ui.send

class FileInfoContainer(val id : Int, val descriptor: UriFileDescriptor) {
    override fun toString() = descriptor.toString()
}
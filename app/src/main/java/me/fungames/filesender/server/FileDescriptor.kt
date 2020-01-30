package me.fungames.filesender.server

import java.io.InputStream

open class BasicFileDescriptor(val fileName: String, val fileSize: Long) {
    override fun toString() = fileName
}

abstract class FileDescriptor(fileName : String, fileSize : Long) : BasicFileDescriptor(fileName, fileSize) {
    abstract fun openInputStream() : InputStream
}
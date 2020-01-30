package me.fungames.filesender.model.payloads

import me.fungames.filesender.server.BasicFileDescriptor

data class FileListUpdatePacket(val files : Map<Int, BasicFileDescriptor>)
class RequestFileListUpdatePacket
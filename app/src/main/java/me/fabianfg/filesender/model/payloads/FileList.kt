package me.fabianfg.filesender.model.payloads

import me.fabianfg.filesender.server.BasicFileDescriptor

data class FileListUpdatePacket(val files : Map<Int, BasicFileDescriptor>)
class RequestFileListUpdatePacket
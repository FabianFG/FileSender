package me.fungames.filesender.model.payloads

import me.fungames.filesender.server.FailReason

data class FileShareRequestPacket(val fileHandleId : Int, val fileSize : Long, val fileName : String, val chunkCount : Long, val chunkSize : Long)

data class FileShareAcceptPacket(val fileHandleId : Int)

data class FileShareDeniedPacket(val fileHandleId : Int, val reason: FailReason)

data class FileShareChunkReceived(val fileHandleId: Int, val chunkId : Long, val chunkSize: Long)

data class FileShareInvalidPacket(val message : String)

data class RequestFileDownloadPacket(val fileId : Int)

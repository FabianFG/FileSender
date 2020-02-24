package me.fabianfg.filesender.client

import me.fabianfg.filesender.debug
import me.fabianfg.filesender.info
import me.fabianfg.filesender.model.payloads.FileShareChunkReceived
import me.fabianfg.filesender.sendPacket
import org.java_websocket.WebSocket
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class FileReceiveHandle(private val socket : WebSocket, val fileHandleId : Int, val fileSize : Long, val fileName : String, val chunkCount : Long, private val chunkSize : Long, downloadDirectory : String, val onStarted : (FileReceiveHandle) -> Unit, val onProgressUpdate : (FileReceiveHandle) -> Unit, val onCompleted : (FileReceiveHandle) -> Unit) {

    val file = File("$downloadDirectory/$fileName")
    private val raFile = RandomAccessFile(file, "rw")

    var numReceivedChunks = 0L
    var receivedBytes = 0L

    init {
        debug("Reserving $fileSize bytes")
        raFile.setLength(fileSize)
        debug("Done")
    }

    fun receivedChunk(chunkId : Long, buffer : ByteBuffer) {
        if (chunkId == 0L)
            onStarted(this)
        val offset = chunkId * chunkSize
        raFile.seek(offset)
        val chunkSize = buffer.remaining()
        raFile.write(buffer.array(), buffer.position(), chunkSize)
        numReceivedChunks++
        receivedBytes += chunkSize
        onProgressUpdate(this)
        socket.sendPacket(FileShareChunkReceived(fileHandleId, chunkId, chunkSize.toLong()))
        socket.sendPing()
        if(numReceivedChunks == chunkCount) {
            info("Received $fileName successfully")
            raFile.close()
            onCompleted(this)
        }
    }
}
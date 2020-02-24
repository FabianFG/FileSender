package me.fungames.filesender.server

import me.fungames.filesender.*
import me.fungames.filesender.model.payloads.FileShareRequestPacket
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask


enum class FileHandleState {
    INACTIVE,
    WAITING_FOR_CLIENT,
    ACTIVE,
    COMPLETED,
    FAILED
}

enum class FailReason {
    CLIENT_DISCONNECTED,
    CLIENT_DENIED,
    CLIENT_NOT_ENOUGH_STORAGE,
    TIMEOUT,
    FAILED_TO_OPEN_INPUT_STREAM
}

class FileHandle(val fileHandleId : Int, val client : ClientInfo, val file : FileDescriptor) {
    var state = FileHandleState.INACTIVE
        private set
    val chunkCount = calculateChunkCount()

    var sendBytes = 0L
        private set
    var sendChunks = 0
        private set

    private var confirmedBytes = 0L
    private var confirmedChunks = 0

    val fileSize
        get() = file.fileSize
    val fileName
        get() = file.fileName

    var onError : ((FailReason) -> Unit)? = null
    var onStart : ((FileHandle) -> Unit)? = null
    var onProgressUpdate : ((FileHandle) -> Unit)? = null
    var onComplete : ((FileHandle) -> Unit)? = null

    private lateinit var inputStream: InputStream

    private val queue = LinkedBlockingQueue<Long>(MAX_QUEUE_CHUNKS)

    fun start() {
        if (!client.active) {
            error("File Share $fileHandleId failed: Client was not active anymore")
            state = FileHandleState.FAILED
            onError?.invoke(FailReason.CLIENT_DISCONNECTED)
        } else {
            client.webSocket.sendPacket(
                FileShareRequestPacket(fileHandleId, fileSize, fileName, chunkCount,
                DEFAULT_CHUNK_SIZE.toLong()
            )
            )
            Timer("TimeoutWaiter").schedule(timerTask {
                if (state == FileHandleState.WAITING_FOR_CLIENT) {
                    state = FileHandleState.FAILED
                    onError?.invoke(FailReason.TIMEOUT)
                }
            }, CLIENT_TIMEOUT.toLong())
            state = FileHandleState.WAITING_FOR_CLIENT
        }
    }

    fun clientConfirmed() {
        try {
            inputStream = file.openInputStream()
        } catch (e : Exception) {
            error("File Share $fileHandleId failed: Failed to open input stream", e)
            state = FileHandleState.FAILED
            onError?.invoke(FailReason.FAILED_TO_OPEN_INPUT_STREAM)
            return
        }
        state = FileHandleState.ACTIVE
        onStart?.invoke(this)
        val buffer = ByteBuffer.allocate(4 + 8 + DEFAULT_CHUNK_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until chunkCount) {
            val count = inputStream.read(buffer.array(), 12, DEFAULT_CHUNK_SIZE)
            buffer.limit(12 + count)
            buffer.position(0)
            buffer.putInt(fileHandleId)
            buffer.putLong(i)
            buffer.position(0)
            if (!client.active) {
                error("File Share $fileHandleId failed: Client was not active anymore")
                state = FileHandleState.FAILED
                onError?.invoke(FailReason.CLIENT_DISCONNECTED)
                return
            }
            client.webSocket.send(buffer)
            client.webSocket.sendPing()
            sendChunks++
            sendBytes += count
            //this should block if the queue is full
            val success = queue.offer(i, QUEUE_TIMEOUT, TimeUnit.MILLISECONDS)
            if (!success) {
                error("File Share $fileHandleId failed: Timeout while waiting for queue")
                state = FileHandleState.FAILED
                onError?.invoke(FailReason.TIMEOUT)
                return
            }
            //Problem: Chunks are only submitted into an queue, the client might not have received them yet
            //onProgressUpdate?.invoke(this)
        }
        state = FileHandleState.COMPLETED
        onComplete?.invoke(this)
        inputStream.close()
    }

    fun onClientChunkReceived(chunkId : Long, chunkSize : Long) {
        //This should free the queue and opens it for next chunk
        queue.remove(chunkId)
        confirmedChunks++
        confirmedBytes += chunkSize
        onProgressUpdate?.invoke(this)
    }

    fun clientDenied(reason: FailReason) {
        state = FileHandleState.FAILED
        onError?.invoke(reason)
    }

    private fun calculateChunkCount(): Long {
        var fullSize = fileSize
        while (fullSize % DEFAULT_CHUNK_SIZE != 0L)
            fullSize++
        return fullSize / DEFAULT_CHUNK_SIZE
    }
}
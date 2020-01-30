package me.fungames.filesender

import android.util.Log
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.jsonSerializer
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import me.fungames.filesender.config.getChunkSize
import me.fungames.filesender.config.getClientTimeout
import me.fungames.filesender.config.getConnTimeout
import me.fungames.filesender.frontend.ui.send.UriFileDescriptor
import me.fungames.filesender.model.Packet
import org.java_websocket.WebSocket

const val TAG = "FileSenderWebSocket"

fun info(message : Any?) = Log.i(TAG, message.toString())
fun debug(message: Any?) = Log.d(TAG, message.toString())
fun warn(message: Any?) = Log.w(TAG, message.toString())
fun error(message: Any?) = Log.e(TAG, message.toString())
fun error(message: Any?, t : Throwable) = Log.e(TAG, message.toString(), t)

val gson = GsonBuilder().registerTypeAdapter(jsonSerializer<UriFileDescriptor> { jsonObject("fileName" to it.src.fileName, "fileSize" to it.src.fileSize) }).create()!!
val DEFAULT_CHUNK_SIZE
    get() = getChunkSize()
val CLIENT_TIMEOUT
    get() = getClientTimeout()
val CONN_TIMEOUT
    get() = getConnTimeout()
val MAX_QUEUE_CHUNKS
    get() = 50 //TODO
val QUEUE_TIMEOUT
    get() = 10000L //TODO

fun WebSocket.sendPacket(payload : Any) {
    val packet = Packet(payload::class.java.name, gson.toJsonTree(payload))
    send(gson.toJson(packet))
}
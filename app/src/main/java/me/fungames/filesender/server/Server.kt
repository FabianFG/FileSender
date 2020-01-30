package me.fungames.filesender.server

import com.google.gson.JsonParseException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.fungames.filesender.*
import me.fungames.filesender.model.Packet
import me.fungames.filesender.model.ServerInfo
import me.fungames.filesender.model.StatusCode
import me.fungames.filesender.model.payloads.*
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.*

abstract class Server(val name : String, val version : String, port : Int) : WebSocketServer(InetSocketAddress(port)) {

    init {
        isReuseAddr = true
    }

    //Our Server Info
    private val serverInfo = ServerInfo(name)

    //Client Handling
    val unauthorizedClients = WeakHashMap<WebSocket, Any?>()
    val authorizedClientsBySocket = mutableMapOf<WebSocket, ClientInfo>()
    val authorizedClients = mutableMapOf<Int, ClientInfo>()

    //Control of file share
    val activeFileShares = mutableMapOf<Int, FileHandle>()

    private var nextFileHandleId = 1

    private val files = mutableMapOf<Int, FileDescriptor>()

    private var nextFileId = 1

    fun addFile(fileDescriptor: FileDescriptor) : Int {
        files[nextFileId] = fileDescriptor
        nextFileId++
        broadcastPacket(FileListUpdatePacket(files))
        return nextFileId - 1
    }

    fun removeFile(fileId : Int) {
        files.remove(fileId)
        broadcastPacket(FileListUpdatePacket(files))
    }

    fun sendFile(clientId : Int, fileId : Int, onError : ((FailReason) -> Unit)? = null, onStart : ((FileHandle) -> Unit)? = null, onProgressUpdate : ((FileHandle) -> Unit)? = null, onComplete : ((FileHandle) -> Unit)? = null) {
        val client = authorizedClients[clientId]
        val file = files[fileId]
        if (client != null && file != null) {
            initFileShare(file, client, onError, onStart, onProgressUpdate, onComplete)
        }
    }

    fun kickClient(clientId: Int) {
        val client = authorizedClients[clientId]
        if (client != null) {
            client.webSocket.close(CloseCode.KICKED_BY_SERVER, "Kicked by the server")
            authorizedClients.remove(clientId)
            authorizedClientsBySocket.remove(client.webSocket)
            onClientDisconnected(client)
        }
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        unauthorizedClients[conn] = null
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        unauthorizedClients.remove(conn)
        val clientInfo = authorizedClientsBySocket.remove(conn)
        clientInfo?.let {
            authorizedClients.remove(clientInfo.clientId)
            onClientDisconnected(clientInfo)
        }
    }

    override fun onMessage(conn: WebSocket, message: String) {
        try {
            val packet = gson.fromJson(message, Packet::class.java)
            onPacket(conn, packet)
        } catch (e : JsonParseException) {
            conn.close(1007, "Receiving abnormal packets that are not following the json scheme")
            removeConnection(conn)
        }
    }

    override fun onMessage(conn: WebSocket, message: ByteBuffer) {
        info("$conn: binary message, length: ${message.capacity()}")
    }

    override fun onStart() {
        info("Server started on port $port with name '$name' and version '$version'")
        connectionLostTimeout = CONN_TIMEOUT
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        ex.printStackTrace()
    }

    private fun onPacket(conn: WebSocket, packet: Packet) {
        try {
            val clazz = Class.forName(packet.className)
            when(val payload = gson.fromJson(packet.payload, clazz)!!) {
                is AuthPacket -> authClient(conn, payload)
                is FileShareAcceptPacket -> fileShareAccept(conn, payload)
                is FileShareDeniedPacket -> fileShareDenied(conn, payload)
                is FileShareChunkReceived -> fileShareChunkReceived(conn, payload)
                is RequestFileListUpdatePacket -> updateFileList(conn)
                is RequestFileDownloadPacket -> requestFileDownload(conn, payload)
            }
        } catch (e : Exception) {
            warn("Invalid packet of type ${packet.className}, couldn't be deserialized")
            warn("${e::class.java.simpleName}: ${e.message}")
        }
    }

    private var nextClientId = 1

    private fun requestFileDownload(conn: WebSocket, packet: RequestFileDownloadPacket) {
        val client = authorizedClientsBySocket[conn]
        val file = files[packet.fileId]
        if (client != null && file != null) {
            val (onError, onStart, onProgressUpdate, onComplete) = fileDownloadByClient(file, client)
            initFileShare(file, client, onError, onStart, onProgressUpdate, onComplete)
        }
    }

    private fun updateFileList(conn: WebSocket) {
        val client = authorizedClientsBySocket[conn]
        if(client != null) {
            conn.sendPacket(FileListUpdatePacket(files))
        }
    }

    private fun authClient(conn: WebSocket, authPacket: AuthPacket) {
        if (unauthorizedClients.containsKey(conn)) {
            if (authPacket.clientVersion != version) {
                warn("Denied auth for client ${authPacket.clientName}, client version: ${authPacket.clientVersion} != server version: $version")
                conn.sendPacket(AuthDeniedPacket(StatusCode.CLIENT_VERSION_NOT_COMPATIBLE, "Client has an incompatible client version ${authPacket.clientVersion}, server is on version $version", serverInfo))
            } else {
                val client = ClientInfo(nextClientId, authPacket.clientName, conn)
                nextClientId++
                unauthorizedClients.remove(conn)
                authorizedClientsBySocket[conn] = client
                authorizedClients[client.clientId] = client
                conn.sendPacket(AuthAcceptedPacket(client.clientId, client.clientName, serverInfo))
                onClientConnected(client)
                conn.sendPacket(FileListUpdatePacket(files))
            }
        } else {
            warn("Already authenticated client is trying to authenticate again as ${authPacket.clientName}")
        }
    }

    private fun fileShareAccept(conn : WebSocket, packet: FileShareAcceptPacket) {
        val client = authorizedClientsBySocket[conn]
        if (client != null) {
            val handle = activeFileShares[packet.fileHandleId]
            if (handle != null && handle.client.clientId == client.clientId && handle.state == FileHandleState.WAITING_FOR_CLIENT) {
                GlobalScope.launch {
                    handle.clientConfirmed()
                }
            } else {
                conn.sendPacket(FileShareInvalidPacket("Given handle id does not exist or is not supposed for your client"))
            }
        }
    }

    private fun fileShareDenied(conn: WebSocket, packet: FileShareDeniedPacket) {
        val client = authorizedClientsBySocket[conn]
        if (client != null) {
            val handle = activeFileShares[packet.fileHandleId]
            if (handle != null && handle.client.clientId == client.clientId && handle.state == FileHandleState.WAITING_FOR_CLIENT) {
                handle.clientDenied()
            }
        }
    }

    private fun fileShareChunkReceived(conn: WebSocket, packet: FileShareChunkReceived) {
        val client = authorizedClientsBySocket[conn]
        if (client != null) {
            val handle = activeFileShares[packet.fileHandleId]
            if (handle != null && handle.client.clientId == client.clientId) {
                handle.onClientChunkReceived(packet.chunkId, packet.chunkSize)
            }
        }
    }

    private fun initFileShare(file : FileDescriptor, client : ClientInfo, onError : ((FailReason) -> Unit)? = null, onStart : ((FileHandle) -> Unit)? = null, onProgressUpdate : ((FileHandle) -> Unit)? = null, onComplete : ((FileHandle) -> Unit)? = null) {
        val handle = FileHandle(nextFileHandleId, client, file)
        handle.onError = onError
        handle.onStart = onStart
        handle.onProgressUpdate = onProgressUpdate
        handle.onComplete = onComplete
        activeFileShares[handle.fileHandleId] = handle
        nextFileHandleId++
        handle.start()
    }

    abstract fun onClientConnected(clientInfo: ClientInfo)

    abstract fun onClientDisconnected(clientInfo: ClientInfo)

    data class ShareListeners(val onError : (FailReason) -> Unit, val onStart : (FileHandle) -> Unit, val onProgressUpdate: (FileHandle) -> Unit, val onComplete: (FileHandle) -> Unit)

    abstract fun fileDownloadByClient(
        fileDescriptor: FileDescriptor,
        client: ClientInfo
    ) : ShareListeners

    private fun broadcastPacket(payload : Any) {
        authorizedClients.values.forEach {
            it.webSocket.sendPacket(payload)
        }
    }
}
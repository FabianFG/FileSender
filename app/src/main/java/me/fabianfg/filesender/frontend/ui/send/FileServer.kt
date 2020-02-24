package me.fabianfg.filesender.frontend.ui.send

import android.util.Log
import me.fabianfg.filesender.TAG
import me.fabianfg.filesender.server.ClientInfo
import me.fabianfg.filesender.server.FileDescriptor
import me.fabianfg.filesender.server.Server
import org.java_websocket.WebSocket
import java.net.BindException

class FileServer(private val sendActivity : SendActivity, name: String, version: String, port: Int) : Server(name, version, port) {
    override fun onClientConnected(clientInfo: ClientInfo) {
        sendActivity.updateClientList(authorizedClients.values)
    }

    override fun onClientDisconnected(clientInfo: ClientInfo) {
        sendActivity.updateClientList(authorizedClients.values)
    }

    override fun fileDownloadByClient(
        fileDescriptor: FileDescriptor,
        client: ClientInfo
    ) = sendActivity.prepareSendFile(fileDescriptor, client)

    override fun onError(conn: WebSocket?, ex: Exception) {
        when(ex) {
            is BindException -> sendActivity.onBindFailed(ex)
            else -> Log.e(TAG, "Server had an uncaught exception", ex)
        }
    }
}
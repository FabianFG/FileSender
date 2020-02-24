package me.fabianfg.filesender.server

import org.java_websocket.WebSocket

data class ClientInfo(val clientId : Int, var clientName : String, val webSocket: WebSocket) {
    val active
        get() = webSocket.isOpen

    override fun toString() = clientName
}
package me.fungames.filesender.frontend.ui.receive

import android.util.Log
import me.fungames.filesender.TAG
import me.fungames.filesender.client.Client
import me.fungames.filesender.config.getName
import me.fungames.filesender.config.getVersion
import me.fungames.filesender.model.payloads.AuthAcceptedPacket
import me.fungames.filesender.model.payloads.AuthDeniedPacket
import me.fungames.filesender.model.payloads.FileShareRequestPacket
import me.fungames.filesender.server.BasicFileDescriptor
import java.net.ConnectException

class FileClient(val receiveActivity: ReceiveActivity, url : String) : Client(getName(), getVersion(), url) {
    override fun reviewFileRequest(packet: FileShareRequestPacket) {
        receiveActivity.onFileShareRequest(packet)
    }

    override fun onLogin(packet: AuthAcceptedPacket) {
        receiveActivity.onLogin(packet)
    }

    override fun onLoginFailed(packet: AuthDeniedPacket) {
        receiveActivity.onLoginFailed(packet)
    }

    override fun onFileListUpdate(files: Map<Int, BasicFileDescriptor>) {
        receiveActivity.onFileListUpdate(files)
    }

    override fun onError(ex: Exception) {
        when(ex) {
            is ConnectException -> receiveActivity.onConnectFailed(ex)
            else -> Log.e(TAG, "Client had an uncaught exception", ex)
        }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        if (code == 1000 || (code == -1 && reason.startsWith("failed to connect to")))
            return
        receiveActivity.onClose(code, reason, remote)
    }
}
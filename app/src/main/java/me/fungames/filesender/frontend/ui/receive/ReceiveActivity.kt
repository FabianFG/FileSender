package me.fungames.filesender.frontend.ui.receive

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_receive.*
import kotlinx.android.synthetic.main.content_receive.*
import kotlinx.android.synthetic.main.fileshare_accept_dialog.*
import kotlinx.android.synthetic.main.fileshare_receive_dialog.*
import kotlinx.android.synthetic.main.fileshare_waiting_for_client_dialog.*
import kotlinx.android.synthetic.main.fileshare_waiting_for_client_dialog.remainingTimeView
import me.fungames.filesender.CLIENT_TIMEOUT
import me.fungames.filesender.R
import me.fungames.filesender.client.Client
import me.fungames.filesender.config.doOpenFilesAfterReceiving
import me.fungames.filesender.config.getServerPort
import me.fungames.filesender.frontend.services.ReceiverService
import me.fungames.filesender.frontend.services.SenderTileService
import me.fungames.filesender.model.payloads.AuthAcceptedPacket
import me.fungames.filesender.model.payloads.AuthDeniedPacket
import me.fungames.filesender.model.payloads.FileShareRequestPacket
import me.fungames.filesender.server.BasicFileDescriptor
import me.fungames.filesender.server.CloseCode
import me.fungames.filesender.utils.getServerIpAddr
import me.fungames.filesender.utils.setDynamicHeight
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.math.roundToInt


class ReceiveActivity : AppCompatActivity() {


    companion object {
        const val permissionRequestCode = 69
        const val TAG = "ReceiveActivity"
    }

    var topTitle
        get() = toolbar_layout.title
        set(value) {toolbar_layout.title = value}

    lateinit var fileClient : FileClient

    var isRunning = false
        private set

    val fileListContent = mutableListOf<FileInfoContainer>()
    lateinit var fileListAdapter : ArrayAdapter<FileInfoContainer>

    val serviceConn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {}
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Bypass file uri exposure
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        setContentView(R.layout.activity_receive)
        setSupportActionBar(toolbar)
        topTitle = getString(R.string.inactive)
        fileList.emptyView = emptyFiles
        serverName.visibility = View.GONE
        fileClient = FileClient(this, "ws://${getServerIpAddr()?:throw IOException("Failed to obtain server ip address")}:${getServerPort()}")
        fab.setOnClickListener {
            if (isRunning) {
                stopClient()
            } else {
                fab.isClickable = false
                startClient()
            }
        }
        fileListAdapter = FileListAdapter(this, R.layout.simple_string_list_item, fileListContent)
        fileList.adapter = fileListAdapter
        requestStoragePermission()
        bindService(Intent(this, ReceiverService::class.java), serviceConn, Context.BIND_AUTO_CREATE)
        if (intent != null && intent.getBooleanExtra("autoConnect", false)) {
            fab.performClick()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConn)
        fileClient.close()
    }

    private fun stopClient() {
        //Stop the client
        fileClient.close()
        fileClient = FileClient(this, "ws://${getServerIpAddr()?:throw IOException("Failed to obtain server ip address")}:${getServerPort()}")
        isRunning = false
        fileListContent.clear()
        runOnUiThread {
            fileListAdapter.notifyDataSetChanged()
            fab.setImageResource(android.R.drawable.ic_media_play)
            //Icon would disappear if not be redrawn
            fab.hide()
            fab.show()
            toolbar_layout.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            topTitle = getString(R.string.inactive)
            serverName.visibility = View.GONE
        }
    }

    private fun startClient() {
        fileClient.connect()
        isRunning = true
        fab.setImageResource(android.R.drawable.ic_media_pause)
        //Icon would disappear if not be redrawn
        fab.hide()
        fab.show()
        toolbar_layout.setBackgroundColor(resources.getColor(android.R.color.holo_orange_dark))
        topTitle = getString(R.string.connecting)
    }

    fun onLogin(packet: AuthAcceptedPacket) = runOnUiThread {
        serverName.text = getString(R.string.serv_name, packet.serverInfo.serverName)
        serverName.visibility = View.VISIBLE
        fab.isClickable = true
        toolbar_layout.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
        topTitle = getString(R.string.connected)
    }

    fun onLoginFailed(packet: AuthDeniedPacket) = runOnUiThread {
        fab.isClickable = true
        AlertDialog.Builder(this)
            .setTitle(R.string.login_failed)
            .setMessage(packet.message)
            .create()
            .show()
    }

    fun onFileListUpdate(files: Map<Int, BasicFileDescriptor>) {
        fileListContent.clear()
        files.forEach { (id, desc) -> fileListContent.add(FileInfoContainer(id, desc)) }
        runOnUiThread {
            fileListAdapter.notifyDataSetChanged()
            fileList.setDynamicHeight()
        }
    }

    fun downloadFile(file : FileInfoContainer) {
        fileClient.downloadFile(file.id)
    }

    fun onConnectFailed(ex : ConnectException) {
        fab.isClickable = true
        stopClient()
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(R.string.connection_failed)
                .setMessage("${ex::class.java.simpleName}: ${ex.message}")
                .create()
                .show()
        }
    }

    fun getCloseReason(code: Int) : String? = when(code) {
        1001 -> getString(R.string.server_closed)
        CloseCode.KICKED_BY_SERVER -> getString(R.string.kicked_by_server)
        else -> null
    }

    fun onClose(code : Int, reason : String, remote : Boolean) {
        stopClient()
        val message = getString(if (remote) R.string.connection_lost_by_server else R.string.connection_lost_by_client, getCloseReason(code)?: "$code $reason")
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(R.string.connection_lost)
                .setMessage(message)
                .create()
                .show()
        }
    }

    @SuppressLint("SetTextI18n")
    fun onFileShareRequest(packet: FileShareRequestPacket) = runOnUiThread {
        val acceptDialog = AcceptFileShareDialog(this)
        acceptDialog.setCancelable(false)
        acceptDialog.show()
        acceptDialog.acceptFileNameView.text = "${packet.fileName} (${Formatter.formatFileSize(this, packet.fileSize)})"
        acceptDialog.acceptRemainingTimeBar.max = CLIENT_TIMEOUT

        val dialog = FileshareReceiveDialog(this)
        dialog.setCancelable(false)

        val countDown = object : CountDownTimer(CLIENT_TIMEOUT.toLong(), 10) {
            override fun onTick(millisUntilFinished: Long) {
                acceptDialog.acceptRemainingTimeBar.progress = millisUntilFinished.toInt()
                acceptDialog.acceptRemainingTimeView.text = "${millisUntilFinished/1000}s"
            }

            override fun onFinish() {
                acceptDialog.cancel()
                fileClient.fileShareRequestHandled(packet, false, null)
            }
        }.start()

        acceptDialog.yesButton.setOnClickListener {
            countDown.cancel()
            acceptDialog.cancel()
            val info = Client.FileHandleInfo(me.fungames.filesender.config.getDir(),
                /*onStart*/{
                    runOnUiThread {
                        dialog.show()
                        dialog.progressBar.max = it.chunkCount.toInt()
                        dialog.progressBar.progress = 0
                        dialog.fileNameView.text = it.fileName
                    }
                },
                /*onProgressUpdate*/ {
                    val processed = Formatter.formatFileSize(this, it.receivedBytes)
                    val max = Formatter.formatFileSize(this, it.fileSize)
                    val progressString = "$processed/$max"
                    val percentageString = "${(100.0/it.fileSize*it.receivedBytes).roundToInt()}%"
                    runOnUiThread {
                        dialog.progressBar.progress = it.numReceivedChunks.toInt()
                        dialog.progressView.text = progressString
                        dialog.percentageView.text = percentageString
                    }
                },
                /*onCompleted*/ {
                    acceptDialog.cancel()
                    countDown.cancel()
                    dialog.cancel()
                    if (doOpenFilesAfterReceiving()) {
                        runOnUiThread {
                            val mimeMap = MimeTypeMap.getSingleton()
                            val intent = Intent(Intent.ACTION_VIEW)
                            val mimeType = mimeMap.getMimeTypeFromExtension(it.file.extension) ?: "*/*"
                            intent.setDataAndType(Uri.fromFile(it.file), mimeType)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            try {
                                startActivity(intent)
                            } catch (e : ActivityNotFoundException) {
                                val file = it.file
                                intent.setDataAndType(Uri.fromFile(file), "*/*")
                                runCatching { startActivity(intent) }
                                    .onFailure { Snackbar.make(fab, getString(R.string.file_open_failed, file.name), Snackbar.LENGTH_LONG).show() }
                            }
                        }
                    }
                }
            )
            fileClient.fileShareRequestHandled(packet, true, info)
        }
        acceptDialog.noButton.setOnClickListener {
            countDown.cancel()
            acceptDialog.cancel()
            fileClient.fileShareRequestHandled(packet, false, null)
        }
        acceptDialog.show()
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), permissionRequestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionRequestCode && permissions.isNotEmpty() && permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Granted external storage permissions")
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                restartActivity()
            } else {
                AlertDialog.Builder(this)
                    .setTitle(R.string.permission_denied)
                    .setMessage(R.string.permission_denied_body)
                    .setNegativeButton(R.string.ok) { _ , _ ->
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri: Uri =
                            Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        finish()
                        startActivity(intent)
                    }
                    .create()
                    .show()
            }
        }
    }

    private fun restartActivity() {
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}

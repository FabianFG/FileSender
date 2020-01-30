package me.fungames.filesender.frontend.ui.send

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Parcelable
import android.text.format.Formatter
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_send.*
import kotlinx.android.synthetic.main.content_send.*
import kotlinx.android.synthetic.main.fileshare_send_dialog.*
import kotlinx.android.synthetic.main.fileshare_waiting_for_client_dialog.*
import kotlinx.coroutines.*
import me.fungames.filesender.CLIENT_TIMEOUT
import me.fungames.filesender.R
import me.fungames.filesender.config.getName
import me.fungames.filesender.config.getServerPort
import me.fungames.filesender.config.getVersion
import me.fungames.filesender.server.*
import me.fungames.filesender.utils.setDynamicHeight
import java.net.BindException
import kotlin.math.roundToInt


class SendActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    companion object {
        const val fileOpenRequestCode = 69
    }

    var topTitle
        get() = toolbar_layout.title
        set(value) {toolbar_layout.title = value}

    var fileServer = FileServer(this, getName(), getVersion(), getServerPort())
        private set

    val clientListContent = mutableListOf<ClientInfo>()
    lateinit var clientListAdapter : ClientListAdapter

    val fileListContent = mutableListOf<FileInfoContainer>()
    lateinit var fileListAdapter : FileListAdapter

    var isRunning = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)
        setSupportActionBar(toolbar)
        topTitle = getString(R.string.inactive)
        connectedClients.visibility = View.GONE
        fileList.emptyView = emptyFiles
        clientList.emptyView = emptyClients
        fab.setOnClickListener {
            if (isRunning) {
                stopServer()
            } else {
                startServer()
            }
        }

        addFile.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent, getString(R.string.file_select)), fileOpenRequestCode)
        }

        clientListAdapter = ClientListAdapter(this, R.layout.client_adapter_layout, clientListContent)
        clientList.adapter = clientListAdapter
        fileListAdapter = FileListAdapter(this, R.layout.file_adapter_layout, fileListContent)
        fileList.adapter = fileListAdapter

        when (intent?.action) {
            Intent.ACTION_SEND -> handleFileShare(intent)
            Intent.ACTION_SEND_MULTIPLE -> handleMultipleFileShare(intent)
        }
    }

    fun stopServer() {
        //Stop the server
        clientListContent.clear()
        clientListAdapter.notifyDataSetChanged()
        fileServer.stop()
        fileServer = FileServer(this, getName(), getVersion(), getServerPort())
        isRunning = false
        fab.setImageResource(android.R.drawable.ic_media_play)
        //Icon would disappear if not be redrawn
        fab.hide()
        fab.show()
        toolbar_layout.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        connectedClients.visibility = View.GONE
        topTitle = getString(R.string.inactive)
    }

    fun startServer() {
        //Start the server
        fileListContent.forEach { fileServer.addFile(it.descriptor) }
        fileServer.start()
        isRunning = true
        fab.setImageResource(android.R.drawable.ic_media_pause)
        //Icon would disappear if not be redrawn
        fab.hide()
        fab.show()
        toolbar_layout.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
        connectedClients.text = getString(R.string.con_clients, 0)
        connectedClients.visibility = View.VISIBLE
        topTitle = getString(R.string.running)
    }

    fun onBindFailed(ex : BindException) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(R.string.server_start_failed)
                .setMessage("${ex::class.java.simpleName}: ${ex.message}")
                .create()
                .show()
            stopServer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fileServer.stop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileOpenRequestCode && resultCode == RESULT_OK && data != null) {
            val fileUri = data.data
            if (fileUri != null) {
                registerNewFile(fileUri)
            }
        }
    }

    fun updateClientList(clients : Collection<ClientInfo>) {
        clientListContent.clear()
        clientListContent.addAll(clients)
        runOnUiThread {
            connectedClients.text = getString(R.string.con_clients, clients.size)
            clientListAdapter.notifyDataSetChanged()
            clientList.setDynamicHeight()
        }
    }

    private fun handleFileShare(intent: Intent) {
        val extra = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri?
        extra?.let { registerNewFile(it) }
    }

    @Synchronized
    private fun registerNewFile(uri : Uri) {
        val descriptor = UriFileDescriptor(this, uri)
        val fileId = fileServer.addFile(descriptor)
        fileListContent.add(FileInfoContainer(fileId, descriptor))
        runOnUiThread {
            fileListAdapter.notifyDataSetChanged()
            fileList.setDynamicHeight()
        }
    }

    private fun handleMultipleFileShare(intent: Intent) {
        val extra = intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)
        extra?.forEach { share -> (share as? Uri?)?.let { registerNewFile(it) } }
    }

    fun sendFile(file: FileInfoContainer) {
        ClientSelectDialog(this, clientListContent) {
            if (it != null)
                sendFile(file, it)
        }.show()
    }

    fun sendFile(file: FileInfoContainer, client: ClientInfo) {
        launch(Dispatchers.IO) {
            val (onError, onStart, onProgressUpdate, onComplete) = prepareSendFile(file.descriptor, client)
            fileServer.sendFile(client.clientId, file.id, onError, onStart, onProgressUpdate, onComplete)
        }
    }

    @SuppressLint("SetTextI18n")
    fun prepareSendFile(file : BasicFileDescriptor, client : ClientInfo) : Server.ShareListeners {
        return runBlocking {
            async(Dispatchers.Main) {
                val clientName = client.clientName
                val waitDialog = WaitingForClientDialog(this@SendActivity)
                waitDialog.setCancelable(false)
                waitDialog.show()
                waitDialog.clientNameView.text = clientName
                waitDialog.remainingTimeBar.max = CLIENT_TIMEOUT
                val countDown = object : CountDownTimer(CLIENT_TIMEOUT.toLong(), 10) {
                    override fun onTick(millisUntilFinished: Long) {
                        waitDialog.remainingTimeBar.progress = millisUntilFinished.toInt()
                        waitDialog.remainingTimeView.text = "${millisUntilFinished/1000}s"
                    }

                    override fun onFinish() {
                        waitDialog.cancel()
                    }
                }.start()
                val dialog = FileshareSendDialog(this@SendActivity)
                dialog.setCancelable(false)
                var maxSet = false
                val onError : (FailReason) -> Unit = {
                    runOnUiThread {
                        dialog.cancel()
                        waitDialog.cancel()
                        countDown.cancel()
                        AlertDialog.Builder(this@SendActivity)
                            .setTitle(R.string.fileshare_failed)
                            .setMessage(it.name)
                            .show()
                    }
                }
                val onStart : (FileHandle) -> Unit = {
                    runOnUiThread {
                        countDown.cancel()
                        waitDialog.cancel()
                        dialog.show()
                        dialog.fileNameView.text = file.fileName
                    }
                }
                val onProgressUpdate : (FileHandle) -> Unit = {
                    val processed = Formatter.formatFileSize(this@SendActivity, it.sendBytes)
                    val max = Formatter.formatFileSize(this@SendActivity, it.fileSize)
                    val progressString = "$processed/$max"
                    val percentageString = "${(100.0/it.fileSize*it.sendBytes).roundToInt()}%"
                    runOnUiThread {
                        if (!maxSet) {
                            dialog.progressBar.max = it.chunkCount.toInt()
                            maxSet = true
                        }
                        dialog.progressBar.progress = it.sendChunks
                        dialog.progressView.text = progressString
                        dialog.percentageView.text = percentageString
                    }
                }
                val onComplete : (FileHandle) -> Unit = {
                    runOnUiThread {
                        dialog.cancel()
                        waitDialog.cancel()
                        countDown.cancel()
                    }
                }
                return@async Server.ShareListeners(onError, onStart, onProgressUpdate, onComplete)
            }.await()
        }
    }

    fun removeFile(file: FileInfoContainer) {
        fileListContent.remove(file)
        fileServer.removeFile(file.id)
        runOnUiThread {
            fileListAdapter.notifyDataSetChanged()
            fileList.setDynamicHeight()
        }
    }
}

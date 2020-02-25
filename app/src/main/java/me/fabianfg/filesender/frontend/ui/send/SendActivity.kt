@file:Suppress("DEPRECATION")
package me.fabianfg.filesender.frontend.ui.send

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Parcelable
import android.provider.Settings
import android.text.format.Formatter
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.leinardi.android.speeddial.SpeedDialActionItem
import kotlinx.android.synthetic.main.activity_send.*
import kotlinx.android.synthetic.main.content_send.*
import kotlinx.android.synthetic.main.fileshare_send_dialog.*
import kotlinx.android.synthetic.main.fileshare_waiting_for_client_dialog.*
import kotlinx.coroutines.*
import me.fabianfg.filesender.CLIENT_TIMEOUT
import me.fabianfg.filesender.R
import me.fabianfg.filesender.config.getServerPort
import me.fabianfg.filesender.config.getVersion
import me.fabianfg.filesender.frontend.receivers.ApStateReceiver
import me.fabianfg.filesender.frontend.ui.appsend.AppSendActivity
import me.fabianfg.filesender.server.*
import me.fabianfg.filesender.utils.*
import java.io.File
import java.net.BindException
import kotlin.math.roundToInt


class SendActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    companion object {
        const val fileOpenRequestCode = 69
        const val locationPermissionRequestCode = 71
        const val appOpenRequestCode = 72
    }

    var white = -0x1
    var black = -0x1000000
    private val width = 500

    private var topTitle
        get() = toolbar_layout.title
        set(value) {toolbar_layout.title = value}

    lateinit var fileServer : FileServer
        private set

    private val clientListContent = mutableListOf<ClientInfo>()
    private lateinit var clientListAdapter : ClientListAdapter

    private val fileListContent = mutableListOf<FileInfoContainer>()
    private lateinit var fileListAdapter : FileListAdapter

    private var isRunning = false

    private var hotspotInfoHolder : HotspotInfoHolder? = null

    private lateinit var apStateReceiver : ApStateReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)
        apStateReceiver = ApStateReceiver(this)
        registerReceiver(apStateReceiver, IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"))
        fileServer = FileServer(this, Settings.Secure.getString(contentResolver, "bluetooth_name"), getVersion(), getServerPort())
        setSupportActionBar(toolbar)
        topTitle = getString(R.string.inactive)
        sendInfoLayout.visibility = View.GONE
        qrCodeButton.visibility = View.GONE
        fileList.emptyView = emptyFiles
        clientList.emptyView = emptyClients
        fab.setOnClickListener {
            if (isRunning) {
                stopServer()
            } else {
                if (checkSelfPermissionCompat(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val locManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    if (!locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        return@setOnClickListener
                    }
                    toolbar_layout.setBackgroundColor(resources.getColor(android.R.color.holo_orange_dark))
                    topTitle = getString(R.string.starting)
                    val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startUsingLocalOnlyHotspot(manager)
                    } else {
                        startUsingNormalHotspot(manager)
                    }
                } else {
                    requestLocationPermission()
                }
            }
        }

        clientListAdapter = ClientListAdapter(this, R.layout.client_adapter_layout, clientListContent)
        clientList.adapter = clientListAdapter
        fileListAdapter = FileListAdapter(this, R.layout.file_adapter_layout, fileListContent)
        fileList.adapter = fileListAdapter

        when (intent?.action) {
            Intent.ACTION_SEND -> handleFileShare(intent)
            Intent.ACTION_SEND_MULTIPLE -> handleMultipleFileShare(intent)
        }
        intent?.let { handleApkShare(it) }
        addFileDial.addActionItem(SpeedDialActionItem.Builder(R.id.fab_add_app, android.R.drawable.sym_def_app_icon).setLabel(R.string.add_app).create())
        addFileDial.addActionItem(SpeedDialActionItem.Builder(R.id.fab_add_file, android.R.drawable.ic_menu_add).setLabel(R.string.add_file).create())
        addFileDial.setOnActionSelectedListener {
            when(it.id) {
                R.id.fab_add_app -> startActivityForResult(Intent(this, AppSendActivity::class.java), appOpenRequestCode)
                R.id.fab_add_file -> {
                    val intent = Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT)
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.file_select)), fileOpenRequestCode)
                }
            }
            false
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        when (intent?.action) {
            Intent.ACTION_SEND -> handleFileShare(intent)
            Intent.ACTION_SEND_MULTIPLE -> handleMultipleFileShare(intent)
        }
        intent?.let { handleApkShare(it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startUsingLocalOnlyHotspot(manager: WifiManager) {
        manager.startLocalOnlyHotspot(
            object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onFailed(reason: Int) {
                    val builder = AlertDialog.Builder(this@SendActivity)
                        .setTitle(R.string.failed_to_start_hotspot_title)
                    builder.setMessage(
                        when (reason) {
                            ERROR_GENERIC -> getString(
                                R.string.failed_to_start_hotspot,
                                "ERROR_GENERIC"
                            )
                            ERROR_INCOMPATIBLE_MODE -> getString(R.string.hotspot_already_enabled)
                            ERROR_TETHERING_DISALLOWED -> getString(R.string.disallowed_to_start_hotspot)
                            ERROR_NO_CHANNEL -> getString(
                                R.string.failed_to_start_hotspot,
                                "ERROR_NO_CHANNEL"
                            )
                            else -> getString(R.string.failed_to_start_hotspot_title)
                        }
                    )
                    builder.show()
                    launch { stopServer() }
                }

                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    val result = MultiFormatWriter().encode(
                        reservation.wifiConfiguration.SSID + ":" + reservation.wifiConfiguration.preSharedKey,
                        BarcodeFormat.QR_CODE,
                        width,
                        width
                    )
                    val w: Int = result.width
                    val h: Int = result.height
                    val pixels = IntArray(w * h)
                    for (y in 0 until h) {
                        val offset = y * w
                        for (x in 0 until w) {
                            pixels[offset + x] = if (result.get(x, y)) black else white
                        }
                    }
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bitmap.setPixels(pixels, 0, width, 0, 0, w, h)
                    launch { startServer(reservation, bitmap) }
                }

                override fun onStopped() {
                    launch { stopServer() }
                }
            }, null
        )
    }

    private fun startUsingNormalHotspot(manager: WifiManager) {
        launch(Dispatchers.IO) {
            if (manager.isWifiApEnabled())
                runOnUiThread { startServer() }
            else {
                if (manager.setWifiApEnabled(true)) {
                    runOnUiThread { startServer() }
                } else {
                    runOnUiThread {
                        AlertDialog.Builder(this@SendActivity).setTitle(R.string.warning).setMessage(R.string.failed_to_start_hotspot_try_manually).show()
                        stopServer()
                    }
                }
            }

        }
    }

    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), locationPermissionRequestCode)
        }
    }

    fun stopServer() {
        //Stop the server
        val infoHolder = hotspotInfoHolder
        infoHolder?.close()
        launch(Dispatchers.IO) {
            val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (manager.isWifiApEnabled() && !manager.setWifiApEnabled(false) && infoHolder !is Android8HotspotInfoHolder) {
                runOnUiThread {
                    AlertDialog.Builder(this@SendActivity).setTitle(R.string.warning).setMessage(R.string.failed_to_stop_hotspot_try_manually).show()
                }
            }
        }
        hotspotInfoHolder = null
        clientListContent.clear()
        clientListAdapter.notifyDataSetChanged()
        fileServer.stop()
        fileServer = FileServer(this, Settings.Secure.getString(contentResolver, "bluetooth_name"), getVersion(), getServerPort())
        fileListContent.forEach {
            val id = fileServer.addFile(it.descriptor)
            it.id = id
        }
        isRunning = false
        fab.setImageResource(android.R.drawable.ic_media_play)
        //Icon would disappear if not be redrawn
        fab.hide()
        fab.show()
        toolbar_layout.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        hotspotName.visibility = View.VISIBLE
        hotspotPassword.visibility = View.VISIBLE
        sendInfoLayout.visibility = View.GONE
        qrCodeButton.visibility = View.GONE
        topTitle = getString(R.string.inactive)
    }

    private fun startServer() {
        //Start the server
        hotspotInfoHolder = FakeHotspotInfoHolder()
        fileServer.start()
        isRunning = true
        fab.setImageResource(android.R.drawable.ic_media_pause)
        //Icon would disappear if not be redrawn
        fab.hide()
        fab.show()
        toolbar_layout.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
        sendInfoLayout.visibility = View.VISIBLE
        connectedClients.text = getString(R.string.con_clients, 0)
        hotspotName.visibility = View.GONE
        hotspotPassword.visibility = View.GONE
        qrCodeButton.visibility = View.GONE
        topTitle = getString(R.string.running)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startServer(reservation: WifiManager.LocalOnlyHotspotReservation, qrCode : Bitmap) {
        val dialog = Dialog(this@SendActivity)
        dialog.setContentView(R.layout.qr_dialog)
        val img = dialog.findViewById<ImageView>(R.id.qrCodeView)
        img.setImageBitmap(qrCode)
        this.hotspotInfoHolder = Android8HotspotInfoHolder(reservation, dialog)
        //Start the server
        fileServer.start()
        isRunning = true
        fab.setImageResource(android.R.drawable.ic_media_pause)
        //Icon would disappear if not be redrawn
        fab.hide()
        fab.show()
        toolbar_layout.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
        sendInfoLayout.visibility = View.VISIBLE
        connectedClients.text = getString(R.string.con_clients, 0)
        hotspotName.text = getString(R.string.hotspot_name, reservation.wifiConfiguration.SSID)
        hotspotPassword.text = getString(R.string.hotspot_password, reservation.wifiConfiguration.preSharedKey)
        qrCodeButton.visibility = View.VISIBLE
        topTitle = getString(R.string.running)
        showQrCodeDialog()
    }

    private fun showQrCodeDialog() {
        val info = hotspotInfoHolder
        if (info != null && info is Android8HotspotInfoHolder) {
            info.qrCodeDialog.show()
        }
    }

    fun onQrCodeButtonClicked(view: View) {
        unused(view)
        showQrCodeDialog()
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
        unregisterReceiver(apStateReceiver)
        stopServer()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileOpenRequestCode && resultCode == RESULT_OK && data != null) {
            val fileUri = data.data
            if (fileUri != null) {
                registerNewFile(fileUri)
            }
        } else if (requestCode == appOpenRequestCode && resultCode == Activity.RESULT_OK && data != null) {
            handleApkShare(data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == locationPermissionRequestCode && permissions.isNotEmpty() && permissions[0] == Manifest.permission.ACCESS_COARSE_LOCATION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fab.performClick()
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

    private fun handleApkShare(intent: Intent) {
        val path = intent.getStringExtra(AppSendActivity.APK_PATH)
        val name = intent.getStringExtra(AppSendActivity.APK_NAME)
        if (path != null && name != null) {
            val fileDesc = ApkFileDescriptor(File(path), name)
            registerNewFile(fileDesc)
        }
    }

    private fun registerNewFile(descriptor: FileDescriptor) {
        val fileId = fileServer.addFile(descriptor)
        fileListContent.add(FileInfoContainer(fileId, descriptor))
        runOnUiThread {
            fileListAdapter.notifyDataSetChanged()
            fileList.setDynamicHeight()
        }
    }

    @Synchronized
    private fun registerNewFile(uri : Uri) {
        val descriptor = UriFileDescriptor(this, uri)
        registerNewFile(descriptor)
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

    private fun sendFile(file: FileInfoContainer, client: ClientInfo) {
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
                val dialog = FileShareSendDialog(this@SendActivity)
                dialog.setCancelable(false)
                var maxSet = false
                val onError : (FailReason) -> Unit = {
                    val message = when(it) {
                        FailReason.CLIENT_DISCONNECTED -> getString(R.string.fail_reason_client_disconnected)
                        FailReason.CLIENT_DENIED -> getString(R.string.fail_reason_client_denied)
                        FailReason.CLIENT_NOT_ENOUGH_STORAGE -> getString(R.string.fail_reason_client_not_enough_storage)
                        FailReason.TIMEOUT -> getString(R.string.fail_reason_timeout)
                        FailReason.FAILED_TO_OPEN_INPUT_STREAM -> getString(R.string.fail_reason_failed_to_open_input_stream)
                    }
                    runOnUiThread {
                        dialog.cancel()
                        waitDialog.cancel()
                        countDown.cancel()
                        AlertDialog.Builder(this@SendActivity)
                            .setTitle(R.string.fileshare_failed)
                            .setMessage(message)
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

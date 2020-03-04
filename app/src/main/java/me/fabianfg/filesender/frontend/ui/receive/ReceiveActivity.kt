//Mainly because of all the old wifi classes being deprecated
@file:Suppress("DEPRECATION")
package me.fabianfg.filesender.frontend.ui.receive

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.StatFs
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_receive.*
import kotlinx.android.synthetic.main.content_receive.*
import kotlinx.android.synthetic.main.fileshare_accept_dialog.*
import kotlinx.android.synthetic.main.fileshare_receive_dialog.*
import kotlinx.coroutines.*
import me.fabianfg.filesender.CLIENT_TIMEOUT
import me.fabianfg.filesender.R
import me.fabianfg.filesender.client.Client
import me.fabianfg.filesender.config.*
import me.fabianfg.filesender.frontend.receivers.WifiReceiver
import me.fabianfg.filesender.model.payloads.AuthAcceptedPacket
import me.fabianfg.filesender.model.payloads.AuthDeniedPacket
import me.fabianfg.filesender.model.payloads.FileShareRequestPacket
import me.fabianfg.filesender.server.BasicFileDescriptor
import me.fabianfg.filesender.server.CloseCode
import me.fabianfg.filesender.server.FailReason
import me.fabianfg.filesender.utils.*
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


class NetworkStateChanged(private val receiveActivity: ReceiveActivity) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val info = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
        if (info != null && info.isConnected) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            receiveActivity.onNetworkConnected(wifiInfo.ssid)
        }
    }

}

class ReceiveActivity : AppCompatActivity(), CoroutineScope by MainScope() {


    companion object {
        const val storagePermissionRequestCode = 69
        const val cameraPermissionRequestCode = 70
        const val scanQrRequestCode = 6969
        const val TAG = "ReceiveActivity"
    }

    private var wifiReceiver: WifiReceiver? = null
    private var topTitle
        get() = toolbar_layout.title
        set(value) {toolbar_layout.title = value}

    lateinit var fileClient : FileClient

    private var isRunning = false

    private val fileListContent = mutableListOf<FileInfoContainer>()

    private var onStopClientListeners = ConcurrentHashMap<() -> Unit, Boolean>()

    private lateinit var fileListAdapter : ArrayAdapter<FileInfoContainer>

    private val expectedWifis = mutableListOf<String>()

    private lateinit var networkStateChanged : NetworkStateChanged

    private lateinit var sharedPrefs : SharedPreferences

    private var overrideGateway : String? = null

    fun onScanQrCodeButtonClicked(view: View) {
        unused(view)
        if (checkSelfPermissionCompat(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(Intent(this, ScanQrActivity::class.java), scanQrRequestCode)
        } else {
            requestCameraPermission()
        }
    }

    fun onNetworkConnected(ssid : String) {
        Log.d(TAG, "Connected to $ssid, expected networks: $expectedWifis")
        val unknownFound = if (ssid == "<unknown ssid>" && expectedWifis.isNotEmpty()) {
            expectedWifis.clear()
            true
        } else false
        if(expectedWifis.remove(ssid) || unknownFound) {
            wifiWaitDialog?.setMessage(getString(R.string.attempting_to_connect))
            launch(Dispatchers.IO) {
                val conn = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                if (conn.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)?.isConnectedOrConnecting == true) {
                    runOnUiThread {
                        AlertDialog.Builder(this@ReceiveActivity).setTitle(R.string.warning).setMessage(R.string.mobile_data_warning).show()
                        wifiWaitDialog?.cancel()
                    }
                } else {
                    for (i in 0 until getMaxConnectAttempts()) {
                        Log.d(TAG, "Connection attempt $i...")
                        if (checkHostAvailable(getServerIpAddr()?:throw IOException("Failed to obtain server ip address"), getServerPort(), 3000)) {
                            Log.d(TAG, "Connection attempt $i was successful")
                            runOnUiThread { startClient() }
                            break
                        }
                        delay(1000)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        setContentView(R.layout.activity_receive)
        setSupportActionBar(toolbar)
        update(this)
        topTitle = getString(R.string.inactive)
        fileList.emptyView = emptyFiles
        serverName.visibility = View.GONE
        fileClient = FileClient(this, "ws://${getServerIpAddr()?:throw IOException("Failed to obtain server ip address")}:${getServerPort()}", getName())
        fab.setOnClickListener {
            if (isRunning) {
                stopClient()
            } else {
                val conn = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                if (conn.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)?.isConnectedOrConnecting == true) {
                    AlertDialog.Builder(this).setTitle(R.string.warning).setMessage(R.string.mobile_data_warning).show()
                } else {
                    startClient()
                }
            }
        }
        fileListAdapter = FileListAdapter(this, R.layout.simple_string_list_item, fileListContent)
        fileList.adapter = fileListAdapter
        requestStoragePermission()
        //bindService(Intent(this, ReceiverService::class.java), serviceConn, Context.BIND_AUTO_CREATE)
        if (intent != null && intent.getBooleanExtra("autoConnect", false)) {
            fab.performClick()
        }
        networkStateChanged = NetworkStateChanged(this)
        registerReceiver(networkStateChanged, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkStateChanged)
        //unbindService(serviceConn)
        fileClient.close()
    }

    private fun stopClient() {
        //Stop the client
        Log.d(TAG, "Stopping client")
        fileClient.close()
        if (wifiWaitDialog?.isShowing == false) {
            onStopClientListeners.forEach { (it, unregister) ->
                it()
                if (unregister)
                    unregisterOnClientStop(it)
            }
        }
        isRunning = false
        fileListContent.clear()
        runOnUiThread {
            fab.isClickable = true
            fileListAdapter.notifyDataSetChanged()
            fab.setImageResource(android.R.drawable.ic_media_play)
            //Icon would disappear if not be redrawn
            fab.hide()
            fab.show()
            toolbar_layout.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            topTitle = getString(R.string.inactive)
            serverName.visibility = View.GONE
        }
        Log.d(TAG, "Stopped client")
    }

    private fun startClient() {
        if (isRunning)
            return
        isRunning = true
        fab.isClickable = false
        val gatewayIP = overrideGateway ?: getServerIpAddr() ?: throw IOException("Failed to obtain server ip address")
        Log.d(TAG, "Override Gateway: $overrideGateway")
        Log.d(TAG, "Used IP: $gatewayIP")
        fileClient = FileClient(this, "ws://$gatewayIP:${getServerPort()}", getName())
        launch(Dispatchers.IO) {
            Log.d(TAG,"Connect Blocking: 1000ms timeout")
            if (!fileClient.connectBlocking(1000, TimeUnit.MILLISECONDS)) {
                Log.d(TAG, "Failed to connect")
            } else {
                Log.d(TAG, "Connection succeeded")
            }
        }
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
        runOnUiThread {
            fab.setImageResource(android.R.drawable.ic_media_pause)
            //Icon would disappear if not be redrawn
            fab.hide()
            fab.show()
        }
        wifiWaitDialog?.cancel()
    }

    fun onLoginFailed(packet: AuthDeniedPacket) = runOnUiThread {
        fab.isClickable = true
        stopClient()
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

    fun onConnectFailed(ex : Exception) {
        val byQrConnect = wifiWaitDialog?.isShowing == true
        stopClient()
        if (byQrConnect) {
            runOnUiThread {
                Log.d(TAG, "${ex::class.java.simpleName}: ${ex.message}")
                Log.d(TAG, "QR-Connect was still active, attempting to start again")
                startClient()
            }
        } else {
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle(R.string.connection_failed)
                    .setMessage("${ex::class.java.simpleName}: ${ex.message}")
                    .create()
                    .show()
            }
        }
    }

    private fun getCloseReason(code: Int) : String? = when(code) {
        1001 -> getString(R.string.server_closed)
        CloseCode.KICKED_BY_SERVER -> getString(R.string.kicked_by_server)
        else -> null
    }

    fun onClose(code : Int, reason : String, remote : Boolean) {
        stopClient()
        val message = getString(if (remote) R.string.connection_lost_by_server else R.string.connection_lost_by_client, getCloseReason(code)?: "$code $reason")
        if (code == -1 && reason == "Host unreachable") {
            val byQrConnect = wifiWaitDialog?.isShowing == true
            if (byQrConnect) {
                runOnUiThread { startClient() }
            }
        } else {
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle(R.string.connection_lost)
                    .setMessage(message)
                    .create()
                    .show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun onFileShareRequest(packet: FileShareRequestPacket) = runOnUiThread {
        Log.d(TAG, "Checking storage")
        val stat = StatFs(getDir())
        val availableBytes = stat.blockSizeLong * stat.availableBlocksLong
        Log.d(TAG, "Available storage: ${Formatter.formatFileSize(this, availableBytes)}")
        if (availableBytes < packet.fileSize) {
            Log.d(TAG, "Not enough storage")
            fileClient.fileShareRequestHandled(packet, false, null, FailReason.CLIENT_NOT_ENOUGH_STORAGE)
            AlertDialog.Builder(this)
                .setTitle(R.string.fileshare_failed)
                .setMessage(getString(R.string.not_enough_storage, packet.fileName, Formatter.formatFileSize(this, packet.fileSize), Formatter.formatFileSize(this, packet.fileSize - availableBytes)))
                .setNeutralButton(R.string.ok, null)
            return@runOnUiThread
        }
        val acceptDialog = AcceptFileShareDialog(this)
        acceptDialog.setCancelable(false)
        acceptDialog.show()
        acceptDialog.acceptFileNameView.text = "${packet.fileName} (${Formatter.formatFileSize(this, packet.fileSize)})"
        acceptDialog.acceptRemainingTimeBar.max = CLIENT_TIMEOUT

        val dialog = FileShareReceiveDialog(this)
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


        val onConnLost = {
            acceptDialog.cancel()
            countDown.cancel()
            dialog.cancel()
        }

        registerOnClientStop(false, onConnLost)

        acceptDialog.yesButton.setOnClickListener {
            countDown.cancel()
            acceptDialog.cancel()
            val info = Client.FileHandleInfo(
                getDir(),
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
                    unregisterOnClientStop(onConnLost)
                    if (doOpenFilesAfterReceiving()) {
                        sharedPrefs.edit(true) {
                            val oldSet = sharedPrefs.getStringSet("file_receive_history", mutableSetOf())!!
                            oldSet.add(it.file.absolutePath)
                            putStringSet("file_receive_history", oldSet)
                        }
                        runOnUiThread { openFile(it.file) }
                    }
                }
            )

            val file = File("${getDir()}/${packet.fileName}")
            if (file.exists()) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.warning)
                    .setMessage(getString(R.string.override_warning, packet.fileName))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        fileClient.fileShareRequestHandled(packet, true, info)
                    }
                    .setNegativeButton(R.string.no) { _, _ ->
                        fileClient.fileShareRequestHandled(packet, false, info)
                        unregisterOnClientStop(onConnLost)
                    }
                    .show()
            } else {
                fileClient.fileShareRequestHandled(packet, true, info)
            }
        }
        acceptDialog.noButton.setOnClickListener {
            countDown.cancel()
            acceptDialog.cancel()
            fileClient.fileShareRequestHandled(packet, false, null)
            unregisterOnClientStop(onConnLost)

        }
        acceptDialog.show()
    }

    private fun registerOnClientStop(removeAfterInvoke : Boolean, onConnLost : () -> Unit) {
        onStopClientListeners[onConnLost] = removeAfterInvoke
    }

    private fun unregisterOnClientStop(onConnLost : () -> Unit) {
        onStopClientListeners.remove(onConnLost)
    }

    private fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraPermissionRequestCode)
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), storagePermissionRequestCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            scanQrRequestCode -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val text = data.getStringExtra(ScanQrActivity.SCANNED_RESULT) ?: return
                    if (text.contains(':')) {
                        val hotspotName = text.substringBefore(':')
                        val hotspotPassword = text.substringAfter(':')
                        connectToWifi(hotspotName, hotspotPassword)
                    }
                }
            }
        }
    }

    var wifiWaitDialog : AlertDialog? = null

    private fun connectToWifi(ssid : String, preSharedKey : String) {


        wifiWaitDialog = AlertDialog.Builder(this)
            .setTitle(R.string.connecting_to_wifi)
            .setMessage(getString(R.string.connecting_to_wifi_body, ssid))
            .setNegativeButton(R.string.abort) { _, _ ->
                wifiWaitDialog?.cancel()
                if (wifiReceiver != null)
                    runCatching {
                        unregisterReceiver(wifiReceiver)
                        wifiReceiver = null
                    }
            }
            .setCancelable(false)
            .show()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //TODO Untested code
            //We can't activate wifi on Android 10 anymore so request the user to do so
            //wifiWaitDialog?.setMessage(getString(R.string.android_10_wifi_warning))
            val onWifiEnabled = {
                if (wifiReceiver != null)
                    runCatching {
                        unregisterReceiver(wifiReceiver)
                        wifiReceiver = null
                    }
                val spec = WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(preSharedKey)
                    .build()
                val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).setNetworkSpecifier(spec).build()
                val conn = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val callback = object: ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        conn.bindProcessToNetwork(network)
                        var correctGateway : String? = null
                        val routes = conn.getLinkProperties(network)?.routes
                        if (routes != null) {
                            for (i in 0 until getMaxConnectAttempts()) {
                                Log.d(TAG, "Connect Attempt $i...")
                                for (route in routes) {
                                    val gateway = route.gateway
                                    if (gateway != null) {
                                        if(checkHostAvailable(gateway.hostAddress, getServerPort(), 1000)) {
                                            Log.d(TAG, "Found valid gateway out of routes")
                                            correctGateway = gateway.hostAddress
                                            break
                                        }
                                    }
                                }
                                if (correctGateway != null)
                                    break
                            }
                            if (correctGateway == null)
                                runOnUiThread { Toast.makeText(this@ReceiveActivity, "Did not find a valid gateway", Toast.LENGTH_LONG).show() }
                        }
                        this@ReceiveActivity.launch {
                            wifiWaitDialog?.setMessage(getString(R.string.attempting_to_connect))
                            overrideGateway = correctGateway
                            startClient()
                        }
                    }

                    override fun onLost(network: Network) {
                        this@ReceiveActivity.launch {
                            wifiWaitDialog?.cancel()
                            stopClient()
                        }
                    }
                }

                registerOnClientStop(true) {
                    conn.bindProcessToNetwork(null)
                    conn.unregisterNetworkCallback(callback)
                    overrideGateway = null
                }
                conn.requestNetwork(request, callback)
            }
            if (wifiReceiver == null) {
                wifiReceiver = WifiReceiver(onWifiEnabled)
                registerReceiver(wifiReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            }
        } else {
            //Connecting to a wifi on older than android 10
            val config = WifiConfiguration()
            config.SSID = "\"$ssid\""
            config.preSharedKey = "\"$preSharedKey\""
            val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wifi.isWifiEnabled) {
                wifi.isWifiEnabled = true
            }
            val netId = wifi.addNetwork(config)
            wifi.disconnect()
            wifi.enableNetwork(netId, true)
            wifi.reconnect()
            expectedWifis.add("\"$ssid\"")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == storagePermissionRequestCode && permissions.isNotEmpty() && permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Granted external storage permissions")
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) && requestCode == storagePermissionRequestCode) {
                restartActivity()
            } else if(requestCode == storagePermissionRequestCode) {
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
            if (requestCode == cameraPermissionRequestCode && permissions.isNotEmpty() && permissions[0] == Manifest.permission.CAMERA && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanQrCodeButton.performClick()
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

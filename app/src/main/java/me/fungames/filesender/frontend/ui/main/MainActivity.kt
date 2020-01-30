package me.fungames.filesender.frontend.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import me.fungames.filesender.R
import me.fungames.filesender.config.getDir
import me.fungames.filesender.frontend.ui.receive.ReceiveActivity
import me.fungames.filesender.frontend.ui.send.SendActivity
import java.net.InetAddress
import java.net.UnknownHostException


class MainActivity : AppCompatActivity() {

    companion object {
        private const val RESULT_CODE_FILE_DIALOG = 123
        lateinit var mainActivity: MainActivity
    }

    var files = mutableMapOf<String, Uri>()

    var fileNum = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        when (intent?.action) {
            Intent.ACTION_SEND -> handleFileShare(intent)
            Intent.ACTION_SEND_MULTIPLE -> handleMultipleFileShare(intent)
        }
        intent?.let { handleFileShare(it) }
        mainActivity = this
    }

    fun startHotspot(view: View) {
        val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    super.onStarted(reservation)
                    Snackbar.make(view, reservation.wifiConfiguration.preSharedKey, 100000).show()
                }
            }, Handler())
        }
    }

    fun openHotspot(view : View) {
        startActivity(Intent(this, ReceiveActivity::class.java))
        //Toast.makeText(applicationContext, "${hotspot.isON}", Toast.LENGTH_SHORT).show()
        //if (!server.isRunning)
        //    server.start()
        //Toast.makeText(applicationContext, "${hotspot.isON}", Toast.LENGTH_SHORT).show()
    }

    fun getServerIp(view: View) {
        startActivity(Intent(this, SendActivity::class.java))
        //Toast.makeText(applicationContext, "${getServerIpAddr()}", Toast.LENGTH_SHORT).show()
    }

    fun addFile(view: View) {
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, "Select a file"),
            RESULT_CODE_FILE_DIALOG
        )
    }

    fun listFiles(view: View) {
        Snackbar.make(view, getDir(), Snackbar.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_CODE_FILE_DIALOG && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { registerNewFile(it) }
        }
    }

    private fun handleFileShare(intent: Intent) {
        val extra = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri?
        extra?.let { registerNewFile(it) }
    }

    @Synchronized
    private fun registerNewFile(uri : Uri) {
        files[fileNum.toString()] = uri
        fileNum++
        Toast.makeText(applicationContext, "Registered new file: ${uri.path}", Toast.LENGTH_SHORT).show()
    }

    private fun handleMultipleFileShare(intent: Intent) {
        val extra = intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)
        extra?.forEach { share -> (share as? Uri?)?.let { registerNewFile(it) } }
    }


}

package me.fungames.filesender.frontend.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.fungames.filesender.config.getServerPort
import me.fungames.filesender.frontend.ui.receive.ReceiveActivity
import me.fungames.filesender.utils.checkHostAvailable
import me.fungames.filesender.utils.getServerIpAddr
import java.io.IOException
import java.util.*
import kotlin.concurrent.timerTask


/*class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val serviceIntent = Intent(context, ReceiverService::class.java)
        context.startService(serviceIntent)
    }
}*/

class ReceiverService : Service(), CoroutineScope by MainScope() {

    companion object {
        const val TAG = "ReceiverService"
    }

    var running = false

    var currentlyBound = 0

    var timer = Timer("FileSenderScanner")

    override fun onBind(intent: Intent?): IBinder? {
       /* currentlyBound++
        if (running) {
            timer.cancel()
            timer = Timer("FileSenderScanner")
            running = false
        }*/
        return null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        /*currentlyBound--
        if (currentlyBound <= 0) {
            currentlyBound = 0
            timer.schedule(timerTask { run.invoke() }, 0L, 10000L)
            running = true
        }*/
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Toast.makeText(applicationContext, "Service destroyed", Toast.LENGTH_SHORT).show()
        timer.cancel()
    }

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(applicationContext, "Service created", Toast.LENGTH_SHORT).show()
    }

    private val run = {
        val running = checkHostAvailable(getServerIpAddr()?:throw IOException("Failed to obtain server ip address"), getServerPort(), 5000)
        if (running) {
            Log.i(TAG, "Detected Filesender server")
            launch {
                Toast.makeText(baseContext, "Detected File Sender Server running on the connected network", Toast.LENGTH_LONG).show()
                val activity = Intent(baseContext, ReceiveActivity::class.java)
                activity.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                activity.putExtra("autoConnect", true)
                startActivity(activity)
            }

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Toast.makeText(applicationContext, "Service start", Toast.LENGTH_SHORT).show()
        if (!running && currentlyBound == 0) {
            timer.schedule(timerTask { run.invoke() }, 0L, 10000L)
            running = true
        }
        return START_STICKY
    }
}
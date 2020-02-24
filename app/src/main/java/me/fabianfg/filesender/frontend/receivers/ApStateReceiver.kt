package me.fabianfg.filesender.frontend.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import kotlinx.coroutines.launch
import me.fabianfg.filesender.frontend.ui.send.SendActivity

class ApStateReceiver(private val sendActivity: SendActivity) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.net.wifi.WIFI_AP_STATE_CHANGED") {
            val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0) % 10 == WifiManager.WIFI_STATE_ENABLED
            if (!state)
                sendActivity.launch { sendActivity.stopServer() }
        }
    }
}
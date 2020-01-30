package me.fungames.filesender.frontend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import me.fungames.filesender.frontend.ui.send.SendActivity

class ApStateReceiver(val sendActivity: SendActivity) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.net.wifi.WIFI_AP_STATE_CHANGED") {
            val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0) % 10 == WifiManager.WIFI_STATE_ENABLED
            //sendActivity.sender.onHotspotStateChanged(state)
        }
    }

}
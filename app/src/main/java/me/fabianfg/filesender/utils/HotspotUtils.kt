@file:Suppress("DEPRECATION")
package me.fabianfg.filesender.utils

import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Log

private const val TAG = "HotspotUtils"

fun WifiManager.setWifiApEnabled(enabled : Boolean) : Boolean {
    return try {
        val method = WifiManager::class.java.getMethod("setWifiApEnabled", WifiConfiguration::class.java, Boolean::class.java)
        method.invoke(this, null, enabled) as? Boolean? ?: false
    } catch (e : Exception) {
        Log.e(TAG, "Failed to setWifiApEnabled", e)
        false
    }
}

fun WifiManager.isWifiApEnabled() : Boolean {
    return try {
        val method = WifiManager::class.java.getMethod("isWifiApEnabled")
        method.invoke(this) as? Boolean? ?: false
    } catch (e : Exception) {
        Log.e(TAG, "Failed to check isWifiApEnabled", e)
        false
    }
}
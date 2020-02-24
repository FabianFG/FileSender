package me.fabianfg.filesender.utils

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.UnknownHostException

fun Context.getServerIpAddr(): String? {
    val wifiManager = this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val dhcpInfo = wifiManager.dhcpInfo
    val ipAddress = convert2Bytes(dhcpInfo.gateway)
    try {
        return InetAddress.getByAddress(ipAddress).hostAddress
    } catch (e: UnknownHostException) {
        e.printStackTrace()
    }
    return null
}

private fun convert2Bytes(hostAddress: Int): ByteArray {
    return byteArrayOf(
        (0xff and hostAddress).toByte(),
        (0xff and (hostAddress shr 8)).toByte(),
        (0xff and (hostAddress shr 16)).toByte(),
        (0xff and (hostAddress shr 24)).toByte()
    )
}
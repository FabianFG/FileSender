package me.fungames.filesender.frontend.ui.send

import android.app.Dialog
import android.net.wifi.WifiManager

class Android8HotspotInfoHolder(private val reservation: WifiManager.LocalOnlyHotspotReservation, val qrCodeDialog : Dialog) : HotspotInfoHolder {
    override fun close() {
        reservation.close()
        qrCodeDialog.dismiss()
    }
}
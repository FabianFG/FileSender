package me.fungames.filesender.frontend.ui.receive

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Window
import me.fungames.filesender.R
import kotlin.math.roundToInt

class FileshareReceiveDialog(val receiveActivity: ReceiveActivity) : Dialog(receiveActivity) {

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val inflater = LayoutInflater.from(receiveActivity)
        val view = inflater.inflate(R.layout.fileshare_receive_dialog, null)
        val displayMetrics = DisplayMetrics()
        receiveActivity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        view.minimumWidth = (displayMetrics.widthPixels * 0.9).roundToInt()
        setContentView(view)
    }
}
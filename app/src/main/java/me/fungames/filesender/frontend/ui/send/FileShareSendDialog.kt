package me.fungames.filesender.frontend.ui.send

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Window
import me.fungames.filesender.R
import kotlin.math.roundToInt

class FileShareSendDialog(private val sendActivity: SendActivity) : Dialog(sendActivity) {

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val inflater = LayoutInflater.from(sendActivity)
        val view = inflater.inflate(R.layout.fileshare_send_dialog, null)
        val displayMetrics = DisplayMetrics()
        sendActivity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        view.minimumWidth = (displayMetrics.widthPixels * 0.9).roundToInt()
        setContentView(view)
    }
}
package me.fungames.filesender.frontend.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import me.fungames.filesender.R
import me.fungames.filesender.frontend.ui.appsend.AppSendActivity
import me.fungames.filesender.frontend.ui.receive.ReceiveActivity
import me.fungames.filesender.frontend.ui.send.SendActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onReceiveClick(view : View) {
        startActivity(Intent(this, ReceiveActivity::class.java))
    }

    fun onSendClick(view: View) {
        startActivity(Intent(this, SendActivity::class.java))
    }
}

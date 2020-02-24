package me.fungames.filesender.frontend.ui.help

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_help.*
import me.fungames.filesender.R

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        helpListView.setAdapter(HelpListAdapter(this))
    }
}

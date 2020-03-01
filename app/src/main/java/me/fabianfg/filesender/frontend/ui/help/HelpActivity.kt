package me.fabianfg.filesender.frontend.ui.help

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_help.*
import me.fabianfg.filesender.R
import me.fabianfg.filesender.config.update

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        update(this)
        setContentView(R.layout.activity_help)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        helpListView.setAdapter(HelpListAdapter(this))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

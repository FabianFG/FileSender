package me.fungames.filesender.frontend.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import de.psdev.licensesdialog.LicensesDialog
import me.fungames.filesender.R
import me.fungames.filesender.frontend.ui.help.HelpActivity
import me.fungames.filesender.frontend.ui.history.FileHistoryActivity
import me.fungames.filesender.frontend.ui.receive.ReceiveActivity
import me.fungames.filesender.frontend.ui.send.SendActivity
import me.fungames.filesender.utils.unused


class MainActivity : AppCompatActivity() {

    private lateinit var licensesDialog: LicensesDialog
    private lateinit var aboutDialog : AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        licensesDialog = LicensesDialog.Builder(this)
            .setNotices(R.raw.notices)
            .setIncludeOwnLicense(true)
            .build()
        val view = LayoutInflater.from(this).inflate(R.layout.about_dialog, findViewById(R.id.aboutView), false)
        view.findViewById<TextView>(R.id.aboutVersion).text = getString(R.string.version_format, packageManager.getPackageInfo(packageName, 0).versionName)
        aboutDialog = AlertDialog.Builder(this).setView(view).setNeutralButton(R.string.ok, null).setIcon(R.drawable.ic_launcher_foreground).create()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_help -> {
                startActivity(Intent(this, HelpActivity::class.java))
            }
            R.id.action_licenses -> {
                licensesDialog.show()
            }
            R.id.action_about -> {
                aboutDialog.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onReceiveClick(view : View) {
        unused(view)
        startActivity(Intent(this, ReceiveActivity::class.java))
    }

    fun onSendClick(view: View) {
        unused(view)
        startActivity(Intent(this, SendActivity::class.java))
    }

    fun onHistoryClick(view: View) {
        unused(view)
        startActivity(Intent(this, FileHistoryActivity::class.java))
    }
}

@file:Suppress("DEPRECATION")

package me.fabianfg.filesender.frontend.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import de.psdev.licensesdialog.LicensesDialog
import kotlinx.android.synthetic.main.activity_main.*
import me.fabianfg.filesender.R
import me.fabianfg.filesender.config.update
import me.fabianfg.filesender.frontend.ui.help.HelpActivity
import me.fabianfg.filesender.frontend.ui.history.FileHistoryActivity
import me.fabianfg.filesender.frontend.ui.receive.ReceiveActivity
import me.fabianfg.filesender.frontend.ui.send.SendActivity
import me.fabianfg.filesender.utils.checkSelfPermissionCompat
import me.fabianfg.filesender.utils.unused
import java.io.File


class MainActivity : AppCompatActivity() {

    companion object {
        private const val storageRequestCode = 69
    }

    private lateinit var licensesDialog: LicensesDialog
    private lateinit var aboutDialog : AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        update(this)
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

    fun onSendAppClick(view: View) {
        unused(view)
        if (checkSelfPermissionCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                var file = File(me.fabianfg.filesender.config.getDir() + "/${getString(R.string.app_name)}_v${packageInfo.versionName}(${packageInfo.versionCode}).apk")
                try {
                    file = File(applicationInfo.sourceDir)
                        .copyTo(file, overwrite = false)
                } catch (e : FileAlreadyExistsException) {}
                putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this@MainActivity, "me.fabianfg.provider", file))
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                type = "*/*"
            }
            startActivity(Intent.createChooser(intent, getString(R.string.send_app_button)))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    storageRequestCode)
            } else {
                AlertDialog.Builder(this).setTitle(R.string.permission_denied).setMessage(R.string.permission_denied_body)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == storageRequestCode && permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            btn_send_app.performClick()
        }
    }
}

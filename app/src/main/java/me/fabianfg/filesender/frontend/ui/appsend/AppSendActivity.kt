package me.fabianfg.filesender.frontend.ui.appsend

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_app_send.*
import me.fabianfg.filesender.R
import me.fabianfg.filesender.config.update

class AppSendActivity : AppCompatActivity() {

    companion object {
        const val APK_PATH = "me.fungames.filesender.frontend.ui.appsend.AppSendActivity.APK_RESULT_PATH"
        const val APK_NAME = "me.fungames.filesender.frontend.ui.appsend.AppSendActivity.APK_RESULT_NAME"
    }

    private val apps = mutableListOf<ApplicationInfo>()
    private lateinit var adapter : AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        update(this)
        setContentView(R.layout.activity_app_send)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val scannedApps = packageManager.getInstalledApplications(0).filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }.sortedBy { it.loadLabel(packageManager).toString() }
        apps.addAll(scannedApps)
        adapter = AppListAdapter(this, R.layout.app_list_item, apps)
        appListView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

package me.fabianfg.filesender.frontend.ui.history

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_file_history.*
import me.fabianfg.filesender.R
import me.fabianfg.filesender.config.update
import me.fabianfg.filesender.frontend.ui.receive.ReceiveActivity
import me.fabianfg.filesender.utils.checkSelfPermissionCompat
import java.io.File

class FileHistoryActivity : AppCompatActivity() {

    companion object {
        const val TAG = "FileHistoryActivity"
    }


    private lateinit var sharedPrefs : SharedPreferences

    private lateinit var adapter : FileHistoryListAdapter
    private val files = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        update(this)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPrefs.getStringSet("file_receive_history", emptySet())
        requestStoragePermission()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(R.layout.activity_file_history)
        adapter = FileHistoryListAdapter(this, R.layout.file_history_list_item, files)
        historyListView.adapter = adapter
        historyListView.emptyView = noFilesReceivedYetView
        if (checkSelfPermissionCompat(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            updateFileList()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun updateFileList() {
        updateVerifiedFileList()
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        updateFileList()
    }

    private fun updateVerifiedFileList() {
        val fileSet = sharedPrefs.getStringSet("file_receive_history", emptySet())!!
        files.clear()
        fileSet.forEach {
            val file = File(it)
            if (file.exists())
                files.add(file)
        }
    }

    fun onFileDelete(file: File) {
        files.remove(file)
        adapter.notifyDataSetChanged()
        runCatching { file.delete() }.onFailure { Log.e(TAG, "Failed to delete file ${file.absolutePath}") }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                ReceiveActivity.storagePermissionRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == ReceiveActivity.storagePermissionRequestCode && permissions.isNotEmpty() && permissions[0] == Manifest.permission.READ_EXTERNAL_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(ReceiveActivity.TAG, "Granted external storage permissions")
            updateFileList()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) && requestCode == ReceiveActivity.storagePermissionRequestCode) {
                restartActivity()
            } else if(requestCode == ReceiveActivity.storagePermissionRequestCode) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.permission_denied)
                    .setMessage(R.string.permission_denied_body)
                    .setNegativeButton(R.string.ok) { _ , _ ->
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri: Uri =
                            Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        finish()
                        startActivity(intent)
                    }
                    .create()
                    .show()
            }
        }
    }

    private fun restartActivity() {
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}

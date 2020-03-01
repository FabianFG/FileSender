package me.fabianfg.filesender.config

import android.content.Context
import android.os.Environment
import android.provider.Settings
import android.util.Log
import me.fabianfg.filesender.BuildConfig
import java.io.File


private const val TAG = "FileCatchConfig"

fun update(context : Context) {
    val downloadPath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    if (downloadPath != null) {
        try {
            downloadPath.mkdir()
        } catch (e : Exception) {
            Log.w(TAG, "Failed to mkdir() for download directory")
        }
        dir = downloadPath.absolutePath
    }
    else {
        Log.w(TAG, "Failed to fetch download directory, falling back to default")
        val file = File("/storage/emulated/0/Download")
        try {
            file.mkdir()
        } catch (e : Exception) {
            Log.w(TAG, "Failed to mkdir() for fallback download directory")
        }
        dir = file.absolutePath
    }
    name = Settings.Secure.getString(context.contentResolver, "bluetooth_name")
}

private lateinit var dir : String
private lateinit var name : String

fun getChunkSize() = 10000
fun getClientTimeout() = 10000
fun getConnTimeout() = 10
fun getName() = name
fun getDir() = dir
fun getVersion() = BuildConfig.VERSION_NAME
fun getServerPort() = 6969
fun doOpenFilesAfterReceiving() = true
fun getMaxConnectAttempts() = 5

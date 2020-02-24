package me.fabianfg.filesender.config

import android.os.Environment
import me.fabianfg.filesender.BuildConfig

fun getChunkSize() = 10000
fun getClientTimeout() = 10000
fun getConnTimeout() = 10
fun getName() = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
@Suppress("DEPRECATION")
fun getDir() : String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
fun getVersion() = BuildConfig.VERSION_NAME
fun getServerPort() = 6969
fun doOpenFilesAfterReceiving() = true
fun getMaxConnectAttempts() = 5
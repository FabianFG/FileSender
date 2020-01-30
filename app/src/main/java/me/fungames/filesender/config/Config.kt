package me.fungames.filesender.config

import android.os.Environment
import me.fungames.filesender.BuildConfig

fun getChunkSize() = 10000
fun getClientTimeout() = 10000
fun getConnTimeout() = 10
fun getName() = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
@Suppress("DEPRECATION")
fun getDir() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath!!
fun getVersion() = BuildConfig.VERSION_NAME
fun getServerPort() = 6969
fun doOpenFilesAfterReceiving() = true

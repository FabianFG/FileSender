package me.fungames.filesender.utils

import android.app.ActivityManager
import android.content.Context

fun Context.isMyServiceRunning(serviceClass: Class<*>) {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    manager.getRunningServices(Int.MAX_VALUE)
}
package me.fungames.filesender.utils

import android.app.Activity
import android.os.Build

fun Activity.checkSelfPermissionCompat(permission: String): Int {
    return  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        checkSelfPermission(permission)
    } else {
        packageManager.checkPermission(permission, packageName)
    }
}

@Suppress("UNUSED_PARAMETER")
fun unused(vararg any: Any?) {}
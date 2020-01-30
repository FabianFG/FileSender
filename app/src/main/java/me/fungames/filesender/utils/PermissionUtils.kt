package me.fungames.filesender.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(api = Build.VERSION_CODES.M)
fun Activity.neverAskAgainSelected(permission: String): Boolean {
    val prevShouldShowStatus = getRatinaleDisplayStatus(permission)
    val currShouldShowStatus =
        shouldShowRequestPermissionRationale(permission)
    return prevShouldShowStatus != currShouldShowStatus
}

fun Context.setShouldShowStatus(permission: String) {
    val genPrefs: SharedPreferences =
        getSharedPreferences("GENERIC_PREFERENCES", Context.MODE_PRIVATE)
    val editor = genPrefs.edit()
    editor.putBoolean(permission, true)
    editor.apply()
}

fun Context.getRatinaleDisplayStatus(
    permission: String
): Boolean {
    val genPrefs: SharedPreferences =
        getSharedPreferences("GENERIC_PREFERENCES", Context.MODE_PRIVATE)
    return genPrefs.getBoolean(permission, false)
}
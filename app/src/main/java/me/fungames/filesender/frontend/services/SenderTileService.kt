package me.fungames.filesender.frontend.services

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.preference.PreferenceManager

//TODO
@RequiresApi(Build.VERSION_CODES.N)
class SenderTileService: TileService(){

    lateinit var pref : SharedPreferences

    var active
        get() = pref.getBoolean("tileActivated", false)
        set(value) = pref.edit { putBoolean("tileActivated", value) }

    override fun onCreate() {
        super.onCreate()
        pref = PreferenceManager.getDefaultSharedPreferences(baseContext)
        if (!pref.contains("tileActivated")) {
            pref.edit { putBoolean("tileActivated", false) }
        }
        Toast.makeText(this, "Create", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "Destroy", Toast.LENGTH_LONG).show()
    }

    override fun onClick() {
        super.onClick()
        if (active) {
            stopService(Intent(this, ReceiverService::class.java))
            qsTile.state = Tile.STATE_INACTIVE
            active = false
        } else {
            startService(Intent(this, ReceiverService::class.java))
            qsTile.state = Tile.STATE_ACTIVE
            active = true
        }
        qsTile.updateTile()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        stopService(Intent(this, ReceiverService::class.java))
    }

    override fun onTileAdded() {
        super.onTileAdded()
        if (active) {
            startService(Intent(this, ReceiverService::class.java))
            qsTile.state = Tile.STATE_ACTIVE
            active = true
        } else {
            stopService(Intent(this, ReceiverService::class.java))
            qsTile.state = Tile.STATE_INACTIVE
            active = false
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        if (active) {
            startService(Intent(this, ReceiverService::class.java))
            qsTile.state = Tile.STATE_ACTIVE
            active = true
        } else {
            stopService(Intent(this, ReceiverService::class.java))
            qsTile.state = Tile.STATE_INACTIVE
            active = false
        }
        qsTile.updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
    }
}
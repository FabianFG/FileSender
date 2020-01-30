package me.fungames.filesender.frontend.services

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi

//TODO
@RequiresApi(Build.VERSION_CODES.N)
class SenderTileService: TileService(){

    override fun onClick() {
        super.onClick()
        Toast.makeText(this, "Clicked", Toast.LENGTH_LONG).show()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        Toast.makeText(this, "Tile removed", Toast.LENGTH_LONG).show()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        Toast.makeText(this, "Tile added", Toast.LENGTH_LONG).show()
        // Do something when the user add the Tile
    }

    override fun onStartListening() {
        super.onStartListening()
        Toast.makeText(this, "Start listening", Toast.LENGTH_LONG).show()
        startService(Intent(this, ReceiverService::class.java))
        // Called when the Tile becomes visible
    }

    override fun onStopListening() {
        super.onStopListening()
        Toast.makeText(this, "Stop listening", Toast.LENGTH_LONG).show()
        // Called when the tile is no longer visible
    }
}
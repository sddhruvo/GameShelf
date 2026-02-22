package com.gamevault.app.service

import android.content.Intent
import android.service.quicksettings.TileService
import com.gamevault.app.ui.MainActivity

class GameVaultTileService : TileService() {

    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityAndCollapse(intent)
    }
}

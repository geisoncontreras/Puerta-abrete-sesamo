package com.abretesesamo.app.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abretesesamo.app.data.SettingsRepository

/** Vuelve a activar la puerta al reiniciar el teléfono o al actualizar la app. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val settings = SettingsRepository(context)
        if (settings.lockEnabled && settings.rhythm != null) LockService.start(context)
    }
}

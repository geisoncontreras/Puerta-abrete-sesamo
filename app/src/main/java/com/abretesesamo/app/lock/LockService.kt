package com.abretesesamo.app.lock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.abretesesamo.app.R
import com.abretesesamo.app.ui.settings.MainActivity

/**
 * Servicio en primer plano (con notificación fija) que vigila cuándo se apaga la pantalla.
 *
 * Android no deja que una app "sustituya" el bloqueo seguro del sistema. Lo que hacemos es
 * poner nuestra puerta justo cuando la pantalla se apaga, de modo que al encenderla lo primero
 * que se ve es la puerta, por ENCIMA del bloqueo normal (showWhenLocked).
 *
 * ACTION_SCREEN_OFF solo se puede escuchar registrando el receptor en código, por eso hace
 * falta un servicio vivo.
 */
class LockService : Service() {

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) LockActivity.show(context)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOffReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Puerta activa",
            NotificationManager.IMPORTANCE_MIN,
        ).apply { description = "Mantiene la puerta lista para cuando bloquees el teléfono" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_door)
        .setContentTitle("Tu puerta secreta está activa")
        .setContentText("Aparecerá cada vez que bloquees el teléfono")
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        )
        .build()

    companion object {
        private const val CHANNEL_ID = "puerta_activa"
        private const val NOTIFICATION_ID = 7

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LockService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockService::class.java))
        }
    }
}

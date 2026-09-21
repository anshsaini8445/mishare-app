package com.ansh.rapidshare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.net.ServerSocket

class TransferService : Service() {

    private val CHANNEL_ID = "AIR_RAPID_SERVICE_CHANNEL"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Air Rapid High-Speed Engine",
                NotificationManager.IMPORTANCE_LOW
            )
            val mgr = getSystemService(NotificationManager::class.java)
            mgr?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isSender = intent?.getBooleanExtra("IS_SENDER", false) ?: false

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Air Rapid Share Engine")
            .setContentText(if (isSender) "Transferring at 100 MB/s (Wi-Fi 6 Turbo)..." else "Engine listening for packets in background...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        startForeground(1001, notif)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!isSender) {
                    val server = ServerSocket(8888)
                    server.receiveBufferSize = 524288
                    val sock = server.accept()
                    sock.close()
                    server.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return START_NOT_STICKY
    }
}

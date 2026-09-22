package com.ansh.rapidshare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class TransferService : Service() {

    private val CHANNEL_ID = "AIR_RAPID_SERVICE_CHANNEL"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID,
                "Air Rapid High-Speed Engine",
                NotificationManager.IMPORTANCE_LOW
            )
            val mgr = getSystemService(NotificationManager::class.java)
            mgr?.createNotificationChannel(chan)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isSender = intent?.getBooleanExtra("IS_SENDER", false) ?: false
        val targetIp = intent?.getStringExtra("TARGET_IP") ?: "192.168.43.1"
        val files = intent?.getStringArrayListExtra("FILES") ?: ArrayList()

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Air Rapid Share Engine")
            .setContentText(if (isSender) "Streaming files..." else "Ready for files...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        startForeground(1001, notif)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isSender) {
                    val sock = Socket(targetIp, 8888)
                    sock.tcpNoDelay = true
                    sock.sendBufferSize = 262144

                    val dos = DataOutputStream(BufferedOutputStream(sock.getOutputStream()))
                    dos.writeInt(files.size)

                    for (p in files) {
                        val f = File(p)
                        if (!f.exists()) continue
                        dos.writeUTF(f.name)
                        dos.writeLong(f.length())

                        val fis = FileInputStream(f)
                        val buf = ByteArray(262144)
                        var r: Int
                        while (fis.read(buf).also { r = it } != -1) {
                            dos.write(buf, 0, r)
                        }
                        fis.close()
                        dos.flush()
                    }
                    dos.close()
                    sock.close()
                } else {
                    val server = ServerSocket(8888)
                    server.receiveBufferSize = 262144
                    val sock = server.accept()
                    sock.tcpNoDelay = true

                    val dis = DataInputStream(BufferedInputStream(sock.getInputStream()))
                    val count = dis.readInt()

                    val destFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AirRapidShare")
                    if (!destFolder.exists()) destFolder.mkdirs()

                    for (i in 0 until count) {
                        val name = dis.readUTF()
                        val len = dis.readLong()

                        val outFile = File(destFolder, name)
                        val fos = FileOutputStream(outFile)
                        val buf = ByteArray(262144)
                        var rem = len
                        while (rem > 0) {
                            val r = dis.read(buf, 0, Math.min(buf.size.toLong(), rem).toInt())
                            if (r == -1) break
                            fos.write(buf, 0, r)
                            rem -= r
                        }
                        fos.close()

                        MediaScannerConnection.scanFile(
                            applicationContext,
                            arrayOf(outFile.absolutePath),
                            null,
                            null
                        )
                    }
                    dis.close()
                    sock.close()
                    server.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }
}

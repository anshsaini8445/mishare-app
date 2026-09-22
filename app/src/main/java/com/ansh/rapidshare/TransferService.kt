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
        val targetIp = intent?.getStringExtra("TARGET_IP") ?: "192.168.43.1"
        val files = intent?.getStringArrayListExtra("FILE_PATHS") ?: ArrayList()

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Air Rapid Share Engine")
            .setContentText(if (isSender) "Sending files at 100 MB/s..." else "Listening for incoming files...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        startForeground(1001, notif)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isSender) {
                    runSenderPipeline(targetIp, files)
                } else {
                    runReceiverPipeline()
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

    private fun runSenderPipeline(targetIp: String, filePaths: List<String>) {
        try {
            val socket = Socket(targetIp, 8888)
            socket.tcpNoDelay = true
            socket.sendBufferSize = 262144

            val dos = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
            dos.writeInt(filePaths.size)

            for (path in filePaths) {
                val file = File(path)
                if (!file.exists()) continue

                dos.writeUTF(file.name)
                dos.writeLong(file.length())

                val fis = FileInputStream(file)
                val buffer = ByteArray(262144)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    dos.write(buffer, 0, read)
                }
                fis.close()
                dos.flush()
            }
            dos.close()
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun runReceiverPipeline() {
        try {
            val serverSocket = ServerSocket(8888)
            serverSocket.receiveBufferSize = 262144
            val clientSocket = serverSocket.accept()
            clientSocket.tcpNoDelay = true

            val dis = DataInputStream(BufferedInputStream(clientSocket.getInputStream()))
            val totalFiles = dis.readInt()

            val destFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AirRapidShare")
            if (!destFolder.exists()) destFolder.mkdirs()

            for (i in 0 until totalFiles) {
                val fileName = dis.readUTF()
                val fileSize = dis.readLong()

                val outputFile = File(destFolder, fileName)
                val fos = FileOutputStream(outputFile)

                val buffer = ByteArray(262144)
                var remaining = fileSize
                while (remaining > 0) {
                    val read = dis.read(buffer, 0, Math.min(buffer.size.toLong(), remaining).toInt())
                    if (read == -1) break
                    fos.write(buffer, 0, read)
                    remaining -= read
                }
                fos.close()

                MediaScannerConnection.scanFile(
                    applicationContext,
                    arrayOf(outputFile.absolutePath),
                    null,
                    null
                )
            }
            dis.close()
            clientSocket.close()
            serverSocket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

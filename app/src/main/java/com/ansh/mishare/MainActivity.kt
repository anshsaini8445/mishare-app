package com.ansh.mishare

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private val PORT = 8888
    private val PICK_FILE_REQ = 101

    private lateinit var tvStatus: TextView
    private lateinit var tvTransferLog: TextView
    private lateinit var imgQrCode: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvTransferLog = findViewById(R.id.tvTransferLog)
        imgQrCode = findViewById(R.id.imgQrCode)

        findViewById<Button>(R.id.btnReceive).setOnClickListener {
            startReceiverServer()
        }

        findViewById<Button>(R.id.btnSend).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, PICK_FILE_REQ)
        }
    }

    // 1. Offline Receiver Engine (Server Socket)
    private fun startReceiverServer() {
        tvTransferLog.text = "Waiting for Sender... Hotspot IP: 192.168.43.1"
        imgQrCode.visibility = View.VISIBLE
        generateQRCode("MISHARE:192.168.43.1:$PORT")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverSocket = ServerSocket(PORT)
                val socket = serverSocket.accept()
                withContext(Dispatchers.Main) {
                    tvTransferLog.text = "Connected! Receiving binary stream..."
                }

                val inputStream = DataInputStream(socket.getInputStream())
                val fileName = inputStream.readUTF()
                val fileSize = inputStream.readLong()

                val downloadsDir = getExternalFilesDir(null)
                val targetFile = File(downloadsDir, fileName)
                val fos = FileOutputStream(targetFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L

                while (totalRead < fileSize) {
                    bytesRead = inputStream.read(buffer, 0, minOf(buffer.size.toLong(), fileSize - totalRead).toInt())
                    if (bytesRead == -1) break
                    fos.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                }

                fos.close()
                socket.close()
                serverSocket.close()

                withContext(Dispatchers.Main) {
                    tvTransferLog.text = "Completed: $fileName saved!"
                    Toast.makeText(this@MainActivity, "File received successfully!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvTransferLog.text = "Error: ${e.message}"
                }
            }
        }
    }

    // 2. Offline Sender Engine (Client Socket)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQ && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                sendSelectedFile(uri)
            }
        }
    }

    private fun sendSelectedFile(uri: Uri) {
        val targetIP = "192.168.43.1" // Default hotspot IP
        tvTransferLog.text = "Connecting to receiver: $targetIP..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket(targetIP, PORT)
                val outputStream = DataOutputStream(socket.getOutputStream())
                val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return@launch
                val fileLength = pfd.statSize
                val inputStream = contentResolver.openInputStream(uri) ?: return@launch

                outputStream.writeUTF("Shared_File_${System.currentTimeMillis()}")
                outputStream.writeLong(fileLength)

                val buffer = ByteArray(8192)
                var len: Int
                while (inputStream.read(buffer).also { len = it } != -1) {
                    outputStream.write(buffer, 0, len)
                }
                outputStream.flush()
                socket.close()
                pfd.close()

                withContext(Dispatchers.Main) {
                    tvTransferLog.text = "File sent successfully via 5GHz Direct!"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvTransferLog.text = "Failed to connect to receiver IP: $targetIP"
                }
            }
        }
    }

    private fun generateQRCode(content: String) {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        imgQrCode.setImageBitmap(bitmap)
    }
}

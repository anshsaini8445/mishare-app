package com.ansh.rapidshare

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.net.NetworkInterface

class ReceiveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive)

        findViewById<TextView>(R.id.btnBackReceive).setOnClickListener { finish() }

        val model = Build.MODEL ?: "Android Device"
        findViewById<TextView>(R.id.tvDeviceHeader).text = model

        val localIp = fetchLocalIP()
        val qrString = "INSHARE://IP:$localIp;PORT:8888;DEVICE:$model;;"
        findViewById<ImageView>(R.id.ivQRCode).setImageBitmap(generateQR(qrString))
        findViewById<TextView>(R.id.tvHotspotSSID).text = "Device IP: $localIp (Port 8888)"

        val serviceIntent = Intent(this, TransferService::class.java).apply {
            putExtra("IS_SENDER", false)
        }
        startService(serviceIntent)
    }

    private fun fetchLocalIP(): String {
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces()
            for (intf in ifaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') < 0) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "192.168.43.1"
    }

    private fun generateQR(text: String): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}

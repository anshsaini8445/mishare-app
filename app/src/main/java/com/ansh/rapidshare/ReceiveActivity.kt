package com.ansh.rapidshare

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class ReceiveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive)

        findViewById<TextView>(R.id.btnBackReceive).setOnClickListener { finish() }

        val model = Build.MODEL ?: "Air Rapid Device"
        findViewById<TextView>(R.id.tvDeviceHeader).text = model

        val qrContent = "AIR_RAPID://SSID:DIRECT-AIR-RAPID-6G;IP:192.168.43.1;PORT:8888;BAND:6GHZ;;"
        findViewById<ImageView>(R.id.ivQRCode).setImageBitmap(makeQR(qrContent))

        findViewById<Button>(R.id.btnWifiDirectSwitch).setOnClickListener {
            showModeSelect()
        }

        findViewById<Button>(R.id.btnReceiveFromPC).setOnClickListener {
            startActivity(Intent(this, WebShareActivity::class.java))
        }

        val serviceIntent = Intent(this, TransferService::class.java).apply {
            putExtra("IS_SENDER", false)
        }
        startService(serviceIntent)
    }

    private fun showModeSelect() {
        val options = arrayOf(
            "Wi-Fi Direct\nTurn on Wi-Fi to connect (Fastest)",
            "Hotspot\nCreate a Hotspot to connect (Universal)"
        )
        AlertDialog.Builder(this)
            .setTitle("Transfer mode")
            .setMessage("No Internet data is consumed in both two modes.")
            .setSingleChoiceItems(options, 0) { dialog, which ->
                val chosen = if (which == 0) "Wi-Fi Direct (6GHz)" else "Hotspot Mode"
                Toast.makeText(this, "Active: $chosen", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun makeQR(data: String): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}

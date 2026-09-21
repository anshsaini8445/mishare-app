package com.ansh.rapidshare

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class WebShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_share)

        findViewById<TextView>(R.id.btnBackWeb).setOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.optHotspotMode).setOnClickListener {
            showIPDialog("Hotspot Mode (Faster)", "192.168.43.1")
        }

        findViewById<LinearLayout>(R.id.optWifiMode).setOnClickListener {
            showIPDialog("Wi-Fi Mode (Convenient)", "192.168.1.100")
        }
    }

    private fun showIPDialog(mode: String, ip: String) {
        AlertDialog.Builder(this)
            .setTitle(mode)
            .setMessage("Connect your PC/iPhone to same network and open:\nhttp://$ip:8888")
            .setPositiveButton("OK", null)
            .show()
    }
}

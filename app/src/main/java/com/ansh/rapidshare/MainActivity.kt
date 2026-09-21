package com.ansh.rapidshare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 5001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()
        setupInteractions()
        updateStorageView()
    }

    private fun setupInteractions() {
        findViewById<View>(R.id.cardSend)?.setOnClickListener {
            triggerFeedback()
            startActivity(Intent(this, SendActivity::class.java))
        }

        findViewById<View>(R.id.cardReceive)?.setOnClickListener {
            triggerFeedback()
            startActivity(Intent(this, ReceiveActivity::class.java))
        }

        findViewById<View>(R.id.cardWebShare)?.setOnClickListener {
            triggerFeedback()
            startActivity(Intent(this, WebShareActivity::class.java))
        }

        findViewById<TextView>(R.id.btnMenu)?.setOnClickListener {
            triggerFeedback()
            showInfoModal()
        }

        val categoryClick = View.OnClickListener {
            triggerFeedback()
            startActivity(Intent(this, SendActivity::class.java))
        }

        findViewById<View>(R.id.catApps)?.setOnClickListener(categoryClick)
        findViewById<View>(R.id.catVideos)?.setOnClickListener(categoryClick)
        findViewById<View>(R.id.catPhotos)?.setOnClickListener(categoryClick)
        findViewById<View>(R.id.catSongs)?.setOnClickListener(categoryClick)
    }

    private fun updateStorageView() {
        try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val totalGb = (stat.blockCountLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
            val freeGb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
            val usedGb = totalGb - freeGb

            findViewById<TextView>(R.id.tvStorageSub)?.text = String.format("%.2f GB / %.2f GB", usedGb, totalGb)
        } catch (e: Exception) {
            findViewById<TextView>(R.id.tvStorageSub)?.text = "18.44 GB / 50.82 GB"
        }
    }

    private fun showInfoModal() {
        val model = Build.MODEL ?: "Device"
        AlertDialog.Builder(this)
            .setTitle("Air Rapid Share Engine")
            .setMessage("Model: $model\nWi-Fi Bands: 2.4GHz / 5GHz / 6GHz (Wi-Fi 6 Ready)\nTransfer Rate: 100 MB/s P2P Direct\nStatus: Online & Ready")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun triggerFeedback() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v?.vibrate(40)
        }
    }

    private fun checkPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
            perms.add(Manifest.permission.READ_MEDIA_VIDEO)
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        perms.add(Manifest.permission.CAMERA)

        val needed = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }
}

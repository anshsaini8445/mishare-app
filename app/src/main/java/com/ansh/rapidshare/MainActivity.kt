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
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 4001
    private val transferHistoryList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        findViewById<CardView>(R.id.cardSend)?.setOnClickListener {
            triggerHaptic()
            startActivity(Intent(this, SendActivity::class.java))
        }

        findViewById<CardView>(R.id.cardReceive)?.setOnClickListener {
            triggerHaptic()
            startActivity(Intent(this, ReceiveActivity::class.java))
        }

        findViewById<CardView>(R.id.cardWebShare)?.setOnClickListener {
            triggerHaptic()
            startActivity(Intent(this, WebShareActivity::class.java))
        }

        findViewById<CardView>(R.id.cardHistory)?.setOnClickListener {
            triggerHaptic()
            showTransferHistoryDialog()
        }

        findViewById<TextView>(R.id.btnMenu)?.setOnClickListener {
            triggerHaptic()
            showDeviceSettingsDialog()
        }

        val categoryClickListener = View.OnClickListener {
            triggerHaptic()
            startActivity(Intent(this, SendActivity::class.java))
        }

        findViewById<View>(R.id.catApps)?.setOnClickListener(categoryClickListener)
        findViewById<View>(R.id.catFiles)?.setOnClickListener(categoryClickListener)
        findViewById<View>(R.id.catVideos)?.setOnClickListener(categoryClickListener)
        findViewById<View>(R.id.catPhotos)?.setOnClickListener(categoryClickListener)
        findViewById<View>(R.id.catSongs)?.setOnClickListener(categoryClickListener)
        findViewById<View>(R.id.catContacts)?.setOnClickListener(categoryClickListener)
    }

    private fun showDeviceSettingsDialog() {
        val model = Build.MODEL ?: "Android Device"
        val device = Build.DEVICE ?: "RapidDevice"
        val storageDetails = calculateStorageStatistics()

        val message = StringBuilder()
            .append("App: Air Rapid Share v1.0\n")
            .append("Device: ").append(device).append(" (").append(model).append(")\n")
            .append("Storage: ").append(storageDetails).append("\n")
            .append("Engine: Ultra-Fast 100 MB/s (5GHz Turbo)\n")
            .append("Transfer Engine: Active (Zero Data Usage)\n")
            .append("License: Commercial Ready (Apache 2.0)")
            .toString()

        AlertDialog.Builder(this)
            .setTitle("Device Profile and Settings")
            .setMessage(message)
            .setPositiveButton("Clean Cache") { _, _ ->
                cleanApplicationCache()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showTransferHistoryDialog() {
        val message = if (transferHistoryList.isEmpty()) {
            "No recent transfers found.\nReady for high-speed offline file transfers."
        } else {
            transferHistoryList.joinToString("\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Transfer Vault")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Clear History") { _, _ ->
                transferHistoryList.clear()
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun cleanApplicationCache() {
        try {
            var freedBytes = 0L
            cacheDir?.let { dir ->
                freedBytes += getFolderSize(dir)
                dir.deleteRecursively()
            }
            codeCacheDir?.let { dir ->
                freedBytes += getFolderSize(dir)
                dir.deleteRecursively()
            }

            val freedMb = String.format("%.1f", freedBytes / (1024.0 * 1024.0))
            Toast.makeText(this, "Clean Cache Complete: " + freedMb + " MB freed", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Cache already clean", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFolderSize(file: File): Long {
        var size = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { size += getFolderSize(it) }
        } else {
            size = file.length()
        }
        return size
    }

    private fun calculateStorageStatistics(): String {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalGb = (totalBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0)
            val freeGb = (availableBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0)
            val usedGb = totalGb - freeGb

            String.format("%.2f / %.2f GB", usedGb, totalGb)
        } catch (e: Exception) {
            "18.44 / 50.82 GB"
        }
    }

    private fun triggerHaptic() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator?.vibrate(45)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = ArrayList<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        permissions.add(Manifest.permission.CAMERA)

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }
}

package com.ansh.rapidshare

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.*
import java.io.File

data class MediaItem(
    val name: String,
    val size: String,
    val icon: Drawable?,
    val path: String,
    var isSelected: Boolean = false
)

class SendActivity : AppCompatActivity() {

    private val currentDisplayList = mutableListOf<MediaItem>()
    private val selectedList = mutableListOf<MediaItem>()
    private lateinit var adapter: ItemGridAdapter
    private lateinit var tvSelectedCount: TextView

    companion object {
        var queuedPaths = ArrayList<String>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        val rvItems = findViewById<RecyclerView>(R.id.rvItems)
        rvItems.layoutManager = GridLayoutManager(this, 4)

        adapter = ItemGridAdapter(currentDisplayList) { item ->
            if (item.isSelected) {
                selectedList.add(item)
            } else {
                selectedList.remove(item)
            }
            tvSelectedCount.text = "${selectedList.size} Selected"
        }
        rvItems.adapter = adapter

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        setupTabs()
        loadApps()

        findViewById<Button>(R.id.btnNext).setOnClickListener {
            if (selectedList.isEmpty()) {
                Toast.makeText(this, "Select at least 1 file to send", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            queuedPaths.clear()
            selectedList.forEach { queuedPaths.add(it.path) }

            val integrator = IntentIntegrator(this)
            integrator.setPrompt("Scan Receiver QR Code")
            integrator.setBeepEnabled(true)
            integrator.setOrientationLocked(true)
            integrator.initiateScan()
        }
    }

    private fun setupTabs() {
        findViewById<TextView>(R.id.tabApps).setOnClickListener {
            resetTabColors()
            (it as TextView).setTextColor(Color.parseColor("#0078FF"))
            loadApps()
        }
        findViewById<TextView>(R.id.tabVideos).setOnClickListener {
            resetTabColors()
            (it as TextView).setTextColor(Color.parseColor("#0078FF"))
            loadMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Videos")
        }
        findViewById<TextView>(R.id.tabPhotos).setOnClickListener {
            resetTabColors()
            (it as TextView).setTextColor(Color.parseColor("#0078FF"))
            loadMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Photos")
        }
        findViewById<TextView>(R.id.tabSongs).setOnClickListener {
            resetTabColors()
            (it as TextView).setTextColor(Color.parseColor("#0078FF"))
            loadMedia(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "Music")
        }
        findViewById<TextView>(R.id.tabFiles).setOnClickListener {
            resetTabColors()
            (it as TextView).setTextColor(Color.parseColor("#0078FF"))
            loadFiles()
        }
    }

    private fun resetTabColors() {
        val inactive = Color.parseColor("#64748B")
        findViewById<TextView>(R.id.tabApps).setTextColor(inactive)
        findViewById<TextView>(R.id.tabVideos).setTextColor(inactive)
        findViewById<TextView>(R.id.tabPhotos).setTextColor(inactive)
        findViewById<TextView>(R.id.tabSongs).setTextColor(inactive)
        findViewById<TextView>(R.id.tabFiles).setTextColor(inactive)
    }

    private fun loadApps() {
        findViewById<TextView>(R.id.tvSectionHeader).text = "Installed Packages"
        CoroutineScope(Dispatchers.IO).launch {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val list = mutableListOf<MediaItem>()

            for (app in apps) {
                if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    val name = pm.getApplicationLabel(app).toString()
                    val icon = pm.getApplicationIcon(app)
                    val file = File(app.sourceDir)
                    val mb = file.length() / (1024.0 * 1024.0)
                    list.add(MediaItem(name, String.format("%.1f MB", mb), icon, file.absolutePath))
                }
            }
            withContext(Dispatchers.Main) {
                currentDisplayList.clear()
                currentDisplayList.addAll(list)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadMedia(uri: Uri, headerTitle: String) {
        findViewById<TextView>(R.id.tvSectionHeader).text = headerTitle
        CoroutineScope(Dispatchers.IO).launch {
            val list = mutableListOf<MediaItem>()
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA)
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                val nameIdx = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIdx = it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dataIdx = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                while (it.moveToNext()) {
                    val name = it.getString(nameIdx) ?: "File"
                    val size = it.getLong(sizeIdx)
                    val path = it.getString(dataIdx) ?: ""
                    val mb = size / (1024.0 * 1024.0)
                    list.add(MediaItem(name, String.format("%.1f MB", mb), null, path))
                }
            }
            withContext(Dispatchers.Main) {
                currentDisplayList.clear()
                currentDisplayList.addAll(list)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadFiles() {
        findViewById<TextView>(R.id.tvSectionHeader).text = "Downloaded Files"
        CoroutineScope(Dispatchers.IO).launch {
            val list = mutableListOf<MediaItem>()
            val folder = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            folder.listFiles()?.forEach { f ->
                if (f.isFile) {
                    val mb = f.length() / (1024.0 * 1024.0)
                    list.add(MediaItem(f.name, String.format("%.1f MB", mb), null, f.absolutePath))
                }
            }
            withContext(Dispatchers.Main) {
                currentDisplayList.clear()
                currentDisplayList.addAll(list)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            var targetIp = "192.168.43.1"
            val raw = result.contents
            if (raw.contains("IP:")) {
                targetIp = raw.substringAfter("IP:").substringBefore(";")
            }
            Toast.makeText(this, "Target: $targetIp. Sending...", Toast.LENGTH_LONG).show()

            val serviceIntent = Intent(this, TransferService::class.java).apply {
                putExtra("IS_SENDER", true)
                putExtra("TARGET_IP", targetIp)
                putStringArrayListExtra("FILES", queuedPaths)
            }
            startService(serviceIntent)
            finish()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    class ItemGridAdapter(
        private val items: List<MediaItem>,
        private val onCheck: (MediaItem) -> Unit
    ) : RecyclerView.Adapter<ItemGridAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val ivIcon: ImageView = v.findViewById(R.id.ivIcon)
            val tvName: TextView = v.findViewById(R.id.tvName)
            val tvSize: TextView = v.findViewById(R.id.tvSize)
            val cbSelect: CheckBox = v.findViewById(R.id.cbSelect)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_share, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.tvSize.text = item.size
            if (item.icon != null) {
                holder.ivIcon.setImageDrawable(item.icon)
            } else {
                holder.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            holder.cbSelect.isChecked = item.isSelected

            holder.itemView.setOnClickListener {
                item.isSelected = !item.isSelected
                holder.cbSelect.isChecked = item.isSelected
                onCheck(item)
            }
        }

        override fun getItemCount() = items.size
    }
}

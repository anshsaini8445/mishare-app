package com.ansh.rapidshare

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WebShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_share)

        findViewById<TextView>(R.id.btnBackWeb).setOnClickListener { finish() }
    }
}

package com.example.keylink

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class TrackpadShortcutsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trackpad_shortcuts)
        
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
    }
}

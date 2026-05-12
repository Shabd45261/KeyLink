package com.example.keylink

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class CustomizeKeyboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customize_keyboard)

        findViewById<Button>(R.id.btnResize).setOnClickListener {
            val intent = Intent(this, KeyboardActivity::class.java)
            intent.putExtra("RESIZE_MODE", true)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnKeyColors).setOnClickListener {
            startActivity(Intent(this, KeyColorMenuActivity::class.java))
        }
    }
}

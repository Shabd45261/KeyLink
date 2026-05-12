package com.example.keylink

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
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

        findViewById<Button>(R.id.btnResetLayout).setOnClickListener {
            val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
            sharedPref.edit()
                .remove("kb_width_scale")
                .remove("kb_height_scale")
                .remove("kb_x_offset")
                .remove("kb_y_offset")
                .apply()
            Toast.makeText(this, "Layout reset to default", Toast.LENGTH_SHORT).show()
        }
    }
}

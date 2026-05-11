package com.example.keylink

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
        val cbVibration = findViewById<CheckBox>(R.id.cbVibration)
        val btnResize = findViewById<LinearLayout>(R.id.btnResizeKeyboard)

        cbVibration.isChecked = sharedPref.getBoolean("vibration_enabled", true)
        cbVibration.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("vibration_enabled", isChecked).apply()
        }

        btnResize.setOnClickListener {
            val intent = Intent(this, KeyboardActivity::class.java)
            intent.putExtra("RESIZE_MODE", true)
            startActivity(intent)
        }
    }
}

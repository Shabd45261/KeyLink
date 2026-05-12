package com.example.keylink

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class KeyColorMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_color_menu)

        findViewById<Button>(R.id.btnAllKeys).setOnClickListener {
            // Open popup for all keys
            showGlobalColorPopup()
        }

        findViewById<Button>(R.id.btnSpecificKeys).setOnClickListener {
            Toast.makeText(this, "Long-press any key in Keyboard to customize it", Toast.LENGTH_LONG).show()
            finish()
        }

        findViewById<Button>(R.id.btnResetDefaults).setOnClickListener {
            val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
            sharedPref.edit()
                .remove("kb_key_color")
                .remove("kb_key_alpha")
                .remove("kb_vibrate_all")
                // Specific keys reset would be harder if stored as JSON, but let's assume we clear all customization
                .remove("specific_key_configs")
                .apply()
            Toast.makeText(this, "Reset to defaults", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showGlobalColorPopup() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_global_color)
        
        val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
        val sbTrans = dialog.findViewById<android.widget.SeekBar>(R.id.sbTransparency)
        val cbVibrate = dialog.findViewById<android.widget.CheckBox>(R.id.cbVibrate)
        val grid = dialog.findViewById<android.widget.GridLayout>(R.id.colorGrid)
        
        sbTrans.progress = (sharedPref.getFloat("kb_key_alpha", 1.0f) * 100).toInt()
        cbVibrate.isChecked = sharedPref.getBoolean("kb_vibrate_all", true)
        
        var selectedColor = sharedPref.getInt("kb_key_color", android.graphics.Color.parseColor("#2A2A2A"))
        
        for (i in 0 until grid.childCount) {
            val child = grid.getChildAt(i)
            child.setOnClickListener {
                selectedColor = android.graphics.Color.parseColor(it.tag.toString())
                Toast.makeText(this, "Color selected", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<android.widget.Button>(R.id.btnReset).setOnClickListener {
            sharedPref.edit()
                .remove("kb_key_color")
                .remove("kb_key_alpha")
                .remove("kb_vibrate_all")
                .apply()
            dialog.dismiss()
        }
        
        dialog.findViewById<android.widget.Button>(R.id.btnApply).setOnClickListener {
            sharedPref.edit()
                .putInt("kb_key_color", selectedColor)
                .putFloat("kb_key_alpha", sbTrans.progress / 100f)
                .putBoolean("kb_vibrate_all", cbVibrate.isChecked)
                .apply()
            dialog.dismiss()
            Toast.makeText(this, "Global settings applied", Toast.LENGTH_SHORT).show()
        }
        
        dialog.show()
    }
}

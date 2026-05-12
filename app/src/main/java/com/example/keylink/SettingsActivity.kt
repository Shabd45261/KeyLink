package com.example.keylink

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvSelectedSound: TextView
    private lateinit var btnClearSound: Button
    private val PICK_AUDIO_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
        
        // General
        val cbVibration = findViewById<CheckBox>(R.id.cbVibration)
        cbVibration.isChecked = sharedPref.getBoolean("vibration_enabled", true)
        cbVibration.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("vibration_enabled", isChecked).apply()
        }
        
        val cbAutoResize = findViewById<CheckBox>(R.id.cbAutoResize)
        cbAutoResize.isChecked = sharedPref.getBoolean("kb_auto_resize", false)
        cbAutoResize.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("kb_auto_resize", isChecked).apply()
        }

        // Keyboard
        findViewById<Button>(R.id.btnCustomizeKeyboard).setOnClickListener {
            startActivity(Intent(this, CustomizeKeyboardActivity::class.java))
        }

        findViewById<Button>(R.id.btnSelectSound).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "audio/*"
            startActivityForResult(intent, PICK_AUDIO_REQUEST)
        }
        
        tvSelectedSound = findViewById(R.id.tvSelectedSound)
        val savedUri = sharedPref.getString("custom_sound_uri", null)
        updateSoundUI(savedUri)

        // Trackpad
        findViewById<Button>(R.id.btnTrackpadBg).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        findViewById<Button>(R.id.btnMouseButtonSettings).setOnClickListener {
            showMouseButtonSettings()
        }
    }

    private fun updateSoundUI(uriString: String?) {
        if (uriString != null) {
            tvSelectedSound.text = "Custom Sound Selected"
        } else {
            tvSelectedSound.text = "None"
        }
    }

    private val PICK_IMAGE_REQUEST = 1002

    private fun showMouseButtonSettings() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_mouse_btn_settings)
        val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
        
        val sbAlpha = dialog.findViewById<android.widget.SeekBar>(R.id.sbBtnAlpha)
        sbAlpha.progress = (sharedPref.getFloat("mouse_btn_alpha", 1.0f) * 100).toInt()
        
        dialog.findViewById<Button>(R.id.btnApplyMouse).setOnClickListener {
            sharedPref.edit()
                .putFloat("mouse_btn_alpha", sbAlpha.progress / 100f)
                .apply()
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_AUDIO_REQUEST -> {
                    data?.data?.let { uri ->
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
                        sharedPref.edit().putString("custom_sound_uri", uri.toString()).apply()
                        updateSoundUI(uri.toString())
                    }
                }
                PICK_IMAGE_REQUEST -> {
                    data?.data?.let { uri ->
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
                        sharedPref.edit().putString("trackpad_bg_uri", uri.toString()).apply()
                        Toast.makeText(this, "Trackpad background updated", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

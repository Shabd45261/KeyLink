package com.example.keylink

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class KeyboardActivity : AppCompatActivity() {

    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var pcIp: String? = null
    
    private var lastX = 0f
    private var lastY = 0f
    private var accumulatedDx = 0f
    private var accumulatedDy = 0f
    
    // Smooth Mouse Variables
    private var smoothDx = 0f
    private var smoothDy = 0f
    private val smoothingFactor = 0.25f // Lower = smoother, but more lag
    
    private var isCapsLock = false
    private lateinit var vibrator: Vibrator
    private var vibrationEnabled = true
    
    private var isResizeMode = false
    
    private val commandQueue = LinkedBlockingQueue<String>()

    private lateinit var statusDot: View
    private lateinit var tvStatus: TextView

    private var mediaPlayer: MediaPlayer? = null
    private var customSoundUri: String? = null
    
    private val keySettings = mutableMapOf<String, KeySetting>()

    data class KeySetting(
        val color: Int?,
        val alpha: Float?,
        val vibrate: Boolean?,
        val soundUri: String?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        
        // Fullscreen immersive sticky
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        
        setContentView(R.layout.activity_keyboard)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
        statusDot = findViewById(R.id.connectionStatusDot)
        tvStatus = findViewById(R.id.tvConnectionStatus)

        val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
        vibrationEnabled = sharedPref.getBoolean("vibration_enabled", true)
        customSoundUri = sharedPref.getString("custom_sound_uri", null)
        
        if (customSoundUri != null) {
            initMediaPlayer(customSoundUri!!)
        }
        
        isResizeMode = intent.getBooleanExtra("RESIZE_MODE", false)

        pcIp = intent.getStringExtra("PC_IP") ?: "192.168.1.100"
        
        connectToPc()
        startCommandSender()
        setupTrackpad()
        setupTrackpoint()
        
        val container = findViewById<KeyboardLayout>(R.id.keyboardContainer)
        
        val autoResizeEnabled = sharedPref.getBoolean("kb_auto_resize", false)
        if (autoResizeEnabled) {
            // Auto-resize: Scale to fill more screen but keep centered
            container.widthScale = 1.05f 
            container.heightScale = 1.1f
            container.xOffset = 0f
            container.yOffset = 0f
        } else {
            container.widthScale = sharedPref.getFloat("kb_width_scale", 1.0f)
            container.heightScale = sharedPref.getFloat("kb_height_scale", 1.0f)
            container.xOffset = sharedPref.getFloat("kb_x_offset", 0f)
            container.yOffset = sharedPref.getFloat("kb_y_offset", 0f)
        }

        loadKeySettings()
        setupAllKeys(container)

        if (isResizeMode) {
            setupResizeLogic(container)
        } else {
            findViewById<View>(R.id.resizeOverlay).visibility = View.GONE
            findViewById<View>(R.id.btnSaveResize).visibility = View.GONE
        }

        val btnSwitchTrackpad = findViewById<ImageButton>(R.id.btnSwitchTrackpad)
        val btnSwitchGamepad = findViewById<ImageButton>(R.id.btnSwitchGamepad)

        btnSwitchTrackpad.setOnClickListener {
            val intent = Intent(this, KeypadActivity::class.java)
            intent.putExtra("PC_IP", pcIp)
            startActivity(intent)
        }

        btnSwitchGamepad.setOnClickListener {
            val intent = Intent(this, GamepadActivity::class.java)
            intent.putExtra("PC_IP", pcIp)
            startActivity(intent)
        }
    }

    private fun startCommandSender() {
        thread {
            while (!isFinishing) {
                try {
                    val command = commandQueue.take()
                    out?.println(command)
                    out?.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupResizeLogic(container: KeyboardLayout) {
        val overlay = findViewById<ResizeOverlayView>(R.id.resizeOverlay)
        val btnSave = findViewById<Button>(R.id.btnSaveResize)

        overlay.visibility = View.VISIBLE
        btnSave.visibility = View.VISIBLE
        
        container.setBackgroundColor(0x33448AFF)
        overlay.setTarget(container) {
            // Callback when resized if needed
        }

        btnSave.setOnClickListener {
            // Save settings
            getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE).edit()
                .putFloat("kb_width_scale", container.widthScale)
                .putFloat("kb_height_scale", container.heightScale)
                .putFloat("kb_x_offset", container.xOffset)
                .putFloat("kb_y_offset", container.yOffset)
                .apply()
            
            Toast.makeText(this, "Layout Saved", Toast.LENGTH_SHORT).show()
            
            // Exit resize mode and go back or refresh
            val intent = Intent(this, CustomizeKeyboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }

        // Disable normal key touches in resize mode
        container.setOnTouchListener { _, _ -> true }
    }
    
    private fun initMediaPlayer(uriString: String) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@KeyboardActivity, Uri.parse(uriString))
                prepare()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playCustomSound() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.pause()
                    it.seekTo(0)
                }
                it.start()
            } catch (e: Exception) {
                // If something went wrong, try re-init once
                mediaPlayer = null
                customSoundUri?.let { uri -> initMediaPlayer(uri) }
                mediaPlayer?.start()
            }
        }
    }

    private fun loadKeySettings() {
        val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
        val json = sharedPref.getString("specific_key_settings", null)
        if (json != null) {
            try {
                val obj = org.json.JSONObject(json)
                obj.keys().forEach { key ->
                    val s = obj.getJSONObject(key)
                    keySettings[key] = KeySetting(
                        if (s.has("color")) s.getInt("color") else null,
                        if (s.has("alpha")) s.getDouble("alpha").toFloat() else null,
                        if (s.has("vibrate")) s.getBoolean("vibrate") else null,
                        if (s.has("soundUri")) s.getString("soundUri") else null
                    )
                }
            } catch (e: Exception) {}
        }
    }

    private fun setupAllKeys(view: View) {
        if (view is Button) {
            val keyText = view.text.toString()
            val setting = keySettings[keyText]
            
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            var repeatRunnable: Runnable? = null

            // Apply colors
            val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
            val globalColor = sharedPref.getInt("kb_key_color", Color.parseColor("#2A2A2A"))
            val globalAlpha = sharedPref.getFloat("kb_key_alpha", 1.0f)
            
            val finalColor = setting?.color ?: globalColor
            val finalAlpha = setting?.alpha ?: globalAlpha
            
            // Set background color with alpha to keep text visible
            val alphaInt = (finalAlpha * 255).toInt()
            val colorWithAlpha = Color.argb(alphaInt, Color.red(finalColor), Color.green(finalColor), Color.blue(finalColor))
            
            view.setBackgroundColor(colorWithAlpha)
            // If it's a MaterialButton, we might need to use backgroundTintList
            if (view is com.google.android.material.button.MaterialButton) {
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(finalColor).withAlpha(alphaInt)
                view.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.BLACK))
                view.setStrokeWidth(1)
            }

            view.setOnTouchListener { v, event ->
                val action = event.actionMasked
                
                when (action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        val vibrate = setting?.vibrate ?: vibrationEnabled
                        if (vibrate) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                vibrator.vibrate(20)
                            }
                        }
                        
                        if (setting?.soundUri != null) {
                            playSpecificSound(setting.soundUri)
                        } else {
                            playCustomSound()
                        }
                        sendKeyCommand("DOWN", keyText)

                        // Start auto-repeat for non-modifier keys
                        if (!isModifier(keyText)) {
                            repeatRunnable?.let { handler.removeCallbacks(it) }
                            repeatRunnable = object : Runnable {
                                override fun run() {
                                    sendKeyCommand("DOWN", keyText)
                                    handler.postDelayed(this, 50)
                                }
                            }
                            handler.postDelayed(repeatRunnable!!, 400) // Initial delay
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        sendKeyCommand("UP", keyText)
                        repeatRunnable?.let { handler.removeCallbacks(it) }
                        repeatRunnable = null
                    }
                }
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                    v.performClick()
                }
                true
            }

            view.setOnLongClickListener {
                showSpecificKeyPopup(keyText, view)
                true
            }

        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setupAllKeys(view.getChildAt(i))
            }
        }
    }

    private fun playSpecificSound(uri: String) {
        try {
            val mp = MediaPlayer()
            mp.setDataSource(this, Uri.parse(uri))
            mp.prepare()
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Exception) {}
    }

    private fun showSpecificKeyPopup(keyText: String, view: Button) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_global_color)
        
        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        tvTitle.text = "Customize Key: $keyText"
        
        val sbTransparency = dialog.findViewById<android.widget.SeekBar>(R.id.sbTransparency)
        val cbVibrate = dialog.findViewById<android.widget.CheckBox>(R.id.cbVibrate)
        val btnSound = dialog.findViewById<Button>(R.id.btnSelectKeySound)
        val grid = dialog.findViewById<android.widget.GridLayout>(R.id.colorGrid)
        
        btnSound.visibility = View.VISIBLE
        
        val currentSetting = keySettings[keyText]
        sbTransparency.progress = ((currentSetting?.alpha ?: view.alpha) * 100).toInt()
        cbVibrate.isChecked = currentSetting?.vibrate ?: vibrationEnabled
        
        var selectedColor = currentSetting?.color ?: Color.parseColor("#2A2A2A")
        
        for (i in 0 until grid.childCount) {
            val child = grid.getChildAt(i)
            child.setOnClickListener {
                selectedColor = Color.parseColor(it.tag.toString())
                Toast.makeText(this, "Color selected", Toast.LENGTH_SHORT).show()
            }
        }

        btnSound.setOnClickListener {
            // In a real app, I'd open a file picker. For now, let's just show a message.
            Toast.makeText(this, "Sound picker coming soon", Toast.LENGTH_SHORT).show()
        }

        dialog.findViewById<Button>(R.id.btnReset).setOnClickListener {
            keySettings.remove(keyText)
            saveKeySettings()
            view.setBackgroundColor(getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE).getInt("kb_key_color", Color.parseColor("#2A2A2A")))
            view.alpha = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE).getFloat("kb_key_alpha", 1.0f)
            dialog.dismiss()
        }
        
        dialog.findViewById<Button>(R.id.btnApply).setOnClickListener {
            val alpha = sbTransparency.progress / 100f
            val vibrate = cbVibrate.isChecked
            
            val newSetting = KeySetting(selectedColor, alpha, vibrate, currentSetting?.soundUri)
            keySettings[keyText] = newSetting
            saveKeySettings()
            
            val alphaInt = (alpha * 255).toInt()
            if (view is com.google.android.material.button.MaterialButton) {
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(selectedColor).withAlpha(alphaInt)
                view.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.BLACK))
                view.setStrokeWidth(1)
            } else {
                val colorWithAlpha = Color.argb(alphaInt, Color.red(selectedColor), Color.green(selectedColor), Color.blue(selectedColor))
                view.setBackgroundColor(colorWithAlpha)
            }
            
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun saveKeySettings() {
        val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
        val json = org.json.JSONObject()
        keySettings.forEach { (key, setting) ->
            val s = org.json.JSONObject()
            setting.color?.let { s.put("color", it) }
            setting.alpha?.let { s.put("alpha", it.toDouble()) }
            setting.vibrate?.let { s.put("vibrate", it) }
            setting.soundUri?.let { s.put("soundUri", it) }
            json.put(key, s)
        }
        sharedPref.edit().putString("specific_key_settings", json.toString()).apply()
    }

    private fun isModifier(keyText: String): Boolean {
        val k = keyText.lowercase()
        return k == "ctrl" || k == "alt" || k == "shift" || k == "win" || k == "caps" || k == "fn"
    }

    private fun sendKeyCommand(action: String, keyText: String) {
        var key = keyText.trim().lowercase()
        
        // Fix Mappings using Unicode for arrow symbols
        key = when (key) {
            "\u2191", "up" -> "up"
            "\u2193", "down" -> "down"
            "\u2190", "left" -> "left"
            "\u2192", "right" -> "right"
            "pgup" -> "pageup"
            "pgdn" -> "pagedown"
            "bksp" -> "backspace"
            "backspace" -> "backspace"
            "caps" -> {
                if (action == "DOWN") {
                    isCapsLock = !isCapsLock
                    val dotId = resources.getIdentifier("caps_status_dot", "id", packageName)
                    if (dotId != 0) {
                        findViewById<View>(dotId)?.visibility = if (isCapsLock) View.VISIBLE else View.GONE
                    }
                }
                "capslock"
            }
            "enter" -> "enter"
            "esc" -> "esc"
            "prtsc" -> "printscreen"
            "ins" -> "insert"
            "del" -> "delete"
            " " -> "space"
            else -> if (key.isEmpty()) "space" else key
        }
        
        sendCommand("$action:$key")
    }

    private fun setupTrackpad() {
        val trackpad = findViewById<View>(R.id.trackpadView)
        trackpad.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    smoothDx = 0f
                    smoothDy = 0f
                    v.performClick()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val rawDx = event.x - lastX
                    val rawDy = event.y - lastY
                    
                    // Simple Exponential Smoothing
                    smoothDx = smoothDx + smoothingFactor * (rawDx - smoothDx)
                    smoothDy = smoothDy + smoothingFactor * (rawDy - smoothDy)
                    
                    accumulatedDx += smoothDx
                    accumulatedDy += smoothDy
                    
                    val sendX = (accumulatedDx * 1.5f).toInt() // Sensitivity boost
                    val sendY = (accumulatedDy * 1.5f).toInt()
                    
                    if (sendX != 0 || sendY != 0) {
                        sendCommand("MOUSE:$sendX,$sendY")
                        accumulatedDx -= sendX / 1.5f
                        accumulatedDy -= sendY / 1.5f
                    }
                    lastX = event.x
                    lastY = event.y
                    true
                }
                else -> true
            }
        }
    }

    private fun setupTrackpoint() {
        val trackpoint = findViewById<View>(R.id.trackpoint)
        trackpoint.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val centerX = v.width / 2f
                    val centerY = v.height / 2f
                    
                    val rawDx = (event.x - centerX) / 6f
                    val rawDy = (event.y - centerY) / 6f

                    smoothDx = smoothDx + smoothingFactor * (rawDx - smoothDx)
                    smoothDy = smoothDy + smoothingFactor * (rawDy - smoothDy)
                    
                    accumulatedDx += smoothDx
                    accumulatedDy += smoothDy
                    
                    val sendX = accumulatedDx.toInt()
                    val sendY = accumulatedDy.toInt()
                    
                    if (sendX != 0 || sendY != 0) {
                        sendCommand("MOUSE:$sendX,$sendY")
                        accumulatedDx -= sendX
                        accumulatedDy -= sendY
                    }
                    true
                }
                MotionEvent.ACTION_DOWN -> {
                    smoothDx = 0f
                    smoothDy = 0f
                    v.performClick()
                    true
                }
                else -> true
            }
        }
    }

    private fun connectToPc() {
        thread {
            try {
                socket = Socket()
                socket?.connect(InetSocketAddress(pcIp, 9999), 3000)
                out = PrintWriter(socket!!.getOutputStream(), true)
                sendCommand("CONNECTED:Android")
                
                runOnUiThread {
                    statusDot.setBackgroundColor(0xFF4CAF50.toInt()) // Green
                    tvStatus.text = "Connected to $pcIp"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    statusDot.setBackgroundColor(0xFFFF5252.toInt()) // Red
                    tvStatus.text = "Connection Failed"
                    val errorMsg = e.localizedMessage ?: "Unknown Error"
                    Toast.makeText(this, "Failed to connect to $pcIp: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendCommand(command: String) {
        commandQueue.offer(command)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        thread { try { socket?.close() } catch (e: Exception) {} }
    }
}

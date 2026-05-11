package com.example.keylink

import android.content.Context
import android.content.Intent
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
    
    private var isCapsLock = false
    private lateinit var vibrator: Vibrator
    private var vibrationEnabled = true
    
    private var isResizeMode = false
    
    private val commandQueue = LinkedBlockingQueue<String>()

    private lateinit var statusDot: View
    private lateinit var tvStatus: TextView

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
        
        isResizeMode = intent.getBooleanExtra("RESIZE_MODE", false)

        pcIp = intent.getStringExtra("PC_IP") ?: "192.168.1.100"
        
        connectToPc()
        startCommandSender()
        setupTrackpad()
        setupTrackpoint()
        
        val container = findViewById<KeyboardLayout>(R.id.keyboardContainer)
        container.widthScale = sharedPref.getFloat("kb_width_scale", 1.0f)
        container.heightScale = sharedPref.getFloat("kb_height_scale", 1.0f)
        container.xOffset = sharedPref.getFloat("kb_x_offset", 0f)
        container.yOffset = sharedPref.getFloat("kb_y_offset", 0f)

        setupAllKeys(container)

        if (isResizeMode) {
            setupResizeLogic(container)
        }

        val btnLeftTrackpad = findViewById<ImageButton>(R.id.btnLeftTrackpad)
        val btnRightTrackpad = findViewById<ImageButton>(R.id.btnRightTrackpad)

        val openKeypad = View.OnClickListener {
            val intent = Intent(this, KeypadActivity::class.java)
            intent.putExtra("PC_IP", pcIp)
            startActivity(intent)
        }

        btnLeftTrackpad.setOnClickListener(openKeypad)
        btnRightTrackpad.setOnClickListener(openKeypad)
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
        container.setBackgroundColor(0x33448AFF)
        
        var startX1 = 0f
        var startY1 = 0f
        var startX2 = 0f
        var startY2 = 0f
        var baseWidthScale = container.widthScale
        var baseHeightScale = container.heightScale
        var baseXOffset = container.xOffset
        var baseYOffset = container.yOffset

        container.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX1 = event.x
                    startY1 = event.y
                    baseXOffset = container.xOffset
                    baseYOffset = container.yOffset
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    startX1 = event.getX(0)
                    startY1 = event.getY(0)
                    startX2 = event.getX(1)
                    startY2 = event.getY(1)
                    baseWidthScale = container.widthScale
                    baseHeightScale = container.heightScale
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        // Move
                        container.xOffset = baseXOffset + (event.x - startX1)
                        container.yOffset = baseYOffset + (event.y - startY1)
                    } else if (event.pointerCount == 2) {
                        // Resize (PowerPoint style)
                        val dx = Math.abs(event.getX(0) - event.getX(1))
                        val dy = Math.abs(event.getY(0) - event.getY(1))
                        val startDx = Math.abs(startX1 - startX2)
                        val startDy = Math.abs(startY1 - startY2)
                        
                        if (startDx > 10) container.widthScale = baseWidthScale * (dx / startDx)
                        if (startDy > 10) container.heightScale = baseHeightScale * (dy / startDy)
                    }
                    container.requestLayout()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    // Save settings
                    getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE).edit()
                        .putFloat("kb_width_scale", container.widthScale)
                        .putFloat("kb_height_scale", container.heightScale)
                        .putFloat("kb_x_offset", container.xOffset)
                        .putFloat("kb_y_offset", container.yOffset)
                        .apply()
                }
            }
            v.performClick()
            true
        }
    }
    
    private fun setupAllKeys(view: View) {
        if (view is Button) {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            var repeatRunnable: Runnable? = null

            view.setOnTouchListener { v, event ->
                val keyText = (v as Button).text.toString()
                val action = event.actionMasked
                
                when (action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        if (vibrationEnabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                vibrator.vibrate(20)
                            }
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
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setupAllKeys(view.getChildAt(i))
            }
        }
    }

    private fun isModifier(keyText: String): Boolean {
        val k = keyText.lowercase()
        return k == "ctrl" || k == "alt" || k == "shift" || k == "win" || k == "caps" || k == "fn"
    }

    private fun sendKeyCommand(action: String, keyText: String) {
        var key = keyText.lowercase()
        
        // Fix Mappings
        key = when (key) {
            "↑" -> "up"
            "↓" -> "down"
            "←" -> "left"
            "→" -> "right"
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
                    v.performClick()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.x - lastX).toInt()
                    val dy = (event.y - lastY).toInt()
                    if (dx != 0 || dy != 0) {
                        sendCommand("MOUSE:$dx,$dy")
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
                    val dx = ((event.x - centerX) / 4).toInt()
                    val dy = ((event.y - centerY) / 4).toInt()
                    if (dx != 0 || dy != 0) {
                        sendCommand("MOUSE:$dx,$dy")
                    }
                    true
                }
                MotionEvent.ACTION_DOWN -> {
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
                    Toast.makeText(this, "Failed to connect to $pcIp", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendCommand(command: String) {
        commandQueue.offer(command)
    }

    override fun onDestroy() {
        super.onDestroy()
        thread { try { socket?.close() } catch (e: Exception) {} }
    }
}

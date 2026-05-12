package com.example.keylink

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.math.abs

class KeypadActivity : AppCompatActivity() {

    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var pcIp: String? = null
    
    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false
    
    // Multi-finger tracking
    private var startY3 = 0f
    private var startX3 = 0f
    private var startY4 = 0f
    private var startX4 = 0f
    private var gestureThreshold = 100f
    private var gesturePerformed = false

    private val commandQueue = LinkedBlockingQueue<String>()
    private lateinit var scaleDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        pcIp = intent.getStringExtra("PC_IP") ?: "192.168.1.100"
        setContentView(R.layout.activity_keypad)
        
        connectToPc()
        startCommandSender()
        setupTrackpad()
        setupMouseButtons()
        setupProfileSwitches()
    }

    private fun setupProfileSwitches() {
        findViewById<ImageButton>(R.id.btnFlipOrientation).setOnClickListener {
            requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        findViewById<ImageButton>(R.id.btnSwitchKeyboard).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnSwitchGamepad).setOnClickListener {
            val intent = Intent(this, GamepadActivity::class.java)
            intent.putExtra("PC_IP", pcIp)
            startActivity(intent)
            finish()
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

    private fun connectToPc() {
        thread {
            try {
                socket = Socket()
                socket?.connect(InetSocketAddress(pcIp, 9999), 3000)
                out = PrintWriter(socket!!.getOutputStream(), true)
                sendCommand("CONNECTED:Android-Keypad")
                runOnUiThread {
                    Toast.makeText(this, "Connected to $pcIp", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to connect to $pcIp", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupTrackpad() {
        val trackpad = findViewById<View>(R.id.largeTrackpad)
        val ivBg = findViewById<android.widget.ImageView>(R.id.ivTrackpadBg)
        
        val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
        val bgUri = sharedPref.getString("trackpad_bg_uri", null)
        if (bgUri != null) {
            try {
                ivBg.setImageURI(Uri.parse(bgUri))
            } catch (e: Exception) {}
        }
        
        val lBtn = findViewById<Button>(R.id.btnLeftClick)
        val rBtn = findViewById<Button>(R.id.btnRightClick)
        val btnColor = sharedPref.getInt("mouse_btn_color", Color.parseColor("#BB86FC"))
        val btnAlpha = sharedPref.getFloat("mouse_btn_alpha", 1.0f)
        
        lBtn.setBackgroundColor(btnColor)
        lBtn.alpha = btnAlpha
        rBtn.setBackgroundColor(btnColor)
        rBtn.alpha = btnAlpha
        
        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                if (abs(scaleFactor - 1.0) > 0.01) {
                    sendCommand("ZOOM:$scaleFactor")
                    gesturePerformed = true
                }
                return true
            }
        })

        trackpad.setOnTouchListener { v, event ->
            scaleDetector.onTouchEvent(event)
            
            val action = event.actionMasked
            val pointerCount = event.pointerCount

            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    gesturePerformed = false
                    isDragging = false
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (pointerCount == 3) {
                        startX3 = event.getX(0)
                        startY3 = event.getY(0)
                    } else if (pointerCount == 4) {
                        startX4 = event.getX(0)
                        startY4 = event.getY(0)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (pointerCount == 1) {
                        val dx = (event.x - lastX).toInt()
                        val dy = (event.y - lastY).toInt()
                        if (dx != 0 || dy != 0) {
                            if (isDragging) {
                                sendCommand("DRAG:$dx,$dy")
                            } else {
                                sendCommand("MOUSE:$dx,$dy")
                            }
                        }
                    } else if (pointerCount == 2 && !scaleDetector.isInProgress) {
                        val dx = (event.getX(0) - lastX).toInt()
                        val dy = (event.getY(0) - lastY).toInt()
                        if (abs(dx) > 2 || abs(dy) > 2) {
                            sendCommand("SCROLL:$dx,$dy")
                            gesturePerformed = true
                        }
                    } else if (pointerCount == 3 && !gesturePerformed) {
                        val dx = event.getX(0) - startX3
                        val dy = event.getY(0) - startY3
                        if (abs(dy) > gestureThreshold) {
                            if (dy < 0) sendCommand("GESTURE:3_UP") else sendCommand("GESTURE:3_DOWN")
                            gesturePerformed = true
                        } else if (abs(dx) > gestureThreshold) {
                            if (dx < 0) sendCommand("GESTURE:3_LEFT") else sendCommand("GESTURE:3_RIGHT")
                            gesturePerformed = true
                        }
                    } else if (pointerCount == 4 && !gesturePerformed) {
                        val dx = event.getX(0) - startX4
                        val dy = event.getY(0) - startY4
                        if (abs(dx) > gestureThreshold) {
                            if (dx < 0) sendCommand("GESTURE:4_LEFT") else sendCommand("GESTURE:4_RIGHT")
                            gesturePerformed = true
                        } else if (abs(dy) > gestureThreshold) {
                            if (dy < 0) sendCommand("GESTURE:4_UP") else sendCommand("GESTURE:4_DOWN")
                            gesturePerformed = true
                        }
                    }
                    lastX = event.getX(0)
                    lastY = event.getY(0)
                }
                MotionEvent.ACTION_UP -> {
                    val duration = event.eventTime - event.downTime
                    if (pointerCount == 1 && !gesturePerformed && duration < 200) {
                        val dist = abs(event.x - lastX) + abs(event.y - lastY)
                        if (dist < 20) sendCommand("CLICK:left")
                    }
                    if (isDragging) {
                        sendCommand("DRAG_RELEASE")
                        isDragging = false
                    }
                    v.performClick()
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (pointerCount == 2 && !gesturePerformed) {
                        sendCommand("CLICK:right")
                        gesturePerformed = true
                    } else if (pointerCount == 3 && !gesturePerformed) {
                        sendCommand("CLICK:middle")
                        gesturePerformed = true
                    } else if (pointerCount == 4 && !gesturePerformed) {
                        sendCommand("GESTURE:4_TAP")
                        gesturePerformed = true
                    }
                }
            }
            true
        }
        
        // Long press for drag
        trackpad.setOnLongClickListener {
            isDragging = true
            sendCommand("DRAG_START")
            true
        }
    }

    private fun setupMouseButtons() {
        findViewById<Button>(R.id.btnLeftClick).setOnClickListener {
            sendCommand("CLICK:left")
        }
        findViewById<Button>(R.id.btnRightClick).setOnClickListener {
            sendCommand("CLICK:right")
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

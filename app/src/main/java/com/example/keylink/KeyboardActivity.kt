package com.example.keylink

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

class KeyboardActivity : AppCompatActivity() {

    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var pcIp: String? = null
    
    private var lastX = 0f
    private var lastY = 0f

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

        pcIp = intent.getStringExtra("PC_IP") ?: "192.168.1.100"
        
        connectToPc()
        setupTrackpad()
        setupTrackpoint()
        
        val container = findViewById<ViewGroup>(R.id.keyboardContainer)
        setupAllKeys(container)

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

    private fun setupAllKeys(view: View) {
        if (view is Button) {
            view.setOnClickListener {
                var key = view.text.toString().lowercase()
                key = when (key) {
                    "↑" -> "up"
                    "↓" -> "down"
                    "←" -> "left"
                    "→" -> "right"
                    "pgup" -> "pgup"
                    "pgdn" -> "pgdn"
                    "bksp" -> "bksp"
                    else -> key
                }
                sendCommand("KEY:$key")
            }
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setupAllKeys(view.getChildAt(i))
            }
        }
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
                socket = Socket(pcIp, 9999)
                out = PrintWriter(socket!!.getOutputStream(), true)
                sendCommand("CONNECTED:Android")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendCommand(command: String) {
        thread {
            try {
                out?.println(command)
                out?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        thread { try { socket?.close() } catch (e: Exception) {} }
    }
}

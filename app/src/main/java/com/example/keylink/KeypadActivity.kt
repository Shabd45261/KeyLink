package com.example.keylink

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

class KeypadActivity : AppCompatActivity() {

    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var pcIp: String? = null
    
    private var lastX = 0f
    private var lastY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pcIp = intent.getStringExtra("PC_IP") ?: "192.168.1.100"
        setContentView(R.layout.activity_keypad)
        
        connectToPc()
        setupTrackpad()
        setupKeypad()
        setupMouseButtons()
    }

    private fun connectToPc() {
        thread {
            try {
                socket = Socket(pcIp, 9999)
                out = PrintWriter(socket!!.getOutputStream(), true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupTrackpad() {
        val trackpad = findViewById<View>(R.id.largeTrackpad)
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

    private fun setupMouseButtons() {
        findViewById<Button>(R.id.btnLeftClick).setOnClickListener { sendCommand("CLICK:left") }
        findViewById<Button>(R.id.btnRightClick).setOnClickListener { sendCommand("CLICK:right") }
    }

    private fun setupKeypad() {
        val numIds = intArrayOf(
            R.id.num_0, R.id.num_1, R.id.num_2, R.id.num_3, R.id.num_4,
            R.id.num_5, R.id.num_6, R.id.num_7, R.id.num_8, R.id.num_9,
            R.id.num_dot, R.id.num_enter
        )

        for (id in numIds) {
            findViewById<Button>(id).setOnClickListener { view ->
                var key = (view as Button).text.toString().lowercase()
                if (key == "enter") key = "enter"
                sendCommand("KEY:$key")
            }
        }
    }

    private fun sendCommand(command: String) {
        thread {
            try {
                out?.println(command)
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

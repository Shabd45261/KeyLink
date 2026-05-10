package com.example.keylink

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

import android.view.MotionEvent
import android.widget.Button

class KeyboardActivity : AppCompatActivity() {

    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var pcIp: String? = null
    
    private var lastX = 0f
    private var lastY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        pcIp = intent.getStringExtra("PC_IP") ?: "192.168.1.100"
        
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        
        setContentView(R.layout.activity_keyboard)

        connectToPc()
        setupTrackpad()
        setupKeyboard()

        val btnLeftKeypad = findViewById<ImageButton>(R.id.btnLeftKeypad)
        val btnRightKeypad = findViewById<ImageButton>(R.id.btnRightKeypad)

        val openKeypad = View.OnClickListener {
            val intent = Intent(this, KeypadActivity::class.java)
            intent.putExtra("PC_IP", pcIp)
            startActivity(intent)
        }

        btnLeftKeypad.setOnClickListener(openKeypad)
        btnRightKeypad.setOnClickListener(openKeypad)
    }

    private fun setupTrackpad() {
        val trackpad = findViewById<View>(R.id.trackpadView)
        trackpad.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
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
                MotionEvent.ACTION_UP -> {
                    // Could implement click on tap here
                    true
                }
                else -> false
            }
        }
    }

    private fun setupKeyboard() {
        val keyIds = intArrayOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t, R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g, R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b, R.id.key_n, R.id.key_m,
            R.id.key_space, R.id.key_backspace, R.id.key_enter, R.id.key_ctrl, R.id.key_alt, R.id.key_shift
        )

        for (id in keyIds) {
            findViewById<Button>(id).setOnClickListener { view ->
                val keyText = (view as Button).text.toString().lowercase()
                sendCommand("KEY:$keyText")
            }
        }
    }

    private fun connectToPc() {
        thread {
            try {
                socket = Socket(pcIp, 9999)
                out = PrintWriter(socket!!.getOutputStream(), true)
                sendCommand("CONNECTED:Android Client")
            } catch (e: Exception) {
                e.printStackTrace()
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
        thread {
            try {
                socket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

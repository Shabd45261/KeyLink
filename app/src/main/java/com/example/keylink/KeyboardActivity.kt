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

class KeyboardActivity : AppCompatActivity() {

    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var pcIp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        pcIp = intent.getStringExtra("PC_IP") ?: "192.168.1.100" // Default for testing
        
        // Fullscreen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        
        setContentView(R.layout.activity_keyboard)

        connectToPc()

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

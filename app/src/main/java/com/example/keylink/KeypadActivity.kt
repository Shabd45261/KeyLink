package com.example.keylink

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.widget.Button
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

class KeypadActivity : AppCompatActivity() {

    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var pcIp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pcIp = intent.getStringExtra("PC_IP") ?: "192.168.1.100"
        setContentView(R.layout.activity_keypad)
        
        connectToPc()
        setupKeypad()
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

    private fun setupKeypad() {
        val numIds = intArrayOf(
            R.id.num_0, R.id.num_1, R.id.num_2, R.id.num_3, R.id.num_4,
            R.id.num_5, R.id.num_6, R.id.num_7, R.id.num_8, R.id.num_9,
            R.id.num_dot, R.id.num_enter
        )

        for (id in numIds) {
            findViewById<Button>(id).setOnClickListener { view ->
                val keyText = (view as Button).text.toString().lowercase()
                sendCommand("KEY:$keyText")
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
        thread { socket?.close() }
    }
}

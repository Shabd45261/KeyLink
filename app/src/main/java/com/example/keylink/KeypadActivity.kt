package com.example.keylink

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class KeypadActivity : AppCompatActivity() {

    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var pcIp: String? = null
    
    private var lastX = 0f
    private var lastY = 0f

    private val commandQueue = LinkedBlockingQueue<String>()

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
        setupKeypad()
        setupMouseButtons()
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
        findViewById<Button>(R.id.btnLeftClick).setOnClickListener {
            sendCommand("CLICK:left")
        }
        findViewById<Button>(R.id.btnRightClick).setOnClickListener {
            sendCommand("CLICK:right")
        }
    }

    private fun setupKeypad() {
        val numIds = intArrayOf(
            R.id.num_0, R.id.num_1, R.id.num_2, R.id.num_3, R.id.num_4,
            R.id.num_5, R.id.num_6, R.id.num_7, R.id.num_8, R.id.num_9,
            R.id.num_dot, R.id.num_enter
        )

        for (id in numIds) {
            val button = findViewById<Button>(id)
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            var repeatRunnable: Runnable? = null
            
            button.setOnTouchListener { v, event ->
                val keyText = (v as Button).text.toString().lowercase()
                val action = event.actionMasked
                
                when (action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        sendCommand("DOWN:$keyText")
                        
                        // Auto-repeat for numbers and dot
                        if (keyText != "enter") {
                            repeatRunnable?.let { handler.removeCallbacks(it) }
                            repeatRunnable = object : Runnable {
                                override fun run() {
                                    sendCommand("DOWN:$keyText")
                                    handler.postDelayed(this, 50)
                                }
                            }
                            handler.postDelayed(repeatRunnable!!, 400)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        sendCommand("UP:$keyText")
                        repeatRunnable?.let { handler.removeCallbacks(it) }
                        repeatRunnable = null
                    }
                }
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                    v.performClick()
                }
                true
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

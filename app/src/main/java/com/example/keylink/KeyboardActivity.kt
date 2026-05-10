package com.example.keylink

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class KeyboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        
        setContentView(R.layout.activity_keyboard)

        val btnLeftKeypad = findViewById<ImageButton>(R.id.btnLeftKeypad)
        val btnRightKeypad = findViewById<ImageButton>(R.id.btnRightKeypad)

        val openKeypad = View.OnClickListener {
            val intent = Intent(this, KeypadActivity::class.java)
            startActivity(intent)
        }

        btnLeftKeypad.setOnClickListener(openKeypad)
        btnRightKeypad.setOnClickListener(openKeypad)
    }
}

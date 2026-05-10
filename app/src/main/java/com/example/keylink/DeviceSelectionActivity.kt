package com.example.keylink

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class DeviceSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_selection)

        val deviceListView = findViewById<ListView>(R.id.deviceListView)
        
        // Mock data for devices
        val devices = listOf("Desktop-PC (Online)", "Laptop-Work (Offline)", "Media-Center (Online)")
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devices)
        deviceListView.adapter = adapter

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = devices[position]
            if (selectedDevice.contains("Online")) {
                val intent = Intent(this, KeyboardActivity::class.java)
                startActivity(intent)
            }
        }
    }
}

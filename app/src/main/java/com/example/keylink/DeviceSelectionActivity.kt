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
        
        // In a real app, we'd scan the network. For now, let's add a way to input an IP.
        val devices = mutableListOf("Add New Device", "Desktop-PC (Online)", "Laptop-Work (Offline)")
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devices)
        deviceListView.adapter = adapter

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                showConnectDialog()
            } else {
                val selectedDevice = devices[position]
                if (selectedDevice.contains("Online")) {
                    startActivity(Intent(this, KeyboardActivity::class.java))
                }
            }
        }
    }

    private fun showConnectDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Connect to PC")
        val input = android.widget.EditText(this)
        input.hint = "Enter PC IP Address (e.g. 192.168.1.5)"
        builder.setView(input)
        builder.setPositiveButton("Connect") { _, _ ->
            val ip = input.text.toString()
            val intent = Intent(this, KeyboardActivity::class.java)
            intent.putExtra("PC_IP", ip)
            startActivity(intent)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }
}

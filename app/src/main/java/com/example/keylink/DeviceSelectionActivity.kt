package com.example.keylink

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DeviceSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_selection)

        val recyclerView = findViewById<RecyclerView>(R.id.deviceRecyclerView)
        val tvTotalCount = findViewById<TextView>(R.id.tvTotalCount)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)

        val devices = listOf(
            Device("John's Workstation", "Seen 1m ago", "192.168.1.101", DeviceType.MONITOR, true),
            Device("MacBook Pro M3", "Seen 12m ago", "192.168.1.102", DeviceType.LAPTOP, true),
            Device("Office-Laptop-D2", "Last connected May 8, 2026", "192.168.1.103", DeviceType.LAPTOP, false),
            Device("Gaming-Rig-Home", "Seen 30s ago", "192.168.1.104", DeviceType.MONITOR, true),
            Device("Samsung Galaxy S24", "Last connected 2 days ago", "192.168.1.105", DeviceType.PHONE, false),
            Device("Studio Monitor PC", "Last connected June 12, 2025", "192.168.1.106", DeviceType.MONITOR, false)
        )

        tvTotalCount.text = "${devices.size} Total"

        val adapter = DeviceAdapter(devices) { device ->
            if (device.isOnline) {
                val intent = Intent(this, KeyboardActivity::class.java)
                intent.putExtra("PC_IP", device.ip)
                startActivity(intent)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            showConnectDialog()
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

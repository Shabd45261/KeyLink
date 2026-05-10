package com.example.keylink

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject

class DeviceSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvTotalCount: TextView
    private val deviceList = mutableListOf<Device>()
    private lateinit var adapter: DeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_selection)

        recyclerView = findViewById(R.id.deviceRecyclerView)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        val ivSettings = findViewById<ImageView>(R.id.ivSettings)

        loadDevices()

        adapter = DeviceAdapter(deviceList, 
            onClick = { device ->
                val intent = Intent(this, KeyboardActivity::class.java)
                intent.putExtra("PC_IP", device.ip)
                startActivity(intent)
            },
            onLongClick = { device ->
                showDeleteDialog(device)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener { showConnectDialog() }
        
        ivSettings.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Settings")
                .setItems(arrayOf("Clear All Connections", "About KeyLink")) { _, which ->
                    when (which) {
                        0 -> clearAllDevices()
                        1 -> Toast.makeText(this, "KeyLink v1.0", Toast.LENGTH_SHORT).show()
                    }
                }.show()
        }
    }

    private fun showDeleteDialog(device: Device) {
        AlertDialog.Builder(this)
            .setTitle("Delete Connection")
            .setMessage("Do you want to remove ${device.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deviceList.remove(device)
                saveDevices()
                updateUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllDevices() {
        deviceList.clear()
        saveDevices()
        updateUI()
    }

    private fun showConnectDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Connect to PC")
        val input = EditText(this)
        input.hint = "Enter PC IP Address (e.g. 192.168.1.5)"
        builder.setView(input)
        builder.setPositiveButton("Connect") { _, _ ->
            val ip = input.text.toString()
            if (ip.isNotEmpty()) {
                val newDevice = Device("PC @ $ip", "Manual Connection", ip, DeviceType.MONITOR, true)
                deviceList.add(0, newDevice)
                saveDevices()
                updateUI()
                
                val intent = Intent(this, KeyboardActivity::class.java)
                intent.putExtra("PC_IP", ip)
                startActivity(intent)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun updateUI() {
        adapter.notifyDataSetChanged()
        tvTotalCount.text = "${deviceList.size} Total"
    }

    private fun saveDevices() {
        val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (device in deviceList) {
            val jsonObject = JSONObject()
            jsonObject.put("name", device.name)
            jsonObject.put("status", device.status)
            jsonObject.put("ip", device.ip)
            jsonObject.put("type", device.type.name)
            jsonObject.put("isOnline", device.isOnline)
            jsonArray.put(jsonObject)
        }
        sharedPref.edit().putString("devices", jsonArray.toString()).apply()
    }

    private fun loadDevices() {
        val sharedPref = getSharedPreferences("KeyLinkPrefs", Context.MODE_PRIVATE)
        val devicesJson = sharedPref.getString("devices", null)
        deviceList.clear()
        if (devicesJson != null) {
            val jsonArray = JSONArray(devicesJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                deviceList.add(Device(
                    obj.getString("name"),
                    obj.getString("status"),
                    obj.getString("ip"),
                    DeviceType.valueOf(obj.getString("type")),
                    obj.getBoolean("isOnline")
                ))
            }
        } else {
            // Default mock data
            deviceList.add(Device("Workstation", "Seen 1m ago", "192.168.1.101", DeviceType.MONITOR, true))
            deviceList.add(Device("MacBook Pro", "Seen 12m ago", "192.168.1.102", DeviceType.LAPTOP, true))
        }
        updateUI()
    }
}

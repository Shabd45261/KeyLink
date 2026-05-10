package com.example.keylink

data class Device(
    val name: String,
    val status: String,
    val ip: String,
    val type: DeviceType,
    val isOnline: Boolean
)

enum class DeviceType {
    MONITOR, LAPTOP, PHONE
}

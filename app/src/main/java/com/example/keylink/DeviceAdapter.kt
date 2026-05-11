package com.example.keylink

import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private val devices: List<Device>,
    private val onClick: (Device) -> Unit,
    private val onLongClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivDeviceIcon)
        val tvName: TextView = view.findViewById(R.id.tvDeviceName)
        val tvStatus: TextView = view.findViewById(R.id.tvDeviceStatus)
        val ivArrow: ImageView = view.findViewById(R.id.ivArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.tvName.text = device.name
        holder.tvStatus.text = device.status
        
        val context = holder.itemView.context
        val iconRes = when (device.type) {
            DeviceType.MONITOR -> R.drawable.ic_monitor
            DeviceType.LAPTOP -> R.drawable.ic_laptop
            DeviceType.PHONE -> R.drawable.ic_phone
        }
        holder.ivIcon.setImageDrawable(AppCompatResources.getDrawable(context, iconRes))

        if (device.isOnline) {
            holder.tvName.setTextColor(Color.WHITE)
            holder.ivIcon.alpha = 1.0f
            holder.ivArrow.setColorFilter(Color.WHITE)
            holder.ivArrow.alpha = 1.0f
        } else {
            holder.tvName.setTextColor(Color.parseColor("#444444"))
            holder.ivIcon.alpha = 0.3f
            holder.ivArrow.setColorFilter(Color.parseColor("#FF5252"))
            holder.ivArrow.alpha = 0.5f
        }

        holder.itemView.setOnClickListener { onClick(device) }
        holder.itemView.setOnLongClickListener {
            onLongClick(device)
            true
        }
    }

    override fun getItemCount() = devices.size
}

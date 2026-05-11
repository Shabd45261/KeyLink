package com.example.keylink

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Shortcut(val name: String, val keys: String, val isHeader: Boolean = false)

class ShortcutAdapter(
    private val shortcuts: List<Shortcut>,
    private val onShortcutClick: (Shortcut) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (shortcuts[position].isHeader) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shortcut, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val shortcut = shortcuts[position]
        if (holder is HeaderViewHolder) {
            holder.textView.text = shortcut.name
            holder.textView.setTextColor(Color.parseColor("#448AFF"))
            holder.textView.textSize = 18f
            holder.textView.setPadding(0, 32, 0, 16)
        } else if (holder is ItemViewHolder) {
            holder.tvName.text = shortcut.name
            holder.tvKeys.text = shortcut.keys
            holder.itemView.setOnClickListener { onShortcutClick(shortcut) }
        }
    }

    override fun getItemCount() = shortcuts.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvShortcutName)
        val tvKeys: TextView = view.findViewById(R.id.tvShortcutKeys)
    }
}

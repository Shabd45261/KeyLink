package com.example.keylink

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

class ShortcutsActivity : AppCompatActivity() {

    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var pcIp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shortcuts)

        pcIp = intent.getStringExtra("PC_IP")
        if (pcIp != null) {
            connectToPc()
        }

        val rv = findViewById<RecyclerView>(R.id.rvShortcuts)
        rv.layoutManager = LinearLayoutManager(this)

        val shortcuts = mutableListOf<Shortcut>()

        // SYSTEM SHORTCUTS
        shortcuts.add(Shortcut("SYSTEM SHORTCUTS", "", true))
        shortcuts.add(Shortcut("Open Start", "Win"))
        shortcuts.add(Shortcut("Quick Settings", "Win + A"))
        shortcuts.add(Shortcut("Focus system tray", "Win + B"))
        shortcuts.add(Shortcut("Copilot / Chat", "Win + C"))
        shortcuts.add(Shortcut("Show desktop", "Win + D"))
        shortcuts.add(Shortcut("Open File Explorer", "Win + E"))
        shortcuts.add(Shortcut("Feedback Hub", "Win + F"))
        shortcuts.add(Shortcut("Xbox Game Bar", "Win + G"))
        shortcuts.add(Shortcut("Voice typing", "Win + H"))
        shortcuts.add(Shortcut("Settings", "Win + I"))
        shortcuts.add(Shortcut("Connect devices", "Win + K"))
        shortcuts.add(Shortcut("Lock PC", "Win + L"))
        shortcuts.add(Shortcut("Minimize all", "Win + M"))
        shortcuts.add(Shortcut("Project display", "Win + P"))
        shortcuts.add(Shortcut("Run dialog", "Win + R"))
        shortcuts.add(Shortcut("Search", "Win + S"))
        shortcuts.add(Shortcut("Accessibility", "Win + U"))
        shortcuts.add(Shortcut("Clipboard history", "Win + V"))
        shortcuts.add(Shortcut("Power menu", "Win + X"))
        shortcuts.add(Shortcut("Emoji panel", "Win + ."))
        shortcuts.add(Shortcut("System info", "Win + Pause"))

        // WINDOW MANAGEMENT
        shortcuts.add(Shortcut("WINDOW MANAGEMENT", "", true))
        shortcuts.add(Shortcut("Switch apps", "Alt + Tab"))
        shortcuts.add(Shortcut("Task view", "Win + Tab"))
        shortcuts.add(Shortcut("Close app", "Alt + F4"))
        shortcuts.add(Shortcut("Task Manager", "Ctrl + Shift + Esc"))
        shortcuts.add(Shortcut("Maximize", "Win + Up"))
        shortcuts.add(Shortcut("Minimize", "Win + Down"))
        shortcuts.add(Shortcut("Snap left", "Win + Left"))
        shortcuts.add(Shortcut("Snap right", "Win + Right"))
        shortcuts.add(Shortcut("New virtual desktop", "Win + Ctrl + D"))
        shortcuts.add(Shortcut("Switch desktops", "Win + Ctrl + Left"))
        shortcuts.add(Shortcut("Close desktop", "Win + Ctrl + F4"))

        // FILE EXPLORER
        shortcuts.add(Shortcut("FILE EXPLORER", "", true))
        shortcuts.add(Shortcut("New window", "Ctrl + N"))
        shortcuts.add(Shortcut("Close window", "Ctrl + W"))
        shortcuts.add(Shortcut("New folder", "Ctrl + Shift + N"))
        shortcuts.add(Shortcut("Properties", "Alt + Enter"))
        shortcuts.add(Shortcut("Back", "Alt + Left"))
        shortcuts.add(Shortcut("Forward", "Alt + Right"))
        shortcuts.add(Shortcut("Parent folder", "Alt + Up"))
        shortcuts.add(Shortcut("Rename", "F2"))
        shortcuts.add(Shortcut("Refresh", "F5"))

        // TEXT EDITING
        shortcuts.add(Shortcut("TEXT EDITING", "", true))
        shortcuts.add(Shortcut("Copy", "Ctrl + C"))
        shortcuts.add(Shortcut("Cut", "Ctrl + X"))
        shortcuts.add(Shortcut("Paste", "Ctrl + V"))
        shortcuts.add(Shortcut("Undo", "Ctrl + Z"))
        shortcuts.add(Shortcut("Redo", "Ctrl + Y"))
        shortcuts.add(Shortcut("Select all", "Ctrl + A"))
        shortcuts.add(Shortcut("Save", "Ctrl + S"))
        shortcuts.add(Shortcut("Print", "Ctrl + P"))
        shortcuts.add(Shortcut("Find", "Ctrl + F"))

        // SCREENSHOT
        shortcuts.add(Shortcut("SCREENSHOT", "", true))
        shortcuts.add(Shortcut("Snipping Tool", "Win + Shift + S"))
        shortcuts.add(Shortcut("Active window screenshot", "Alt + PrtSc"))
        shortcuts.add(Shortcut("Save screenshot auto", "Win + PrtSc"))

        // BROWSER
        shortcuts.add(Shortcut("BROWSER", "", true))
        shortcuts.add(Shortcut("New tab", "Ctrl + T"))
        shortcuts.add(Shortcut("Close tab", "Ctrl + W"))
        shortcuts.add(Shortcut("Reopen tab", "Ctrl + Shift + T"))
        shortcuts.add(Shortcut("Next tab", "Ctrl + Tab"))
        shortcuts.add(Shortcut("Previous tab", "Ctrl + Shift + Tab"))
        shortcuts.add(Shortcut("Bookmark", "Ctrl + D"))
        shortcuts.add(Shortcut("Zoom in", "Ctrl + +"))
        shortcuts.add(Shortcut("Zoom out", "Ctrl + -"))

        // POWER USER
        shortcuts.add(Shortcut("POWER USER", "", true))
        shortcuts.add(Shortcut("Secret admin menu", "Win + X"))
        shortcuts.add(Shortcut("Fix black screen/GPU", "Win + Ctrl + Shift + B"))
        shortcuts.add(Shortcut("Permanent delete", "Shift + Del"))

        rv.adapter = ShortcutAdapter(shortcuts) { shortcut ->
            if (pcIp == null) {
                Toast.makeText(this, "Connect to a PC first", Toast.LENGTH_SHORT).show()
            } else {
                sendShortcut(shortcut.keys)
            }
        }
    }

    private fun sendShortcut(keysStr: String) {
        val keys = keysStr.replace(" ", "").split("+").map { k ->
            when (k.lowercase()) {
                "win" -> "win"
                "ctrl" -> "ctrl"
                "alt" -> "alt"
                "shift" -> "shift"
                "up" -> "up"
                "down" -> "down"
                "left" -> "left"
                "right" -> "right"
                "esc" -> "esc"
                "tab" -> "tab"
                "prtsc" -> "prtsc"
                "." -> "."
                else -> k.lowercase()
            }
        }.joinToString(",")

        thread {
            try {
                out?.println("HOTKEY:$keys")
                out?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    override fun onDestroy() {
        super.onDestroy()
        thread { try { socket?.close() } catch (e: Exception) {} }
    }
}

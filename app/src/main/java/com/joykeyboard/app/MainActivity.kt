package com.joykeyboard.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 32)
            setBackgroundColor(Color.rgb(240, 248, 243))
        }

        val title = TextView(this).apply {
            text = "JOY QUICK REPLY"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(20, 110, 65))
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Use with your current keyboard, including Gboard"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(0, 10, 0, 24)
        }
        root.addView(subtitle)

        val manage = Button(this).apply {
            text = "Manage Quick Replies"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, QuickReplyActivity::class.java))
            }
        }
        root.addView(manage)

        val accessibility = Button(this).apply {
            text = "Enable Floating Quick Reply"
            setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        root.addView(accessibility)

        val note = TextView(this).apply {
            text = "Enable Joy Quick Reply in Accessibility settings. Then type / in a supported text field to search and insert a saved reply. Gboard stays your default keyboard."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(8, 20, 8, 0)
        }
        root.addView(note)

        setContentView(root)
    }
}
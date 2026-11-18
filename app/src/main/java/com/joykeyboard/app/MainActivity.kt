
package com.joykeyboard.app

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.*

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("joy_keyboard_settings", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 30, 24, 30)
            setBackgroundColor(Color.rgb(240, 248, 243))
        }

        val title = TextView(this).apply {
            text = "JOY KEYBOARD"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(20, 110, 65))
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Keyboard Settings"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        root.addView(subtitle)

        val enable = Button(this).apply {
            text = "Enable Joy Keyboard"
            setOnClickListener {
                startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS"))
            }
        }
        root.addView(enable)

        val select = Button(this).apply {
            text = "Select Joy Keyboard"
            setOnClickListener {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
                imm.showInputMethodPicker()
            }
        }
        root.addView(select)

        val quickReply = Button(this).apply {
            text = "WhatsApp Quick Reply"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, QuickReplyActivity::class.java))
            }
        }
        root.addView(quickReply)

        val heading = TextView(this).apply {
            text = "Keyboard Size"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }
        root.addView(heading)

        val value = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 17f
        }
        root.addView(value)

        val slider = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("keyboard_size", 50)
        }

        fun updateLabel(progress: Int) {
            val size = when {
                progress < 35 -> "Small"
                progress < 70 -> "Medium"
                else -> "Large"
            }
            value.text = "$size - $progress%"
        }

        updateLabel(slider.progress)

        slider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    updateLabel(progress)
                    if (fromUser) {
                        prefs.edit()
                            .putInt("keyboard_size", progress)
                            .apply()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        root.addView(slider)

        val note = TextView(this).apply {
            text = "Size change will apply when you reopen the keyboard."
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }
        root.addView(note)

        val reset = Button(this).apply {
            text = "Reset Size"
            setOnClickListener {
                slider.progress = 50
                prefs.edit().putInt("keyboard_size", 50).apply()
                updateLabel(50)
            }
        }
        root.addView(reset)

        setContentView(root)
    }
}

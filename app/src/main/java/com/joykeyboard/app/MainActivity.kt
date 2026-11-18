
package com.joykeyboard.app

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.*

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("joy_keyboard_settings", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setPadding(24, 30, 24, 30)

        val title = TextView(this)
        title.text = "JOY KEYBOARD"
        title.textSize = 28f
        title.gravity = Gravity.CENTER
        root.addView(title)

        val enable = Button(this)
        enable.text = "Enable Joy Keyboard"
        enable.setOnClickListener {
            startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS"))
        }
        root.addView(enable)

        val select = Button(this)
        select.text = "Select Joy Keyboard"
        select.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
            imm.showInputMethodPicker()
        }
        root.addView(select)

        val quickReply = Button(this)
        quickReply.text = "WhatsApp Quick Reply"
        quickReply.setOnClickListener {
            startActivity(Intent(this, QuickReplyActivity::class.java))
        }
        root.addView(quickReply)

        val heading = TextView(this)
        heading.text = "\nKeyboard Size"
        heading.textSize = 22f
        heading.gravity = Gravity.CENTER
        root.addView(heading)

        val value = TextView(this)
        value.gravity = Gravity.CENTER
        root.addView(value)

        val slider = SeekBar(this)
        slider.max = 100
        slider.progress = prefs.getInt("keyboard_size", 50)

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

        val reset = Button(this)
        reset.text = "Reset Size"
        reset.setOnClickListener {
            slider.progress = 50
            prefs.edit().putInt("keyboard_size", 50).apply()
            updateLabel(50)
        }
        root.addView(reset)

        setContentView(root)
    }
}

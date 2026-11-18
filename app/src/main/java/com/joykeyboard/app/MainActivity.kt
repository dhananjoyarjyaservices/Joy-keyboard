
package com.joykeyboard.app

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

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

        val subtitle = TextView(this)
        subtitle.text = "Your keyboard, your rules."
        subtitle.textSize = 16f
        subtitle.gravity = Gravity.CENTER

        root.addView(subtitle)

        val enable = Button(this)
        enable.text = "Enable Joy Keyboard"

        enable.setOnClickListener {
            startActivity(
                Intent("android.settings.INPUT_METHOD_SETTINGS")
            )
        }

        root.addView(enable)

        val select = Button(this)
        select.text = "Select Joy Keyboard"

        select.setOnClickListener {
            val imm = getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

            imm.showInputMethodPicker()
        }

        root.addView(select)

        val info = TextView(this)
        info.text = "\nClipboard and Quick Reply features are coming."
        info.gravity = Gravity.CENTER
        info.textSize = 14f

        root.addView(info)

        setContentView(root)
    }
}

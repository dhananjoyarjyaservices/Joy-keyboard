
package com.joykeyboard.app

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
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
        title.textSize = 30f
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

        val quickReply = Button(this)
        quickReply.text = "WhatsApp Quick Reply"

        quickReply.setOnClickListener {
            startActivity(
                Intent(this, QuickReplyActivity::class.java)
            )
        }

        root.addView(quickReply)

        val info = TextView(this)
        info.text = "\nClipboard and more features coming soon."
        info.gravity = Gravity.CENTER

        root.addView(info)

        setContentView(root)
    }
}

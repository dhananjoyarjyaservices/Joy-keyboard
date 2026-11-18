
package com.joykeyboard.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class JoyKeyboardService : InputMethodService() {

    private var caps = false
    private var symbols = false
    private var root: LinearLayout? = null

    private val prefs by lazy {
        getSharedPreferences("joy_keyboard_settings", MODE_PRIVATE)
    }

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 5, 4, 5)
            setBackgroundColor(Color.rgb(231, 246, 238))
        }
        renderKeyboard()
        return root!!
    }

    private fun renderKeyboard() {
        val layout = root ?: return
        layout.removeAllViews()

        val size = prefs.getInt("keyboard_size", 50).coerceIn(0, 100)
        val height = dp(40 + size * 30 / 100)

        val toolbar = LinearLayout(this).apply {
            gravity = Gravity.CENTER
        }

        addTool(toolbar, "▦") {}
        addTool(toolbar, "💬") {
            startActivity(Intent(this, QuickReplyActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        addTool(toolbar, "⚙") {
            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        layout.addView(toolbar)

        addRow(layout, listOf("1","2","3","4","5","6","7","8","9","0"), height)

        if (!symbols) {
            addRow(layout, listOf("q","w","e","r","t","y","u","i","o","p"), height)
            addRow(layout, listOf("a","s","d","f","g","h","j","k","l"), height)

            val row = LinearLayout(this)
            addKey(row, "⇧", 1.3f, height) {
                caps = !caps
                renderKeyboard()
            }

            listOf("z","x","c","v","b","n","m").forEach {
                addKey(row, if (caps) it.uppercase() else it, 1f, height) {
                    commit(if (caps) it.uppercase() else it)
                }
            }

            addKey(row, "⌫", 1.3f, height) {
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
            layout.addView(row)

        } else {
            addRow(layout, listOf("@","#","₹","_","&","-","+","(",")","/"), height)
            addRow(layout, listOf("=","*","\"","'",":",";","!","?","×"), height)
        }

        val bottom = LinearLayout(this)

        addKey(bottom, if (symbols) "ABC" else "?123", 1.4f, height) {
            symbols = !symbols
            renderKeyboard()
        }

        addKey(bottom, ",", 0.7f, height) { commit(",") }
        addKey(bottom, "☺", 0.8f, height) {}
        addKey(bottom, "SPACE", 3.5f, height) { commit(" ") }
        addKey(bottom, ".", 0.7f, height) { commit(".") }
        addKey(bottom, "↵", 1.3f, height) {
            currentInputConnection?.commitText("\n", 1)
        }

        layout.addView(bottom)
    }

    private fun addRow(
        layout: LinearLayout,
        keys: List<String>,
        height: Int
    ) {
        val row = LinearLayout(this)
        keys.forEach {
            addKey(row, it, 1f, height) { commit(it) }
        }
        layout.addView(row)
    }

    private fun addTool(row: LinearLayout, label: String, action: () -> Unit) {
        val button = TextView(this)
        button.text = label
        button.textSize = 22f
        button.gravity = Gravity.CENTER
        button.setTextColor(Color.BLACK)
        button.setOnClickListener { action() }
        row.addView(button, LinearLayout.LayoutParams(0, dp(42), 1f))
    }

    private fun addKey(
        row: LinearLayout,
        label: String,
        weight: Float,
        height: Int,
        action: () -> Unit
    ) {
        val key = TextView(this)
        key.text = label
        key.textSize = if (label == "SPACE") 14f else 18f
        key.gravity = Gravity.CENTER
        key.setTextColor(Color.rgb(30, 30, 30))
        key.typeface = Typeface.DEFAULT
        key.background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(9).toFloat()
        }
        key.setOnClickListener { action() }

        val params = LinearLayout.LayoutParams(0, height, weight)
        params.setMargins(dp(2), dp(3), dp(2), dp(3))
        row.addView(key, params)
    }

    private fun commit(value: String) {
        currentInputConnection?.commitText(value, 1)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

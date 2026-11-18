
package com.joykeyboard.app

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class JoyKeyboardService : InputMethodService() {

    private var caps = false

    private val green = Color.rgb(22, 163, 74)
    private val keyColor = Color.rgb(255, 255, 255)

    override fun onCreateInputView(): View {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(4, 8, 4, 8)
        root.setBackgroundColor(Color.rgb(231, 246, 238))

        val toolbar = LinearLayout(this)
        toolbar.gravity = Gravity.CENTER
        toolbar.orientation = LinearLayout.HORIZONTAL

        toolbar.addView(toolButton("▦") {
            // Clipboard feature will be added next
        })

        toolbar.addView(toolButton("💬") {
            // Quick Reply feature will be added next
        })

        toolbar.addView(toolButton("⚙") {
            // Settings feature will be added next
        })

        root.addView(toolbar)

        addKeyRow(root, listOf("Q","W","E","R","T","Y","U","I","O","P"))
        addKeyRow(root, listOf("A","S","D","F","G","H","J","K","L"))
        
        val third = LinearLayout(this)
        third.orientation = LinearLayout.HORIZONTAL
        third.gravity = Gravity.CENTER

        third.addView(keyButton("⇧", 1f) {
            caps = !caps
        })

        listOf("Z","X","C","V","B","N","M").forEach {
            third.addView(keyButton(it, 1f) {
                commit(it)
            })
        }

        third.addView(keyButton("⌫", 1.2f) {
            currentInputConnection?.deleteSurroundingText(1, 0)
        })

        root.addView(third)

        val bottom = LinearLayout(this)
        bottom.orientation = LinearLayout.HORIZONTAL

        bottom.addView(keyButton("?123", 1.3f) {})
        bottom.addView(keyButton(",", 0.7f) {
            commit(",")
        })
        bottom.addView(keyButton("☺", 0.9f) {})
        bottom.addView(keyButton("SPACE", 4f) {
            commit(" ")
        })
        bottom.addView(keyButton(".", 0.7f) {
            commit(".")
        })
        bottom.addView(keyButton("↵", 1.3f) {
            currentInputConnection?.commitText("\n", 1)
        })

        root.addView(bottom)

        return root
    }

    private fun addKeyRow(
        root: LinearLayout,
        keys: List<String>
    ) {
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER
        row.orientation = LinearLayout.HORIZONTAL

        keys.forEach { key ->
            row.addView(keyButton(key, 1f) {
                commit(key)
            })
        }

        root.addView(row)
    }

    private fun commit(value: String) {
        val text = if (value.length == 1 && value[0].isLetter()) {
            if (caps) value.uppercase() else value.lowercase()
        } else {
            value
        }

        currentInputConnection?.commitText(text, 1)
    }

    private fun toolButton(
        label: String,
        action: () -> Unit
    ): TextView {
        val button = TextView(this)
        button.text = label
        button.textSize = 23f
        button.gravity = Gravity.CENTER
        button.setTextColor(Color.BLACK)
        button.setOnClickListener { action() }

        button.layoutParams = LinearLayout.LayoutParams(
            0,
            52,
            1f
        )

        return button
    }

    private fun keyButton(
        label: String,
        weight: Float,
        action: () -> Unit
    ): Button {
        val button = Button(this)
        button.text = label
        button.textSize = 17f
        button.setTextColor(Color.rgb(30, 30, 30))
        button.typeface = Typeface.DEFAULT
        button.isAllCaps = false
        button.background = GradientDrawable().apply {
            setColor(keyColor)
            cornerRadius = 12f
        }

        button.setPadding(0, 0, 0, 0)
        button.setOnClickListener { action() }

        val params = LinearLayout.LayoutParams(
            0,
            52,
            weight
        )

        params.setMargins(2, 4, 2, 4)
        button.layoutParams = params

        return button
    }
}

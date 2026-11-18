
package com.joykeyboard.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class QuickReplyActivity : Activity() {

    private lateinit var store: QuickReplyStore
    private lateinit var codeInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = QuickReplyStore(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 20)
            setBackgroundColor(Color.rgb(245, 250, 247))
        }

        val title = TextView(this).apply {
            text = "WhatsApp Quick Reply"
            textSize = 24f
            setTextColor(Color.rgb(22, 120, 65))
        }
        root.addView(title)

        codeInput = EditText(this).apply {
            hint = "Enter code (example: mstock)"
            isSingleLine = true
        }
        root.addView(codeInput)

        messageInput = EditText(this).apply {
            hint = "Type your message or link"
            minLines = 3
            gravity = Gravity.TOP
        }
        root.addView(messageInput)

        val save = Button(this).apply {
            text = "Save Quick Reply"
        }
        root.addView(save)

        save.setOnClickListener {
            val code = codeInput.text.toString().trim()
                .removePrefix("\\")

            val message = messageInput.text.toString().trim()

            if (code.isBlank() || message.isBlank()) {
                Toast.makeText(
                    this,
                    "Enter code and message",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Correct save function call
                store.save(
                    QuickReply(
                        code = code,
                        message = message
                    )
                )

                codeInput.text.clear()
                messageInput.text.clear()

                refreshList()

                Toast.makeText(
                    this,
                    "Quick Reply Saved",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val heading = TextView(this).apply {
            text = "My Saved Replies"
            textSize = 20f
            setTextColor(Color.BLACK)
            setPadding(0, 16, 0, 8)
        }
        root.addView(heading)

        val scroll = ScrollView(this)

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(list)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1, 0, 1f
            )
        )

        setContentView(root)

        refreshList()
    }

    private fun refreshList() {
        list.removeAllViews()

        store.getAll().forEach { reply ->

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                setBackgroundColor(Color.WHITE)
            }

            val code = TextView(this).apply {
                text = "\\" + reply.code
                textSize = 18f
                setTextColor(Color.rgb(20, 130, 65))
            }

            val message = TextView(this).apply {
                text = reply.message
                textSize = 14f
                setTextColor(Color.DKGRAY)
            }

            val delete = Button(this).apply {
                text = "Delete"
            }

            delete.setOnClickListener {
                store.delete(reply.code)
                refreshList()

                Toast.makeText(
                    this@QuickReplyActivity,
                    "Quick Reply Deleted",
                    Toast.LENGTH_SHORT
                ).show()
            }

            item.addView(code)
            item.addView(message)
            item.addView(delete)

            val params = LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.setMargins(0, 6, 0, 6)

            list.addView(item, params)
        }
    }
}

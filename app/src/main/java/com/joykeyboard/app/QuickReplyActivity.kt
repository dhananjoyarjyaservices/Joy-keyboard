
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

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 24, 20, 20)
        root.setBackgroundColor(Color.rgb(245, 250, 247))

        val title = TextView(this)
        title.text = "WhatsApp Quick Reply"
        title.textSize = 24f
        title.setTextColor(Color.rgb(22, 120, 65))
        root.addView(title)

        codeInput = EditText(this)
        codeInput.hint = "Enter code (example: mstock)"
        root.addView(codeInput)

        messageInput = EditText(this)
        messageInput.hint = "Type your message or link"
        messageInput.minLines = 3
        messageInput.gravity = Gravity.TOP
        root.addView(messageInput)

        val save = Button(this)
        save.text = "Save Quick Reply"

        root.addView(save)

        save.setOnClickListener {
            val code = codeInput.text.toString().trim()
            val message = messageInput.text.toString()

            if (code.isBlank() || message.isBlank()) {
                Toast.makeText(
                    this,
                    "Enter code and message",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                store.save(code, message)
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

        val heading = TextView(this)
        heading.text = "My Saved Replies"
        heading.textSize = 20f
        heading.setTextColor(Color.BLACK)
        root.addView(heading)

        val scroll = ScrollView(this)
        list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL

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

            val item = LinearLayout(this)
            item.orientation = LinearLayout.VERTICAL
            item.setPadding(12, 12, 12, 12)

            val code = TextView(this)
            code.text = reply.code
            code.textSize = 18f
            code.setTextColor(Color.rgb(20, 130, 65))

            val message = TextView(this)
            message.text = reply.message
            message.textSize = 14f
            message.setTextColor(Color.DKGRAY)

            val delete = Button(this)
            delete.text = "Delete"

            delete.setOnClickListener {
                store.delete(reply.code)
                refreshList()
            }

            item.addView(code)
            item.addView(message)
            item.addView(delete)

            list.addView(item)
        }
    }
}

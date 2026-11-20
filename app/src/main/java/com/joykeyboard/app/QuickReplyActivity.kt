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
    private lateinit var searchInput: EditText
    private lateinit var list: LinearLayout
    private var editingCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = QuickReplyStore(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 20, 18, 16)
            setBackgroundColor(Color.rgb(245, 250, 247))
        }

        val title = TextView(this).apply {
            text = "Joy Quick Reply"
            textSize = 25f
            setTextColor(Color.rgb(20, 120, 65))
            setPadding(0, 0, 0, 12)
        }
        root.addView(title)

        codeInput = EditText(this).apply {
            hint = "Shortcut name (example: mstock)"
            isSingleLine = true
        }
        root.addView(codeInput)

        messageInput = EditText(this).apply {
            hint = "Saved message or link"
            minLines = 3
            gravity = Gravity.TOP
        }
        root.addView(messageInput)

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val save = Button(this).apply {
            text = "Save Reply"
            setOnClickListener { saveReply() }
        }
        val cancel = Button(this).apply {
            text = "Clear"
            setOnClickListener { clearEditor() }
        }
        actions.addView(save, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(cancel, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(actions)

        searchInput = EditText(this).apply {
            hint = "Search saved replies"
            isSingleLine = true
            addTextChangedListener(SimpleTextWatcher { refreshList() })
        }
        root.addView(searchInput)

        val heading = TextView(this).apply {
            text = "My Saved Replies"
            textSize = 19f
            setTextColor(Color.BLACK)
            setPadding(0, 12, 0, 8)
        }
        root.addView(heading)

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
        refreshList()
    }

    private fun saveReply() {
        val code = codeInput.text.toString().trim().removePrefix("/")
        val message = messageInput.text.toString().trim()
        if (code.isBlank() || message.isBlank()) {
            Toast.makeText(this, "Enter shortcut and message", Toast.LENGTH_SHORT).show()
            return
        }
        store.save(QuickReply(code, message))
        Toast.makeText(this, if (editingCode == null) "Reply saved" else "Reply updated", Toast.LENGTH_SHORT).show()
        clearEditor()
        refreshList()
    }

    private fun clearEditor() {
        editingCode = null
        codeInput.text.clear()
        messageInput.text.clear()
    }

    private fun refreshList() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val query = if (::searchInput.isInitialized) searchInput.text.toString() else ""
        val replies = store.search(query)

        if (replies.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No saved replies found"
                textSize = 14f
                setTextColor(Color.GRAY)
                setPadding(8, 16, 8, 16)
            }
            list.addView(empty)
            return
        }

        replies.forEach { reply ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 10, 12, 10)
                setBackgroundColor(Color.WHITE)
            }
            val code = TextView(this).apply {
                text = "/${reply.code}"
                textSize = 17f
                setTextColor(Color.rgb(20, 130, 65))
            }
            val message = TextView(this).apply {
                text = reply.message
                textSize = 14f
                setTextColor(Color.DKGRAY)
            }
            val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val edit = Button(this).apply {
                text = "Edit"
                setOnClickListener {
                    editingCode = reply.code
                    codeInput.setText(reply.code)
                    messageInput.setText(reply.message)
                    codeInput.setSelection(codeInput.text.length)
                }
            }
            val delete = Button(this).apply {
                text = "Delete"
                setOnClickListener {
                    store.delete(reply.code)
                    if (editingCode.equals(reply.code, true)) clearEditor()
                    refreshList()
                    Toast.makeText(this@QuickReplyActivity, "Reply deleted", Toast.LENGTH_SHORT).show()
                }
            }
            buttons.addView(edit, LinearLayout.LayoutParams(0, -2, 1f))
            buttons.addView(delete, LinearLayout.LayoutParams(0, -2, 1f))
            item.addView(code)
            item.addView(message)
            item.addView(buttons)
            val params = LinearLayout.LayoutParams(-1, -2)
            params.setMargins(0, 6, 0, 6)
            list.addView(item, params)
        }
    }
}

private class SimpleTextWatcher(private val changed: () -> Unit) :
    android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        changed()
    }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
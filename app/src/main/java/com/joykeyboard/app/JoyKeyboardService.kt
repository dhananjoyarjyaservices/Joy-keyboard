
package com.joykeyboard.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import java.util.Locale

class JoyKeyboardService : InputMethodService(),
    SpellCheckerSession.SpellCheckerSessionListener {

    private var caps = false
    private var symbols = false
    private var root: LinearLayout? = null

    private var suggestions = listOf<String>()
    private var spellCheckerSession: SpellCheckerSession? = null

    private val handler = Handler(Looper.getMainLooper())

    private var deleteRunnable: Runnable? = null
    private var keyPopup: PopupWindow? = null

    private val prefs by lazy {
        getSharedPreferences("joy_keyboard_settings", MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()

        try {
            val manager = getSystemService(
                Context.TEXT_SERVICES_MANAGER_SERVICE
            ) as TextServicesManager

            spellCheckerSession = manager.newSpellCheckerSession(
                null,
                Locale.getDefault(),
                this,
                true
            )
        } catch (e: Exception) {
            spellCheckerSession = null
        }
    }

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(5), dp(4), dp(5))
            setBackgroundColor(Color.rgb(231, 246, 238))
        }

        renderKeyboard()
        return root!!
    }

    private fun renderKeyboard() {
        val layout = root ?: return
        layout.removeAllViews()

        val size = prefs.getInt(
            "keyboard_size",
            50
        ).coerceIn(0, 100)

        val keyHeight = dp(40 + size * 30 / 100)

        // Toolbar
        val toolbar = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }

        addTool(toolbar, "▦") {}

        addTool(toolbar, "💬") {
            startActivity(
                Intent(this, QuickReplyActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        addTool(toolbar, if (caps) "⇧ CAPS" else "⇧") {
            caps = !caps
            renderKeyboard()
        }

        addTool(toolbar, "⚙") {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        layout.addView(toolbar)

        // Quick Reply results
        val quickReplies = getQuickReplyResults()

        if (quickReplies.isNotEmpty()) {
            val replyRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            quickReplies.take(3).forEach { reply ->

                val item = TextView(this).apply {
                    text = reply.code
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setTextColor(Color.BLACK)
                    setPadding(
                        dp(10), dp(6), dp(10), dp(6)
                    )
                    background = roundedBackground(Color.WHITE)

                    setOnClickListener {
                        val before = currentInputConnection
                            ?.getTextBeforeCursor(200, 0)
                            ?.toString() ?: ""

                        val slashIndex = before.lastIndexOf('\\')

                        if (slashIndex >= 0) {
                            val deleteLength =
                                before.length - slashIndex

                            currentInputConnection
                                ?.deleteSurroundingText(
                                    deleteLength, 0
                                )

                            currentInputConnection
                                ?.commitText(reply.message, 1)
                        }

                        suggestions = emptyList()
                        renderKeyboard()
                    }
                }

                val params = LinearLayout.LayoutParams(
                    0,
                    dp(38),
                    1f
                )

                params.setMargins(
                    dp(2), dp(2), dp(2), dp(2)
                )

                replyRow.addView(item, params)
            }

            layout.addView(replyRow)
        }

        // Word suggestions
        if (suggestions.isNotEmpty() &&
            quickReplies.isEmpty()
        ) {
            val suggestionRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            suggestions.take(3).forEach { word ->

                val item = TextView(this).apply {
                    text = word
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setTextColor(Color.BLACK)
                    setPadding(
                        dp(5), dp(5), dp(5), dp(5)
                    )

                    setOnClickListener {
                        val before = currentInputConnection
                            ?.getTextBeforeCursor(100, 0)
                            ?.toString() ?: ""

                        val currentWord = before
                            .takeLastWhile {
                                !it.isWhitespace()
                            }

                        if (currentWord.isNotEmpty()) {
                            currentInputConnection
                                ?.deleteSurroundingText(
                                    currentWord.length, 0
                                )

                            currentInputConnection
                                ?.commitText("$word ", 1)
                        }

                        suggestions = emptyList()
                        renderKeyboard()
                    }
                }

                suggestionRow.addView(
                    item,
                    LinearLayout.LayoutParams(
                        0, dp(35), 1f
                    )
                )
            }

            layout.addView(suggestionRow)
        }

        if (!symbols) {

            addRow(
                layout,
                listOf(
                    "1","2","3","4","5",
                    "6","7","8","9","0"
                ),
                keyHeight
            )

            addRow(
                layout,
                listOf(
                    "q","w","e","r","t",
                    "y","u","i","o","p"
                ),
                keyHeight
            )

            addRow(
                layout,
                listOf(
                    "a","s","d","f","g",
                    "h","j","k","l"
                ),
                keyHeight
            )

            val row = LinearLayout(this)

            addKey(row, "⇧", 1.3f, keyHeight) {
                caps = !caps
                renderKeyboard()
            }

            listOf(
                "z","x","c","v","b","n","m"
            ).forEach { letter ->

                val output = if (caps) {
                    letter.uppercase()
                } else {
                    letter
                }

                addKey(row, output, 1f, keyHeight) {
                    commit(output)
                }
            }

            addBackspaceKey(row, 1.3f, keyHeight)

            layout.addView(row)

        } else {

            addRow(
                layout,
                listOf(
                    "@","#","₹","$","%",
                    "&","-","_","(",")"
                ),
                keyHeight
            )

            addRow(
                layout,
                listOf(
                    "+","=","/","*","\"",
                    "'",":",";","!","?"
                ),
                keyHeight
            )

            addRow(
                layout,
                listOf(
                    "¥","×","•","<",">",
                    "[","]","{","}","\\"
                ),
                keyHeight
            )

            addRow(
                layout,
                listOf(
                    "~","`","^","°","©",
                    "®","™","€","£","|"
                ),
                keyHeight
            )
        }

        // Bottom row
        val bottom = LinearLayout(this)

        addKey(
            bottom,
            if (symbols) "ABC" else "?123",
            1.4f,
            keyHeight
        ) {
            symbols = !symbols
            renderKeyboard()
        }

        addKey(bottom, ",", 0.7f, keyHeight) {
            commit(",")
        }

        addKey(bottom, "\\", 0.7f, keyHeight) {
            commit("\\")
        }

        addKey(bottom, "SPACE", 3.5f, keyHeight) {
            commit(" ")
        }

        addKey(bottom, ".", 0.7f, keyHeight) {
            commit(".")
        }

        addBackspaceKey(bottom, 1.1f, keyHeight)

        addKey(bottom, "↵", 1.1f, keyHeight) {
            currentInputConnection?.commitText("\n", 1)
        }

        layout.addView(bottom)
    }

    // Quick Reply search
    private fun getQuickReplyResults(): List<QuickReply> {

        val before = currentInputConnection
            ?.getTextBeforeCursor(100, 0)
            ?.toString() ?: ""

        val slashIndex = before.lastIndexOf('\\')

        if (slashIndex < 0) {
            return emptyList()
        }

        val query = before.substring(slashIndex + 1)

        return try {
            QuickReplyStore(this)
                .search(query)
                .take(3)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun addRow(
        layout: LinearLayout,
        keys: List<String>,
        height: Int
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        keys.forEach { value ->

            val output = if (caps && !symbols) {
                value.uppercase()
            } else {
                value
            }

            addKey(row, output, 1f, height) {
                commit(output)
            }
        }

        layout.addView(row)
    }

    private fun addTool(
        row: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = TextView(this).apply {
            text = label
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setOnClickListener {
                action()
            }
        }

        row.addView(
            button,
            LinearLayout.LayoutParams(
                0, dp(42), 1f
            )
        )
    }

    private fun addKey(
        row: LinearLayout,
        label: String,
        weight: Float,
        height: Int,
        action: () -> Unit
    ) {
        val key = TextView(this).apply {
            text = label

            textSize = if (label == "SPACE") {
                14f
            } else {
                21f
            }

            gravity = Gravity.CENTER
            setTextColor(Color.rgb(30, 30, 30))
            typeface = Typeface.DEFAULT
            background = roundedBackground(Color.WHITE)

            setOnTouchListener { view, event ->

                when (event.action) {

                    MotionEvent.ACTION_DOWN -> {
                        showKeyPopup(view, label)
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        dismissKeyPopup()
                    }
                }

                false
            }

            setOnClickListener {
                action()
            }
        }

        val params = LinearLayout.LayoutParams(
            0, height, weight
        )

        params.setMargins(
            dp(3), dp(4), dp(3), dp(4)
        )

        row.addView(key, params)
    }

    // Long press backspace
    private fun addBackspaceKey(
        row: LinearLayout,
        weight: Float,
        height: Int
    ) {
        val key = TextView(this).apply {
            text = "⌫"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            background = roundedBackground(Color.WHITE)

            setOnTouchListener { _, event ->

                when (event.action) {

                    MotionEvent.ACTION_DOWN -> {

                        stopDeleting()

                        deleteOneCharacter()

                        val runnable = object : Runnable {
                            override fun run() {
                                deleteOneCharacter()

                                handler.postDelayed(
                                    this, 45
                                )
                            }
                        }

                        deleteRunnable = runnable

                        handler.postDelayed(
                            runnable, 350
                        )

                        true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {

                        stopDeleting()

                        renderKeyboard()

                        true
                    }

                    else -> true
                }
            }
        }

        val params = LinearLayout.LayoutParams(
            0, height, weight
        )

        params.setMargins(
            dp(3), dp(4), dp(3), dp(4)
        )

        row.addView(key, params)
    }

    private fun showKeyPopup(
        view: View,
        label: String
    ) {
        if (label == "SPACE" || label == "⌫") {
            return
        }

        dismissKeyPopup()

        val popupText = TextView(this).apply {
            text = label
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            background = roundedBackground(Color.WHITE)
        }

        val popup = PopupWindow(
            popupText,
            dp(48),
            dp(52),
            false
        )

        val location = IntArray(2)

        view.getLocationOnScreen(location)

        try {
            popup.showAtLocation(
                view,
                Gravity.TOP or Gravity.START,
                location[0] + view.width / 2 - dp(24),
                location[1] - dp(55)
            )

            keyPopup = popup

        } catch (e: Exception) {
            keyPopup = null
        }
    }

    private fun dismissKeyPopup() {
        try {
            keyPopup?.dismiss()
        } catch (e: Exception) {
        }

        keyPopup = null
    }

    private fun stopDeleting() {
        deleteRunnable?.let {
            handler.removeCallbacks(it)
        }

        deleteRunnable = null
    }

    private fun deleteOneCharacter() {
        currentInputConnection
            ?.deleteSurroundingText(1, 0)

        suggestions = emptyList()
    }

    private fun commit(value: String) {

        currentInputConnection?.commitText(
            value, 1
        )

        updateSuggestions()

        renderKeyboard()
    }

    // Spell checker suggestions
    private fun updateSuggestions() {

        val before = currentInputConnection
            ?.getTextBeforeCursor(100, 0)
            ?.toString() ?: ""

        val word = before.takeLastWhile {
            !it.isWhitespace()
        }

        if (word.length < 2 || word.startsWith("\\")) {
            suggestions = emptyList()
            return
        }

        try {
            spellCheckerSession?.getSuggestions(
                TextInfo(word),
                5
            )
        } catch (e: Exception) {
            suggestions = emptyList()
        }
    }

    // Correct Android listener signature
    override fun onGetSuggestions(
        results: Array<SuggestionsInfo>
    ) {
        val found = mutableListOf<String>()

        results.forEach { info ->

            for (i in 0 until info.suggestionsCount) {

                val suggestion = info.getSuggestionAt(i)

                if (!suggestion.isNullOrBlank()) {
                    found.add(suggestion)
                }
            }
        }

        handler.post {

            suggestions = found.distinct().take(3)

            renderKeyboard()
        }
    }

    // Correct Android listener signature
    override fun onGetSentenceSuggestions(
        results: Array<SentenceSuggestionsInfo>
    ) {
        // Not used
    }

    private fun roundedBackground(
        color: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(9).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }

    override fun onDestroy() {

        stopDeleting()
        dismissKeyPopup()

        spellCheckerSession?.close()
        spellCheckerSession = null

        super.onDestroy()
    }
}

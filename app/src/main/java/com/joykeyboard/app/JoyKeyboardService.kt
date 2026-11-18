
package com.joykeyboard.app

import android.content.ClipboardManager
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
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class JoyKeyboardService : InputMethodService(),
    SpellCheckerSession.SpellCheckerSessionListener {

    private var caps = false
    private var symbols = false
    private var clipboardOpen = false

    private var root: LinearLayout? = null
    private var suggestionArea: LinearLayout? = null

    private var suggestions = listOf<String>()
    private var spellCheckerSession: SpellCheckerSession? = null

    private val handler = Handler(Looper.getMainLooper())
    private var deleteRunnable: Runnable? = null

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
        suggestionArea = null

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

        addTool(toolbar, "▦") {
            clipboardOpen = !clipboardOpen

            if (clipboardOpen) {
                captureCurrentClipboard()
            }

            renderKeyboard()
        }

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

        // Clipboard panel
        if (clipboardOpen) {
            renderClipboardPanel(layout)
            return
        }

        // Dedicated suggestion area
        suggestionArea = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        layout.addView(
            suggestionArea,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(40)
            )
        )

        refreshSuggestionArea()

        if (!symbols) {
            addRow(
                layout,
                listOf("1","2","3","4","5","6","7","8","9","0"),
                keyHeight
            )

            addRow(
                layout,
                listOf("q","w","e","r","t","y","u","i","o","p"),
                keyHeight
            )

            addRow(
                layout,
                listOf("a","s","d","f","g","h","j","k","l"),
                keyHeight
            )

            val row = LinearLayout(this)

            addKey(row, "⇧", 1.3f, keyHeight) {
                caps = !caps
                renderKeyboard()
            }

            listOf("z","x","c","v","b","n","m").forEach { letter ->
                val output = if (caps) letter.uppercase() else letter

                addKey(row, output, 1f, keyHeight) {
                    commit(output)
                }
            }

            addBackspaceKey(row, 1.3f, keyHeight)
            layout.addView(row)

        } else {
            addRow(
                layout,
                listOf("@","#","₹","$","%","&","-","_","(",")"),
                keyHeight
            )

            addRow(
                layout,
                listOf("+","=","/","*","\"","'",":",";","!","?"),
                keyHeight
            )

            addRow(
                layout,
                listOf("¥","×","•","<",">","[","]","{","}","\\"),
                keyHeight
            )

            addRow(
                layout,
                listOf("~","`","^","°","©","®","™","€","£","|"),
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
            suggestions = emptyList()
            refreshSuggestionArea()
        }

        layout.addView(bottom)
    }

    // Clipboard capture
    private fun captureCurrentClipboard() {
        try {
            val clipboard = getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

            if (clipboard.hasPrimaryClip()) {
                val text = clipboard.primaryClip
                    ?.getItemAt(0)
                    ?.coerceToText(this)
                    ?.toString()

                if (!text.isNullOrBlank()) {
                    ClipboardStore(this).add(text)
                }
            }
        } catch (e: Exception) {
        }
    }

    // Clipboard panel
    private fun renderClipboardPanel(layout: LinearLayout) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "Clipboard History"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        addTool(header, "Clear") {
            ClipboardStore(this).clear()
            renderKeyboard()
        }

        addTool(header, "✕") {
            clipboardOpen = false
            renderKeyboard()
        }

        layout.addView(header)

        val history = ClipboardStore(this).getAll()

        if (history.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No clipboard history yet"
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.DKGRAY)
            }

            layout.addView(empty)
            return
        }

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        history.forEach { item ->
            val entry = TextView(this).apply {
                text = item
                textSize = 14f
                maxLines = 2
                setTextColor(Color.BLACK)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = roundedBackground(Color.WHITE)

                setOnClickListener {
                    currentInputConnection?.commitText(item, 1)
                    clipboardOpen = false
                    renderKeyboard()
                }

                setOnLongClickListener {
                    ClipboardStore(this@JoyKeyboardService).delete(item)
                    renderKeyboard()
                    true
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.setMargins(dp(3), dp(3), dp(3), dp(3))
            list.addView(entry, params)
        }

        val scroll = ScrollView(this).apply {
            addView(list)
        }

        layout.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(170)
            )
        )
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
            QuickReplyStore(this).search(query).take(3)
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
            val output = if (caps && !symbols) value.uppercase() else value

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
            LinearLayout.LayoutParams(0, dp(42), 1f)
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
            textSize = if (label == "SPACE") 14f else 21f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(30, 30, 30))
            typeface = Typeface.DEFAULT
            background = roundedBackground(Color.WHITE)

            setOnClickListener {
                action()
            }
        }

        val params = LinearLayout.LayoutParams(0, height, weight)
        params.setMargins(dp(3), dp(4), dp(3), dp(4))
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
                                handler.postDelayed(this, 60)
                            }
                        }

                        deleteRunnable = runnable
                        handler.postDelayed(runnable, 350)
                        true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        stopDeleting()
                        updateSuggestions()
                        refreshSuggestionArea()
                        true
                    }

                    else -> true
                }
            }
        }

        val params = LinearLayout.LayoutParams(0, height, weight)
        params.setMargins(dp(3), dp(4), dp(3), dp(4))
        row.addView(key, params)
    }

    private fun stopDeleting() {
        deleteRunnable?.let {
            handler.removeCallbacks(it)
        }

        deleteRunnable = null
    }

    private fun deleteOneCharacter() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        suggestions = emptyList()
        refreshSuggestionArea()
    }

    // Normal typing: no full keyboard redraw
    private fun commit(value: String) {
        currentInputConnection?.commitText(value, 1)
        updateSuggestions()
        refreshSuggestionArea()
    }

    // Update only suggestion row
    private fun refreshSuggestionArea() {
        val area = suggestionArea ?: return

        if (clipboardOpen) return

        area.removeAllViews()

        val quickReplies = getQuickReplyResults()

        if (quickReplies.isNotEmpty()) {
            quickReplies.take(3).forEach { reply ->
                val item = TextView(this).apply {
                    text = reply.code
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setTextColor(Color.BLACK)
                    setPadding(dp(8), dp(5), dp(8), dp(5))
                    background = roundedBackground(Color.WHITE)

                    setOnClickListener {
                        val before = currentInputConnection
                            ?.getTextBeforeCursor(200, 0)
                            ?.toString() ?: ""

                        val slashIndex = before.lastIndexOf('\\')

                        if (slashIndex >= 0) {
                            val deleteLength = before.length - slashIndex

                            currentInputConnection
                                ?.deleteSurroundingText(deleteLength, 0)

                            currentInputConnection
                                ?.commitText(reply.message, 1)
                        }

                        suggestions = emptyList()
                        updateSuggestions()
                        refreshSuggestionArea()
                    }
                }

                area.addView(
                    item,
                    LinearLayout.LayoutParams(0, dp(36), 1f)
                )
            }

            return
        }

        suggestions.take(3).forEach { word ->
            val item = TextView(this).apply {
                text = word
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                setPadding(dp(5), dp(4), dp(5), dp(4))

                setOnClickListener {
                    val before = currentInputConnection
                        ?.getTextBeforeCursor(100, 0)
                        ?.toString() ?: ""

                    val currentWord = before.takeLastWhile {
                        !it.isWhitespace()
                    }

                    if (currentWord.isNotEmpty()) {
                        currentInputConnection
                            ?.deleteSurroundingText(currentWord.length, 0)

                        currentInputConnection
                            ?.commitText("$word ", 1)
                    }

                    suggestions = emptyList()
                    updateSuggestions()
                    refreshSuggestionArea()
                }
            }

            area.addView(
                item,
                LinearLayout.LayoutParams(0, dp(36), 1f)
            )
        }
    }

    // Spell checker
    private fun updateSuggestions() {
        val before = currentInputConnection
            ?.getTextBeforeCursor(100, 0)
            ?.toString() ?: ""

        val word = before.takeLastWhile {
            !it.isWhitespace()
        }

        if (word.length < 2 || word.startsWith("\\")) {
            suggestions = emptyList()
            refreshSuggestionArea()
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

    override fun onGetSuggestions(results: Array<SuggestionsInfo>) {
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
            if (root == null || clipboardOpen) return@post

            suggestions = found.distinct().take(3)
            refreshSuggestionArea()
        }
    }

    override fun onGetSentenceSuggestions(
        results: Array<SentenceSuggestionsInfo>
    ) {
        // Not used
    }

    private fun roundedBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(9).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        stopDeleting()
        suggestionArea = null
        root = null

        spellCheckerSession?.close()
        spellCheckerSession = null

        super.onDestroy()
    }
}

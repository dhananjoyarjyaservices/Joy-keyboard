package com.joykeyboard.app

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class JoyKeyboardService : InputMethodService() {

    private var caps = false
    private var symbols = false
    private var clipboardOpen = false
    private var backspacePressed = false

    private var root: LinearLayout? = null
    private var suggestionArea: LinearLayout? = null

    private val handler = Handler(Looper.getMainLooper())

    private var deleteRunnable: Runnable? = null
    private var quickReplyRunnable: Runnable? = null
    private var keyboardBounceRunnable: Runnable? = null

    private val prefs by lazy {
        getSharedPreferences("joy_keyboard_settings", MODE_PRIVATE)
    }

    private val toneGenerator by lazy {
        ToneGenerator(AudioManager.STREAM_SYSTEM, 35)
    }

    private fun playKeySound() {
        if (!prefs.getBoolean("keyboard_sound", true)) return

        try {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 45)
        } catch (_: Exception) {
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

        stopDeleting()
        cancelQuickReplyRefresh()
        cancelKeyboardBounce()

        layout.removeAllViews()
        suggestionArea = null
        layout.translationY = 0f

        val size = prefs.getInt("keyboard_size", 50).coerceIn(0, 100)
        val keyHeight = dp(40 + size * 30 / 100)

        val toolbar = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }

        addTool(toolbar, "▦") {
            clipboardOpen = !clipboardOpen
            if (clipboardOpen) captureCurrentClipboard()
            renderKeyboard()
        }

        addTool(toolbar, "💬") {
            startActivity(
                Intent(this, QuickReplyActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        addTool(toolbar, "⇧") {
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

        if (clipboardOpen) {
            renderClipboardPanel(layout)
            return
        }

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

        if (!symbols) {
            addRow(layout, listOf("1","2","3","4","5","6","7","8","9","0"), keyHeight)
            addRow(layout, listOf("q","w","e","r","t","y","u","i","o","p"), keyHeight)
            addRow(layout, listOf("a","s","d","f","g","h","j","k","l"), keyHeight)

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
            addRow(layout, listOf("@","#","₹","$","%","&","-","_","(",")"), keyHeight)
            addRow(layout, listOf("+","=","/","*","\"","'",":",";","!","?"), keyHeight)
            addRow(layout, listOf("¥","×","•","<",">","[","]","{","}","\\"), keyHeight)
            addRow(layout, listOf("~","`","^","°","©","®","™","€","£","|"), keyHeight)
        }

        val bottom = LinearLayout(this)

        addKey(bottom, if (symbols) "ABC" else "?123", 1.4f, keyHeight) {
            symbols = !symbols
            renderKeyboard()
        }

        addKey(bottom, ",", 0.7f, keyHeight) { commit(",") }
        addKey(bottom, "\\", 0.7f, keyHeight) { commit("\\") }
        addKey(bottom, "SPACE", 3.5f, keyHeight) { commit(" ") }
        addKey(bottom, ".", 0.7f, keyHeight) { commit(".") }

        addBackspaceKey(bottom, 1.1f, keyHeight)

        addKey(bottom, "↵", 1.1f, keyHeight) {
            stopDeleting()
            playKeySound()

            try {
                currentInputConnection?.commitText("\n", 1)
                scheduleKeyboardBounce()
            } catch (_: Exception) {
            }

            clearQuickReplyArea()
        }

        layout.addView(bottom)
    }

    // Smooth keyboard bounce after typing
    private fun scheduleKeyboardBounce() {
        cancelKeyboardBounce()

        val task = Runnable {
            val keyboard = root ?: return@Runnable

            keyboard.animate().cancel()
            keyboard.translationY = 0f

            keyboard.animate()
                .translationY(-dp(4).toFloat())
                .setDuration(120)
                .withEndAction {
                    keyboard.animate()
                        .translationY(0f)
                        .setDuration(140)
                        .start()
                }
                .start()
        }

        keyboardBounceRunnable = task
        handler.postDelayed(task, 500)
    }

    private fun cancelKeyboardBounce() {
        keyboardBounceRunnable?.let {
            handler.removeCallbacks(it)
        }

        keyboardBounceRunnable = null

        root?.animate()?.cancel()
        root?.translationY = 0f
    }

    private fun commit(value: String) {
        stopDeleting()

        try {
            val connection = currentInputConnection ?: return
            connection.commitText(value, 1)

            scheduleQuickReplyRefresh()
            scheduleKeyboardBounce()
        } catch (_: Exception) {
        }
    }

    private fun scheduleQuickReplyRefresh() {
        cancelQuickReplyRefresh()

        val task = Runnable {
            if (root != null && !clipboardOpen) {
                refreshQuickReplies()
            }
        }

        quickReplyRunnable = task
        handler.postDelayed(task, 300)
    }

    private fun cancelQuickReplyRefresh() {
        quickReplyRunnable?.let {
            handler.removeCallbacks(it)
        }

        quickReplyRunnable = null
    }

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
        } catch (_: Exception) {
        }
    }

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

        header.addView(title, LinearLayout.LayoutParams(0, dp(42), 1f))

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
                    stopDeleting()

                    try {
                        currentInputConnection?.commitText(item, 1)
                        clipboardOpen = false
                        renderKeyboard()
                    } catch (_: Exception) {
                    }
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

    private fun refreshQuickReplies() {
        val area = suggestionArea ?: return
        if (clipboardOpen) return

        val before = try {
            currentInputConnection
                ?.getTextBeforeCursor(100, 0)
                ?.toString() ?: ""
        } catch (_: Exception) {
            ""
        }

        val slashIndex = before.lastIndexOf('\\')

        if (slashIndex < 0) {
            clearQuickReplyArea()
            return
        }

        val query = before.substring(slashIndex + 1)

        val replies = try {
            QuickReplyStore(this).search(query).take(3)
        } catch (_: Exception) {
            emptyList()
        }

        area.removeAllViews()

        replies.forEach { reply ->
            val item = TextView(this).apply {
                text = reply.code
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                setPadding(dp(8), dp(5), dp(8), dp(5))
                background = roundedBackground(Color.WHITE)

                setOnClickListener {
                    stopDeleting()

                    try {
                        val connection = currentInputConnection
                            ?: return@setOnClickListener

                        val textBefore = connection
                            .getTextBeforeCursor(200, 0)
                            ?.toString() ?: ""

                        val index = textBefore.lastIndexOf('\\')

                        if (index >= 0) {
                            val deleteLength = textBefore.length - index
                            connection.deleteSurroundingText(deleteLength, 0)
                            connection.commitText(reply.message, 1)
                        }

                        clearQuickReplyArea()
                        scheduleKeyboardBounce()
                    } catch (_: Exception) {
                    }
                }
            }

            area.addView(item, LinearLayout.LayoutParams(0, dp(36), 1f))
        }
    }

    private fun clearQuickReplyArea() {
        suggestionArea?.removeAllViews()
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
            setOnClickListener { action() }
        }

        row.addView(button, LinearLayout.LayoutParams(0, dp(42), 1f))
    }

    private fun animateKeyPress(key: View) {
        key.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    stopDeleting()

                    view.animate()
                        .translationY(-dp(5).toFloat())
                        .scaleX(0.96f)
                        .scaleY(0.96f)
                        .setDuration(100)
                        .start()
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    view.animate()
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
            }

            false
        }
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
                stopDeleting()
                playKeySound()
                action()
            }
        }

        animateKeyPress(key)

        val params = LinearLayout.LayoutParams(0, height, weight)
        params.setMargins(dp(3), dp(4), dp(3), dp(4))
        row.addView(key, params)
    }

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

            isClickable = true
            isFocusable = false

            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        stopDeleting()
                        backspacePressed = true

                        // Selected text gets deleted first
                        deleteOneCharacter()

                        view.animate()
                            .translationY(-dp(5).toFloat())
                            .scaleX(0.96f)
                            .scaleY(0.96f)
                            .setDuration(100)
                            .start()

                        val repeatTask = object : Runnable {
                            override fun run() {
                                if (!backspacePressed) return

                                deleteOneCharacter()
                                handler.postDelayed(this, 75)
                            }
                        }

                        deleteRunnable = repeatTask
                        handler.postDelayed(repeatTask, 400)

                        true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                    MotionEvent.ACTION_OUTSIDE,
                    MotionEvent.ACTION_POINTER_UP -> {
                        stopDeleting()

                        view.animate()
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()

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

    // Delete selected text, otherwise delete one character
    private fun deleteOneCharacter() {
        try {
            val connection = currentInputConnection ?: return
            playKeySound()

            val selectedText = connection.getSelectedText(0)

            if (!selectedText.isNullOrEmpty()) {
                connection.commitText("", 1)
            } else {
                connection.deleteSurroundingText(1, 0)
            }

            scheduleQuickReplyRefresh()
            scheduleKeyboardBounce()

        } catch (_: Exception) {
        }
    }

    private fun stopDeleting() {
        backspacePressed = false

        deleteRunnable?.let {
            handler.removeCallbacks(it)
        }

        deleteRunnable = null
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

    override fun onFinishInput() {
        stopDeleting()
        cancelQuickReplyRefresh()
        cancelKeyboardBounce()
        super.onFinishInput()
    }

    override fun onDestroy() {
        stopDeleting()
        cancelQuickReplyRefresh()
        cancelKeyboardBounce()
        handler.removeCallbacksAndMessages(null)

        suggestionArea = null
        root = null

        try {
            toneGenerator.release()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }
}

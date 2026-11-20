package com.joykeyboard.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class QuickReplyAccessibilityService : AccessibilityService() {

    private var panel: View? = null
    private var targetNode: AccessibilityNodeInfo? = null
    private var currentText = ""
    private var inserting = false
    private val store by lazy { QuickReplyStore(this) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || inserting) return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED) return

        val node = event.source ?: return
        if (!node.isEditable || node.isPassword) {
            hidePanel()
            return
        }

        val text = node.text?.toString() ?: ""
        val slash = text.lastIndexOf('/')
        if (slash < 0) {
            hidePanel()
            return
        }

        targetNode?.recycle()
        targetNode = AccessibilityNodeInfo.obtain(node)
        currentText = text

        val query = text.substring(slash + 1)
        val matches = store.search(query).take(3)
        if (matches.isEmpty()) {
            hidePanel()
            return
        }
        showPanel(matches)
    }

    // Compact one-line suggestion strip. It must not cover the keyboard keys.
    private fun showPanel(matches: List<QuickReply>) {
        hidePanel()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(Color.rgb(250, 255, 251))
            elevation = dp(8).toFloat()
        }

        val brand = TextView(this).apply {
            text = "Joy"
            textSize = 13f
            setTextColor(Color.rgb(20, 110, 65))
            setPadding(dp(5), 0, dp(8), 0)
        }
        container.addView(brand)

        matches.forEach { reply ->
            val item = TextView(this).apply {
                text = "/${reply.code}"
                textSize = 14f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                setTextColor(Color.DKGRAY)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setBackgroundColor(Color.WHITE)
                setOnClickListener { insertReply(reply) }
            }
            val params = LinearLayout.LayoutParams(0, dp(42), 1f)
            params.setMargins(dp(3), 0, dp(3), 0)
            container.addView(item, params)
        }

        val close = TextView(this).apply {
            text = "×"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { hidePanel() }
        }
        container.addView(close)

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(54),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Gboard's suggestion-strip area, just above the letter keys.
            gravity = Gravity.BOTTOM
            y = dp(285)
        }

        try {
            wm.addView(container, params)
            panel = container
        } catch (_: Exception) {
            panel = null
        }
    }

    private fun insertReply(reply: QuickReply) {
        val node = targetNode ?: return
        val slash = currentText.lastIndexOf('/')
        if (slash < 0) return

        val replacement = currentText.substring(0, slash) + reply.message
        inserting = true
        try {
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    replacement
                )
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } catch (_: Exception) {
        } finally {
            inserting = false
            hidePanel()
        }
    }

    private fun hidePanel() {
        val view = panel ?: return
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.removeView(view)
        } catch (_: Exception) {
        }
        panel = null
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onInterrupt() {
        hidePanel()
    }

    override fun onDestroy() {
        hidePanel()
        targetNode?.recycle()
        targetNode = null
        super.onDestroy()
    }
}
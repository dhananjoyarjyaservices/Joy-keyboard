package com.joykeyboard.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
        val matches = store.search(query).take(5)
        if (matches.isEmpty()) {
            hidePanel()
            return
        }
        showPanel(matches)
    }

    private fun showPanel(matches: List<QuickReply>) {
        hidePanel()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(250, 255, 251))
            elevation = dp(12).toFloat()
        }

        val heading = TextView(this).apply {
            text = "Joy Quick Reply"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(20, 110, 65))
            setPadding(dp(8), dp(4), dp(8), dp(8))
        }
        container.addView(heading)

        matches.forEach { reply ->
            val item = TextView(this).apply {
                text = "/${reply.code}   ·   ${reply.message.take(70)}"
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(dp(10), dp(12), dp(10), dp(12))
                setBackgroundColor(Color.WHITE)
                setOnClickListener { insertReply(reply) }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dp(5)
            container.addView(item, params)
        }

        val close = TextView(this).apply {
            text = "Close"
            textSize = 13f
            gravity = Gravity.END
            setTextColor(Color.GRAY)
            setPadding(dp(8), dp(8), dp(8), dp(4))
            setOnClickListener { hidePanel() }
        }
        container.addView(close)

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = dp(8)
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

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

                    // Immediately delete one character
                    deleteOneCharacter()

                    // Press animation
                    view.animate()
                        .translationY(-dp(5).toFloat())
                        .scaleX(0.96f)
                        .scaleY(0.96f)
                        .setDuration(100)
                        .start()

                    // Start continuous deletion after 400ms
                    val repeatTask = object : Runnable {
                        override fun run() {
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
                MotionEvent.ACTION_OUTSIDE -> {
                    // Stop deleting when finger is released
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

private fun deleteOneCharacter() {
    try {
        currentInputConnection?.deleteSurroundingText(1, 0)
        scheduleQuickReplyRefresh()
    } catch (_: Exception) {
    }
}

private fun stopDeleting() {
    deleteRunnable?.let {
        handler.removeCallbacks(it)
    }
    deleteRunnable = null
}

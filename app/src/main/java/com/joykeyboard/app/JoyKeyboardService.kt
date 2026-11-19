private fun animateKeyPress(key: View) {
    key.setOnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                stopDeleting()

                view.animate().cancel()
                view.animate()
                    .translationY(-dp(5).toFloat())
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(100)
                    .start()
            }

            MotionEvent.ACTION_UP -> {
                view.animate()
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction {
                        view.postDelayed({
                            if (!view.isAttachedToWindow) return@postDelayed

                            view.animate()
                                .translationY(-dp(7).toFloat())
                                .setDuration(130)
                                .withEndAction {
                                    view.animate()
                                        .translationY(0f)
                                        .setDuration(150)
                                        .start()
                                }
                                .start()
                        }, 500)
                    }
                    .start()
            }

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

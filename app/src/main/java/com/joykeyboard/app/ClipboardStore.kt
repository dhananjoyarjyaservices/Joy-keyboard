
package com.joykeyboard.app

import android.content.Context
import org.json.JSONArray

class ClipboardStore(context: Context) {

    private val prefs = context.getSharedPreferences(
        "joy_clipboard_history",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_HISTORY = "items"
        private const val MAX_ITEMS = 50
    }

    fun getAll(): List<String> {
        return try {
            val json = JSONArray(
                prefs.getString(KEY_HISTORY, "[]")
            )

            (0 until json.length()).mapNotNull {
                json.optString(it).takeIf { value ->
                    value.isNotBlank()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(text: String) {
        val value = text.trim()
        if (value.isBlank()) return

        val updated = getAll()
            .filterNot { it == value }
            .toMutableList()

        updated.add(0, value)

        val json = JSONArray()
        updated.take(MAX_ITEMS).forEach {
            json.put(it)
        }

        prefs.edit()
            .putString(KEY_HISTORY, json.toString())
            .apply()
    }

    fun delete(text: String) {
        val json = JSONArray()
        getAll()
            .filterNot { it == text }
            .forEach { json.put(it) }

        prefs.edit()
            .putString(KEY_HISTORY, json.toString())
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_HISTORY)
            .apply()
    }
}

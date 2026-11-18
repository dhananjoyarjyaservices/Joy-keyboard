
package com.joykeyboard.app

import android.content.Context
import org.json.JSONArray

class ClipboardStore(context: Context) {

    private val prefs = context.getSharedPreferences(
        "joy_clipboard",
        Context.MODE_PRIVATE
    )

    fun getAll(): List<String> {
        val json = prefs.getString("history", "[]") ?: "[]"

        return try {
            val array = JSONArray(json)
            (0 until array.length()).map {
                array.getString(it)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(text: String) {
        if (text.isBlank()) return

        val history = getAll().toMutableList()

        history.remove(text)
        history.add(0, text)

        val array = JSONArray()

        history.take(100).forEach {
            array.put(it)
        }

        prefs.edit()
            .putString("history", array.toString())
            .apply()
    }

    fun delete(text: String) {
        val history = getAll().filterNot {
            it == text
        }

        val array = JSONArray()

        history.forEach {
            array.put(it)
        }

        prefs.edit()
            .putString("history", array.toString())
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove("history")
            .apply()
    }
}

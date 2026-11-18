
package com.joykeyboard.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class QuickReply(
    val code: String,
    val message: String
)

class QuickReplyStore(context: Context) {

    private val prefs = context.getSharedPreferences(
        "joy_quick_replies",
        Context.MODE_PRIVATE
    )

    private val key = "replies"

    fun getAll(): List<QuickReply> {
        val result = mutableListOf<QuickReply>()

        try {
            val json = JSONArray(prefs.getString(key, "[]"))

            for (i in 0 until json.length()) {
                val item = json.optJSONObject(i) ?: continue

                val code = item.optString("code", "")
                val message = item.optString("message", "")

                if (code.isNotBlank()) {
                    result.add(
                        QuickReply(
                            code = code,
                            message = message
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    fun save(reply: QuickReply) {
        val code = reply.code.trim().removePrefix("\\")
        val message = reply.message.trim()

        if (code.isBlank() || message.isBlank()) return

        val replies = getAll().toMutableList()

        // Update existing shortcut or add new one
        val index = replies.indexOfFirst {
            it.code.equals(code, ignoreCase = true)
        }

        val newReply = QuickReply(code, message)

        if (index >= 0) {
            replies[index] = newReply
        } else {
            replies.add(newReply)
        }

        saveList(replies)
    }

    fun delete(code: String) {
        val replies = getAll().filterNot {
            it.code.equals(
                code.trim().removePrefix("\\"),
                ignoreCase = true
            )
        }

        saveList(replies)
    }

    fun search(query: String): List<QuickReply> {
        val searchText = query.trim().removePrefix("\\")

        if (searchText.isBlank()) {
            return getAll()
        }

        return getAll().filter {
            it.code.contains(searchText, ignoreCase = true) ||
            it.message.contains(searchText, ignoreCase = true)
        }
    }

    private fun saveList(replies: List<QuickReply>) {
        val json = JSONArray()

        replies.forEach { reply ->
            val item = JSONObject().apply {
                put("code", reply.code)
                put("message", reply.message)
            }

            json.put(item)
        }

        prefs.edit()
            .putString(key, json.toString())
            .apply()
    }
}

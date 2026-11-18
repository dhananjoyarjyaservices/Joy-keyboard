
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

    fun getAll(): List<QuickReply> {
        val json = prefs.getString("replies", "[]")
            ?: "[]"

        val result = mutableListOf<QuickReply>()

        try {
            val array = JSONArray(json)

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                result.add(
                    QuickReply(
                        code = item.getString("code"),
                        message = item.getString("message")
                    )
                )
            }
        } catch (_: Exception) {
        }

        return result
    }

    fun save(code: String, message: String) {
        val replies = getAll().toMutableList()

        replies.removeAll {
            it.code.equals(code.trim(), ignoreCase = true)
        }

        replies.add(
            QuickReply(
                code.trim(),
                message
            )
        )

        val array = JSONArray()

        replies.forEach {
            val item = JSONObject()
            item.put("code", it.code)
            item.put("message", it.message)
            array.put(item)
        }

        prefs.edit()
            .putString("replies", array.toString())
            .apply()
    }

    fun delete(code: String) {
        val replies = getAll().filterNot {
            it.code.equals(code, ignoreCase = true)
        }

        val array = JSONArray()

        replies.forEach {
            val item = JSONObject()
            item.put("code", it.code)
            item.put("message", it.message)
            array.put(item)
        }

        prefs.edit()
            .putString("replies", array.toString())
            .apply()
    }

    fun search(query: String): List<QuickReply> {
        return getAll().filter {
            it.code.contains(query, ignoreCase = true)
        }
    }
}

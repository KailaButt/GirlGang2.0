package com.example.consolicalm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Favorites + simple Calm history.
 * Kept lightweight (SharedPreferences + JSON) to match the project's current persistence approach.
 */

data class CalmHistoryEntry(
    val timestampMillis: Long,
    val title: String,
    val minutes: Int,
    /** 1–5 rating captured after a Calm session (0 means not rated / legacy entries). */
    val rating: Int = 0,
    /** Optional one-line note captured after a Calm session. */
    val note: String = ""
)

class CalmToolkitPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("calm_toolkit_prefs", Context.MODE_PRIVATE)

    // ---- Favorites ----
    fun getFavorites(): Set<String> = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()

    fun isFavorite(key: String): Boolean = getFavorites().contains(key)

    fun toggleFavorite(key: String) {
        val current = getFavorites().toMutableSet()
        if (current.contains(key)) current.remove(key) else current.add(key)
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    // ---- History ----
    fun addHistoryEntry(entry: CalmHistoryEntry, maxEntries: Int = 50) {
        val current = getHistoryEntries().toMutableList()
        current.add(0, entry) // newest first
        val trimmed = current.take(maxEntries)
        prefs.edit().putString(KEY_HISTORY_JSON, encodeHistory(trimmed)).apply()
    }

    fun getHistoryEntries(): List<CalmHistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        return decodeHistory(raw)
    }

    private fun encodeHistory(list: List<CalmHistoryEntry>): String {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject()
                    .put("ts", e.timestampMillis)
                    .put("title", e.title)
                    .put("min", e.minutes)
                    .put("rating", e.rating)
                    .put("note", e.note)
            )
        }
        return arr.toString()
    }

    private fun decodeHistory(json: String): List<CalmHistoryEntry> {
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        CalmHistoryEntry(
                            timestampMillis = obj.optLong("ts", 0L),
                            title = obj.optString("title", ""),
                            minutes = obj.optInt("min", 0),
                            rating = obj.optInt("rating", 0),
                            note = obj.optString("note", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_HISTORY_JSON = "history_json"
    }
}

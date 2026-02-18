package com.example.consolicalm

import android.content.Context
import java.util.Calendar

/**
 * Lightweight persistence for Calm tab (streak, daily goal, challenge completion).
 * Uses SharedPreferences to stay simple for a capstone project.
 */
class CalmPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("calm_prefs", Context.MODE_PRIVATE)

    var streakCount: Int
        get() = prefs.getInt(KEY_STREAK_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_STREAK_COUNT, value).apply()

    var lastCompletionKey: Int
        get() = prefs.getInt(KEY_LAST_COMPLETION, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_COMPLETION, value).apply()

    var todaySessionsKey: Int
        get() = prefs.getInt(KEY_TODAY_SESSIONS_KEY, 0)
        set(value) = prefs.edit().putInt(KEY_TODAY_SESSIONS_KEY, value).apply()

    var todaySessionsCount: Int
        get() = prefs.getInt(KEY_TODAY_SESSIONS_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_TODAY_SESSIONS_COUNT, value).apply()

    var challengeCompletedKey: Int
        get() = prefs.getInt(KEY_CHALLENGE_COMPLETED, 0)
        set(value) = prefs.edit().putInt(KEY_CHALLENGE_COMPLETED, value).apply()

    fun getTodayKey(): Int = dateKey(Calendar.getInstance())

    fun getYesterdayKey(): Int {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateKey(cal)
    }

    /**
     * Update streak + daily counts when the user finishes any Calm session.
     */
    fun recordSessionCompleted() {
        val today = getTodayKey()
        val yesterday = getYesterdayKey()

        // Update streak
        when {
            lastCompletionKey == today -> {
                // already counted today
            }
            lastCompletionKey == yesterday -> {
                streakCount = (streakCount.coerceAtLeast(1)) + 1
                lastCompletionKey = today
            }
            else -> {
                streakCount = 1
                lastCompletionKey = today
            }
        }

        // Update today's count
        if (todaySessionsKey != today) {
            todaySessionsKey = today
            todaySessionsCount = 0
        }
        todaySessionsCount += 1
    }

    companion object {
        private const val KEY_STREAK_COUNT = "streak_count"
        private const val KEY_LAST_COMPLETION = "last_completion_key"
        private const val KEY_TODAY_SESSIONS_KEY = "today_sessions_key"
        private const val KEY_TODAY_SESSIONS_COUNT = "today_sessions_count"
        private const val KEY_CHALLENGE_COMPLETED = "challenge_completed_key"
    }
}

/**
 * Stable day key: YYYYMMDD (local time).
 */
fun dateKey(cal: Calendar): Int {
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return (y * 10000) + (m * 100) + d
}

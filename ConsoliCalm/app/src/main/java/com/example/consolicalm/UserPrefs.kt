package com.example.consolicalm

import android.content.Context

class UserPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var nickname: String?
        get() = prefs.getString("nickname", null)
        set(value) {
            prefs.edit().putString("nickname", value).apply()
        }
}
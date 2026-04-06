package com.example.consolicalm

data class WeeklyInsight(
    val uid: String = "",
    val nickname: String = "",
    val pointsEarned: Int = 0,
    val studySessions: Int = 0,
    val tasksCompleted: Int = 0,
    val calmMinutes: Int = 0,
    val moodCheckIns: Int = 0,
    val weekId: String = "",
    val updatedAt: Long = 0L
)
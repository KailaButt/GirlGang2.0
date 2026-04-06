package com.example.consolicalm

import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

fun currentWeekId(): String {
    val today = LocalDate.now()
    val weekFields = WeekFields.of(Locale.getDefault())
    val week = today.get(weekFields.weekOfWeekBasedYear())
    val year = today.get(weekFields.weekBasedYear())
    return "%04d-W%02d".format(year, week)
}
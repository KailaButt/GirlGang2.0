package com.example.consolicalm

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.consolicalm.ui.theme.PacificoFont
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

data class CalmMoodOption(
    val emoji: String,
    val label: String,
    val tint: Color
)

private val CALM_OPTIONS: List<CalmMoodOption> = listOf(
    CalmMoodOption("😊", "Happy",      Color(0xFFB9CBC4)),
    CalmMoodOption("😌", "Calm",       Color(0xFF9EC4B8)),
    CalmMoodOption("😐", "Neutral",    Color(0xFFCDCDC9)),
    CalmMoodOption("😔", "Low",        Color(0xFFB5B8C4)),
    CalmMoodOption("😰", "Anxious",    Color(0xFFCDB8A8)),
    CalmMoodOption("😤", "Frustrated", Color(0xFFCDB3B3)),
    CalmMoodOption("😴", "Tired",      Color(0xFFC4C0CB)),
    CalmMoodOption("🤩", "Excited",    Color(0xFFCEC4A8))
)

data class CalendarDay(val year: Int, val month: Int, val day: Int) {
    // Used only for display in the dialog
    fun toDisplayString(): String {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day)
        val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
        val dayOfWeek = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: ""
        return "$dayOfWeek, $monthName $day, $year"
    }

    // Used for storage keys and equality — never changes
    fun toKey(): String = "%04d-%02d-%02d".format(year, month, day)
}

data class CalendarMonth(val year: Int, val month: Int) {

    fun lengthOfMonth(): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun firstDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        // DAY_OF_WEEK: 1=Sun, 2=Mon ... 7=Sat  →  subtract 1 for 0-based column index
        return cal.get(Calendar.DAY_OF_WEEK) - 1
    }

    fun atDay(d: Int): CalendarDay = CalendarDay(year, month, d)

    fun plusMonths(n: Int): CalendarMonth {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        cal.add(Calendar.MONTH, n)
        return CalendarMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun minusMonths(n: Int): CalendarMonth = plusMonths(-n)

    fun displayName(): String {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        val name = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
        return "$name $year"
    }

    companion object {
        fun now(): CalendarMonth {
            val cal = Calendar.getInstance()
            return CalendarMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
    }
}

fun todayAsCalendarDay(): CalendarDay {
    val cal = Calendar.getInstance()
    return CalendarDay(
        year  = cal.get(Calendar.YEAR),
        month = cal.get(Calendar.MONTH) + 1,
        day   = cal.get(Calendar.DAY_OF_MONTH)
    )
}

private fun moodKey(date: CalendarDay): String = "mood_${date.toKey()}"

fun saveCalmMoodEntry(
    context: Context,
    date: CalendarDay,
    emoji: String,
    label: String,
    note: String
) {
    val prefs = context.getSharedPreferences("calm_toolkit_prefs", Context.MODE_PRIVATE)
    val json = JSONObject()
        .put("emoji", emoji)
        .put("label", label)
        .put("note",  note)
        .toString()
    prefs.edit().putString(moodKey(date), json).apply()
}

fun loadCalmMoodEntry(
    context: Context,
    date: CalendarDay
): Triple<String, String, String>? {
    val prefs = context.getSharedPreferences("calm_toolkit_prefs", Context.MODE_PRIVATE)
    val raw   = prefs.getString(moodKey(date), null) ?: return null
    return try {
        val obj = JSONObject(raw)
        Triple(
            obj.optString("emoji", ""),
            obj.optString("label", ""),
            obj.optString("note",  "")
        )
    } catch (_: Exception) {
        null
    }
}

@Composable
fun EmojiCalendarScreen() {
    val context  = LocalContext.current
    val sage     = Color(0xFFB9CBC4)
    val slate    = Color(0xFF7B969F)
    val charcoal = Color(0xFF4A5568)
    val cream    = Color(0xFFF8F1EB)

    var currentMonth by remember { mutableStateOf(CalendarMonth.now()) }
    var selectedDate by remember { mutableStateOf<CalendarDay?>(null) }
    var showPicker   by remember { mutableStateOf(false) }
    var entriesCache by remember {
        mutableStateOf<Map<CalendarDay, Triple<String, String, String>>>(emptyMap())
    }

    // Fresh device clock every recomposition — never stale
    val today = todayAsCalendarDay()

    fun refreshCache() {
        val map = mutableMapOf<CalendarDay, Triple<String, String, String>>()
        for (d in 1..currentMonth.lengthOfMonth()) {
            val date = currentMonth.atDay(d)
            loadCalmMoodEntry(context, date)?.let { entry -> map[date] = entry }
        }
        entriesCache = map
    }

    LaunchedEffect(currentMonth) { refreshCache() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cream)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Text("‹", fontSize = 28.sp, color = slate)
            }
            Text(
                text       = currentMonth.displayName(),
                fontFamily = PacificoFont,
                fontSize   = 20.sp,
                color      = charcoal
            )
            TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Text("›", fontSize = 28.sp, color = slate)
            }
        }

        Text(
            text     = "Tap any day to log your mood ✨",
            style    = MaterialTheme.typography.labelSmall,
            color    = slate.copy(alpha = 0.75f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { label ->
                Text(
                    text       = label,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = slate,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val firstOffset = currentMonth.firstDayOfWeek()
        val daysInMonth = currentMonth.lengthOfMonth()

        LazyVerticalGrid(
            columns               = GridCells.Fixed(7),
            modifier              = Modifier.fillMaxWidth(),
            verticalArrangement   = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(firstOffset) {
                Box(modifier = Modifier.aspectRatio(1f))
            }
            items(daysInMonth) { idx ->
                val dayNum  = idx + 1
                val date    = currentMonth.atDay(dayNum)
                val entry   = entriesCache[date]
                val isToday = date == today
                val tint: Color? = entry?.let { (storedEmoji, _, _) ->
                    CALM_OPTIONS.firstOrNull { opt -> opt.emoji == storedEmoji }?.tint
                }

                CalendarDayCell(
                    day     = dayNum,
                    isToday = isToday,
                    emoji   = entry?.first,
                    tint    = tint,
                    sage    = sage,
                    slate   = slate,
                    onClick = {
                        selectedDate = date
                        showPicker   = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            CALM_OPTIONS.forEach { opt ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 3.dp)
                ) {
                    Text(text = opt.emoji, fontSize = 15.sp)
                    Text(
                        text     = opt.label,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = slate.copy(alpha = 0.7f),
                        fontSize = 8.sp
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showPicker && selectedDate != null,
        enter   = fadeIn() + slideInVertically { it / 2 },
        exit    = fadeOut() + slideOutVertically { it / 2 }
    ) {
        selectedDate?.let { date ->
            MoodPickerDialog(
                date      = date,
                isToday   = date == today,
                existing  = entriesCache[date],
                slate     = slate,
                onDismiss = { showPicker = false },
                onSave    = { emoji, label, note ->
                    saveCalmMoodEntry(context, date, emoji, label, note)
                    refreshCache()
                    showPicker = false
                }
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    emoji: String?,
    tint: Color?,
    sage: Color,
    slate: Color,
    onClick: () -> Unit
) {
    val bg = when {
        tint != null -> tint.copy(alpha = 0.55f)
        isToday      -> sage.copy(alpha = 0.4f)
        else         -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(
                if (isToday) Modifier.border(2.dp, sage, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (emoji != null) {
            Text(text = emoji, fontSize = 20.sp)
        } else if (isToday) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = day.toString(),
                    style      = MaterialTheme.typography.bodySmall,
                    color      = sage,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(text = "＋", fontSize = 10.sp, color = sage.copy(alpha = 0.8f))
            }
        } else {
            Text(
                text       = day.toString(),
                style      = MaterialTheme.typography.bodySmall,
                color      = slate.copy(alpha = 0.7f),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun MoodPickerDialog(
    date: CalendarDay,
    isToday: Boolean,
    existing: Triple<String, String, String>?,
    slate: Color,
    onDismiss: () -> Unit,
    onSave: (emoji: String, label: String, note: String) -> Unit
) {
    val cream = Color(0xFFF8F1EB)

    var selected by remember {
        mutableStateOf<CalmMoodOption?>(
            existing?.let { (existingEmoji, _, _) ->
                CALM_OPTIONS.firstOrNull { opt -> opt.emoji == existingEmoji }
            }
        )
    }
    var note by remember { mutableStateOf(existing?.third ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape          = RoundedCornerShape(20.dp),
            color          = cream,
            tonalElevation = 4.dp,
            modifier       = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier            = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text       = if (isToday) "How are you feeling today?" else "How were you feeling?",
                    fontFamily = PacificoFont,
                    fontSize   = 18.sp,
                    color      = slate
                )
                Text(
                    text  = if (isToday) "Today  •  ${date.toDisplayString()}" else date.toDisplayString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = slate.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns               = GridCells.Fixed(4),
                    modifier              = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CALM_OPTIONS.size) { i ->
                        val opt    = CALM_OPTIONS[i]
                        val chosen = selected?.emoji == opt.emoji
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (chosen) opt.tint.copy(alpha = 0.7f)
                                    else opt.tint.copy(alpha = 0.25f)
                                )
                                .border(
                                    width = if (chosen) 2.dp else 0.dp,
                                    color = if (chosen) slate else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selected = opt }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(text = opt.emoji, fontSize = 24.sp)
                            Text(
                                text      = opt.label,
                                style     = MaterialTheme.typography.labelSmall,
                                color     = slate,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it },
                    placeholder   = {
                        Text(
                            text  = "Add a note (optional)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth(),
                    shape      = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            val opt = selected
                            if (opt != null) onSave(opt.emoji, opt.label, note)
                        },
                        enabled  = selected != null,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = slate)
                    ) { Text("Save") }
                }
            }
        }
    }
}
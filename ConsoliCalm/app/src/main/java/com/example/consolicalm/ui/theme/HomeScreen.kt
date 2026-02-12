package com.example.consolicalm.com.example.consolicalm.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

data class MoodEntry(
    val date: Date,
    val mood: String,
    val note: String = ""
)

@Composable
fun HomeScreen(
    calmPoints: Int,
    nextRewardGoal: Int,
    onRewardsClick: () -> Unit,
    onTodoClick: () -> Unit,
    onMeditationClick: () -> Unit,
    onStudyClick: () -> Unit,
    onEarnPoints: (Int) -> Unit
) {
    var showMoodDialog by remember { mutableStateOf(false) }
    val moodEntries = remember { mutableStateListOf<MoodEntry>() }
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    // Initialize with sample data for the past 30 days
    LaunchedEffect(Unit) {
        if (moodEntries.isEmpty()) {
            val moods = listOf("😊", "😊", "😐", "😊", "😴", "😊", "😔", "😊", "😐", "😤",
                "😊", "😐", "😊", "😴", "😊", "😐", "😊", "😔", "😊", "😐",
                "😊", "😴", "😊", "😤", "😊", "😐", "😊", "😔", "😊", "😐")
            for (i in 0 until 30) {
                val cal = Calendar.getInstance().apply {
                    time = today
                    add(Calendar.DAY_OF_YEAR, -(29 - i))
                }
                moodEntries.add(MoodEntry(cal.time, moods[i]))
            }
        }
    }

    val todaysMoodEntry = moodEntries.find { isSameDay(it.date, today) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Calm Points
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Your Calm Points", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "$calmPoints",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${nextRewardGoal - calmPoints} points until next reward",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- 30-DAY MOOD GRAPH ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "30-Day Mood 🌈",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showMoodDialog = true },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Today")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mood legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MoodLegendItem("😊", "Great")
                    MoodLegendItem("😐", "Okay")
                    MoodLegendItem("😔", "Low")
                    MoodLegendItem("😤", "Stressed")
                    MoodLegendItem("😴", "Tired")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 30-day mood grid
                val last30Days = (0 until 30).map {
                    Calendar.getInstance().apply {
                        time = today
                        add(Calendar.DAY_OF_YEAR, -it)
                    }.time
                }.reversed()

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(last30Days) { date ->
                        val entry = moodEntries.find { isSameDay(it.date, date) }
                        val mood = entry?.mood ?: "⚪"
                        val isToday = isSameDay(date, today)
                        val dayFormat = SimpleDateFormat("d", Locale.getDefault())

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(32.dp)
                        ) {
                            // Mood emoji
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (mood) {
                                            "😊" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                            "😐" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                                            "😔" -> Color(0xFF9C27B0).copy(alpha = 0.2f)
                                            "😤" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                            "😴" -> Color(0xFF607D8B).copy(alpha = 0.2f)
                                            else -> Color.Gray.copy(alpha = 0.1f)
                                        }
                                    )
                                    .clickable {
                                        if (isToday) showMoodDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mood,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Day indicator
                            Text(
                                text = dayFormat.format(date),
                                fontSize = 11.sp,
                                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Today's mood prompt
                if (todaysMoodEntry == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMoodDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EmojiEmotions, contentDescription = null)
                            Text("How are you feeling today? Tap to check in.")
                        }
                    }
                } else {
                    Text(
                        text = "Today: ${todaysMoodEntry.mood} ${todaysMoodEntry.note.takeIf { it.isNotBlank() }?.let { "— $it" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Quick Actions Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickActionButton(
                        icon = Icons.Filled.Timer,
                        label = "Study",
                        onClick = onStudyClick,
                        color = MaterialTheme.colorScheme.primary
                    )
                    QuickActionButton(
                        icon = Icons.Filled.CheckCircle,
                        label = "To-Do",
                        onClick = onTodoClick,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    QuickActionButton(
                        icon = Icons.Filled.Favorite,
                        label = "Calm",
                        onClick = onMeditationClick,
                        color = MaterialTheme.colorScheme.error
                    )
                    QuickActionButton(
                        icon = Icons.Filled.CardGiftcard,
                        label = "Rewards",
                        onClick = onRewardsClick,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Encouragement card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✨ You're doing great",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Every small step counts. Your mood patterns help you understand what supports you best.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Daily challenge
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily Challenge", style = MaterialTheme.typography.titleSmall)
                    Text("Take 3 deep breaths", style = MaterialTheme.typography.bodyLarge)
                }
                Button(
                    onClick = { onEarnPoints(5) },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("+5 pts")
                }
            }
        }
    }

    // Mood Check-In Dialog
    if (showMoodDialog) {
        MoodCheckInDialog(
            onDismiss = { showMoodDialog = false },
            onSave = { mood, note ->
                val existing = moodEntries.find { isSameDay(it.date, today) }
                if (existing != null) {
                    moodEntries.remove(existing)
                }
                moodEntries.add(MoodEntry(today, mood, note))
                moodEntries.sortBy { it.date.time }
                onEarnPoints(2)
                showMoodDialog = false
            }
        )
    }
}

// Helper function to compare dates without time
private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun MoodLegendItem(emoji: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun MoodCheckInDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var selectedMood by remember { mutableStateOf("😊") }
    var note by remember { mutableStateOf("") }

    val moods = listOf("😊", "😐", "😔", "😤", "😴")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How are you feeling today?") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose your mood:", style = MaterialTheme.typography.bodyMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    moods.forEach { mood ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedMood == mood)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable { selectedMood = mood },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mood,
                                fontSize = 28.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Add a note (optional)") },
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Text(
                    text = "+2 Calm Points for checking in",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedMood, note) },
                enabled = selectedMood.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
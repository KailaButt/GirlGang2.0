package com.example.consolicalm.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.consolicalm.ui.theme.ConsoliCalmTheme

enum class Mood(val emoji: String, val label: String) {
    CALM("😌", "Calm"),
    OKAY("🙂", "Okay"),
    DISTRACTED("😵‍💫", "Distracted"),
    STRESSED("😣", "Stressed")
}

private fun tipsFor(mood: Mood?): List<String> {
    return when (mood) {
        Mood.CALM -> listOf("Keep it light today.", "Start with one small task.", "Protect your calm with short sessions.")
        Mood.OKAY -> listOf("Pick something easy first.", "Try a 5-minute timer.", "Do the smallest step.")
        Mood.DISTRACTED -> listOf("Put your phone face down.", "Try 5 minutes only.", "Remove one distraction.", "Do the easiest part first.")
        Mood.STRESSED -> listOf("Breathe for 60 seconds.", "Break it into tiny steps.", "You don’t need to finish—just start.")
        null -> emptyList()
    }
}

@Composable
fun HomeScreen(name: String = "Kaila") {
    var selectedMood by remember { mutableStateOf<Mood?>(null) }

    val tips = tipsFor(selectedMood)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Hi $name 👋",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        Text("How are you feeling today?")

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Mood.values().forEach { mood ->
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selectedMood == mood)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            selectedMood = mood
                            println("Mood logged: ${mood.name}")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = mood.emoji, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Show tips after selection
        if (selectedMood != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick tips for ${selectedMood!!.label}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    tips.take(5).forEach { tip ->
                        Text("• $tip")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Thanks for checking in 🌱",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    ConsoliCalmTheme {
        HomeScreen()
    }
}


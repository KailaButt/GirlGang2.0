package com.example.consolicalm

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
import androidx.compose.ui.unit.dp

enum class Mood(val emoji: String, val label: String) {
    CALM("😌", "Calm"),
    OKAY("🙂", "Okay"),
    DISTRACTED("😵‍💫", "Distracted"),
    STRESSED("😣", "Stressed")
}

private fun tipsFor(mood: Mood?): List<String> {
    return when (mood) {
        Mood.CALM -> listOf("Keep it light today", "Start with one small task", "Protect your calm")
        Mood.OKAY -> listOf("Pick something easy first", "Try a 5-minute timer")
        Mood.DISTRACTED -> listOf("Put phone away", "Try 5 minute session", "Remove distractions")
        Mood.STRESSED -> listOf("Breathe slowly", "Break task into tiny steps")
        null -> emptyList()
    }
}

@Composable
fun HomeScreen(
    calmPoints: Int,
    nextRewardGoal: Int,
    onRewardsClick: () -> Unit
) {

    var selectedMood by remember { mutableStateOf<Mood?>(null) }

    val progress = calmPoints.toFloat() / nextRewardGoal
    val pointsLeft = nextRewardGoal - calmPoints

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ⭐ Calm Points Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRewardsClick() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(14.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calm Points ", fontWeight = FontWeight.Bold)
                    Text("$calmPoints pts")
                }

                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f,1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    if (pointsLeft <= 0)
                        "Reward unlocked! Tap to redeem 🎁"
                    else
                        "$pointsLeft points until next gift card 🎁"
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "How are you feeling today?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Mood.values().forEach { mood ->

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selectedMood == mood)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { selectedMood = mood },
                    contentAlignment = Alignment.Center
                ) {
                    Text(mood.emoji, style = MaterialTheme.typography.headlineMedium)
                }

            }

        }

        Spacer(Modifier.height(16.dp))

        if (selectedMood != null) {

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(14.dp)) {

                    Text("Quick tips", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    tipsFor(selectedMood).forEach {
                        Text("• $it")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("Thanks for checking in 🌱")

        }

    }
}


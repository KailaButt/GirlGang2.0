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
        Mood.CALM -> listOf("Keep it light today!", "Start with one small task", "Protect your calm")
        Mood.OKAY -> listOf("Pick something easy first", "Try a 5-minute timer")
        Mood.DISTRACTED -> listOf("Put phone away", "Try 5 minute session", "Remove distractions")
        Mood.STRESSED -> listOf("Breathe slowly", "Break task into tiny steps")
        null -> emptyList()
    }
}

private fun microStepFor(reason: String): String {
    val r = reason.lowercase()
    return when {
        r.contains("tired") || r.contains("exhaust") -> "Micro-step: set a 5-minute timer and do the easiest part only."
        r.contains("overwhelm") || r.contains("too much") -> "Micro-step: write a 3-item mini list. Start with the smallest."
        r.contains("phone") || r.contains("scroll") -> "Micro-step: put your phone face down for 5 minutes."
        r.contains("don’t know") || r.contains("confused") -> "Micro-step: open the assignment and read only the instructions."
        else -> "Micro-step: do 2 minutes of setup (open tabs, gather materials)."
    }
}

private val QUOTES = listOf(
    "Start where you are. Use what you have. Do what you can.",
    "Small steps count.",
    "Progress, not perfection.",
    "You only have to start.",
    "One focused minute is still a win."
)

private fun randomQuote(): String = QUOTES.random()

@Composable
fun HomeScreen(
    calmPoints: Int,
    nextRewardGoal: Int,
    onRewardsClick: () -> Unit
) {

    var selectedMood by remember { mutableStateOf<Mood?>(null) }

    var quote by remember { mutableStateOf(randomQuote()) }

    // ✅ Added for Feature 2: Reason textbox + micro-step output
    var reasonText by remember { mutableStateOf("") }
    var microStep by remember { mutableStateOf<String?>(null) }

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

        // 💬 Daily Quote
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily quote", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
                Text("“$quote”")
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

        // ✅ Added for Feature 2: Reason + Micro-step (UI placeholder)
        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(14.dp)) {

                Text("What’s making it hard to start?", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: I feel overwhelmed…") },
                    singleLine = false,
                    minLines = 2
                )

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        microStep = if (reasonText.isBlank()) null else microStepFor(reasonText)
                    },
                    enabled = reasonText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Get a micro-step")
                }

                if (microStep != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(microStep!!)
                    Spacer(Modifier.height(8.dp))
                    Text("Suggested next step: try a 5-minute focus session ⏱️")
                }
            }
        }
    }
}

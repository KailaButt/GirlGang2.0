package com.example.consolicalm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

data class MicroStepResult(
    val title: String,
    val steps: List<String>,
    val pointsReward: Int
)

private fun tipsFor(mood: Mood?): List<String> {
    return when (mood) {
        Mood.CALM -> listOf("Keep it light today!", "Start with one small task", "Protect your calm")
        Mood.OKAY -> listOf("Pick something easy first", "Try a 5-minute timer")
        Mood.DISTRACTED -> listOf("Put phone away", "Try 5 minute session", "Remove distractions")
        Mood.STRESSED -> listOf("Breathe slowly", "Break task into tiny steps")
        null -> emptyList()
    }
}

private fun generateMicroStep(mood: Mood, reason: String): MicroStepResult {
    val r = reason.lowercase()

    return when (mood) {
        Mood.CALM -> MicroStepResult(
            title = "Let’s start gently.",
            steps = listOf(
                "Pick ONE task to start (no pressure to finish)",
                "Do 2 minutes of setup (open tabs, gather materials)",
                "Set a 5-minute timer and begin"
            ),
            pointsReward = 10
        )

        Mood.OKAY -> MicroStepResult(
            title = "Quick momentum plan.",
            steps = listOf(
                "Write your goal in 1 sentence",
                "Do a 5-minute focus sprint",
                "Stop and decide: continue or take a short break"
            ),
            pointsReward = 10
        )

        Mood.DISTRACTED -> MicroStepResult(
            title = "Let’s reduce distractions first.",
            steps = listOf(
                "Put your phone face down or out of reach",
                "Close extra apps/tabs (leave only what you need)",
                "Do one 5-minute focus sprint"
            ),
            pointsReward = 15
        )

        Mood.STRESSED -> {
            val stressHint = when {
                "tired" in r || "exhaust" in r ->
                    "Do the easiest part for 3 minutes only."
                "overwhelm" in r || "too much" in r ->
                    "Write a tiny 3-item list. Start with the smallest."
                "phone" in r || "scroll" in r ->
                    "Put your phone away for 5 minutes."
                "don’t know" in r || "dont know" in r || "confused" in r ->
                    "Open the assignment and read ONLY the instructions."
                else ->
                    "Shrink it: do the smallest possible first action."
            }

            MicroStepResult(
                title = "Reset, then one small step.",
                steps = listOf(
                    "Breathe for 60 seconds: in 4, out 6",
                    stressHint,
                    "Start a 5-minute timer and begin"
                ),
                pointsReward = 20
            )
        }
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
    onRewardsClick: () -> Unit,
    onTodoClick: () -> Unit,
    onMeditationClick: () -> Unit,
    onStudyClick: () -> Unit,          // ✅ ADDED
    onEarnPoints: (Int) -> Unit
) {
    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    var quote by remember { mutableStateOf(randomQuote()) }

    var reasonText by remember { mutableStateOf("") }
    var microStepResult by remember { mutableStateOf<MicroStepResult?>(null) }
    var showDidIt by remember { mutableStateOf(false) }

    val progress = (calmPoints.toFloat() / nextRewardGoal).coerceIn(0f, 1f)
    val pointsLeft = nextRewardGoal - calmPoints

    val canGetMicroStep = selectedMood != null && reasonText.trim().isNotEmpty()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {

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
                    progress = { progress },
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

        Spacer(Modifier.height(10.dp))

        // ✅ NEW: Study button
        OutlinedButton(
            onClick = onStudyClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Study Timer ⏱️")
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onTodoClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("To-Do List ✿")
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onMeditationClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Meditation & Breathing ☾")
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Daily quote", fontWeight = FontWeight.Bold)
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
                        .clickable {
                            selectedMood = mood
                            microStepResult = null
                            showDidIt = false
                        },
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
                    tipsFor(selectedMood).forEach { Text("• $it") }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("Thanks for checking in 🌱")
        }

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
                    onValueChange = {
                        reasonText = it
                        microStepResult = null
                        showDidIt = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: I feel overwhelmed…") },
                    singleLine = false,
                    minLines = 2
                )

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        microStepResult = generateMicroStep(selectedMood!!, reasonText)
                        showDidIt = true
                    },
                    enabled = canGetMicroStep,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Get a micro-step")
                }

                microStepResult?.let { result ->
                    Spacer(Modifier.height(12.dp))
                    Text(result.title, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    result.steps.forEach { Text("• $it") }

                    if (showDidIt) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onEarnPoints(result.pointsReward)
                                showDidIt = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("I did it  (+${result.pointsReward} pts)")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}


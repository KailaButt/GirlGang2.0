package com.example.consolicalm

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class MeditationResource(
    val title: String,
    val description: String,
    val durationLabel: String,
    val url: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationScreen(
    onBack: () -> Unit,
    onEarnPoints: (Int) -> Unit
) {
    val context = LocalContext.current

    // ---- Calm progress (streak / daily goal / challenge) ----
    val prefs = remember(context) { CalmPrefs(context) }
    val sessions = remember { defaultCalmSessions() }
    val todayKey = remember { prefs.getTodayKey() }

    var streakCount by remember { mutableIntStateOf(0) }
    var sessionsToday by remember { mutableIntStateOf(0) }
    var challengeDone by remember { mutableStateOf(false) }

    // Session player state
    var activeSession by remember { mutableStateOf<CalmSession?>(null) }
    var activeIsChallenge by remember { mutableStateOf(false) }

    fun refreshProgress() {
        streakCount = prefs.streakCount
        sessionsToday = if (prefs.todaySessionsKey == todayKey) prefs.todaySessionsCount else 0
        challengeDone = prefs.challengeCompletedKey == todayKey
    }

    LaunchedEffect(Unit) {
        refreshProgress()
    }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open links.", Toast.LENGTH_SHORT).show()
        }
    }

    // Daily Calm Challenge rotates based on the day.
    val challengeBase = sessions[(todayKey % sessions.size).coerceAtLeast(0)]
    val dailyChallenge = remember(challengeBase) {
        // Slightly shorter and higher reward for the daily challenge.
        challengeBase.copy(
            id = "daily_${challengeBase.id}",
            estimatedSeconds = challengeBase.estimatedSeconds.coerceAtMost(150),
            points = 5
        )
    }

    val breathing = listOf(
        MeditationResource(
            title = "Breathing for stress (NHS)",
            description = "Simple guided breathing you can do anywhere.",
            durationLabel = "~5 min",
            url = "https://www.nhs.uk/mental-health/self-help/guides-tools-and-activities/breathing-exercises-for-stress/"
        ),
        MeditationResource(
            title = "4-7-8 relaxing breath (Dr. Weil)",
            description = "A quick technique to calm your nervous system.",
            durationLabel = "~2–3 min",
            url = "https://www.drweil.com/videos-features/videos/breathing-exercises-4-7-8-breath/"
        )
    )

    val shortMeditations = listOf(
        MeditationResource(
            title = "UCLA Mindful: Guided Meditations",
            description = "Stream or download short, beginner-friendly meditations.",
            durationLabel = "3–10 min",
            url = "https://www.uclahealth.org/uclamindful/guided-meditations"
        ),
        MeditationResource(
            title = "Headspace: Let Go of Stress (YouTube)",
            description = "A short guided reset when you feel overwhelmed.",
            durationLabel = "~4 min",
            url = "https://www.youtube.com/watch?v=c1Ndym-IsQg"
        )
    )

    val longerSessions = listOf(
        MeditationResource(
            title = "UCLA Mindful: Weekly Meditations & Talks",
            description = "Longer guided sessions and themes for deeper practice.",
            durationLabel = "10–30+ min",
            url = "https://www.uclahealth.org/uclamindful/weekly-meditations-talks"
        ),
        MeditationResource(
            title = "Headspace channel (YouTube)",
            description = "Browse short meditations, breathing practices, and sleep sounds.",
            durationLabel = "Varies",
            url = "https://www.youtube.com/c/headspace/videos"
        )
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meditation & Breathing", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Take a small pause 🌿", fontWeight = FontWeight.Bold)
                    Text("Pick something short. Even one minute counts.")
                }
            }

            // --- Progress / streak / daily goal ---
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Calm Goal", fontWeight = FontWeight.Bold)
                            Text("Complete 1 Calm session")
                        }
                        AssistChip(
                            onClick = { },
                            enabled = false,
                            label = { Text(if (sessionsToday >= 1) "Done" else "0/1") }
                        )
                    }

                    LinearProgressIndicator(
                        progress = (sessionsToday.coerceAtMost(1) / 1f).coerceIn(0f, 1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null)
                        Text("Streak: ${streakCount.coerceAtLeast(0)} day${if (streakCount == 1) "" else "s"}")
                        Spacer(Modifier.weight(1f))
                        Text("Today: $sessionsToday session${if (sessionsToday == 1) "" else "s"}")
                    }
                }
            }

            // --- Daily Calm Challenge ---
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Column {
                                Text("Today's Calm Challenge", fontWeight = FontWeight.Bold)
                                Text(dailyChallenge.title)
                            }
                        }

                        AssistChip(
                            onClick = { },
                            enabled = false,
                            label = { Text(if (challengeDone) "Completed" else "+${dailyChallenge.points} pts") }
                        )
                    }

                    Text(dailyChallenge.description)

                    Button(
                        onClick = {
                            activeIsChallenge = true
                            activeSession = dailyChallenge
                        },
                        enabled = !challengeDone,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (challengeDone) "Completed" else "Start challenge")
                    }
                }
            }

            // --- Guided Calm Sessions (interactive) ---
            SectionTitle("Guided Calm sessions")
            Text("Tap Start to follow step-by-step with a timer.")
            sessions.forEach { s ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.title, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(s.description)
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { },
                                enabled = false,
                                label = { Text("~${(s.estimatedSeconds / 60).coerceAtLeast(1)} min") }
                            )
                            Button(
                                onClick = {
                                    activeIsChallenge = false
                                    activeSession = s
                                }
                            ) { Text("Start") }
                        }
                    }
                }
            }

            SectionTitle("Quick breathing")
            breathing.forEach { res ->
                ResourceCard(resource = res, onOpen = { openUrl(res.url) })
            }

            SectionTitle("Short meditations")
            shortMeditations.forEach { res ->
                ResourceCard(resource = res, onOpen = { openUrl(res.url) })
            }

            SectionTitle("Longer sessions")
            longerSessions.forEach { res ->
                ResourceCard(resource = res, onOpen = { openUrl(res.url) })
            }
        }
    }

    // Full-screen guided session player
    activeSession?.let { session ->
        CalmSessionPlayer(
            session = session,
            isDailyChallenge = activeIsChallenge,
            onClose = {
                activeSession = null
                activeIsChallenge = false
                refreshProgress()
            },
            onCompleted = { earnedPoints ->
                prefs.recordSessionCompleted()
                if (activeIsChallenge) {
                    prefs.challengeCompletedKey = todayKey
                }
                refreshProgress()
                onEarnPoints(earnedPoints)
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun ResourceCard(
    resource: MeditationResource,
    onOpen: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(resource.title, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(resource.description)
            }

            Spacer(Modifier.width(12.dp))

            AssistChip(
                onClick = onOpen,
                label = { Text(resource.durationLabel) }
            )
        }
    }
}


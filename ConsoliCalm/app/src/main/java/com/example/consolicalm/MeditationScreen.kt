package com.example.consolicalm

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val toolkitPrefs = remember(context) { CalmToolkitPrefs(context) }
    val sessions = remember { defaultCalmSessions() }
    val todayKey = remember { prefs.getTodayKey() }

    var streakCount by remember { mutableIntStateOf(0) }
    var sessionsToday by remember { mutableIntStateOf(0) }
    var challengeDone by remember { mutableStateOf(false) }

    // Favorites + history
    var favorites by remember { mutableStateOf(setOf<String>()) }
    var history by remember { mutableStateOf(listOf<CalmHistoryEntry>()) }
    var showHistory by remember { mutableStateOf(false) }

    // Session player state
    var activeSession by remember { mutableStateOf<CalmSession?>(null) }
    var activeIsChallenge by remember { mutableStateOf(false) }

    fun refreshProgress() {
        streakCount = prefs.streakCount
        sessionsToday = if (prefs.todaySessionsKey == todayKey) prefs.todaySessionsCount else 0
        challengeDone = prefs.challengeCompletedKey == todayKey

        favorites = toolkitPrefs.getFavorites()
        history = toolkitPrefs.getHistoryEntries()
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

    val allResources = remember(breathing, shortMeditations, longerSessions) {
        breathing + shortMeditations + longerSessions
    }

    fun resourceKey(res: MeditationResource): String = "resource:${res.url}"
    fun sessionKey(session: CalmSession): String = "session:${session.id}"
    fun toggleFavorite(key: String) {
        toolkitPrefs.toggleFavorite(key)
        favorites = toolkitPrefs.getFavorites()
    }

    val favoriteSessions = remember(favorites, sessions) {
        sessions.filter { favorites.contains(sessionKey(it)) }
    }
    val favoriteResources = remember(favorites, allResources) {
        allResources.filter { favorites.contains(resourceKey(it)) }
    }


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

            // --- My Toolkit (favorites) ---
            SectionTitle("My Toolkit")
            if (favoriteSessions.isEmpty() && favoriteResources.isEmpty()) {
                Text("Star a session or resource to save it here.")
            } else {
                if (favoriteSessions.isNotEmpty()) {
                    Text("Saved sessions", fontWeight = FontWeight.SemiBold)
                    favoriteSessions.forEach { s ->
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
                                    Text(s.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.width(10.dp))
                                IconButton(onClick = { toggleFavorite(sessionKey(s)) }) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Unfavorite",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
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

                if (favoriteResources.isNotEmpty()) {
                    Text("Saved resources", fontWeight = FontWeight.SemiBold)
                    favoriteResources.forEach { res ->
                        ResourceCard(
                            resource = res,
                            isFavorite = true,
                            onToggleFavorite = { toggleFavorite(resourceKey(res)) },
                            onOpen = { openUrl(res.url) }
                        )
                    }
                }
            }

            // --- Calm history ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Calm History")
                TextButton(onClick = { showHistory = true }, enabled = history.isNotEmpty()) {
                    Text("View all")
                }
            }
            if (history.isEmpty()) {
                Text("Complete a guided session to start your history log.")
            } else {
                history.take(3).forEach { entry ->
                    HistoryRow(entry = entry)
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

                            IconButton(onClick = { toggleFavorite(sessionKey(s)) }) {
                                val isFav = favorites.contains(sessionKey(s))
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = if (isFav) "Unfavorite" else "Favorite",
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }

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
                ResourceCard(
                    resource = res,
                    isFavorite = favorites.contains(resourceKey(res)),
                    onToggleFavorite = { toggleFavorite(resourceKey(res)) },
                    onOpen = { openUrl(res.url) }
                )
            }

            SectionTitle("Short meditations")
            shortMeditations.forEach { res ->
                ResourceCard(
                    resource = res,
                    isFavorite = favorites.contains(resourceKey(res)),
                    onToggleFavorite = { toggleFavorite(resourceKey(res)) },
                    onOpen = { openUrl(res.url) }
                )
            }

            SectionTitle("Longer sessions")
            longerSessions.forEach { res ->
                ResourceCard(
                    resource = res,
                    isFavorite = favorites.contains(resourceKey(res)),
                    onToggleFavorite = { toggleFavorite(resourceKey(res)) },
                    onOpen = { openUrl(res.url) }
                )
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

                // history log (date + session + minutes)
                val minutes = ((session.estimatedSeconds + 59) / 60).coerceAtLeast(1)
                toolkitPrefs.addHistoryEntry(
                    CalmHistoryEntry(
                        timestampMillis = System.currentTimeMillis(),
                        title = session.title,
                        minutes = minutes
                    )
                )

                refreshProgress()
                onEarnPoints(earnedPoints)
            }
        )
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            confirmButton = {
                TextButton(onClick = { showHistory = false }) { Text("Close") }
            },
            title = { Text("Calm History") },
            text = {
                if (history.isEmpty()) {
                    Text("No sessions completed yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(history) { entry ->
                            HistoryRow(entry = entry)
                        }
                    }
                }
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
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
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

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            AssistChip(
                onClick = onOpen,
                label = { Text(resource.durationLabel) }
            )
        }
    }
}

@Composable
private fun HistoryRow(entry: CalmHistoryEntry) {
    val fmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val dateStr = remember(entry.timestampMillis) { fmt.format(Date(entry.timestampMillis)) }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(dateStr)
            }
            AssistChip(
                onClick = { },
                enabled = false,
                label = { Text("${entry.minutes} min") }
            )
        }
    }
}


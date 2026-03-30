package com.example.consolicalm

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class MoodEntry(
    val date: Date,
    val mood: String,
    val note: String = ""
)

@Composable
fun HomeScreen(
    calmPoints: Int,
    nextRewardGoal: Int,
    onProfileClick: () -> Unit,
    onRewardsClick: () -> Unit,
    onTodoClick: () -> Unit,
    onMeditationClick: () -> Unit,
    onStudyClick: () -> Unit,
    onInsightsClick: () -> Unit,
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

    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Not logged in"
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var activityFeed by remember { mutableStateOf<List<ActivityItem>>(emptyList()) }
    var friendIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    user?.let {
        val userRef = db.collection("public_users").document(it.uid)
        userRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val friendCode = it.uid.take(6).uppercase()
                val newUser = hashMapOf(
                    "uid" to it.uid,
                    "nickname" to "",
                    "friendCode" to friendCode
                )
                userRef.set(newUser)
            }
        }
    }

    val userContext = LocalContext.current
    val userPrefs = UserPrefs(userContext)
    val nickname = userPrefs.nickname
    val displayName = nickname ?: currentUserEmail

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("consoli_prefs", Context.MODE_PRIVATE) }
    var openStreak by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        openStreak = updateAppOpenStreak(prefs, today)

        val lastPostedStreak = prefs.getInt("last_posted_streak", 0)
        if (openStreak > 1 && openStreak > lastPostedStreak) {
            saveActivity("hit a $openStreak day streak 🔥")
            prefs.edit().putInt("last_posted_streak", openStreak).apply()
        }
    }

    val scope = rememberCoroutineScope()

    var quoteText by remember { mutableStateOf<String?>(null) }
    var quoteAuthor by remember { mutableStateOf<String?>(null) }
    var quoteLoading by remember { mutableStateOf(false) }
    var quoteError by remember { mutableStateOf<String?>(null) }

    val todayKey = remember {
        LocalDate.now(ZoneId.systemDefault()).toString()
    }

    var dailyChallengeText by remember { mutableStateOf("Take 3 deep breaths") }
    var dailyChallengeLoading by remember { mutableStateOf(false) }
    var dailyChallengeCompleted by remember { mutableStateOf(false) }

    suspend fun loadDailyChallengeIfNeeded() {
        val storedDate = prefs.getString("daily_challenge_date", null)
        val storedText = prefs.getString("daily_challenge_text", null)
        val completedDate = prefs.getString("daily_challenge_completed_date", null)

        if (storedDate == todayKey) {
            val text = storedText?.trim().orEmpty()
            dailyChallengeText = if (text.isNotBlank()) text else "Take 3 deep breaths"
            dailyChallengeCompleted = (completedDate == todayKey)
            dailyChallengeLoading = false
            return
        }

        dailyChallengeLoading = true
        try {
            val result = withContext(Dispatchers.IO) {
                DailyChallengeClient.api.getRandomChallenge()
            }
            val activity = result.activity?.trim().orEmpty()
            dailyChallengeText = if (activity.isNotBlank()) activity else "Take 3 deep breaths"
        } catch (_: Exception) {
            val fallback = storedText?.trim().orEmpty()
            dailyChallengeText = if (fallback.isNotBlank()) fallback else "Take 3 deep breaths"
        } finally {
            prefs.edit()
                .putString("daily_challenge_date", todayKey)
                .putString("daily_challenge_text", dailyChallengeText)
                .remove("daily_challenge_completed_date")
                .commit()

            dailyChallengeCompleted = false
            dailyChallengeLoading = false
        }
    }

    suspend fun loadZenQuoteToday() {
        quoteLoading = true
        quoteError = null
        try {
            val result = withContext(Dispatchers.IO) {
                ZenQuotesClient.api.getTodayQuote()
            }
            val first = result.firstOrNull()
            quoteText = first?.q
            quoteAuthor = first?.a
        } catch (e: Exception) {
            quoteError = "Couldn’t load quote. Check internet and try again."
        } finally {
            quoteLoading = false
        }
    }

    suspend fun loadZenQuoteRandom() {
        quoteLoading = true
        quoteError = null
        try {
            val result = withContext(Dispatchers.IO) {
                ZenQuotesClient.api.getRandomQuote()
            }
            val first = result.firstOrNull()
            quoteText = first?.q
            quoteAuthor = first?.a
        } catch (e: Exception) {
            quoteError = "Couldn’t load quote. Check internet and try again."
        } finally {
            quoteLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (quoteText == null && !quoteLoading) {
            loadZenQuoteToday()
        }
    }

    LaunchedEffect(Unit) {
        loadDailyChallengeIfNeeded()
    }

    LaunchedEffect(Unit) {
        if (moodEntries.isEmpty()) {
            val moods = listOf(
                "😊", "😊", "😐", "😊", "😴", "😊", "😔", "😊", "😐", "😤",
                "😊", "😐", "😊", "😴", "😊", "😐", "😊", "😔", "😊", "😐",
                "😊", "😴", "😊", "😤", "😊", "😐", "😊", "😔", "😊", "😐"
            )
            for (i in 0 until 30) {
                val cal = Calendar.getInstance().apply {
                    time = today
                    add(Calendar.DAY_OF_YEAR, -(29 - i))
                }
                moodEntries.add(MoodEntry(cal.time, moods[i]))
            }
        }
    }

    LaunchedEffect(user?.uid) {
        if (user == null) {
            friendIds = emptySet()
            return@LaunchedEffect
        }

        try {
            val result = db.collection("public_users")
                .document(user.uid)
                .collection("friends")
                .get()
                .await()

            friendIds = result.documents.map { it.id }.toSet()
        } catch (_: Exception) {
            friendIds = emptySet()
        }
    }

    DisposableEffect(user?.uid, friendIds) {
        if (user == null || friendIds.isEmpty()) {
            activityFeed = emptyList()
            onDispose { }
        } else {
            val listener = listenToTodayActivityFeed { items ->
                activityFeed = items
                    .filter { it.uid in friendIds }
                    .take(10)
            }

            onDispose {
                listener.remove()
            }
        }
    }

    val todaysMoodEntry = moodEntries.find { isSameDay(it.date, today) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello,",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile"
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    quoteLoading -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Loading a quote…", style = MaterialTheme.typography.bodySmall)
                    }

                    quoteError != null -> {
                        Text(quoteError!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { scope.launch { loadZenQuoteToday() } }) {
                            Text("Try again")
                        }
                    }

                    quoteText != null -> {
                        Text(
                            text = "“${quoteText!!}”",
                            style = MaterialTheme.typography.titleMedium,
                            lineHeight = 28.sp
                        )
                        Text(
                            text = "— ${quoteAuthor.orEmpty()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Quotes by ZenQuotes.io",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    else -> {
                        Text("Tap New to load a quote.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.size(255.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "🔥 $openStreak day streak",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Calm Points",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "$calmPoints",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${nextRewardGoal - calmPoints} until next reward",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
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
                        modifier = Modifier.height(38.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Today")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (mood) {
                                            "😊" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                            "😐" -> Color(0xFFFFC107).copy(alpha = 0.25f)
                                            "😔" -> Color(0xFFAB47BC).copy(alpha = 0.2f)
                                            "😤" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                            "😴" -> Color(0xFF78909C).copy(alpha = 0.2f)
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

                            Text(
                                text = dayFormat.format(date),
                                fontSize = 11.sp,
                                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (todaysMoodEntry == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMoodDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.EmojiEmotions, contentDescription = null)
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!dailyChallengeCompleted && !dailyChallengeLoading) {
                        onEarnPoints(5)
                        saveActivity("completed the daily challenge ✨")
                        dailyChallengeCompleted = true
                        prefs.edit()
                            .putString("daily_challenge_completed_date", todayKey)
                            .commit()
                    }
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Challenge ✨",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (dailyChallengeLoading) "Loading your challenge…" else dailyChallengeText,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "+5 pts",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                val buttonText = when {
                    dailyChallengeLoading -> "Loading..."
                    dailyChallengeCompleted -> "Completed today 💖"
                    else -> "Tap to complete"
                }

                Button(
                    onClick = {
                        if (!dailyChallengeCompleted && !dailyChallengeLoading) {
                            onEarnPoints(5)
                            saveActivity("completed the daily challenge ✨")
                            dailyChallengeCompleted = true
                            prefs.edit()
                                .putString("daily_challenge_completed_date", todayKey)
                                .commit()
                        }
                    },
                    enabled = !dailyChallengeCompleted && !dailyChallengeLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(buttonText)
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onInsightsClick() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            Box(
                                modifier = Modifier.size(46.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QueryStats,
                                    contentDescription = null
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Weekly Insights 📈",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "See your weekly stats and friend leaderboard",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Track wins, streaks, and rankings",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Friends Activity 💬",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (friendIds.isEmpty()) {
                    Text(
                        text = "Add friends to see their activity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else if (activityFeed.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        )
                    ) {
                        Text(
                            text = "No friend activity yet today 🌸",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    activityFeed.forEach { activity ->
                        ActivityBubble(activity = activity)
                    }
                }
            }
        }
    }

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
                saveActivity("checked in $mood")
                showMoodDialog = false
            }
        )
    }
}

@Composable
private fun ActivityBubble(activity: ActivityItem) {
    val bubbleColor = activityBubbleColor(activity.message)
    val initial = activity.displayName
        .ifBlank { activity.email.ifBlank { "S" } }
        .trim()
        .firstOrNull()
        ?.uppercaseChar()
        ?.toString()
        ?: "S"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = bubbleColor
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.displayName.ifBlank { activity.email.ifBlank { "Someone" } },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = formatActivityTimestamp(activity.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = activity.message,
                style = MaterialTheme.typography.bodyMedium
            )

            ReactionSection(activity = activity)
        }
    }
}

@Composable
private fun ReactionSection(activity: ActivityItem) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExistingReactionsRow(activity = activity)
        AddReactionRow(activity = activity)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExistingReactionsRow(activity: ActivityItem) {
    val db = FirebaseFirestore.getInstance()
    var showDialog by remember { mutableStateOf(false) }
    var selectedEmoji by remember { mutableStateOf("") }
    var reactorNames by remember { mutableStateOf<List<String>>(emptyList()) }

    val activeReactions = activity.reactions
        .filterValues { it.isNotEmpty() }
        .toSortedMap()

    if (activeReactions.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            activeReactions.forEach { (emoji, uids) ->
                Surface(
                    modifier = Modifier.clickable {
                        selectedEmoji = emoji
                        showDialog = true
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = emoji, fontSize = 14.sp)
                        Text(
                            text = uids.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (showDialog && selectedEmoji == emoji) {
                    LaunchedEffect(selectedEmoji, uids) {
                        val names = uids.mapNotNull { uid ->
                            try {
                                val doc = db.collection("public_users").document(uid).get().await()
                                val nickname = doc.getString("nickname")?.trim().orEmpty()
                                if (nickname.isNotBlank()) nickname else doc.getString("email")
                            } catch (_: Exception) {
                                null
                            }
                        }
                        reactorNames = names
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Reactions $selectedEmoji") },
            text = {
                if (reactorNames.isEmpty()) {
                    Text("No names to show yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reactorNames.forEach { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddReactionRow(activity: ActivityItem) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    val emojiOptions = listOf("❤️", "🔥", "😂", "👏", "✨", "🥹")

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        emojiOptions.forEach { emoji ->
            val users = activity.reactions[emoji] ?: emptyList()
            val reactedByMe = currentUid != null && users.contains(currentUid)

            Surface(
                modifier = Modifier.clickable {
                    addReaction(activity.id, emoji)
                },
                shape = RoundedCornerShape(50),
                color = if (reactedByMe) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                }
            ) {
                Text(
                    text = emoji,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun activityBubbleColor(message: String): Color {
    val lower = message.lowercase()

    return when {
        "streak" in lower -> Color(0xFFFFF1E6)
        "daily challenge" in lower -> Color(0xFFF4EDFF)
        "checked in" in lower -> when {
            "😊" in message -> Color(0xFFF3F8EA)
            "😐" in message -> Color(0xFFF7F2E8)
            "😔" in message -> Color(0xFFF3ECFA)
            "😤" in message -> Color(0xFFFFEFE6)
            "😴" in message -> Color(0xFFEAF2F6)
            else -> Color(0xFFF2F4F7)
        }
        else -> Color(0xFFF6F2ED)
    }
}

private fun formatActivityTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "today"

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minute = 60_000L
    val hour = 60 * minute

    return when {
        diff < minute -> "just now"
        diff < hour -> "${diff / minute}m ago"
        diff < 24 * hour -> "${diff / hour}h ago"
        else -> "today"
    }
}

private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
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

private fun normalizeDay(d: Date): Long {
    val c = Calendar.getInstance().apply {
        time = d
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
}

private fun updateAppOpenStreak(
    prefs: android.content.SharedPreferences,
    today: Date
): Int {
    val KEY_LAST_OPEN = "last_open_day"
    val KEY_OPEN_STREAK = "open_streak"

    val todayDay = normalizeDay(today)
    val lastOpen = prefs.getLong(KEY_LAST_OPEN, -1L)
    var streak = prefs.getInt(KEY_OPEN_STREAK, 0)

    if (lastOpen == -1L) {
        streak = 1
    } else {
        val diffDays = ((todayDay - lastOpen) / (24L * 60L * 60L * 1000L)).toInt()
        streak = when (diffDays) {
            0 -> streak
            1 -> streak + 1
            else -> 1
        }
    }

    prefs.edit()
        .putLong(KEY_LAST_OPEN, todayDay)
        .putInt(KEY_OPEN_STREAK, streak)
        .apply()

    return streak
}
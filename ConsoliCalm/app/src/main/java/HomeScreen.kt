package com.example.consolicalm
import androidx.compose.foundation.horizontalScroll
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class MoodEntry(
    val date: Date,
    val mood: String,
    val note: String = ""
)

private fun getTodayKey(): String {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day)
}

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
    var selectedMoodDate by remember { mutableStateOf<Date?>(null) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val moodEntries = remember { mutableStateListOf<MoodEntry>() }

    // Always fresh from device clock — no remember so it never goes stale
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    // Strip month navigation — starts on current real month
    var stripYear  by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var stripMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) } // 0-based

    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Not logged in"
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var activityFeed by remember { mutableStateOf<List<ActivityItem>>(emptyList()) }
    var friendIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var addedYouList by remember { mutableStateOf<List<AddedYouItem>>(emptyList()) }
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var notificationStatus by remember { mutableStateOf("") }

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

    val unreadNotificationCount = notifications.count { !it.read }
    val bellCount = addedYouList.size + unreadNotificationCount

    val scope = rememberCoroutineScope()

    var quoteText by remember { mutableStateOf<String?>(null) }
    var quoteAuthor by remember { mutableStateOf<String?>(null) }
    var quoteLoading by remember { mutableStateOf(false) }
    var quoteError by remember { mutableStateOf<String?>(null) }

    val todayKey = remember { getTodayKey() }

    var dailyChallengeText by remember { mutableStateOf("Take 3 deep breaths") }
    var dailyChallengeLoading by remember { mutableStateOf(false) }
    var dailyChallengeCompleted by remember { mutableStateOf(false) }

    suspend fun loadAddedYouRequests(currentUid: String) {
        val result = db.collection("public_users")
            .document(currentUid)
            .collection("added_you")
            .get()
            .await()
        addedYouList = result.documents.mapNotNull {
            it.toObject(AddedYouItem::class.java)
        }.sortedByDescending { it.timestamp }
    }

    suspend fun loadNotifications(currentUid: String) {
        val result = db.collection("public_users")
            .document(currentUid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
        notifications = result.documents.map { doc ->
            AppNotification(
                id = doc.id,
                type = doc.getString("type") ?: "",
                fromUid = doc.getString("fromUid") ?: "",
                fromName = doc.getString("fromName") ?: "",
                activityId = doc.getString("activityId") ?: "",
                activityMessage = doc.getString("activityMessage") ?: "",
                emoji = doc.getString("emoji") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L,
                read = doc.getBoolean("read") ?: false
            )
        }.sortedByDescending { it.timestamp }
    }

    suspend fun acceptFriendRequest(item: AddedYouItem): String {
        val me = FirebaseAuth.getInstance().currentUser ?: return "Not logged in"
        val friendData = hashMapOf(
            "uid" to item.fromUid,
            "nickname" to item.fromName,
            "friendCode" to item.fromFriendCode,
            "addedAt" to System.currentTimeMillis()
        )
        db.collection("public_users").document(me.uid).collection("friends")
            .document(item.fromUid).set(friendData).await()
        db.collection("public_users").document(me.uid).collection("added_you")
            .document(item.fromUid).delete().await()
        loadAddedYouRequests(me.uid)
        return "Friend request accepted ✅"
    }

    suspend fun declineFriendRequest(item: AddedYouItem): String {
        val me = FirebaseAuth.getInstance().currentUser ?: return "Not logged in"
        db.collection("public_users").document(me.uid).collection("added_you")
            .document(item.fromUid).delete().await()
        loadAddedYouRequests(me.uid)
        return "Friend request declined"
    }

    suspend fun markAllNotificationsRead() {
        val me = FirebaseAuth.getInstance().currentUser ?: return
        val batch = db.batch()
        notifications.filter { !it.read }.forEach { item ->
            val ref = db.collection("public_users").document(me.uid)
                .collection("notifications").document(item.id)
            batch.update(ref, "read", true)
        }
        batch.commit().await()
        loadNotifications(me.uid)
    }

    suspend fun loadDailyChallengeIfNeeded() {
        val storedDate = prefs.getString("daily_challenge_date", null)
        val storedText = prefs.getString("daily_challenge_text", null)
        val completedDate = prefs.getString("daily_challenge_completed_date", null)
        if (storedDate == todayKey) {
            val text = storedText?.trim().orEmpty()
            dailyChallengeText = if (text.isNotBlank()) text else "Take 3 deep breaths"
            dailyChallengeCompleted = completedDate == todayKey
            dailyChallengeLoading = false
            return
        }
        dailyChallengeLoading = true
        try {
            val result = withContext(Dispatchers.IO) { DailyChallengeClient.api.getRandomChallenge() }
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
            val result = withContext(Dispatchers.IO) { ZenQuotesClient.api.getTodayQuote() }
            val first = result.firstOrNull()
            quoteText = first?.q
            quoteAuthor = first?.a
        } catch (_: Exception) {
            quoteError = "Couldn't load quote. Check internet and try again."
        } finally {
            quoteLoading = false
        }
    }

    LaunchedEffect(Unit) {
        openStreak = updateAppOpenStreak(prefs, today)
        val lastPostedStreak = prefs.getInt("last_posted_streak", 0)
        if (openStreak > 1 && openStreak > lastPostedStreak) {
            saveActivity("hit a $openStreak day streak 🔥")
            prefs.edit().putInt("last_posted_streak", openStreak).apply()
        }
    }

    LaunchedEffect(Unit) {
        if (quoteText == null && !quoteLoading) loadZenQuoteToday()
    }

    LaunchedEffect(Unit) {
        loadDailyChallengeIfNeeded()
    }

    LaunchedEffect(user?.uid) {
        if (user == null) {
            friendIds = emptySet()
            addedYouList = emptyList()
            notifications = emptyList()
            return@LaunchedEffect
        }
        try {
            val result = db.collection("public_users").document(user.uid)
                .collection("friends").get().await()
            friendIds = result.documents.map { it.id }.toSet()
        } catch (_: Exception) { friendIds = emptySet() }
        try { loadAddedYouRequests(user.uid) } catch (_: Exception) { addedYouList = emptyList() }
        try { loadNotifications(user.uid) } catch (_: Exception) { notifications = emptyList() }
    }

    DisposableEffect(user?.uid, friendIds) {
        if (user == null || friendIds.isEmpty()) {
            activityFeed = emptyList()
            onDispose { }
        } else {
            val listener = listenToTodayActivityFeed { items ->
                activityFeed = items.filter { it.uid in friendIds }.take(10)
            }
            onDispose { listener.remove() }
        }
    }

    val todaysMoodEntry = moodEntries.find { isSameDay(it.date, today) }

    val calmBubbleGradient = Brush.radialGradient(
        colors = listOf(Color(0xFFD9E5E7), Color(0xFFAEC1C7), Color(0xFF89A3AB))
    )

    // Build days for the currently viewed strip month
    val nowCal = Calendar.getInstance()
    val isCurrentMonth = stripYear == nowCal.get(Calendar.YEAR) &&
            stripMonth == nowCal.get(Calendar.MONTH)

    val stripDays = remember(stripYear, stripMonth) {
        val cal = Calendar.getInstance()
        cal.set(stripYear, stripMonth, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val numDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        (1..numDays).map { day ->
            (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }.time
        }
    }

    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val monthLabel = remember(stripYear, stripMonth) {
        val cal = Calendar.getInstance()
        cal.set(stripYear, stripMonth, 1)
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NotificationBellBubble(count = bellCount, onClick = { showNotificationsDialog = true })
                CircleIconBubble(onClick = onProfileClick)
            }
        }

        // ── Quote card ────────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    quoteLoading -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Loading a quote…", style = MaterialTheme.typography.bodySmall)
                    }
                    quoteError != null -> {
                        Text(quoteError!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { scope.launch { loadZenQuoteToday() } }) { Text("Try again") }
                    }
                    quoteText != null -> {
                        Text(text = "\u201c${quoteText!!}\u201d", style = MaterialTheme.typography.titleMedium, lineHeight = 28.sp)
                        Text(text = "— ${quoteAuthor.orEmpty()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(text = "Quotes by ZenQuotes.io", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    else -> Text("Tap New to load a quote.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ── Calm bubble ───────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(255.dp)
                    .clip(CircleShape)
                    .background(calmBubbleGradient)
                    .border(width = 1.5.dp, color = Color.White.copy(alpha = 0.55f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Box(modifier = Modifier.align(Alignment.TopStart).size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.28f)))
                    Box(modifier = Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.20f)))
                    Box(modifier = Modifier.align(Alignment.BottomStart).size(14.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
                    Column(
                        modifier = Modifier.fillMaxSize().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.25f)) {
                            Text(
                                text = "🔥 $openStreak day streak",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3F5962)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Calm Points", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF536A72))
                        Text(calmPoints.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2F4A53))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${nextRewardGoal - calmPoints} until next reward", style = MaterialTheme.typography.bodySmall, color = Color(0xFF536A72).copy(alpha = 0.75f))
                    }
                }
            }
        }

        // ── Mood strip ────────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text("Mood Calendar 🌈", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "Tap a past or current day to log your mood",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        val cal = Calendar.getInstance()
                        cal.set(stripYear, stripMonth, 1)
                        cal.add(Calendar.MONTH, -1)
                        stripYear  = cal.get(Calendar.YEAR)
                        stripMonth = cal.get(Calendar.MONTH)
                    }) {
                        Text("‹", fontSize = 26.sp)
                    }

                    Text(monthLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    TextButton(
                        onClick = {
                            if (!isCurrentMonth) {
                                val cal = Calendar.getInstance()
                                cal.set(stripYear, stripMonth, 1)
                                cal.add(Calendar.MONTH, 1)
                                stripYear  = cal.get(Calendar.YEAR)
                                stripMonth = cal.get(Calendar.MONTH)
                            }
                        },
                        enabled = !isCurrentMonth
                    ) {
                        Text("›", fontSize = 26.sp, color = if (isCurrentMonth) Color.Gray else MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mood legend
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MoodLegendItem("😊", "Great")
                    MoodLegendItem("😌", "Calm")
                    MoodLegendItem("😐", "Okay")
                    MoodLegendItem("😔", "Low")
                    MoodLegendItem("😤", "Stressed")
                    MoodLegendItem("😴", "Tired")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    stripDays.forEach { date ->
                        val entry    = moodEntries.find { isSameDay(it.date, date) }
                        val mood     = entry?.mood ?: "⚪"
                        val isToday  = isSameDay(date, today)
                        // Future = after today → not tappable, greyed out
                        val isFuture = date.after(today)
                        val dayCal   = Calendar.getInstance().apply { time = date }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isFuture -> Color.Gray.copy(alpha = 0.07f)
                                            mood == "😊" || mood == "🤩" || mood == "🥰" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                            mood == "😌" || mood == "🤗" -> Color(0xFF26A69A).copy(alpha = 0.2f)
                                            mood == "😐" || mood == "😶" -> Color(0xFFFFC107).copy(alpha = 0.25f)
                                            mood == "😔" || mood == "😢" || mood == "🥺" -> Color(0xFFAB47BC).copy(alpha = 0.2f)
                                            mood == "😰" || mood == "😅" -> Color(0xFF42A5F5).copy(alpha = 0.2f)
                                            mood == "😤" || mood == "😠" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                            mood == "😴" || mood == "🤒" -> Color(0xFF78909C).copy(alpha = 0.2f)
                                            else -> Color.Gray.copy(alpha = 0.1f)
                                        }
                                    )
                                    .then(
                                        if (!isFuture) Modifier.clickable {
                                            selectedMoodDate = date
                                            showMoodDialog = true
                                        } else Modifier
                                    )
                                    .then(
                                        if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isFuture) "" else mood,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = dayFormat.format(date).take(2),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                color = when {
                                    isToday  -> MaterialTheme.colorScheme.primary
                                    isFuture -> Color.Gray.copy(alpha = 0.3f)
                                    else     -> Color.Gray
                                },
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = dayCal.get(Calendar.DAY_OF_MONTH).toString(),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                color = when {
                                    isToday  -> MaterialTheme.colorScheme.primary
                                    isFuture -> Color.Gray.copy(alpha = 0.3f)
                                    else     -> Color.Gray
                                },
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (todaysMoodEntry != null) {
                    Text(
                        text = "Today: ${todaysMoodEntry.mood}" +
                                todaysMoodEntry.note.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // ── Daily challenge ───────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!dailyChallengeCompleted && !dailyChallengeLoading) {
                        onEarnPoints(5)
                        saveActivity("completed the daily challenge ✨")
                        dailyChallengeCompleted = true
                        prefs.edit().putString("daily_challenge_completed_date", todayKey).commit()
                    }
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EEF0))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Challenge ✨", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (dailyChallengeLoading) "Loading your challenge…" else dailyChallengeText,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp
                        )
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("+5 pts", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Button(
                    onClick = {
                        if (!dailyChallengeCompleted && !dailyChallengeLoading) {
                            onEarnPoints(5)
                            saveActivity("completed the daily challenge ✨")
                            dailyChallengeCompleted = true
                            prefs.edit().putString("daily_challenge_completed_date", todayKey).commit()
                        }
                    },
                    enabled = !dailyChallengeCompleted && !dailyChallengeLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(when {
                        dailyChallengeLoading   -> "Loading..."
                        dailyChallengeCompleted -> "Completed today 💖"
                        else                    -> "Tap to complete"
                    })
                }
            }
        }

        // ── Weekly insights ───────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onInsightsClick() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EEF0))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                            Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Filled.QueryStats, contentDescription = null)
                            }
                        }
                        Column {
                            Text("Weekly Insights 📈", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("See your weekly stats and friend leaderboard", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                        }
                    }
                    Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = null)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Track wins, streaks, and rankings", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Friends activity ──────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Friends Activity 💬", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                when {
                    friendIds.isEmpty() -> Text("Add friends to see their activity.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    activityFeed.isEmpty() -> Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    ) {
                        Text("No friend activity yet today 🌸", modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                    else -> activityFeed.forEach { activity -> ActivityBubble(activity = activity) }
                }
            }
        }
    }

    // ── Mood dialog ───────────────────────────────────────────────────────────
    if (showMoodDialog && selectedMoodDate != null) {
        val targetDate    = selectedMoodDate!!
        val isTargetToday = isSameDay(targetDate, today)
        val dialogTitle   = if (isTargetToday) "How are you feeling today?"
        else "How were you feeling? (${SimpleDateFormat("MMM d", Locale.getDefault()).format(targetDate)})"

        MoodCheckInDialog(
            titleText = dialogTitle,
            onDismiss = { showMoodDialog = false; selectedMoodDate = null },
            onSave = { mood, note ->
                val existing = moodEntries.find { isSameDay(it.date, targetDate) }
                if (existing != null) moodEntries.remove(existing)
                moodEntries.add(MoodEntry(targetDate, mood, note))
                moodEntries.sortBy { it.date.time }
                if (isTargetToday) {
                    onEarnPoints(2)
                    saveActivity("checked in $mood")
                }
                showMoodDialog = false
                selectedMoodDate = null
            }
        )
    }

    // ── Notifications dialog ──────────────────────────────────────────────────
    if (showNotificationsDialog) {
        NotificationsDialog(
            addedYouList = addedYouList,
            notifications = notifications,
            notificationStatus = notificationStatus,
            onDismiss = { showNotificationsDialog = false },
            onAcceptRequest = { item ->
                scope.launch {
                    notificationStatus = try { acceptFriendRequest(item) } catch (_: Exception) { "Couldn't accept request." }
                }
            },
            onDeclineRequest = { item ->
                scope.launch {
                    notificationStatus = try { declineFriendRequest(item) } catch (_: Exception) { "Couldn't decline request." }
                }
            },
            onMarkAllRead = {
                scope.launch {
                    try { markAllNotificationsRead(); notificationStatus = "Marked all as read" }
                    catch (_: Exception) { notificationStatus = "Couldn't update notifications" }
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CircleIconBubble(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(42.dp).clickable { onClick() },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Filled.Person, contentDescription = "Profile")
        }
    }
}

@Composable
private fun NotificationBellBubble(count: Int, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        Surface(
            modifier = Modifier.size(42.dp).clickable { onClick() },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Filled.Notifications, contentDescription = "Notifications")
            }
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (count > 9) "9+" else count.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NotificationsDialog(
    addedYouList: List<AddedYouItem>,
    notifications: List<AppNotification>,
    notificationStatus: String,
    onDismiss: () -> Unit,
    onAcceptRequest: (AddedYouItem) -> Unit,
    onDeclineRequest: (AddedYouItem) -> Unit,
    onMarkAllRead: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Filled.Notifications, contentDescription = null)
                Text("Notifications")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Friend Requests", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (addedYouList.isEmpty()) {
                    Text("No pending requests.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    addedYouList.forEach { item ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(item.fromName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("wants to connect with you", style = MaterialTheme.typography.bodyMedium)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onAcceptRequest(item) }, modifier = Modifier.weight(1f)) { Text("Accept") }
                                    OutlinedButton(onClick = { onDeclineRequest(item) }, modifier = Modifier.weight(1f)) { Text("Decline") }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Activity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (notifications.isNotEmpty()) TextButton(onClick = onMarkAllRead) { Text("Mark all read") }
                }
                if (notifications.isEmpty()) {
                    Text("No notifications yet 🌸", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    notifications.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.read) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = when (item.type) {
                                        "reaction" -> "${item.fromName} reacted ${item.emoji} to your activity"
                                        else -> "New notification"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (item.activityMessage.isNotBlank()) Text("\"${item.activityMessage}\"", style = MaterialTheme.typography.bodyMedium)
                                Text(formatNotificationTimestamp(item.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
                if (notificationStatus.isNotBlank()) Text(notificationStatus, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    )
}

@Composable
private fun ActivityBubble(activity: ActivityItem) {
    val bubbleColor = activityBubbleColor(activity.message)
    val initial = activity.displayName.ifBlank { activity.email.ifBlank { "S" } }.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = bubbleColor)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                    Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        Text(initial, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(activity.displayName.ifBlank { activity.email.ifBlank { "Someone" } }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(formatActivityTimestamp(activity.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            Text(activity.message, style = MaterialTheme.typography.bodyMedium)
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
    val activeReactions = activity.reactions.filterValues { it.isNotEmpty() }.toSortedMap()

    if (activeReactions.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeReactions.forEach { (emoji, uids) ->
                Surface(
                    modifier = Modifier.clickable { selectedEmoji = emoji; showDialog = true },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = emoji, fontSize = 14.sp)
                        Text(text = uids.size.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (showDialog && selectedEmoji == emoji) {
                    LaunchedEffect(selectedEmoji, uids) {
                        val names = uids.mapNotNull { uid ->
                            try {
                                val doc = db.collection("public_users").document(uid).get().await()
                                val nick = doc.getString("nickname")?.trim().orEmpty()
                                if (nick.isNotBlank()) nick else doc.getString("email")
                            } catch (_: Exception) { null }
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
                if (reactorNames.isEmpty()) Text("No names to show yet.")
                else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reactorNames.forEach { name -> Text(name, style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("Close") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddReactionRow(activity: ActivityItem) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    val emojiOptions = listOf("❤️", "🔥", "😂", "👏", "✨", "🥹")

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        emojiOptions.forEach { emoji ->
            val users = activity.reactions[emoji] ?: emptyList()
            val reactedByMe = currentUid != null && users.contains(currentUid)
            Surface(
                modifier = Modifier.clickable { addReaction(activity.id, emoji) },
                shape = RoundedCornerShape(50),
                color = if (reactedByMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            ) {
                Text(text = emoji, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 14.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

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
    val diff = System.currentTimeMillis() - timestamp
    val minute = 60_000L
    val hour = 60 * minute
    return when {
        diff < minute    -> "just now"
        diff < hour      -> "${diff / minute}m ago"
        diff < 24 * hour -> "${diff / hour}h ago"
        else             -> "today"
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(emoji, fontSize = 14.sp)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
private fun MoodCheckInDialog(
    titleText: String = "How are you feeling today?",
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var selectedMood by remember { mutableStateOf("😊") }
    var note by remember { mutableStateOf("") }
    val moods = listOf(
        "😊", "😌", "😐", "😔", "😰", "😤", "😴", "🤩",
        "😢", "😠", "🥰", "😅", "🤒", "🥺", "😶", "🤗"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Choose your mood:", style = MaterialTheme.typography.bodyMedium)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(moods) { mood ->
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedMood == mood) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable { selectedMood = mood },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = mood, fontSize = 26.sp)
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
                Text("+2 Calm Points for checking in", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedMood, note) }, enabled = selectedMood.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun normalizeDay(d: Date): Long {
    val c = Calendar.getInstance().apply {
        time = d
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
}

private fun updateAppOpenStreak(prefs: android.content.SharedPreferences, today: Date): Int {
    val todayDay = normalizeDay(today)
    val lastOpen = prefs.getLong("last_open_day", -1L)
    var streak   = prefs.getInt("open_streak", 0)
    streak = when {
        lastOpen == -1L -> 1
        else -> {
            val diff = ((todayDay - lastOpen) / (24L * 60L * 60L * 1000L)).toInt()
            when (diff) { 0 -> streak; 1 -> streak + 1; else -> 1 }
        }
    }
    prefs.edit().putLong("last_open_day", todayDay).putInt("open_streak", streak).apply()
    return streak
}
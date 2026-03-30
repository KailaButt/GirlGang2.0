package com.example.consolicalm

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.consolicalm.ui.theme.AppScaffold
import com.example.consolicalm.ui.theme.AppTheme
import com.example.consolicalm.ui.theme.ConsoliCalmTheme

enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    REWARDS("Rewards", Icons.Filled.CardGiftcard),
    STUDY("Study", Icons.Filled.Timer),
    TODO("To-Do", Icons.Filled.CheckCircle),
    CALM("Calm", Icons.Filled.Favorite),
    PROFILE("Profile", Icons.Filled.Person)
}

class MainActivity : ComponentActivity() {

    private val PREFS = "consoli_prefs"
    private val KEY_POINTS = "calm_points"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var selectedTheme by remember { mutableStateOf(AppTheme.DEFAULT) }

            ConsoliCalmTheme(appTheme = selectedTheme) {
                AppScaffold {
                    AuthGate {
                        val prefs = remember {
                            getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        }

                        var calmPoints by remember {
                            mutableIntStateOf(prefs.getInt(KEY_POINTS, 240))
                        }

                        LaunchedEffect(calmPoints) {
                            prefs.edit().putInt(KEY_POINTS, calmPoints).apply()
                        }

                        var currentTab by remember { mutableStateOf(AppTab.HOME) }
                        var showInsights by remember { mutableStateOf(false) }

                        val todoItems = remember { mutableStateListOf<TodoItem>() }

                        var focusRunning by remember { mutableStateOf(false) }
                        var breakRunning by remember { mutableStateOf(false) }
                        var strikes by remember { mutableIntStateOf(0) }
                        var pauseRequestToken by remember { mutableIntStateOf(0) }

                        var showStrikeDialog by remember { mutableStateOf(false) }
                        var strikeMessage by remember { mutableStateOf("") }

                        val myWeeklyStats = remember(calmPoints) {
                            WeeklyInsightStats(
                                pointsEarned = calmPoints,
                                focusMinutes = 165,
                                sessionsCompleted = 7,
                                streakDays = 4,
                                bestDay = "Wednesday"
                            )
                        }

                        val weeklyLeaderboard = remember(calmPoints) {
                            listOf(
                                LeaderboardEntry(
                                    name = "Maddie",
                                    points = 420,
                                    sessions = 8
                                ),
                                LeaderboardEntry(
                                    name = "Kaila",
                                    points = calmPoints,
                                    sessions = 7,
                                    isYou = true
                                ),
                                LeaderboardEntry(
                                    name = "Praniksha",
                                    points = 356,
                                    sessions = 6
                                ),
                                LeaderboardEntry(
                                    name = "Kaila B",
                                    points = 312,
                                    sessions = 5
                                )
                            )
                        }

                        fun handleLeaveAttempt(target: AppTab) {
                            val locked = focusRunning && !breakRunning
                            if (!locked) {
                                currentTab = target
                                if (target != AppTab.HOME) {
                                    showInsights = false
                                }
                                return
                            }

                            strikes += 1
                            when (strikes) {
                                1 -> {
                                    strikeMessage =
                                        "⚠️ Focus warning\n\nStay on the Study screen during Focus Time."
                                    showStrikeDialog = true
                                    currentTab = AppTab.STUDY
                                }

                                2 -> {
                                    calmPoints -= 5
                                    strikeMessage =
                                        "🚫 Strike 2\n\nYou left Focus Mode again.\n-5 Calm Points."
                                    showStrikeDialog = true
                                    currentTab = AppTab.STUDY
                                }

                                else -> {
                                    calmPoints -= 10
                                    strikeMessage =
                                        "⛔ Strike 3\n\nTimer paused.\n-10 Calm Points."
                                    showStrikeDialog = true
                                    pauseRequestToken += 1
                                    currentTab = target
                                    if (target != AppTab.HOME) {
                                        showInsights = false
                                    }
                                }
                            }
                        }

                        Scaffold(
                            bottomBar = {
                                NavigationBar(tonalElevation = 6.dp) {
                                    AppTab.entries.forEach { tab ->
                                        NavigationBarItem(
                                            selected = currentTab == tab,
                                            onClick = { handleLeaveAttempt(tab) },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Surface(
                                modifier = Modifier.padding(innerPadding),
                                color = Color.Transparent
                            ) {
                                when (currentTab) {
                                    AppTab.HOME -> {
                                        if (showInsights) {
                                            WeeklyInsightsScreen(
                                                myStats = myWeeklyStats,
                                                leaderboard = weeklyLeaderboard,
                                                onBack = { showInsights = false }
                                            )
                                        } else {
                                            HomeScreen(
                                                calmPoints = calmPoints,
                                                nextRewardGoal = 500,
                                                onProfileClick = { currentTab = AppTab.PROFILE },
                                                onRewardsClick = { currentTab = AppTab.REWARDS },
                                                onTodoClick = { currentTab = AppTab.TODO },
                                                onMeditationClick = { currentTab = AppTab.CALM },
                                                onStudyClick = { currentTab = AppTab.STUDY },
                                                onInsightsClick = { showInsights = true },
                                                onEarnPoints = { earned -> calmPoints += earned }
                                            )
                                        }
                                    }

                                    AppTab.REWARDS -> RewardsScreen(
                                        calmPoints = calmPoints,
                                        onBack = { currentTab = AppTab.HOME }
                                    )

                                    AppTab.PROFILE -> ProfileScreen(
                                        selectedTheme = selectedTheme,
                                        onSelectTheme = { theme -> selectedTheme = theme },
                                        onBack = { currentTab = AppTab.HOME }
                                    )

                                    AppTab.CALM -> MeditationScreen(
                                        onBack = { currentTab = AppTab.HOME },
                                        onEarnPoints = { earned -> calmPoints += earned }
                                    )

                                    AppTab.TODO -> TodoScreen(
                                        items = todoItems,
                                        onAdd = { text ->
                                            if (text.isNotBlank()) {
                                                todoItems.add(TodoItem(text = text.trim()))
                                            }
                                        },
                                        onToggle = { id, checked ->
                                            val idx = todoItems.indexOfFirst { it.id == id }
                                            if (idx != -1) {
                                                todoItems[idx] = todoItems[idx].copy(isDone = checked)
                                            }
                                        },
                                        onEdit = { id, newText ->
                                            val idx = todoItems.indexOfFirst { it.id == id }
                                            if (idx != -1 && newText.isNotBlank()) {
                                                todoItems[idx] = todoItems[idx].copy(text = newText.trim())
                                            }
                                        },
                                        onDelete = { id -> todoItems.removeAll { it.id == id } },
                                        onBack = { currentTab = AppTab.HOME }
                                    )

                                    AppTab.STUDY -> StudyScreen(
                                        onBack = { currentTab = AppTab.HOME },
                                        onSessionComplete = { earned -> calmPoints += earned },
                                        onFocusState = { isRunningFocus, isRunningBreak ->
                                            focusRunning = isRunningFocus || isRunningBreak
                                            breakRunning = isRunningBreak

                                            if (isRunningFocus && currentTab != AppTab.STUDY) {
                                                currentTab = AppTab.STUDY
                                            }
                                        },
                                        pauseRequestToken = pauseRequestToken
                                    )
                                }
                            }
                        }

                        if (showStrikeDialog) {
                            AlertDialog(
                                onDismissRequest = { showStrikeDialog = false },
                                title = { Text("Focus Mode") },
                                text = { Text(strikeMessage) },
                                confirmButton = {
                                    Button(onClick = { showStrikeDialog = false }) {
                                        Text("OK")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
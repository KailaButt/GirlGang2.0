package com.example.consolicalm

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.consolicalm.ui.theme.AppTheme
import com.example.consolicalm.ui.theme.ConsoliCalmTheme

enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    PROFILE("Profile", Icons.Filled.Person),
    STUDY("Study", Icons.Filled.Timer),
    TODO("To-Do", Icons.Filled.CheckCircle),
    CALM("Calm", Icons.Filled.Favorite),
    REWARDS("Rewards", Icons.Filled.CardGiftcard)
}

class MainActivity : ComponentActivity() {

    // ---- SharedPreferences Key ----
    private val PREFS = "consoli_prefs"
    private val KEY_POINTS = "calm_points"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val prefs = remember {
                getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            }

            // ---- Load saved Calm Points (default 240 first time) ----
            var calmPoints by remember {
                mutableIntStateOf(prefs.getInt(KEY_POINTS, 240))
            }

            // ---- Auto-save whenever points change ----
            LaunchedEffect(calmPoints) {
                prefs.edit().putInt(KEY_POINTS, calmPoints).apply()
            }

            // ✅ Theme state (only stored while app is open)
            var selectedTheme by remember { mutableStateOf(AppTheme.DEFAULT) }

            // ✅ Wrap the whole app with the selected theme
            ConsoliCalmTheme(appTheme = selectedTheme) {

                var currentTab by remember { mutableStateOf(AppTab.HOME) }
                val todoItems = remember { mutableStateListOf<TodoItem>() }

                // Focus Lock State
                var focusRunning by remember { mutableStateOf(false) }
                var breakRunning by remember { mutableStateOf(false) }
                var strikes by remember { mutableIntStateOf(0) }
                var pauseRequestToken by remember { mutableIntStateOf(0) }

                var showStrikeDialog by remember { mutableStateOf(false) }
                var strikeMessage by remember { mutableStateOf("") }

                fun handleLeaveAttempt(target: AppTab) {
                    val locked = focusRunning && !breakRunning
                    if (!locked) {
                        currentTab = target
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

                    Surface(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {

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
                                    if (idx != -1)
                                        todoItems[idx] = todoItems[idx].copy(isDone = checked)
                                },
                                onEdit = { id, newText ->
                                    val idx = todoItems.indexOfFirst { it.id == id }
                                    if (idx != -1 && newText.isNotBlank()) {
                                        todoItems[idx] =
                                            todoItems[idx].copy(text = newText.trim())
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

                            AppTab.HOME -> HomeScreen(
                                calmPoints = calmPoints,
                                nextRewardGoal = 500,
                                onProfileClick = { currentTab = AppTab.PROFILE },
                                onRewardsClick = { currentTab = AppTab.REWARDS },
                                onTodoClick = { currentTab = AppTab.TODO },
                                onMeditationClick = { currentTab = AppTab.CALM },
                                onStudyClick = { currentTab = AppTab.STUDY },
                                onEarnPoints = { earned -> calmPoints += earned }
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
                            Button(onClick = { showStrikeDialog = false }) { Text("OK") }
                        }
                    )
                }
            }
        }
    }
}
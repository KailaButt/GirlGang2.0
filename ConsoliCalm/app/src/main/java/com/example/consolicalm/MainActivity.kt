package com.example.consolicalm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.consolicalm.com.example.consolicalm.ui.theme.HomeScreen
import com.example.consolicalm.ui.theme.ConsoliCalmTheme

enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    STUDY("Study", Icons.Filled.Timer),
    TODO("To-Do", Icons.Filled.CheckCircle),
    CALM("Calm", Icons.Filled.Favorite),
    REWARDS("Rewards", Icons.Filled.CardGiftcard)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConsoliCalmTheme {
                var calmPoints by remember { mutableIntStateOf(240) }
                var currentTab by remember { mutableStateOf(AppTab.HOME) }
                val todoItems = remember { mutableStateListOf<TodoItem>() }

                // ---- Focus Lock state shared with StudyScreen ----
                var focusRunning by remember { mutableStateOf(false) } // running + focus only
                var breakRunning by remember { mutableStateOf(false) } // running + break
                var strikes by remember { mutableIntStateOf(0) }
                var pauseRequestToken by remember { mutableIntStateOf(0) } // bump to request pause from Study

                // ---- Popup state ----
                var showStrikeDialog by remember { mutableStateOf(false) }
                var strikeMessage by remember { mutableStateOf("") }

                fun handleLeaveAttempt(target: AppTab) {
                    // Allowed if not in focus running (paused or break)
                    val locked = focusRunning && !breakRunning
                    if (!locked) {
                        currentTab = target
                        return
                    }

                    // They tried to leave during running focus => strike
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
                                "⛔ Strike 3\n\nTimer paused.\n-10 Calm Points.\nYou can leave now, but try again when ready."
                            showStrikeDialog = true

                            // request pause inside StudyScreen
                            pauseRequestToken += 1

                            // Now allow leaving after strike 3
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

                            AppTab.CALM -> MeditationScreen(
                                onBack = { currentTab = AppTab.HOME },
                                onEarnPoints = { earned -> calmPoints += earned }
                            )

                            AppTab.TODO -> TodoScreen(
                                items = todoItems,
                                onAdd = { text ->
                                    if (text.isNotBlank()) todoItems.add(TodoItem(text = text.trim()))
                                },
                                onToggle = { id, checked ->
                                    val idx = todoItems.indexOfFirst { it.id == id }
                                    if (idx != -1) todoItems[idx] = todoItems[idx].copy(isDone = checked)
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
                                // NEW: tell MainActivity whether focus/break is running
                                onFocusState = { isRunningFocus, isRunningBreak ->
                                    focusRunning = isRunningFocus || isRunningBreak
                                    breakRunning = isRunningBreak

                                    // Optional: if focus starts while user is not on study, force them back
                                    if (isRunningFocus && currentTab != AppTab.STUDY) {
                                        currentTab = AppTab.STUDY
                                    }

                                    // reset strikes each time they start a fresh focus (optional)
                                    // if (isRunningFocus) strikes = 0
                                },
                                pauseRequestToken = pauseRequestToken
                            )

                            AppTab.HOME -> HomeScreen(
                                calmPoints = calmPoints,
                                nextRewardGoal = 500,
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




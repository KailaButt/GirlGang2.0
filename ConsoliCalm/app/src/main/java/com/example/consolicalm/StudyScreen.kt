package com.example.consolicalm

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

enum class StudyMode(val focusMinutes: Int, val breakMinutes: Int, val title: String) {
    POMODORO_25_5(25, 5, "Pomodoro 25 / 5"),
    DEEPWORK_60_10(60, 10, "Deep Work 60 / 10")
}

@Composable
fun StudyScreen(
    onBack: () -> Unit,
    onSessionComplete: (Int) -> Unit,
    onFocusState: (isRunningFocus: Boolean, isRunningBreak: Boolean) -> Unit,
    pauseRequestToken: Int
) {
    var selectedMode by remember { mutableStateOf<StudyMode?>(null) }

    if (selectedMode == null) {
        ModeSelectionScreen(
            onSelect = { selectedMode = it },
            onBack = onBack
        )
    } else {
        FocusTimerScreen(
            mode = selectedMode!!,
            onBackToModes = { selectedMode = null },
            onBack = onBack,
            onSessionComplete = onSessionComplete,
            onFocusState = onFocusState,
            pauseRequestToken = pauseRequestToken
        )
    }
}

@Composable
private fun ModeSelectionScreen(
    onSelect: (StudyMode) -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Choose a Study Mode", style = MaterialTheme.typography.headlineSmall)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            onClick = { onSelect(StudyMode.POMODORO_25_5) }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Pomodoro 25 / 5", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("25 min focus → 5 min break → repeat")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            onClick = { onSelect(StudyMode.DEEPWORK_60_10) }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Deep Work 60 / 10", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("60 min focus → 10 min break → repeat")
            }
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun FocusTimerScreen(
    mode: StudyMode,
    onBackToModes: () -> Unit,
    onBack: () -> Unit,
    onSessionComplete: (Int) -> Unit,
    onFocusState: (Boolean, Boolean) -> Unit,
    pauseRequestToken: Int
) {
    val context = LocalContext.current
    val activity = context as Activity

    var isRunning by remember { mutableStateOf(false) }
    var isBreak by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(mode.focusMinutes * 60) }
    var sessionsCompleted by remember { mutableStateOf(0) }

    // Report focus/break lock state to MainActivity
    LaunchedEffect(isRunning, isBreak) {
        val runningFocus = isRunning && !isBreak
        val runningBreak = isRunning && isBreak
        onFocusState(runningFocus, runningBreak)
    }

    // Pause request from MainActivity (strike 3)
    var lastPauseToken by remember { mutableIntStateOf(pauseRequestToken) }
    LaunchedEffect(pauseRequestToken) {
        if (pauseRequestToken != lastPauseToken) {
            lastPauseToken = pauseRequestToken
            isRunning = false // auto pause
        }
    }

    // Keep screen on + immersive ONLY during running focus
    fun setImmersive(enabled: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, !enabled)
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        if (enabled) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(isRunning, isBreak) {
        val focusActive = isRunning && !isBreak

        if (focusActive) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setImmersive(true)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setImmersive(false)
        }

        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setImmersive(false)
        }
    }

    // Timer loop + pomodoro cycling
    LaunchedEffect(isRunning, isBreak, timeLeft) {
        if (!isRunning) return@LaunchedEffect

        while (isRunning && timeLeft > 0) {
            delay(1000)
            timeLeft -= 1
        }

        if (isRunning && timeLeft == 0) {
            if (!isBreak) {
                // Focus finished -> points + switch to break
                sessionsCompleted += 1
                onSessionComplete(10)

                isBreak = true
                timeLeft = mode.breakMinutes * 60
            } else {
                // Break finished -> back to focus (locks again)
                isBreak = false
                timeLeft = mode.focusMinutes * 60
            }
        }
    }

    BackHandler {
        isRunning = false
        onBackToModes()
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(mode.title, style = MaterialTheme.typography.titleLarge)

        Text(
            if (isBreak) "Break Time ☕ (unlocked)" else "Focus Time 📚 (locked)",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(formatTime(timeLeft), style = MaterialTheme.typography.displayLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { isRunning = !isRunning }) {
                Text(if (isRunning) "Pause" else "Start")
            }
            OutlinedButton(onClick = {
                isRunning = false
                isBreak = false
                timeLeft = mode.focusMinutes * 60
                sessionsCompleted = 0
            }) {
                Text("Reset")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Session stats", style = MaterialTheme.typography.titleMedium)
                Text("✅ Focus sessions completed: $sessionsCompleted")
                Text("🍅 Cycle: Focus ${mode.focusMinutes}m → Break ${mode.breakMinutes}m → repeat")
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = {
                isRunning = false
                onBackToModes()
            }) { Text("Back") }

            TextButton(onClick = {
                isRunning = false
                onBack()
            }) { Text("Exit") }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}








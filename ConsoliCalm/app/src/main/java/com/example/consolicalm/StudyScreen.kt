package com.example.consolicalm

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

enum class StudyMode(val focusMinutes: Int, val breakMinutes: Int, val title: String, val subtitle: String) {
    POMODORO_25_5(25, 5, "Pomodoro 25 / 5", "25 min focus → 5 min break → repeat"),
    DEEPWORK_60_10(60, 10, "Deep Work 60 / 10", "60 min focus → 10 min break → repeat"),
    QUICKSTART_5_1(5, 1, "Quick Start 5 / 1", "5 min focus → 1 min break → build momentum")
}

private enum class StudyView { MODES, TIMER, GARDEN_CENTER }

@Composable
fun StudyScreen(
    onBack: () -> Unit,
    onSessionComplete: (Int) -> Unit,
    onFocusState: (isRunningFocus: Boolean, isRunningBreak: Boolean) -> Unit,
    pauseRequestToken: Int
) {
    var selectedMode by remember { mutableStateOf<StudyMode?>(null) }
    var view by remember { mutableStateOf(StudyView.MODES) }

    when (view) {
        StudyView.MODES -> {
            ModeSelectionScreen(
                onSelect = {
                    selectedMode = it
                    view = StudyView.TIMER
                },
                onOpenGardenCenter = { view = StudyView.GARDEN_CENTER },
                onBack = onBack
            )
        }

        StudyView.TIMER -> {
            if (selectedMode == null) {
                view = StudyView.MODES
            } else {
                FocusTimerScreen(
                    mode = selectedMode!!,
                    onBackToModes = {
                        selectedMode = null
                        view = StudyView.MODES
                    },
                    onExit = onBack,
                    onOpenGardenCenter = { view = StudyView.GARDEN_CENTER },
                    onSessionComplete = onSessionComplete,
                    onFocusState = onFocusState,
                    pauseRequestToken = pauseRequestToken
                )
            }
        }

        StudyView.GARDEN_CENTER -> {
            GardenCenterScreen(
                onBackToStudy = {
                    view = if (selectedMode == null) StudyView.MODES else StudyView.TIMER
                }
            )
        }
    }
}

@Composable
private fun ModeSelectionScreen(
    onSelect: (StudyMode) -> Unit,
    onOpenGardenCenter: () -> Unit,
    onBack: () -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            AssistChip(onClick = {}, label = { Text("⭐ ${GardenCenterStore.points}") })
        }

        Text("Choose a Study Mode", style = MaterialTheme.typography.headlineSmall)

        // ✅ Coach card (with recommended mode start)
        ProcrastinationCoachCard(
            onRecommendedMode = { recommended -> onSelect(recommended) }
        )

        ModeCard(mode = StudyMode.POMODORO_25_5, onClick = { onSelect(StudyMode.POMODORO_25_5) })
        ModeCard(mode = StudyMode.DEEPWORK_60_10, onClick = { onSelect(StudyMode.DEEPWORK_60_10) })
        ModeCard(mode = StudyMode.QUICKSTART_5_1, onClick = { onSelect(StudyMode.QUICKSTART_5_1) })

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            onClick = onOpenGardenCenter
        ) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🌿 Garden Center", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Use points to buy new plants and upgrades to grow.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Open ›", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ModeCard(mode: StudyMode, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(mode.title, style = MaterialTheme.typography.titleMedium)
                AssistChip(onClick = onClick, label = { Text("${mode.focusMinutes}m") })
            }
            Text(mode.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FocusTimerScreen(
    mode: StudyMode,
    onBackToModes: () -> Unit,
    onExit: () -> Unit,
    onOpenGardenCenter: () -> Unit,
    onSessionComplete: (Int) -> Unit,
    onFocusState: (Boolean, Boolean) -> Unit,
    pauseRequestToken: Int
) {
    val context = LocalContext.current
    val activity = context as Activity
    val scroll = rememberScrollState()

    var isRunning by remember { mutableStateOf(false) }
    var isBreak by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(mode.focusMinutes * 60) }

    var focusSessionsThisRun by rememberSaveable { mutableIntStateOf(0) }
    var xp by rememberSaveable { mutableIntStateOf(0) }
    var level by rememberSaveable { mutableIntStateOf(1) }

    var studyGoal by rememberSaveable { mutableStateOf("") }
    var showGoalEditor by rememberSaveable { mutableStateOf(true) }

    var showPointsFloat by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, isBreak) {
        onFocusState(isRunning && !isBreak, isRunning && isBreak)
    }

    var lastPauseToken by remember { mutableIntStateOf(pauseRequestToken) }
    LaunchedEffect(pauseRequestToken) {
        if (pauseRequestToken != lastPauseToken) {
            lastPauseToken = pauseRequestToken
            isRunning = false
        }
    }

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

    fun addXp(amount: Int) {
        xp += amount
        while (xp >= 100) {
            xp -= 100
            level += 1
        }
    }

    LaunchedEffect(isRunning, isBreak, timeLeft, mode) {
        if (!isRunning) return@LaunchedEffect

        while (isRunning && timeLeft > 0) {
            delay(1000)
            timeLeft -= 1
        }

        if (isRunning && timeLeft == 0) {
            if (!isBreak) {
                focusSessionsThisRun += 1

                val basePoints = 10
                GardenCenterStore.onFocusSessionComplete(basePoints)
                onSessionComplete(basePoints)

                addXp(25)
                showPointsFloat = true
                showConfetti = true
                delay(900)
                showConfetti = false

                isBreak = true
                timeLeft = mode.breakMinutes * 60
            } else {
                isBreak = false
                timeLeft = mode.focusMinutes * 60
            }
        }
    }

    val totalSeconds = (if (isBreak) mode.breakMinutes else mode.focusMinutes) * 60
    val rawProgress = 1f - (timeLeft.toFloat() / totalSeconds.toFloat())
    val progress by animateFloatAsState(rawProgress.coerceIn(0f, 1f), label = "timerProgress")

    BackHandler {
        isRunning = false
        onBackToModes()
    }

    Box(Modifier.fillMaxSize()) {
        StudyBackground(isBreak = isBreak, isRunningFocus = isRunning && !isBreak)

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            isRunning = false
                            onBackToModes()
                        }) { Text("Back") }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AssistChip(onClick = {}, label = { Text("Lv $level") })
                            AssistChip(onClick = {}, label = { Text("⭐ ${GardenCenterStore.points}") })
                        }
                    }
                }
            },
            bottomBar = {
                Surface(tonalElevation = 6.dp) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    isRunning = !isRunning
                                    if (isRunning && !isBreak) showGoalEditor = false
                                }
                            ) {
                                Text(if (isRunning) "Pause" else "Start")
                            }

                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    isRunning = false
                                    isBreak = false
                                    timeLeft = mode.focusMinutes * 60
                                    focusSessionsThisRun = 0
                                }
                            ) { Text("Restart") }
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onOpenGardenCenter) { Text("Garden Center") }
                            TextButton(onClick = {
                                isRunning = false
                                onExit()
                            }) { Text("Exit") }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scroll),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(14.dp))

                // ✅ Coach card also on timer screen (for mid-session stuck moments)
                ProcrastinationCoachCard(
                    title = "Stuck right now?",
                    onRecommendedMode = null, // already in a mode
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Focus goal", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { showGoalEditor = !showGoalEditor }) {
                                Text(if (showGoalEditor) "Hide" else "Edit")
                            }
                        }

                        if (showGoalEditor) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = studyGoal,
                                    onValueChange = { studyGoal = it },
                                    placeholder = { Text("Ex: Study OS scheduling (30 mins)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    "Tiny goals beat perfect plans. Start the timer and let momentum do the rest.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                if (studyGoal.isBlank()) "No goal set — tap Edit anytime." else studyGoal,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(contentAlignment = Alignment.Center) {
                    AnimatedProgressRing(progress = progress, isBreak = isBreak)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (isBreak) "Break unlocked ☕" else "Focus Mode 🔒",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            formatTime(timeLeft),
                            style = MaterialTheme.typography.displayMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            mode.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    PointsFloat(
                        visible = showPointsFloat,
                        onDone = { showPointsFloat = false },
                        text = "+${10 + (GardenCenterStore.getPlant(GardenCenterStore.selectedPlant)?.upgradeBonusPoints ?: 0)}"
                    )
                }

                Spacer(Modifier.height(14.dp))

                XPBar(xp = xp, level = level, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(14.dp))

                val plant = GardenCenterStore.selectedPlant
                val stage = GardenCenterStore.stageFor(plant)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlantPreview(
                            type = plant,
                            stage = stage,
                            fullyUpgradedPreview = false,
                            modifier = Modifier.size(110.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Currently growing", style = MaterialTheme.typography.titleMedium)
                            Text("${plant.title} • Stage $stage / 4", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Complete sessions to grow it. Spend points in Garden Center.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Run stats", style = MaterialTheme.typography.titleMedium)
                        Text("✅ Focus sessions completed (this run): $focusSessionsThisRun")
                        Text("🌱 Selected plant: ${plant.title}")
                    }
                }

                Spacer(Modifier.height(140.dp))
            }
        }

        ConfettiBurstOverlay(visible = showConfetti)
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}






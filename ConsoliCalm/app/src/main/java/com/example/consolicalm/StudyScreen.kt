package com.example.consolicalm

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val bgTop = Color(0xFFF6F0E8)
    val bgBottom = Color(0xFFECE5DA)
    val cream = Color(0xFFF8F4EE)
    val cream2 = Color(0xFFF2ECE4)
    val deepBlue = Color(0xFF6F8891)
    val blueGray = Color(0xFF536A72)
    val textDark = Color(0xFF3D3A37)
    val subText = Color(0xFF7B8484)
    val border = Color(0xFFE0D8CD)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgTop, bgBottom)
                )
            )
    ) {
        DecorativeStudyBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(
                        "Back",
                        color = deepBlue,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cream,
                    shadowElevation = 2.dp,
                    tonalElevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("⭐")
                        Text(
                            "${GardenCenterStore.points}",
                            color = textDark,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "Choose a Study Mode",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textDark,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Pick the flow that matches your energy right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subText
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = cream.copy(alpha = 0.96f),
                shadowElevation = 4.dp,
                tonalElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Why are you stuck?",
                                style = MaterialTheme.typography.titleLarge,
                                color = textDark
                            )
                            Text(
                                text = "Get a quick reset and a suggested study flow.",
                                style = MaterialTheme.typography.bodySmall,
                                color = subText
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFE6EEF0)
                        ) {
                            Text(
                                text = "",
                                color = deepBlue,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    ProcrastinationCoachCard(
                        onRecommendedMode = { recommended ->
                            onSelect(recommended)
                        }
                    )
                }
            }

            Text(
                text = "Tap a circle to begin",
                color = subText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                val circleSize = 155.dp

                DreamCircle(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(circleSize),
                    title = "Pomodoro",
                    time = "25 / 5",
                    subtitle = "steady rhythm",
                    titleFontSize = 19.sp,
                    timeFontSize = 18.sp,
                    subtitleFontSize = 10.sp,
                    gradient = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD9E5E7),
                            Color(0xFFAEC1C7),
                            Color(0xFF89A3AB)
                        )
                    ),
                    textColor = blueGray,
                    onClick = { onSelect(StudyMode.POMODORO_25_5) }
                )

                DreamCircle(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 10.dp, y = (-6).dp)
                        .size(circleSize),
                    title = "Deep\nWork",
                    time = "60 / 10",
                    subtitle = "locked in",
                    titleFontSize = 18.sp,
                    timeFontSize = 16.sp,
                    subtitleFontSize = 10.sp,
                    gradient = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD9E5E7),
                            Color(0xFFAEC1C7),
                            Color(0xFF89A3AB)
                        )
                    ),
                    textColor = blueGray,
                    onClick = { onSelect(StudyMode.DEEPWORK_60_10) }
                )

                DreamCircle(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-10).dp, y = (-6).dp)
                        .size(circleSize),
                    title = "Quick\nStart",
                    time = "5 / 1",
                    subtitle = "just begin",
                    titleFontSize = 18.sp,
                    timeFontSize = 16.sp,
                    subtitleFontSize = 10.sp,
                    gradient = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD9E5E7),
                            Color(0xFFAEC1C7),
                            Color(0xFF89A3AB)
                        )
                    ),
                    textColor = blueGray,
                    onClick = { onSelect(StudyMode.QUICKSTART_5_1) }
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = cream2.copy(alpha = 0.98f),
                shadowElevation = 4.dp,
                tonalElevation = 0.dp,
                onClick = onOpenGardenCenter,
                border = androidx.compose.foundation.BorderStroke(1.dp, border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFE8F1E6),
                                        Color(0xFFD2E3CC)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌿", style = MaterialTheme.typography.headlineSmall)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Garden Center",
                            style = MaterialTheme.typography.titleMedium,
                            color = textDark
                        )
                        Text(
                            text = "Grow your little world with the points you earn from studying.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = subText
                        )
                    }

                    Text(
                        text = "Open ›",
                        color = deepBlue,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DecorativeStudyBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset(x = (-30).dp, y = 140.dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
        )

        Box(
            modifier = Modifier
                .offset(x = 250.dp, y = 180.dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        )

        Box(
            modifier = Modifier
                .offset(x = 280.dp, y = 520.dp)
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFFDCE5E7).copy(alpha = 0.22f))
        )
    }
}

@Composable
private fun DreamCircle(
    modifier: Modifier = Modifier,
    title: String,
    time: String,
    subtitle: String,
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    timeFontSize: androidx.compose.ui.unit.TextUnit,
    subtitleFontSize: androidx.compose.ui.unit.TextUnit,
    gradient: Brush,
    textColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .shadow(16.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(gradient)
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = 0.55f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = time,
                    fontSize = timeFontSize,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = title,
                    textAlign = TextAlign.Center,
                    fontSize = titleFontSize,
                    lineHeight = if ("\n" in title) titleFontSize.value.plus(3).sp else titleFontSize.value.plus(1).sp,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    textAlign = TextAlign.Center,
                    fontSize = subtitleFontSize,
                    lineHeight = subtitleFontSize.value.plus(2).sp,
                    color = textColor.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
        if (isRunning) {
            MeadowStudyBackground(
                progress = progress,
                isBreak = isBreak
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
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
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                ) {
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
                            ) {
                                Text("Restart")
                            }
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
                    )
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
                    )
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
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
                    )
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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

@Composable
private fun MeadowStudyBackground(
    progress: Float,
    isBreak: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sky_meadow")

    val cloudShift1 by infiniteTransition.animateFloat(
        initialValue = -250f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isBreak) 38000 else 26000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud1"
    )

    val cloudShift2 by infiniteTransition.animateFloat(
        initialValue = 1300f,
        targetValue = -350f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isBreak) 48000 else 34000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud2"
    )

    val clampedProgress = progress.coerceIn(0f, 1f)

    val skyTop = if (isBreak) {
        lerpColor(Color(0xFF2C3E70), Color(0xFF111827), clampedProgress)
    } else {
        lerpColor(Color(0xFF8EDBFF), Color(0xFFFF9E80), clampedProgress)
    }

    val skyMid = if (isBreak) {
        lerpColor(Color(0xFF5B6FAF), Color(0xFF312E81), clampedProgress)
    } else {
        lerpColor(Color(0xFFBEEBFF), Color(0xFFFFC58F), clampedProgress)
    }

    val skyBottom = if (isBreak) {
        lerpColor(Color(0xFF8FA0D8), Color(0xFF4C1D95), clampedProgress)
    } else {
        lerpColor(Color(0xFFEAFBFF), Color(0xFFFFD7B8), clampedProgress)
    }

    val grassColor = if (isBreak) {
        lerpColor(Color(0xFF355C43), Color(0xFF1B3527), clampedProgress)
    } else {
        lerpColor(Color(0xFF6FCF97), Color(0xFF2F855A), clampedProgress)
    }

    val grassHighlight = if (isBreak) {
        Color.White.copy(alpha = 0.03f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

    val sunColor = if (isBreak) Color(0xFFFFE7B0) else Color(0xFFFFD54F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(skyTop, skyMid, skyBottom)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val sunStartX = w * 0.78f
            val sunEndX = w * 0.58f
            val sunStartY = h * 0.18f
            val sunEndY = h * 0.58f

            val sunX = sunStartX + (sunEndX - sunStartX) * clampedProgress
            val sunY = sunStartY + (sunEndY - sunStartY) * clampedProgress
            val sunRadius = if (isBreak) 38f else 50f

            drawCircle(
                color = sunColor,
                radius = sunRadius,
                center = Offset(sunX, sunY)
            )

            drawCloud(
                x = cloudShift1,
                y = h * 0.18f,
                scale = 1.05f,
                color = if (isBreak) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.78f)
            )

            drawCloud(
                x = cloudShift2,
                y = h * 0.28f,
                scale = 0.85f,
                color = if (isBreak) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.68f)
            )

            val grassPath = Path().apply {
                val baseY = h * 0.77f

                moveTo(0f, h)
                lineTo(0f, baseY)

                quadraticBezierTo(
                    w * 0.25f, baseY - 18f,
                    w * 0.50f, baseY + 2f
                )
                quadraticBezierTo(
                    w * 0.75f, baseY + 18f,
                    w, baseY - 8f
                )

                lineTo(w, h)
                close()
            }

            drawPath(
                path = grassPath,
                color = grassColor
            )

            drawRoundRect(
                color = grassHighlight,
                topLeft = Offset(0f, h * 0.76f),
                size = Size(w, 12f),
                cornerRadius = CornerRadius(18f, 18f)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloud(
    x: Float,
    y: Float,
    scale: Float,
    color: Color
) {
    drawCircle(color = color, radius = 28f * scale, center = Offset(x + 35f * scale, y + 18f * scale))
    drawCircle(color = color, radius = 40f * scale, center = Offset(x + 75f * scale, y))
    drawCircle(color = color, radius = 32f * scale, center = Offset(x + 118f * scale, y + 18f * scale))
    drawRoundRect(
        color = color,
        topLeft = Offset(x + 24f * scale, y + 18f * scale),
        size = Size(116f * scale, 34f * scale),
        cornerRadius = CornerRadius(30f, 30f)
    )
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * t,
        green = start.green + (end.green - start.green) * t,
        blue = start.blue + (end.blue - start.blue) * t,
        alpha = start.alpha + (end.alpha - start.alpha) * t
    )
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
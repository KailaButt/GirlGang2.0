package com.example.consolicalm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalmSessionPlayer(
    session: CalmSession,
    isDailyChallenge: Boolean,
    onClose: () -> Unit,
    onCompleted: (earnedPoints: Int) -> Unit
) {
    val initialSeconds = session.estimatedSeconds.coerceAtLeast(30)
    var secondsLeft by remember(session.id) { mutableIntStateOf(initialSeconds) }
    var running by remember(session.id) { mutableStateOf(false) }
    var stepIndex by remember(session.id) { mutableIntStateOf(0) }
    var completed by remember(session.id) { mutableStateOf(false) }

    // Simple “breathing” pulse animation for the session screen.
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Countdown timer
    LaunchedEffect(running, secondsLeft, completed) {
        if (!running || completed) return@LaunchedEffect
        if (secondsLeft <= 0) {
            running = false
            completed = true
            onCompleted(session.points)
            return@LaunchedEffect
        }
        delay(1000)
        secondsLeft -= 1
    }

    fun formatTime(totalSeconds: Int): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%d:%02d".format(m, s)
    }

    // Full-screen dialog to keep the user “in” the Calm experience.
    Dialog(
        onDismissRequest = { if (!running) onClose() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(session.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        val tag = if (isDailyChallenge) "Daily Challenge" else "Guided Session"
                        Text(tag, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { if (!running) onClose() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(session.description)

                        // Pulse bubble + timer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .scale(if (running) pulse else 1f)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(formatTime(secondsLeft), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                Text("remaining", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        val progress = 1f - (secondsLeft.toFloat() / initialSeconds.toFloat())
                        LinearProgressIndicator(progress = progress.coerceIn(0f, 1f))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    running = !running
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (running) "Pause" else "Start")
                            }
                            OutlinedButton(
                                onClick = {
                                    running = false
                                    secondsLeft = initialSeconds
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Reset")
                            }
                        }
                    }
                }

                // Step-by-step card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Step ${stepIndex + 1} of ${session.steps.size}", fontWeight = FontWeight.SemiBold)
                        Text(session.steps[stepIndex], style = MaterialTheme.typography.bodyLarge)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { if (stepIndex > 0) stepIndex -= 1 },
                                enabled = stepIndex > 0
                            ) { Text("Back") }

                            Button(
                                onClick = {
                                    if (stepIndex < session.steps.lastIndex) {
                                        stepIndex += 1
                                    } else {
                                        // Allow manual completion even if timer isn't done.
                                        if (!completed) {
                                            running = false
                                            completed = true
                                            onCompleted(session.points)
                                        }
                                    }
                                }
                            ) {
                                Text(if (stepIndex < session.steps.lastIndex) "Next" else "Finish")
                            }
                        }
                    }
                }

                // Completion banner
                AnimatedVisibility(visible = completed) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Session complete", fontWeight = FontWeight.Bold)
                                    Text("+${session.points} Calm Points")
                                }
                            }
                            Button(onClick = onClose) { Text("Done") }
                        }
                    }
                }
            }
        }
    }
}

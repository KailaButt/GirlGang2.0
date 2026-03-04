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

private data class BreathPhase(val label: String, val seconds: Int)

/**
 * “Quick Calm” 60–90 second reset.
 * A super short, clean timer UI for an extended-exhale reset (inhale 4, exhale 6) with gentle cues.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCalmPlayer(
    onClose: () -> Unit,
    onCompleted: () -> Unit
) {
    // 7 cycles of inhale 4 / exhale 6 = 70 seconds (within the 60–90s target).
    val phases = remember {
        listOf(
            BreathPhase("Inhale", 4),
            BreathPhase("Exhale", 6)
        )
    }
    val cycleSeconds = remember(phases) { phases.sumOf { it.seconds } }
    val totalSeconds = remember(cycleSeconds) { (cycleSeconds * 7).coerceIn(60, 90) }

    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }
    var running by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }

    val elapsed = remember(totalSeconds, secondsLeft) { (totalSeconds - secondsLeft).coerceAtLeast(0) }
    val cycleElapsed = remember(elapsed, cycleSeconds) { if (cycleSeconds == 0) 0 else (elapsed % cycleSeconds) }
    val phaseIndex = remember(cycleElapsed, phases) {
        var remaining = cycleElapsed
        var idx = 0
        for (i in phases.indices) {
            val dur = phases[i].seconds
            if (remaining < dur) {
                idx = i
                break
            }
            remaining -= dur
        }
        idx
    }
    val phaseLabel = phases.getOrNull(phaseIndex)?.label ?: "Breathe"

    // Gentle pulse (stronger during inhale, softer otherwise).
    val infinite = rememberInfiniteTransition(label = "quickCalmPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = if (phaseLabel == "Inhale") 1.08f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(running, secondsLeft, completed) {
        if (!running || completed) return@LaunchedEffect
        if (secondsLeft <= 0) {
            running = false
            completed = true
            onCompleted()
            return@LaunchedEffect
        }
        delay(1000)
        secondsLeft -= 1
    }

    fun formatTime(total: Int): String {
        val m = total / 60
        val s = total % 60
        return "%d:%02d".format(m, s)
    }

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
                        Text("Quick Calm", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Extended exhale (inhale 4, exhale 6)", style = MaterialTheme.typography.bodySmall)
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
                        Text("A ~1-minute reset. Inhale for 4, exhale for 6. Keep it gentle.")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .scale(if (running) pulse else 1f)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(phaseLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text(formatTime(secondsLeft), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                Text("remaining", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        val progress = 1f - (secondsLeft.toFloat() / totalSeconds.toFloat())
                        LinearProgressIndicator(progress = progress.coerceIn(0f, 1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { running = !running },
                                modifier = Modifier.weight(1f),
                                enabled = !completed
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
                                    secondsLeft = totalSeconds
                                    completed = false
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
                                    Text("Nice work", fontWeight = FontWeight.Bold)
                                    Text("Quick reset complete")
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

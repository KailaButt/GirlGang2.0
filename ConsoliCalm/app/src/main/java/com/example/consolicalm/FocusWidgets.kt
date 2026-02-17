package com.example.consolicalm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun StudyBackground(
    isBreak: Boolean,
    isRunningFocus: Boolean,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "bgPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.06f,
        targetValue = if (isRunningFocus) 0.12f else 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val base = if (isBreak) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background
    val overlay = if (isBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    Box(
        modifier
            .fillMaxSize()
            .background(base)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .alpha(pulse)
                .background(overlay)
                .blur(90.dp)
        )
    }
}

@Composable
fun AnimatedProgressRing(
    progress: Float,
    isBreak: Boolean,
    size: Dp = 260.dp,
    stroke: Dp = 14.dp
) {
    val ringColor = if (isBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = Modifier.size(size)) {
        val diameter = this.size.minDimension
        val topLeft = Offset(
            (this.size.width - diameter) / 2f,
            (this.size.height - diameter) / 2f
        )
        val strokePx = stroke.toPx()

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        drawArc(
            color = ringColor,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun XPBar(xp: Int, level: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("XP", style = MaterialTheme.typography.titleSmall)
            Text("Level $level • ${xp}/100", style = MaterialTheme.typography.titleSmall)
        }
        LinearProgressIndicator(
            progress = (xp / 100f).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Finish sessions to level up. Higher levels can unlock themes/badges later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PointsFloat(
    visible: Boolean,
    onDone: () -> Unit,
    text: String = "+10"
) {
    AnimatedVisibility(visible = visible) {
        val anim = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            anim.snapTo(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
            onDone()
        }

        val y = lerp(0f, -70f, anim.value)
        val a = lerp(1f, 0f, anim.value)

        Box(
            Modifier
                .offset(y = y.dp)
                .alpha(a)
        ) {
            Surface(
                tonalElevation = 6.dp,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "⭐ $text",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@Composable
fun FocusTree(
    sessionsCompleted: Int,
    modifier: Modifier = Modifier
) {
    val stage = when {
        sessionsCompleted <= 0 -> 0
        sessionsCompleted == 1 -> 1
        sessionsCompleted in 2..3 -> 2
        sessionsCompleted in 4..6 -> 3
        else -> 4
    }

    val scale by animateFloatAsState(
        targetValue = 1f + stage * 0.06f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "juniperScale"
    )

    // ✅ Requested palette
    val potClay = Color(0xFFB85A3D)       // clay red-brown
    val potClayDark = Color(0xFF8F3E2B)   // rim/shadow
    val trunkBrown = Color(0xFF6B3F2A)    // trunk
    val trunkDark = Color(0xFF4E2A1C)     // trunk shadow/branch
    val juniperGreen = Color(0xFF2E7D32)  // main green
    val juniperDeep = Color(0xFF1B5E20)   // deeper green
    val juniperLight = Color(0xFF43A047)  // highlight green
    val berryBlue = Color(0xFF3F6E9E)     // juniper berry vibe
    val pebble = Color(0xFF6D6D6D)        // rocks

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        val potTopY = h * 0.74f
        val rimW = w * 0.78f * scale
        val rimH = h * 0.085f * scale

        val bodyTopW = w * 0.68f * scale
        val bodyBotW = w * 0.56f * scale
        val bodyH = h * 0.16f * scale

        // Rim
        drawRoundRect(
            color = potClayDark,
            topLeft = Offset(cx - rimW / 2f, potTopY),
            size = Size(rimW, rimH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f, w * 0.10f),
            style = Fill
        )

        // Body trapezoid
        val potBody = Path().apply {
            moveTo(cx - bodyTopW / 2f, potTopY + rimH)
            lineTo(cx + bodyTopW / 2f, potTopY + rimH)
            lineTo(cx + bodyBotW / 2f, potTopY + rimH + bodyH)
            lineTo(cx - bodyBotW / 2f, potTopY + rimH + bodyH)
            close()
        }
        drawPath(potBody, color = potClay, style = Fill)

        // Soil line
        drawLine(
            color = potClayDark,
            start = Offset(cx - bodyTopW * 0.42f, potTopY + rimH * 1.20f),
            end = Offset(cx + bodyTopW * 0.42f, potTopY + rimH * 1.20f),
            strokeWidth = w * 0.028f,
            cap = StrokeCap.Round
        )

        val baseY = potTopY + rimH * 1.05f
        val trunkWidth = w * 0.085f * scale

        val trunkPath = Path().apply {
            moveTo(cx - w * 0.05f * scale, baseY)
            cubicTo(
                cx - w * 0.25f * scale, h * 0.62f,
                cx + w * 0.18f * scale, h * 0.58f,
                cx + w * 0.02f * scale, h * 0.50f
            )
            cubicTo(
                cx - w * 0.02f * scale, h * 0.44f,
                cx + w * 0.30f * scale, h * 0.46f,
                cx + w * 0.16f * scale, h * 0.34f
            )
        }

        drawPath(
            path = trunkPath,
            color = trunkBrown,
            style = Stroke(width = trunkWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // trunk shadow for depth
        drawPath(
            path = trunkPath,
            color = trunkDark,
            style = Stroke(width = trunkWidth * 0.40f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Branches (windswept)
        fun branch(start: Offset, end: Offset, width: Float) {
            drawLine(color = trunkDark, start = start, end = end, strokeWidth = width, cap = StrokeCap.Round)
        }

        val b1s = Offset(cx + w * 0.06f * scale, h * 0.50f)
        branch(b1s, Offset(b1s.x + w * 0.22f * scale, b1s.y - h * 0.10f * scale), trunkWidth * 0.50f)

        val b2s = Offset(cx - w * 0.02f * scale, h * 0.56f)
        branch(b2s, Offset(b2s.x - w * 0.20f * scale, b2s.y - h * 0.07f * scale), trunkWidth * 0.42f)

        if (stage >= 2) {
            val b3s = Offset(cx + w * 0.12f * scale, h * 0.42f)
            branch(b3s, Offset(b3s.x + w * 0.18f * scale, b3s.y - h * 0.06f * scale), trunkWidth * 0.36f)
        }

        fun pad(center: Offset, r: Float) {
            // “cloud pad” with layered greens
            drawCircle(color = juniperDeep, radius = r, center = center)
            drawCircle(color = juniperGreen, radius = r * 0.78f, center = Offset(center.x - r * 0.35f, center.y + r * 0.12f))
            drawCircle(color = juniperGreen, radius = r * 0.72f, center = Offset(center.x + r * 0.35f, center.y + r * 0.10f))
            drawCircle(color = juniperLight, radius = r * 0.45f, center = Offset(center.x + r * 0.18f, center.y - r * 0.18f))
        }

        val rMain = w * 0.15f * scale
        val rSmall = w * 0.11f * scale

        // anchor around top-right (juniper often windswept)
        val crown = Offset(cx + w * 0.18f * scale, h * 0.36f)

        // Always show 2 pads minimum
        pad(Offset(crown.x, crown.y + h * 0.06f * scale), rMain)
        pad(Offset(crown.x + w * 0.18f * scale, crown.y + h * 0.10f * scale), rSmall)

        if (stage >= 1) {
            pad(Offset(crown.x - w * 0.16f * scale, crown.y + h * 0.14f * scale), rSmall)
        }
        if (stage >= 2) {
            pad(Offset(crown.x + w * 0.28f * scale, crown.y + h * 0.02f * scale), rSmall * 0.95f)
        }
        if (stage >= 3) {
            pad(Offset(crown.x + w * 0.05f * scale, crown.y - h * 0.06f * scale), rSmall * 0.90f)
        }
        if (stage >= 4) {
            // berries + extra highlights
            drawCircle(color = berryBlue, radius = w * 0.018f * scale, center = Offset(crown.x + w * 0.10f * scale, crown.y + h * 0.08f * scale))
            drawCircle(color = berryBlue, radius = w * 0.016f * scale, center = Offset(crown.x + w * 0.26f * scale, crown.y + h * 0.12f * scale))
            drawCircle(color = berryBlue, radius = w * 0.014f * scale, center = Offset(crown.x - w * 0.08f * scale, crown.y + h * 0.18f * scale))

            // pot rocks (adds realism)
            drawCircle(color = pebble, radius = w * 0.036f, center = Offset(cx - bodyTopW * 0.22f, potTopY + rimH * 1.42f))
            drawCircle(color = pebble, radius = w * 0.028f, center = Offset(cx - bodyTopW * 0.05f, potTopY + rimH * 1.38f))
        }
    }
}

@Composable
fun ConfettiBurstOverlay(visible: Boolean) {
    if (!visible) return

    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    val anim = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        particles.clear()
        val rnd = Random(System.currentTimeMillis())
        repeat(26) {
            particles += ConfettiParticle(
                angle = rnd.nextFloat() * (2f * Math.PI.toFloat()),
                speed = 260f + rnd.nextFloat() * 420f,
                size = 6f + rnd.nextFloat() * 10f
            )
        }
        anim.snapTo(0f)
        anim.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
        delay(50)
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.95f)
    ) {
        val t = anim.value
        val cx = size.width * 0.5f
        val cy = size.height * 0.32f

        particles.forEachIndexed { idx, p ->
            val dist = p.speed * t
            val x = cx + cos(p.angle) * dist
            val y = cy + sin(p.angle) * dist + (900f * t * t)

            val c = when (idx % 3) {
                0 -> primary
                1 -> secondary
                else -> tertiary
            }

            drawCircle(
                color = c,
                center = Offset(x, y),
                radius = p.size * (1f - t * 0.35f)
            )
        }
    }
}

private data class ConfettiParticle(
    val angle: Float,
    val speed: Float,
    val size: Float
)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

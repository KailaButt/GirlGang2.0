package com.example.consolicalm

import android.content.Context
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Palette: muted blues, tans, creams ──────────────────────────────────────
private val ArcadeCream    = Color(0xFFF5F0E8)
private val ArcadeTan      = Color(0xFFD4C5A9)
private val ArcadeTanDark  = Color(0xFFB8A888)
private val ArcadeBlue     = Color(0xFF6B8FA8)
private val ArcadeBlueDark = Color(0xFF4A6B80)
private val ArcadeBlueDeep = Color(0xFF2E4A5C)
private val ArcadeSage     = Color(0xFF7E9E8E)
private val ArcadeDust     = Color(0xFFE8DFD0)
private val ArcadeInk      = Color(0xFF2C3A42)
private val ArcadeGlow     = Color(0xFF8FB8CC)

// ─── Shared prefs keys ────────────────────────────────────────────────────────
private const val GAMES_PREFS = "games_daily_prefs"
private const val KEY_MEMORY_COMPLETED_DATE = "memory_garden_completed_date"
private const val KEY_CONFLICT_COMPLETED_DATE = "conflict_mode_completed_date"
private const val KEY_WORDRAIN_COMPLETED_DATE = "word_rain_completed_date"

// ─── Data classes ─────────────────────────────────────────────────────────────
data class GameMenuItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val status: String,
    val isAvailable: Boolean,
    val monsterType: MonsterType = MonsterType.BLOB
)

enum class MonsterType { BLOB, HORNS, CYCLOPS, GHOST, DROPLET }

data class MemoryGardenChallenge(
    val title: String,
    val subtitle: String,
    val symbols: List<String>,
    val rewardPoints: Int,
    val suggestedGoal: Int,
    val pairs: Int
)

data class MemoryGardenCard(
    val id: Int,
    val symbol: String,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

enum class ConflictRuleMode { COLOR, ARROW, CONFLICT }

data class ConflictRound(
    val word: String,
    val wordColor: Color,
    val arrowDir: String,
    val activeMode: ConflictRuleMode,
    val correctAnswer: String
)

// ═════════════════════════════════════════════════════════════════════════════
//  TOP-LEVEL NAV
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun GamesScreen(onEarnPoints: (Int) -> Unit) {
    var selectedGameId by remember { mutableStateOf<String?>(null) }
    when (selectedGameId) {
        "memory_garden" -> MemoryGardenMatchScreen(
            onBackToGames = { selectedGameId = null },
            onEarnPoints = onEarnPoints
        )
        "conflict_mode" -> ConflictModeScreen(
            onBackToGames = { selectedGameId = null },
            onEarnPoints = onEarnPoints
        )
        "times_square" -> TimesSquareWordChallengeScreen(
            onBack = { selectedGameId = null },
            onEarnPoints = onEarnPoints
        )
        "word_rain" -> WordRainScreen(
            onBackToGames = { selectedGameId = null },
            onEarnPoints = onEarnPoints
        )
        else -> GamesHubScreen(onSelectGame = { selectedGameId = it })
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  ARCADE HUB
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun GamesHubScreen(onSelectGame: (String) -> Unit) {
    val games = remember {
        listOf(
            GameMenuItem("memory_garden", "Memory Garden", "Match calming symbols to clear today's daily board. Builds focus & memory.", "PLAY", true, MonsterType.BLOB),
            GameMenuItem("conflict_mode", "Conflict Mode", "Fight your instincts — ink color, opposite arrows, or BOTH. Builds cognitive flex.", "PLAY", true, MonsterType.CYCLOPS),
            GameMenuItem("times_square", "Times Square", "A random word appears — write it as synonym, sentence, rhyme & definition. Beat the clock!", "PLAY", true, MonsterType.HORNS),
            GameMenuItem("word_rain", "Word Rain", "Words fall fast — tap only the ones that fit the category. Builds focus & quick thinking.", "PLAY", true, MonsterType.DROPLET)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hub")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "float"
    )

    Box(modifier = Modifier.fillMaxSize().background(ArcadeCream)) {
        // Dot grid background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 28.dp.toPx()
            val cols = (size.width / spacing).toInt() + 1
            val rows = (size.height / spacing).toInt() + 1
            for (r in 0..rows) for (c in 0..cols) {
                drawCircle(ArcadeTan.copy(alpha = 0.5f), 2.dp.toPx(), Offset(c * spacing, r * spacing))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Arcade header ─────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(ArcadeBlueDeep, ArcadeBlueDark, ArcadeBlue)))
                    .border(2.dp, ArcadeGlow.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                    listOf(Offset(28f, 22f), Offset(size.width - 38f, 16f), Offset(size.width - 78f, 54f), Offset(48f, 56f), Offset(size.width / 2 + 70f, 12f)).forEachIndexed { i, pos ->
                        val pulse = 0.5f + 0.5f * sin((floatAnim * 2 * Math.PI + i).toFloat())
                        val r = (3.5f + 2f * pulse).dp.toPx()
                        for (a in 0..3) {
                            val angle = a * (Math.PI / 2).toFloat()
                            drawLine(ArcadeGlow.copy(alpha = 0.25f + 0.45f * pulse), pos, Offset(pos.x + r * cos(angle), pos.y + r * sin(angle)), 1.5.dp.toPx())
                        }
                        drawCircle(ArcadeGlow.copy(alpha = 0.3f + 0.4f * pulse), r * 0.35f, pos)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("BRAIN ARCADE", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ArcadeCream, letterSpacing = 5.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ArcadeGlow.copy(alpha = 0.18f)).padding(horizontal = 12.dp, vertical = 3.dp)) {
                        Text("Boost Your Brainpower", fontSize = 10.sp, color = ArcadeGlow, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Marquee strip ─────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ArcadeInk).padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text("✦ DAILY CHALLENGES  ·  EARN POINTS  ·  BRAIN BUILD  ✦  DAILY CHALLENGES  ·  EARN POINTS  ·", fontSize = 10.sp, color = ArcadeGlow.copy(alpha = 0.85f), letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }

            // ── Game cards ────────────────────────────────────────────────────
            games.forEachIndexed { index, game ->
                val isLocked = !game.isAvailable
                val floatOffset = if (!isLocked) (sin((floatAnim * 2 * Math.PI + index * 1.2).toFloat()) * 3).dp else 0.dp
                val blinkAnim by rememberInfiniteTransition(label = "blink_$index").animateFloat(
                    initialValue = 0f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(2400 + index * 300, easing = LinearEasing), RepeatMode.Restart),
                    label = "blink"
                )
                val eyeOpen = blinkAnim < 0.92f

                Box(
                    modifier = Modifier.fillMaxWidth().offset(y = floatOffset)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isLocked) ArcadeDust else ArcadeCream)
                        .border(if (isLocked) 1.5.dp else 2.dp, if (isLocked) ArcadeTan.copy(alpha = 0.3f) else ArcadeBlue.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                        .clickable(enabled = !isLocked) { onSelectGame(game.id) }
                ) {
                    if (!isLocked) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                            val s = 4.dp.toPx(); val x = 12.dp.toPx(); val y = 10.dp.toPx()
                            drawRect(ArcadeBlue.copy(alpha = 0.2f), Offset(x, y), Size(s, s))
                            drawRect(ArcadeBlue.copy(alpha = 0.2f), Offset(x + s + 2.dp.toPx(), y), Size(s, s))
                            drawRect(ArcadeBlue.copy(alpha = 0.2f), Offset(x, y + s + 2.dp.toPx()), Size(s, s))
                            val rx = size.width - 24.dp.toPx()
                            drawRect(ArcadeBlue.copy(alpha = 0.2f), Offset(rx, y), Size(s, s))
                            drawRect(ArcadeBlue.copy(alpha = 0.2f), Offset(rx + s + 2.dp.toPx(), y), Size(s, s))
                            drawRect(ArcadeBlue.copy(alpha = 0.2f), Offset(rx, y + s + 2.dp.toPx()), Size(s, s))
                        }
                    }

                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val bodyColor = if (isLocked) ArcadeTan.copy(alpha = 0.5f) else when (game.monsterType) {
                            MonsterType.BLOB    -> ArcadeBlue
                            MonsterType.CYCLOPS -> ArcadeSage
                            MonsterType.HORNS   -> ArcadeTanDark
                            MonsterType.GHOST   -> ArcadeGlow.copy(alpha = 0.8f)
                            MonsterType.DROPLET -> Color(0xFF5BA3C9)
                        }
                        Canvas(modifier = Modifier.size(72.dp)) {
                            when (game.monsterType) {
                                MonsterType.BLOB    -> drawBlobMonster(bodyColor, ArcadeCream, eyeOpen, floatAnim, index)
                                MonsterType.CYCLOPS -> drawCyclopsMonster(bodyColor, ArcadeCream, eyeOpen, floatAnim, index)
                                MonsterType.HORNS   -> drawHornsMonster(bodyColor, ArcadeCream, eyeOpen, floatAnim, index)
                                MonsterType.GHOST   -> drawGhostMonster(bodyColor, ArcadeCream, eyeOpen, floatAnim, index)
                                MonsterType.DROPLET -> drawDropletMonster(bodyColor, ArcadeCream, eyeOpen, floatAnim, index)
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(game.title.uppercase(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (isLocked) ArcadeInk.copy(alpha = 0.35f) else ArcadeInk, letterSpacing = 1.5.sp)
                            Text(game.subtitle, fontSize = 12.sp, color = if (isLocked) ArcadeInk.copy(alpha = 0.3f) else ArcadeBlue.copy(alpha = 0.85f), lineHeight = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isLocked) ArcadeTan.copy(alpha = 0.4f) else ArcadeBlueDeep).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(if (isLocked) "🔒  LOCKED" else "▶  ${game.status}", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = if (isLocked) ArcadeInk.copy(alpha = 0.4f) else ArcadeCream, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("✦ Brain games refresh daily ✦", fontSize = 11.sp, color = ArcadeBlue.copy(alpha = 0.55f), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ─── Monster draw functions ───────────────────────────────────────────────────

private fun DrawScope.drawBlobMonster(body: Color, accent: Color, eyeOpen: Boolean, t: Float, idx: Int) {
    val cx = size.width / 2; val cy = size.height / 2 + 4.dp.toPx(); val r = 28.dp.toPx()
    val path = Path().apply {
        val pts = 8
        for (i in 0..pts) {
            val angle = (i.toFloat() / pts) * 2 * Math.PI
            val wobble = 1f + 0.08f * sin((t * 2 * Math.PI * 2 + i * 1.3 + idx).toFloat())
            val px = cx + (r * wobble * cos(angle)).toFloat(); val py = cy + (r * wobble * sin(angle)).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }; close()
    }
    drawPath(path, body)
    drawPath(path, ArcadeInk.copy(alpha = 0.15f), style = Stroke(2.dp.toPx()))
    val eyeY = cy - 6.dp.toPx(); val eyeH = if (eyeOpen) 6.dp.toPx() else 1.5.dp.toPx()
    drawRoundRect(ArcadeInk, Offset(cx - 13.dp.toPx(), eyeY - eyeH / 2), Size(7.dp.toPx(), eyeH), CornerRadius(3.dp.toPx()))
    drawRoundRect(ArcadeInk, Offset(cx + 5.dp.toPx(), eyeY - eyeH / 2), Size(7.dp.toPx(), eyeH), CornerRadius(3.dp.toPx()))
    val smilePath = Path().apply { moveTo(cx - 8.dp.toPx(), cy + 8.dp.toPx()); quadraticBezierTo(cx, cy + 14.dp.toPx(), cx + 8.dp.toPx(), cy + 8.dp.toPx()) }
    drawPath(smilePath, ArcadeInk, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    drawLine(ArcadeInk.copy(alpha = 0.5f), Offset(cx, cy - r), Offset(cx + 5.dp.toPx(), cy - r - 10.dp.toPx()), 1.5.dp.toPx())
    drawCircle(body, 4.dp.toPx(), Offset(cx + 5.dp.toPx(), cy - r - 13.dp.toPx()))
    drawCircle(ArcadeInk.copy(alpha = 0.4f), 4.dp.toPx(), Offset(cx + 5.dp.toPx(), cy - r - 13.dp.toPx()), style = Stroke(1.dp.toPx()))
}

private fun DrawScope.drawCyclopsMonster(body: Color, accent: Color, eyeOpen: Boolean, t: Float, idx: Int) {
    val cx = size.width / 2; val cy = size.height / 2 + 2.dp.toPx(); val w = 30.dp.toPx(); val h = 26.dp.toPx()
    drawRoundRect(body, Offset(cx - w, cy - h), Size(w * 2, h * 2), CornerRadius(10.dp.toPx()))
    drawRoundRect(ArcadeInk.copy(alpha = 0.15f), Offset(cx - w, cy - h), Size(w * 2, h * 2), CornerRadius(10.dp.toPx()), style = Stroke(2.dp.toPx()))
    val eyeR = 11.dp.toPx()
    drawCircle(accent, eyeR, Offset(cx, cy - 2.dp.toPx()))
    if (eyeOpen) {
        drawCircle(ArcadeInk, 6.dp.toPx(), Offset(cx, cy - 2.dp.toPx()))
        drawCircle(accent.copy(alpha = 0.9f), 2.dp.toPx(), Offset(cx + 3.dp.toPx(), cy - 5.dp.toPx()))
    } else {
        drawLine(ArcadeInk, Offset(cx - eyeR + 3.dp.toPx(), cy - 2.dp.toPx()), Offset(cx + eyeR - 3.dp.toPx(), cy - 2.dp.toPx()), 2.5.dp.toPx())
    }
    drawCircle(ArcadeInk.copy(alpha = 0.2f), eyeR, Offset(cx, cy - 2.dp.toPx()), style = Stroke(1.5.dp.toPx()))
    drawRoundRect(body, Offset(cx - w + 4.dp.toPx(), cy + h - 2.dp.toPx()), Size(10.dp.toPx(), 8.dp.toPx()), CornerRadius(4.dp.toPx()))
    drawRoundRect(body, Offset(cx + w - 14.dp.toPx(), cy + h - 2.dp.toPx()), Size(10.dp.toPx(), 8.dp.toPx()), CornerRadius(4.dp.toPx()))
}

private fun DrawScope.drawHornsMonster(body: Color, accent: Color, eyeOpen: Boolean, t: Float, idx: Int) {
    val cx = size.width / 2; val cy = size.height / 2 + 4.dp.toPx(); val r = 24.dp.toPx()
    val hornPath = Path().apply {
        moveTo(cx - 14.dp.toPx(), cy - r + 4.dp.toPx()); lineTo(cx - 20.dp.toPx(), cy - r - 14.dp.toPx()); lineTo(cx - 6.dp.toPx(), cy - r + 2.dp.toPx()); close()
        moveTo(cx + 14.dp.toPx(), cy - r + 4.dp.toPx()); lineTo(cx + 20.dp.toPx(), cy - r - 14.dp.toPx()); lineTo(cx + 6.dp.toPx(), cy - r + 2.dp.toPx()); close()
    }
    drawPath(hornPath, body)
    drawCircle(body, r, Offset(cx, cy))
    drawCircle(ArcadeInk.copy(alpha = 0.15f), r, Offset(cx, cy), style = Stroke(2.dp.toPx()))
    val eyeH = if (eyeOpen) 5.dp.toPx() else 1.5.dp.toPx()
    drawRoundRect(ArcadeInk, Offset(cx - 12.dp.toPx(), cy - 6.dp.toPx() - eyeH / 2), Size(7.dp.toPx(), eyeH), CornerRadius(3.dp.toPx()))
    drawRoundRect(ArcadeInk, Offset(cx + 5.dp.toPx(), cy - 6.dp.toPx() - eyeH / 2), Size(7.dp.toPx(), eyeH), CornerRadius(3.dp.toPx()))
    val frownPath = Path().apply { moveTo(cx - 8.dp.toPx(), cy + 10.dp.toPx()); quadraticBezierTo(cx, cy + 5.dp.toPx(), cx + 8.dp.toPx(), cy + 10.dp.toPx()) }
    drawPath(frownPath, ArcadeInk, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawGhostMonster(body: Color, accent: Color, eyeOpen: Boolean, t: Float, idx: Int) {
    val cx = size.width / 2; val cy = size.height / 2; val w = 26.dp.toPx()
    val ghostPath = Path().apply {
        moveTo(cx - w, cy + 20.dp.toPx()); lineTo(cx - w, cy - 10.dp.toPx())
        quadraticBezierTo(cx - w, cy - 28.dp.toPx(), cx, cy - 28.dp.toPx())
        quadraticBezierTo(cx + w, cy - 28.dp.toPx(), cx + w, cy - 10.dp.toPx())
        lineTo(cx + w, cy + 20.dp.toPx())
        val step = w * 2 / 3; val amp = 5.dp.toPx()
        quadraticBezierTo(cx + w - step / 2, cy + 20.dp.toPx() + amp, cx + step, cy + 20.dp.toPx())
        quadraticBezierTo(cx + step / 2, cy + 20.dp.toPx() - amp, cx, cy + 20.dp.toPx())
        quadraticBezierTo(cx - step / 2, cy + 20.dp.toPx() + amp, cx - step, cy + 20.dp.toPx())
        quadraticBezierTo(cx - w + step / 2, cy + 20.dp.toPx() - amp, cx - w, cy + 20.dp.toPx()); close()
    }
    drawPath(ghostPath, body)
    drawPath(ghostPath, ArcadeInk.copy(alpha = 0.12f), style = Stroke(2.dp.toPx()))
    val eyeH = if (eyeOpen) 6.dp.toPx() else 1.5.dp.toPx()
    drawOval(ArcadeInk, Offset(cx - 14.dp.toPx(), cy - 8.dp.toPx() - eyeH / 2), Size(8.dp.toPx(), eyeH))
    drawOval(ArcadeInk, Offset(cx + 5.dp.toPx(), cy - 8.dp.toPx() - eyeH / 2), Size(8.dp.toPx(), eyeH))
}

// ─── NEW: Droplet monster for Word Rain ───────────────────────────────────────
private fun DrawScope.drawDropletMonster(body: Color, accent: Color, eyeOpen: Boolean, t: Float, idx: Int) {
    val cx = size.width / 2; val cy = size.height / 2 + 2.dp.toPx()
    val bob = sin((t * 2 * Math.PI * 1.5 + idx).toFloat()) * 3.dp.toPx()
    val dropPath = Path().apply {
        moveTo(cx, cy - 26.dp.toPx() + bob)
        cubicTo(cx + 18.dp.toPx(), cy - 8.dp.toPx() + bob, cx + 20.dp.toPx(), cy + 10.dp.toPx() + bob, cx, cy + 22.dp.toPx() + bob)
        cubicTo(cx - 20.dp.toPx(), cy + 10.dp.toPx() + bob, cx - 18.dp.toPx(), cy - 8.dp.toPx() + bob, cx, cy - 26.dp.toPx() + bob)
        close()
    }
    drawPath(dropPath, Brush.verticalGradient(listOf(body.copy(alpha = 0.9f), body), cy - 26.dp.toPx() + bob, cy + 22.dp.toPx() + bob))
    drawPath(dropPath, body.copy(alpha = 0.3f), style = Stroke(2.dp.toPx()))
    drawCircle(accent.copy(alpha = 0.55f), 4.dp.toPx(), Offset(cx - 5.dp.toPx(), cy - 12.dp.toPx() + bob))
    val eyeY = cy + bob; val eyeH = if (eyeOpen) 5.dp.toPx() else 1.2.dp.toPx()
    drawRoundRect(ArcadeInk, Offset(cx - 9.dp.toPx(), eyeY - eyeH / 2), Size(5.dp.toPx(), eyeH), CornerRadius(2.dp.toPx()))
    drawRoundRect(ArcadeInk, Offset(cx + 4.dp.toPx(), eyeY - eyeH / 2), Size(5.dp.toPx(), eyeH), CornerRadius(2.dp.toPx()))
    val smilePath = Path().apply { moveTo(cx - 6.dp.toPx(), cy + 8.dp.toPx() + bob); quadraticBezierTo(cx, cy + 13.dp.toPx() + bob, cx + 6.dp.toPx(), cy + 8.dp.toPx() + bob) }
    drawPath(smilePath, ArcadeInk, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    for (i in 0..2) {
        val dropX = cx + (i - 1) * 10.dp.toPx()
        val dropY = cy + 28.dp.toPx() + i * 7.dp.toPx() + sin((t * 2 * Math.PI * 2 + i * 1.5f).toFloat()) * 4.dp.toPx()
        drawOval(body.copy(alpha = 0.4f), Offset(dropX - 2.dp.toPx(), dropY), Size(4.dp.toPx(), 6.dp.toPx()))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  WORD RAIN GAME  — redesigned
// ═════════════════════════════════════════════════════════════════════════════

// ── Palette ───────────────────────────────────────────────────────────────────
private val WRBackground  = Color(0xFF0D1B2A)
private val WRPanelBg     = Color(0xFF112236)
private val WRAccent      = Color(0xFF6EC6E6)
private val WRAccentLight = Color(0xFFB0E0F5)
private val WRCorrect     = Color(0xFF6EDFA8)
private val WRWrong       = Color(0xFFFF7285)
private val WRNeutral     = Color(0xFF1E3A50)
private val WRGold        = Color(0xFFFFD166)
private val WRTextLight   = Color(0xFFEDF6FF)
private val WRSoft        = Color(0xFF2A4F6A)

data class RainWord(
    val id: Int,
    val text: String,
    val isTarget: Boolean,
    val lane: Int,
    var yFraction: Float,
    var tapped: Boolean = false,
    var correct: Boolean = false,
    var splashProgress: Float = 0f
)

private data class WordRainCategory(
    val name: String,
    val mascot: String,          // emoji shown as big mascot in HUD
    val bgGrad: List<Color>,     // background gradient for this category
    val accentColor: Color,      // pill/tile accent
    val targets: List<String>,
    val decoys: List<String>
)

private val wordRainCategories = listOf(
    WordRainCategory(
        name = "Animals", mascot = "🐌",
        bgGrad = listOf(Color(0xFF0D2A1A), Color(0xFF0D1B2A)),
        accentColor = Color(0xFF6EDFA8),
        targets = listOf("lion","shark","eagle","wolf","bear","fox","deer","frog","crow","seal","elk","owl","crab","moth","mole","gecko","viper","heron","bison","moose"),
        decoys  = listOf("chair","cloud","brick","flame","piano","spoon","towel","grape","stone","brush","plank","boots","glass","crown","blade","zipper","socket","button","candle","marker")
    ),
    WordRainCategory(
        name = "Emotions", mascot = "💗",
        bgGrad = listOf(Color(0xFF2A0D2A), Color(0xFF0D1B2A)),
        accentColor = Color(0xFFD4A8FF),
        targets = listOf("joy","calm","fear","hope","love","rage","envy","awe","grief","pride","shame","trust","bliss","dread","guilt","relief","wonder","angst","glee","gloom"),
        decoys  = listOf("table","clock","bench","smoke","plant","river","tower","metal","paper","fence","wheel","grain","field","sugar","cream","socket","zipper","hammer","mirror","bottle")
    ),
    WordRainCategory(
        name = "Foods", mascot = "🍑",
        bgGrad = listOf(Color(0xFF2A1A0D), Color(0xFF0D1B2A)),
        accentColor = Color(0xFFFFB347),
        targets = listOf("apple","bread","curry","pasta","sushi","mango","steak","lemon","olive","peach","basil","walnut","melon","cocoa","crepe","scone","tacos","ramen","dumpling","waffle"),
        decoys  = listOf("engine","bridge","castle","hammer","mirror","bottle","window","planet","rocket","guitar","forest","pencil","camera","desert","pillow","jacket","gloves","ladder","tunnel","bucket")
    ),
    WordRainCategory(
        name = "Space", mascot = "🌟",
        bgGrad = listOf(Color(0xFF0D0D2A), Color(0xFF0D1B2A)),
        accentColor = Color(0xFFADD8FF),
        targets = listOf("moon","star","comet","orbit","nebula","quasar","pulsar","void","nova","flare","corona","eclipse","aurora","cosmos","meteor","saturn","galaxy","photon","zenith","solstice"),
        decoys  = listOf("kitchen","bedroom","hallway","balcony","cabinet","curtain","blanket","bathtub","chimney","doorbell","mailbox","sandbox","hammock","lantern","bookcase","toaster","cabinet","dresser","cushion","pantry")
    ),
    WordRainCategory(
        name = "Nature", mascot = "☀️",
        bgGrad = listOf(Color(0xFF0A2010), Color(0xFF0D1B2A)),
        accentColor = Color(0xFF88D8A0),
        targets = listOf("river","storm","coral","cliff","dune","grove","fog","tide","reef","lava","petal","moss","frost","peak","gale","brook","canopy","meadow","glacier","estuary"),
        decoys  = listOf("wallet","carpet","zipper","tablet","pillow","jacket","gloves","switch","bucket","ladder","tunnel","socket","button","marker","pencil","crayon","binder","staple","folder","eraser")
    )
)

private const val WR_TOTAL_ROUNDS   = 5
private const val WR_ROUND_DURATION = 30_000L
private const val WR_REWARD_POINTS  = 18
private const val WR_SPAWN_INTERVAL = 750L    // very fast spawn
private const val WR_FALL_DURATION  = 2_200L   // very fast fall

@Composable
fun WordRainScreen(onBackToGames: () -> Unit, onEarnPoints: (Int) -> Unit) {
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences(GAMES_PREFS, Context.MODE_PRIVATE) }
    val dateKey = currentDateKey()

    var phase         by remember { mutableStateOf("INTRO") }
    var roundIndex    by remember { mutableIntStateOf(0) }
    var score         by remember { mutableIntStateOf(0) }
    var lives         by remember { mutableIntStateOf(3) }
    var streak        by remember { mutableIntStateOf(0) }
    var bestStreak    by remember { mutableIntStateOf(0) }
    var missedTargets by remember { mutableIntStateOf(0) }
    var wrongTaps     by remember { mutableIntStateOf(0) }
    var timeLeft      by remember { mutableIntStateOf(WR_ROUND_DURATION.toInt()) }
    var rewardClaimed by remember { mutableStateOf(prefs.getString(KEY_WORDRAIN_COMPLETED_DATE, null) == dateKey) }

    val activeWords   = remember { mutableStateListOf<RainWord>() }
    var nextWordId    by remember { mutableIntStateOf(0) }
    var currentCat    by remember { mutableStateOf(wordRainCategories[0]) }
    val rng           = remember { Random(System.currentTimeMillis()) }
    var feedbackText  by remember { mutableStateOf("") }
    var feedbackColor by remember { mutableStateOf(WRCorrect) }
    var showFeedback  by remember { mutableStateOf(false) }
    val scope         = rememberCoroutineScope()

    fun pickCategory() { currentCat = wordRainCategories[rng.nextInt(wordRainCategories.size)] }

    fun spawnWord() {
        val isTarget = rng.nextFloat() < 0.44f
        val pool = if (isTarget) currentCat.targets else currentCat.decoys
        val text = pool[rng.nextInt(pool.size)]
        activeWords.add(RainWord(id = nextWordId++, text = text, isTarget = isTarget, lane = rng.nextInt(4), yFraction = 0f))
    }

    fun showFeed(text: String, color: Color) {
        feedbackText = text; feedbackColor = color; showFeedback = true
        scope.launch { delay(600); showFeedback = false }
    }

    fun onTapWord(word: RainWord) {
        val idx = activeWords.indexOfFirst { it.id == word.id }
        if (idx < 0 || activeWords[idx].tapped) return
        if (word.isTarget) {
            score++; streak++; if (streak > bestStreak) bestStreak = streak
            activeWords[idx] = activeWords[idx].copy(tapped = true, correct = true, splashProgress = 0f)
            showFeed(if (streak >= 4) "🔥 x$streak!" else if (streak >= 2) "✨ streak!" else "✓ nice!", WRCorrect)
        } else {
            lives--; streak = 0; wrongTaps++
            activeWords[idx] = activeWords[idx].copy(tapped = true, correct = false, splashProgress = 0f)
            showFeed("✗ nope!", WRWrong)
            if (lives <= 0) phase = "RESULT"
        }
    }

    LaunchedEffect(phase, roundIndex) {
        if (phase != "PLAYING") return@LaunchedEffect
        activeWords.clear()
        val startTime = System.currentTimeMillis()
        var lastSpawn = startTime - WR_SPAWN_INTERVAL

        while (phase == "PLAYING" && lives > 0) {
            val now = System.currentTimeMillis()
            timeLeft = (WR_ROUND_DURATION - (now - startTime)).toInt().coerceAtLeast(0)

            if (now - lastSpawn >= WR_SPAWN_INTERVAL) { spawnWord(); lastSpawn = now }

            val dt = 16f / WR_FALL_DURATION
            val toRemove = mutableListOf<Int>()
            for (i in activeWords.indices) {
                val w = activeWords[i]
                if (w.tapped) {
                    if (w.splashProgress >= 1f) toRemove.add(w.id)
                    else activeWords[i] = w.copy(splashProgress = (w.splashProgress + 0.08f).coerceAtMost(1f))
                } else {
                    val newY = w.yFraction + dt
                    if (newY >= 1f) {
                        if (w.isTarget) { missedTargets++; streak = 0; lives--; showFeed("missed!", WRWrong) }
                        toRemove.add(w.id)
                        if (lives <= 0) { phase = "RESULT"; break }
                    } else {
                        activeWords[i] = w.copy(yFraction = newY)
                    }
                }
            }
            toRemove.forEach { id -> activeWords.removeAll { it.id == id } }

            if (timeLeft <= 0) {
                if (roundIndex + 1 < WR_TOTAL_ROUNDS) { roundIndex++; pickCategory() }
                else {
                    phase = "RESULT"
                    if (!rewardClaimed && score >= 10) {
                        rewardClaimed = true
                        prefs.edit().putString(KEY_WORDRAIN_COMPLETED_DATE, dateKey).apply()
                        onEarnPoints(WR_REWARD_POINTS)
                    }
                }
                break
            }
            delay(16)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(WRBackground)) {
        when (phase) {
            "INTRO"   -> WordRainIntro { pickCategory(); phase = "PLAYING" }
            "PLAYING" -> WordRainPlayField(
                words = activeWords.toList(), category = currentCat, lives = lives,
                streak = streak, timeLeft = timeLeft, roundIndex = roundIndex,
                showFeedback = showFeedback, feedbackText = feedbackText, feedbackColor = feedbackColor,
                onTapWord = { onTapWord(it) }, onBack = { phase = "RESULT" }
            )
            "RESULT"  -> WordRainResult(
                score = score, bestStreak = bestStreak, missed = missedTargets, wrong = wrongTaps,
                rewardEarned = !rewardClaimed || score >= 10,
                onPlayAgain = {
                    score = 0; lives = 3; streak = 0; bestStreak = 0
                    missedTargets = 0; wrongTaps = 0; roundIndex = 0
                    activeWords.clear(); pickCategory(); phase = "PLAYING"
                },
                onBack = onBackToGames
            )
        }
    }
}

// ── Cute mascot drawn per category ───────────────────────────────────────────
private fun DrawScope.drawCategoryMascot(cat: WordRainCategory, cx: Float, cy: Float, t: Float, blink: Boolean = false) {
    val bob = sin(t * 2 * Math.PI.toFloat() * 1.2f) * 3.dp.toPx()
    when (cat.name) {
        "Animals" -> {
            // Cute snail for Animals
            // Shell
            drawCircle(Color(0xFFE8A850), 16.dp.toPx(), Offset(cx + 4.dp.toPx(), cy - 4.dp.toPx() + bob))
            drawCircle(Color(0xFFC07820).copy(alpha = 0.35f), 16.dp.toPx(), Offset(cx + 4.dp.toPx(), cy - 4.dp.toPx() + bob), style = Stroke(1.5.dp.toPx()))
            // Shell spiral
            val spiralPath = Path().apply {
                moveTo(cx + 4.dp.toPx(), cy - 4.dp.toPx() + bob)
                cubicTo(cx + 10.dp.toPx(), cy - 10.dp.toPx() + bob, cx + 18.dp.toPx(), cy - 6.dp.toPx() + bob, cx + 18.dp.toPx(), cy - 2.dp.toPx() + bob)
                cubicTo(cx + 18.dp.toPx(), cy + 6.dp.toPx() + bob, cx + 10.dp.toPx(), cy + 10.dp.toPx() + bob, cx + 2.dp.toPx(), cy + 8.dp.toPx() + bob)
            }
            drawPath(spiralPath, Color(0xFFC07820).copy(alpha = 0.5f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
            // Body
            val bodyPath = Path().apply {
                moveTo(cx - 18.dp.toPx(), cy + 10.dp.toPx() + bob)
                cubicTo(cx - 18.dp.toPx(), cy + 2.dp.toPx() + bob, cx - 10.dp.toPx(), cy - 2.dp.toPx() + bob, cx - 2.dp.toPx(), cy + 2.dp.toPx() + bob)
                cubicTo(cx + 6.dp.toPx(), cy + 6.dp.toPx() + bob, cx + 16.dp.toPx(), cy + 8.dp.toPx() + bob, cx + 20.dp.toPx(), cy + 14.dp.toPx() + bob)
                cubicTo(cx + 10.dp.toPx(), cy + 18.dp.toPx() + bob, cx - 8.dp.toPx(), cy + 18.dp.toPx() + bob, cx - 18.dp.toPx(), cy + 10.dp.toPx() + bob)
                close()
            }
            drawPath(bodyPath, Color(0xFF88C870))
            drawPath(bodyPath, Color(0xFF5A9A40).copy(alpha = 0.4f), style = Stroke(1.5.dp.toPx()))
            // Head
            drawCircle(Color(0xFF88C870), 10.dp.toPx(), Offset(cx - 16.dp.toPx(), cy + 4.dp.toPx() + bob))
            drawCircle(Color(0xFF5A9A40).copy(alpha = 0.4f), 10.dp.toPx(), Offset(cx - 16.dp.toPx(), cy + 4.dp.toPx() + bob), style = Stroke(1.5.dp.toPx()))
            // Antennae
            drawLine(Color(0xFF5A9A40), Offset(cx - 20.dp.toPx(), cy - 4.dp.toPx() + bob), Offset(cx - 26.dp.toPx(), cy - 14.dp.toPx() + bob), 1.8.dp.toPx())
            drawLine(Color(0xFF5A9A40), Offset(cx - 13.dp.toPx(), cy - 5.dp.toPx() + bob), Offset(cx - 14.dp.toPx(), cy - 15.dp.toPx() + bob), 1.8.dp.toPx())
            drawCircle(Color(0xFF5A9A40), 2.5.dp.toPx(), Offset(cx - 26.dp.toPx(), cy - 14.dp.toPx() + bob))
            drawCircle(Color(0xFF5A9A40), 2.5.dp.toPx(), Offset(cx - 14.dp.toPx(), cy - 15.dp.toPx() + bob))
            // Eyes — blink
            val eyeH = if (blink) 1.2.dp.toPx() else 4.5.dp.toPx()
            drawOval(Color.White, Offset(cx - 21.dp.toPx(), cy + 1.dp.toPx() - eyeH / 2 + bob), Size(5.dp.toPx(), eyeH))
            drawOval(Color.White, Offset(cx - 12.dp.toPx(), cy + 1.dp.toPx() - eyeH / 2 + bob), Size(5.dp.toPx(), eyeH))
            if (!blink) {
                drawCircle(Color(0xFF1A3A00), 1.8.dp.toPx(), Offset(cx - 18.5.dp.toPx(), cy + 1.dp.toPx() + bob))
                drawCircle(Color(0xFF1A3A00), 1.8.dp.toPx(), Offset(cx - 9.5.dp.toPx(), cy + 1.dp.toPx() + bob))
            }
            // Mouth
            val snailMouth = Path().apply {
                moveTo(cx - 21.dp.toPx(), cy + 7.dp.toPx() + bob)
                quadraticBezierTo(cx - 16.dp.toPx(), cy + 12.dp.toPx() + bob, cx - 11.dp.toPx(), cy + 7.dp.toPx() + bob)
            }
            drawPath(snailMouth, Color(0xFF1A1A1A), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        }
        "Emotions" -> {
            // Cute heart with blinking eyes
            val heartPath = Path().apply {
                moveTo(cx, cy + 18.dp.toPx() + bob)
                cubicTo(cx - 22.dp.toPx(), cy + 4.dp.toPx() + bob, cx - 26.dp.toPx(), cy - 12.dp.toPx() + bob, cx - 14.dp.toPx(), cy - 18.dp.toPx() + bob)
                cubicTo(cx - 6.dp.toPx(), cy - 22.dp.toPx() + bob, cx, cy - 14.dp.toPx() + bob, cx, cy - 10.dp.toPx() + bob)
                cubicTo(cx, cy - 14.dp.toPx() + bob, cx + 6.dp.toPx(), cy - 22.dp.toPx() + bob, cx + 14.dp.toPx(), cy - 18.dp.toPx() + bob)
                cubicTo(cx + 26.dp.toPx(), cy - 12.dp.toPx() + bob, cx + 22.dp.toPx(), cy + 4.dp.toPx() + bob, cx, cy + 18.dp.toPx() + bob)
                close()
            }
            drawPath(heartPath, Color(0xFFFF6B9D))
            drawPath(heartPath, Color(0xFFCC3070).copy(alpha = 0.4f), style = Stroke(1.5.dp.toPx()))
            // Shine
            drawOval(Color.White.copy(alpha = 0.22f), Offset(cx - 18.dp.toPx(), cy - 14.dp.toPx() + bob), Size(10.dp.toPx(), 7.dp.toPx()))
            // Eyes — blink
            val eyeH = if (blink) 1.2.dp.toPx() else 5.dp.toPx()
            drawOval(Color.White, Offset(cx - 10.dp.toPx(), cy - 4.dp.toPx() - eyeH / 2 + bob), Size(6.5.dp.toPx(), eyeH))
            drawOval(Color.White, Offset(cx + 3.dp.toPx(), cy - 4.dp.toPx() - eyeH / 2 + bob), Size(6.5.dp.toPx(), eyeH))
            if (!blink) {
                drawCircle(Color(0xFF4A0030), 2.dp.toPx(), Offset(cx - 6.5.dp.toPx(), cy - 4.dp.toPx() + bob))
                drawCircle(Color(0xFF4A0030), 2.dp.toPx(), Offset(cx + 6.5.dp.toPx(), cy - 4.dp.toPx() + bob))
                drawCircle(Color.White, 0.8.dp.toPx(), Offset(cx - 5.7.dp.toPx(), cy - 4.8.dp.toPx() + bob))
                drawCircle(Color.White, 0.8.dp.toPx(), Offset(cx + 7.3.dp.toPx(), cy - 4.8.dp.toPx() + bob))
            }
            // Mouth — black smile
            val mouthPath = Path().apply {
                moveTo(cx - 6.dp.toPx(), cy + 5.dp.toPx() + bob)
                quadraticBezierTo(cx, cy + 12.dp.toPx() + bob, cx + 6.dp.toPx(), cy + 5.dp.toPx() + bob)
            }
            drawPath(mouthPath, Color(0xFF1A1A1A), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            // Blush
            drawOval(Color(0xFFFF9ABD).copy(alpha = 0.55f), Offset(cx - 20.dp.toPx(), cy + 2.dp.toPx() + bob), Size(8.dp.toPx(), 5.dp.toPx()))
            drawOval(Color(0xFFFF9ABD).copy(alpha = 0.55f), Offset(cx + 12.dp.toPx(), cy + 2.dp.toPx() + bob), Size(8.dp.toPx(), 5.dp.toPx()))
        }
        "Foods" -> {
            // Cute peach with blinking eyes
            val r2 = 20.dp.toPx()
            // Body
            drawCircle(Color(0xFFFFAA66), r2, Offset(cx, cy + bob))
            drawCircle(Color(0xFFE07830).copy(alpha = 0.3f), r2, Offset(cx, cy + bob), style = Stroke(1.5.dp.toPx()))
            // Rosy blush cheeks
            drawOval(Color(0xFFFF7040).copy(alpha = 0.28f), Offset(cx - r2 * 0.9f, cy + 2.dp.toPx() + bob), Size(r2 * 0.9f, r2 * 0.55f))
            drawOval(Color(0xFFFF7040).copy(alpha = 0.28f), Offset(cx + r2 * 0.1f, cy + 2.dp.toPx() + bob), Size(r2 * 0.9f, r2 * 0.55f))
            // Centre crease
            val creasePath = Path().apply {
                moveTo(cx, cy - r2 + bob)
                cubicTo(cx - 2.dp.toPx(), cy - r2 * 0.4f + bob, cx - 2.dp.toPx(), cy + r2 * 0.4f + bob, cx, cy + r2 + bob)
            }
            drawPath(creasePath, Color(0xFFE07830).copy(alpha = 0.2f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            // Shine
            drawOval(Color.White.copy(alpha = 0.25f), Offset(cx - r2 * 0.7f, cy - r2 * 0.55f + bob), Size(r2 * 0.55f, r2 * 0.38f))
            // Stem and leaf
            drawLine(Color(0xFF8B6914), Offset(cx, cy - r2 + bob), Offset(cx, cy - r2 - 8.dp.toPx() + bob), 2.dp.toPx())
            val leafPath = Path().apply {
                moveTo(cx, cy - r2 - 4.dp.toPx() + bob)
                cubicTo(cx - 8.dp.toPx(), cy - r2 - 12.dp.toPx() + bob, cx - 14.dp.toPx(), cy - r2 - 8.dp.toPx() + bob, cx - 6.dp.toPx(), cy - r2 - 2.dp.toPx() + bob)
                close()
            }
            drawPath(leafPath, Color(0xFF5CB85C))
            // Eyes — blink if blink=true
            val eyeH = if (blink) 1.2.dp.toPx() else 5.dp.toPx()
            drawOval(Color.White, Offset(cx - 8.dp.toPx(), cy - 3.dp.toPx() - eyeH / 2 + bob), Size(6.dp.toPx(), eyeH))
            drawOval(Color.White, Offset(cx + 2.dp.toPx(), cy - 3.dp.toPx() - eyeH / 2 + bob), Size(6.dp.toPx(), eyeH))
            if (!blink) {
                drawCircle(Color(0xFF3A1A00), 2.dp.toPx(), Offset(cx - 5.dp.toPx(), cy - 3.dp.toPx() + bob))
                drawCircle(Color(0xFF3A1A00), 2.dp.toPx(), Offset(cx + 5.dp.toPx(), cy - 3.dp.toPx() + bob))
                drawCircle(Color.White, 0.8.dp.toPx(), Offset(cx - 4.2.dp.toPx(), cy - 3.8.dp.toPx() + bob))
                drawCircle(Color.White, 0.8.dp.toPx(), Offset(cx + 5.8.dp.toPx(), cy - 3.8.dp.toPx() + bob))
            }
            // Mouth — black smile
            val mouthPath = Path().apply {
                moveTo(cx - 5.dp.toPx(), cy + 4.dp.toPx() + bob)
                quadraticBezierTo(cx, cy + 10.dp.toPx() + bob, cx + 5.dp.toPx(), cy + 4.dp.toPx() + bob)
            }
            drawPath(mouthPath, Color(0xFF1A1A1A), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            // Blush dots
            drawOval(Color(0xFFFF7040).copy(alpha = 0.45f), Offset(cx - r2 * 0.95f, cy + 3.dp.toPx() + bob), Size(7.dp.toPx(), 4.5.dp.toPx()))
            drawOval(Color(0xFFFF7040).copy(alpha = 0.45f), Offset(cx + r2 * 0.5f, cy + 3.dp.toPx() + bob), Size(7.dp.toPx(), 4.5.dp.toPx()))
        }
        "Space" -> {
            // Cute glowing star with blinking eyes
            val or_ = 18.dp.toPx(); val ir = 9.dp.toPx()
            val starPath = Path().apply {
                for (i in 0..9) {
                    val angle = (i.toFloat() / 10f * 2 * Math.PI - Math.PI / 2).toFloat()
                    val r2 = if (i % 2 == 0) or_ else ir
                    val px = cx + r2 * cos(angle); val py = cy + bob + r2 * sin(angle)
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }; close()
            }
            val starGlow = 0.4f + 0.3f * sin(t * 2 * Math.PI.toFloat() * 2)
            drawPath(starPath, Color(0xFFFFD166).copy(alpha = 0.3f + starGlow * 0.2f))
            drawPath(starPath, Color(0xFFFFD166))
            drawPath(starPath, Color(0xFFFFEEAA).copy(alpha = 0.5f), style = Stroke(1.5.dp.toPx()))
            // Eyes — blink to match all other mascots
            val eyeH = if (blink) 1.2.dp.toPx() else 5.dp.toPx()
            drawOval(Color.White, Offset(cx - 8.dp.toPx(), cy - 3.dp.toPx() - eyeH / 2 + bob), Size(6.dp.toPx(), eyeH))
            drawOval(Color.White, Offset(cx + 2.dp.toPx(), cy - 3.dp.toPx() - eyeH / 2 + bob), Size(6.dp.toPx(), eyeH))
            if (!blink) {
                drawCircle(Color(0xFF3A2A00), 2.dp.toPx(), Offset(cx - 5.dp.toPx(), cy - 3.dp.toPx() + bob))
                drawCircle(Color(0xFF3A2A00), 2.dp.toPx(), Offset(cx + 5.dp.toPx(), cy - 3.dp.toPx() + bob))
                drawCircle(Color.White, 0.8.dp.toPx(), Offset(cx - 4.2.dp.toPx(), cy - 3.8.dp.toPx() + bob))
                drawCircle(Color.White, 0.8.dp.toPx(), Offset(cx + 5.8.dp.toPx(), cy - 3.8.dp.toPx() + bob))
            }
            // Mouth — black smile
            val ssm = Path().apply {
                moveTo(cx - 5.dp.toPx(), cy + 4.dp.toPx() + bob)
                quadraticBezierTo(cx, cy + 9.dp.toPx() + bob, cx + 5.dp.toPx(), cy + 4.dp.toPx() + bob)
            }
            drawPath(ssm, Color(0xFF1A1A1A), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            // Sparkles
            for (i in 0..2) {
                val spAngle = (t * 2 * Math.PI.toFloat() + i * 2.1f)
                val spX = cx + (or_ + 8.dp.toPx()) * cos(spAngle)
                val spY = cy + bob + (or_ + 8.dp.toPx()) * sin(spAngle)
                drawCircle(Color(0xFFFFEEAA).copy(alpha = 0.6f + 0.3f * sin(spAngle.toFloat())), 2.dp.toPx(), Offset(spX, spY))
            }
        }
        else -> {
            // Cute sun for Nature
            val sunR = 16.dp.toPx()
            // Rays
            for (i in 0..7) {
                val angle = (i.toFloat() / 8f) * 2 * Math.PI.toFloat()
                val innerR = sunR + 4.dp.toPx()
                val outerR = sunR + 11.dp.toPx()
                drawLine(
                    Color(0xFFFFD166).copy(alpha = 0.7f),
                    Offset(cx + innerR * cos(angle), cy + bob + innerR * sin(angle)),
                    Offset(cx + outerR * cos(angle), cy + bob + outerR * sin(angle)),
                    3.dp.toPx()
                )
            }
            // Body
            drawCircle(Color(0xFFFFD166), sunR, Offset(cx, cy + bob))
            drawCircle(Color(0xFFE0A800).copy(alpha = 0.3f), sunR, Offset(cx, cy + bob), style = Stroke(1.5.dp.toPx()))
            // Shine
            drawOval(Color.White.copy(alpha = 0.25f), Offset(cx - sunR * 0.7f, cy - sunR * 0.55f + bob), Size(sunR * 0.55f, sunR * 0.38f))
            // Eyes — blink
            val eyeH = if (blink) 1.2.dp.toPx() else 4.5.dp.toPx()
            drawOval(Color.White, Offset(cx - 7.dp.toPx(), cy - 2.dp.toPx() - eyeH / 2 + bob), Size(5.5.dp.toPx(), eyeH))
            drawOval(Color.White, Offset(cx + 1.5.dp.toPx(), cy - 2.dp.toPx() - eyeH / 2 + bob), Size(5.5.dp.toPx(), eyeH))
            if (!blink) {
                drawCircle(Color(0xFF3A2A00), 1.8.dp.toPx(), Offset(cx - 4.5.dp.toPx(), cy - 2.dp.toPx() + bob))
                drawCircle(Color(0xFF3A2A00), 1.8.dp.toPx(), Offset(cx + 4.5.dp.toPx(), cy - 2.dp.toPx() + bob))
                drawCircle(Color.White, 0.7.dp.toPx(), Offset(cx - 3.8.dp.toPx(), cy - 2.7.dp.toPx() + bob))
                drawCircle(Color.White, 0.7.dp.toPx(), Offset(cx + 5.2.dp.toPx(), cy - 2.7.dp.toPx() + bob))
            }
            // Mouth — black smile
            val sunMouth = Path().apply {
                moveTo(cx - 5.dp.toPx(), cy + 5.dp.toPx() + bob)
                quadraticBezierTo(cx, cy + 10.dp.toPx() + bob, cx + 5.dp.toPx(), cy + 5.dp.toPx() + bob)
            }
            drawPath(sunMouth, Color(0xFF1A1A1A), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            // Blush
            drawOval(Color(0xFFFFB300).copy(alpha = 0.4f), Offset(cx - sunR * 0.95f, cy + 3.dp.toPx() + bob), Size(7.dp.toPx(), 4.dp.toPx()))
            drawOval(Color(0xFFFFB300).copy(alpha = 0.4f), Offset(cx + sunR * 0.45f, cy + 3.dp.toPx() + bob), Size(7.dp.toPx(), 4.dp.toPx()))
        }
    }
}

// ── Intro screen ──────────────────────────────────────────────────────────────
@Composable
private fun WordRainIntro(onStart: () -> Unit) {
    val infiniteT = rememberInfiniteTransition(label = "wr_intro")
    val shimmer by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "sh")
    val mascotT by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "mt")

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF091422))))) {
        // Soft star field
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0..40) {
                val sx = (i * 47 + 13) % size.width
                val sy = (i * 61 + 7) % size.height
                val alpha = 0.15f + 0.1f * sin((shimmer * 6f + i * 0.7f).toFloat())
                drawCircle(WRAccentLight.copy(alpha = alpha), (1f + (i % 3) * 0.5f).dp.toPx(), Offset(sx, sy))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

            // Title card with mascots row
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1A3A5C), WRPanelBg)))
                    .border(1.dp, WRAccent.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Mascots row
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.fillMaxWidth()) {
                        wordRainCategories.forEach { cat ->
                            Canvas(modifier = Modifier.weight(1f).height(52.dp)) {
                                drawCategoryMascot(cat, size.width / 2f, size.height / 2f, mascotT)
                            }
                        }
                    }
                    Text("WORD RAIN", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                        color = WRTextLight, letterSpacing = 5.sp)
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(WRAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 3.dp)) {
                        Text("QUICK-THINK BRAIN TRAINER", fontSize = 10.sp, color = WRAccentLight,
                            letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Rules
            Card(colors = CardDefaults.cardColors(containerColor = WRPanelBg),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, WRNeutral, RoundedCornerShape(24.dp))) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("How to play", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = WRTextLight)
                    WRRuleRow("🎯", WRAccent, "A cute category mascot appears. Words rain down — tap only the ones that belong!")
                    WRRuleRow("✅", WRCorrect, "Tap matching words before they hit the bottom.")
                    WRRuleRow("❌", WRWrong, "Wrong taps cost a life. So do missed targets.")
                    WRRuleRow("🔥", WRGold, "Chain correct taps for a streak bonus!")
                    WRRuleRow("❤️", WRWrong, "You have 3 lives — don't lose them all!")
                }
            }

            // Info pills
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WRStatPill("$WR_TOTAL_ROUNDS rounds", "📋", Modifier.weight(1f))
                WRStatPill("30s each", "⏱️", Modifier.weight(1f))
                WRStatPill("reward pts", "🏆", Modifier.weight(1f))
            }

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WRAccent),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("LET IT RAIN  🌧️", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                    color = WRBackground, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WRRuleRow(icon: String, color: Color, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Text(icon, fontSize = 14.sp)
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = WRTextLight.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun WRStatPill(label: String, icon: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(WRNeutral).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(icon, fontSize = 16.sp)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WRTextLight,
                textAlign = TextAlign.Center)
        }
    }
}

// ── Play field ────────────────────────────────────────────────────────────────
@Composable
private fun WordRainPlayField(
    words: List<RainWord>, category: WordRainCategory, lives: Int,
    streak: Int, timeLeft: Int, roundIndex: Int,
    showFeedback: Boolean, feedbackText: String, feedbackColor: Color,
    onTapWord: (RainWord) -> Unit, onBack: () -> Unit
) {
    val timerFrac = (timeLeft.toFloat() / WR_ROUND_DURATION).coerceIn(0f, 1f)
    val timerColor = when { timerFrac > 0.5f -> WRAccent; timerFrac > 0.25f -> WRGold; else -> WRWrong }
    val feedbackAlpha by animateFloatAsState(if (showFeedback) 1f else 0f, tween(200), label = "fa")
    val feedbackScale by animateFloatAsState(if (showFeedback) 1f else 0.6f, tween(180), label = "fs")

    val infiniteT = rememberInfiniteTransition(label = "field")
    val bgT by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "bgt")
    val mascotT by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "mct")

    // Category background gradient
    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.verticalGradient(category.bgGrad + listOf(WRBackground)))) {

        // Ambient particles matching category color
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0..18) {
                val px = (i * 61 + 7) % size.width
                val py = ((bgT * size.height + i * 83) % size.height)
                val alpha = 0.03f + 0.04f * sin((bgT * 5f + i * 0.9f).toFloat())
                drawCircle(category.accentColor.copy(alpha = alpha), (2f + (i % 3)).dp.toPx(), Offset(px, py))
            }
            // Danger zone tint at bottom
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, WRWrong.copy(alpha = 0.07f)),
                size.height * 0.80f, size.height), Offset.Zero, size)
        }

        Column(modifier = Modifier.fillMaxSize()) {

            // ── HUD ──────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(WRPanelBg.copy(alpha = 0.97f), Color.Transparent)))
                .padding(horizontal = 14.dp, vertical = 10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onBack) {
                            Text("← exit", color = WRTextLight.copy(alpha = 0.4f), fontSize = 11.sp)
                        }
                        // Lives
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(3) { i -> Text(if (i < lives) "❤️" else "🖤", fontSize = 17.sp) }
                        }
                        // Streak badge
                        if (streak >= 2) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .background(WRGold.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("🔥 $streak", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = WRGold)
                            }
                        } else {
                            Spacer(Modifier.width(48.dp))
                        }
                    }

                    // Category banner with mascot
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(category.accentColor.copy(alpha = 0.22f), WRSoft)))
                        .border(1.dp, category.accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Inline mascot canvas
                            val blinkAnim by rememberInfiniteTransition(label = "blink_mascot").animateFloat(
                                0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart), label = "bm"
                            )
                            Canvas(modifier = Modifier.size(36.dp)) {
                                drawCategoryMascot(category, size.width / 2f, size.height / 2f, mascotT, blink = blinkAnim > 0.92f)
                            }
                            Column {
                                Text("TAP ALL", fontSize = 9.sp, color = category.accentColor, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Text(category.name, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = WRTextLight)
                            }
                            Spacer(Modifier.weight(1f))
                            Text("${roundIndex + 1} / $WR_TOTAL_ROUNDS", fontSize = 11.sp, color = WRTextLight.copy(alpha = 0.45f))
                        }
                    }

                    // Timer bar
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(WRSoft)) {
                        Box(modifier = Modifier.fillMaxWidth(timerFrac).height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(Brush.horizontalGradient(listOf(timerColor, timerColor.copy(alpha = 0.5f)))))
                    }
                }
            }

            // ── Rain field ────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                words.forEach { word ->
                    val wordAlpha = if (word.tapped) (1f - word.splashProgress).coerceAtLeast(0f) else 1f
                    val wordScale = when {
                        word.tapped && word.correct -> 1f + word.splashProgress * 0.5f
                        word.tapped -> 1f - word.splashProgress * 0.4f
                        else -> 1f
                    }
                    // ALL untapped pills look identical — no hints about which are targets
                    val pillBg = when {
                        !word.tapped -> WRSoft
                        word.correct -> WRCorrect
                        else -> WRWrong
                    }
                    val pillText = when {
                        !word.tapped -> WRTextLight
                        else -> Color.White
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = ((word.lane - 1.5f) * 86).dp, y = (word.yFraction * 450f).dp)
                            .scale(wordScale)
                            .alpha(wordAlpha)
                            .clickable(enabled = !word.tapped) { onTapWord(word) },
                        color = pillBg,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = if (!word.tapped) 8.dp else 0.dp
                    ) {
                        Text(
                            word.text,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pillText,
                            letterSpacing = 0.3.sp
                        )
                    }

                    // Burst particles on tap
                    if (word.tapped && word.splashProgress < 1f) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val lw = size.width / 4f
                            val cx = word.lane * lw + lw / 2f
                            val cy = word.yFraction * size.height * 0.88f
                            val sc = if (word.correct) WRCorrect else WRWrong
                            val count = if (word.correct) 10 else 6
                            for (i in 0 until count) {
                                val angle = (i.toFloat() / count) * 2 * Math.PI.toFloat()
                                val dist = word.splashProgress * 32.dp.toPx()
                                val alpha = (1f - word.splashProgress) * 0.9f
                                val pR = (5f - word.splashProgress * 4f).dp.toPx()
                                drawCircle(sc.copy(alpha = alpha), pR, Offset(cx + dist * cos(angle), cy + dist * sin(angle)))
                            }
                            // Star sparkle for correct
                            if (word.correct && word.splashProgress < 0.5f) {
                                val sparkAlpha = (0.5f - word.splashProgress) * 2f * 0.8f
                                drawCircle(WRGold.copy(alpha = sparkAlpha), (8f - word.splashProgress * 12f).dp.toPx().coerceAtLeast(0f), Offset(cx, cy))
                            }
                        }
                    }
                }

                // Feedback pop
                if (showFeedback) {
                    Text(feedbackText,
                        modifier = Modifier.align(Alignment.Center).scale(feedbackScale).alpha(feedbackAlpha),
                        fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                        color = feedbackColor, textAlign = TextAlign.Center)
                }

                // Bottom danger line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(WRWrong.copy(alpha = 0.25f), Offset(0f, size.height * 0.91f),
                        Offset(size.width, size.height * 0.91f), 1.dp.toPx())
                }
            }
        }
    }
}

// ── Result screen ─────────────────────────────────────────────────────────────
@Composable
private fun WordRainResult(
    score: Int, bestStreak: Int, missed: Int, wrong: Int,
    rewardEarned: Boolean, onPlayAgain: () -> Unit, onBack: () -> Unit
) {
    val grade = when {
        score >= 35 -> "🏆 UNSTOPPABLE"
        score >= 22 -> "⭐ SHARP MIND"
        score >= 10 -> "👍 SOLID RUN"
        else        -> "💪 KEEP GOING"
    }
    val gradeColor = when {
        score >= 35 -> WRGold
        score >= 22 -> WRCorrect
        score >= 10 -> WRAccent
        else        -> Color(0xFFFF9A55)
    }
    val infiniteT = rememberInfiniteTransition(label = "result")
    val shimmer by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "sh")
    val mascotT by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "mt")

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF091422))))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0..35) {
                val sx = (i * 47 + 13) % size.width
                val sy = (i * 61 + 7) % size.height
                val alpha = 0.1f + 0.08f * sin((shimmer * 4f + i * 0.7f).toFloat())
                drawCircle(WRAccentLight.copy(alpha = alpha), (1f + (i % 3) * 0.5f).dp.toPx(), Offset(sx, sy))
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))

            Text("ROUND COMPLETE", fontSize = 11.sp, color = WRAccentLight.copy(alpha = 0.7f),
                letterSpacing = 3.sp, fontWeight = FontWeight.Bold)

            // Mascots celebration row
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(WRPanelBg).border(1.dp, WRNeutral, RoundedCornerShape(24.dp))
                .padding(vertical = 18.dp, horizontal = 8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        wordRainCategories.forEach { cat ->
                            Canvas(modifier = Modifier.weight(1f).height(56.dp)) {
                                drawCategoryMascot(cat, size.width / 2f, size.height / 2f, mascotT)
                            }
                        }
                    }
                    Text(grade, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = gradeColor, textAlign = TextAlign.Center)
                }
            }

            // Stats
            Card(colors = CardDefaults.cardColors(containerColor = WRPanelBg), shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, WRNeutral, RoundedCornerShape(24.dp))) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your run", fontWeight = FontWeight.ExtraBold, color = WRTextLight, fontSize = 15.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WRStatCard("Tapped", "$score ✓", WRCorrect, Modifier.weight(1f))
                        WRStatCard("Best streak", "🔥 $bestStreak", WRGold, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WRStatCard("Missed", "$missed", if (missed == 0) WRCorrect else WRWrong, Modifier.weight(1f))
                        WRStatCard("Wrong", "$wrong", if (wrong == 0) WRCorrect else WRWrong, Modifier.weight(1f))
                    }
                }
            }

            if (rewardEarned) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(WRGold.copy(alpha = 0.12f))
                    .border(1.dp, WRGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .padding(14.dp)) {
                    Text("+$WR_REWARD_POINTS pts earned  🎉", textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold, color = WRGold,
                        modifier = Modifier.fillMaxWidth(), fontSize = 15.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPlayAgain, modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WRAccent),
                    shape = RoundedCornerShape(16.dp)) {
                    Text("Play Again", fontWeight = FontWeight.ExtraBold, color = WRBackground)
                }
                Button(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WRNeutral),
                    shape = RoundedCornerShape(16.dp)) {
                    Text("All Games", fontWeight = FontWeight.Bold, color = WRTextLight)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun WRStatCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp))
        .background(WRBackground.copy(alpha = 0.55f))
        .border(1.dp, WRNeutral, RoundedCornerShape(16.dp))
        .padding(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontSize = 10.sp, color = WRTextLight.copy(alpha = 0.45f), fontWeight = FontWeight.Medium)
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  MEMORY GARDEN — teammate's code, untouched
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun MemoryGardenMatchScreen(
    onBackToGames: () -> Unit,
    onEarnPoints: (Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(GAMES_PREFS, Context.MODE_PRIVATE) }
    val dateKey = currentDateKey()
    val challenge = remember(dateKey) { buildMemoryGardenChallenge(dateKey) }
    val scope = rememberCoroutineScope()

    var shuffleCount by remember(dateKey) { mutableIntStateOf(0) }
    var cards by remember(dateKey, shuffleCount) { mutableStateOf(buildShuffledDeck(challenge, dateKey, shuffleCount)) }
    var firstSelection by remember(dateKey, shuffleCount) { mutableStateOf<Int?>(null) }
    var isResolvingPair by remember(dateKey, shuffleCount) { mutableStateOf(false) }
    var moves by remember(dateKey, shuffleCount) { mutableIntStateOf(0) }
    var showCompleteMessage by remember(dateKey, shuffleCount) { mutableStateOf(false) }
    var rewardClaimedToday by remember(dateKey) {
        mutableStateOf(prefs.getString(KEY_MEMORY_COMPLETED_DATE, null) == dateKey)
    }

    val matchedPairs = cards.count { it.isMatched } / 2
    val totalPairs = challenge.pairs

    fun updateCard(index: Int, block: (MemoryGardenCard) -> MemoryGardenCard) {
        cards = cards.toMutableList().also { updated -> updated[index] = block(updated[index]) }
    }

    fun resetBoard() {
        shuffleCount += 1
        cards = buildShuffledDeck(challenge, dateKey, shuffleCount)
        firstSelection = null; isResolvingPair = false; moves = 0; showCompleteMessage = false
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBackToGames, modifier = Modifier.padding(start = 0.dp)) { Text("← All games") }
        Text("Memory Garden Match", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Play today's Memory Garden Match daily challenge.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)), shape = RoundedCornerShape(22.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(challenge.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(challenge.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("Pairs", totalPairs.toString(), Modifier.weight(1f))
                    StatChip("Reward", "+${challenge.rewardPoints} pts", Modifier.weight(1f))
                    StatChip("Target", "${challenge.suggestedGoal} moves", Modifier.weight(1f))
                }
                Text(
                    text = if (rewardClaimedToday) "You already completed today's Memory Garden Match. You can still replay today's board."
                    else "Tap two cards at a time to match the calming symbols and clear today's board.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily board", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("$matchedPairs / $totalPairs matched", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Moves: $moves", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (showCompleteMessage) Text(if (rewardClaimedToday) "Completed today" else "Challenge cleared", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                MemoryGardenBoard(cards = cards, onCardClick = { index ->
                    if (isResolvingPair) return@MemoryGardenBoard
                    val tappedCard = cards[index]
                    if (tappedCard.isMatched || tappedCard.isFaceUp) return@MemoryGardenBoard
                    updateCard(index) { it.copy(isFaceUp = true) }
                    val firstIndex = firstSelection
                    if (firstIndex == null) {
                        firstSelection = index
                    } else {
                        moves += 1; isResolvingPair = true
                        scope.launch {
                            delay(550)
                            val firstCard = cards[firstIndex]; val secondCard = cards[index]
                            if (firstCard.symbol == secondCard.symbol) {
                                updateCard(firstIndex) { it.copy(isMatched = true) }
                                updateCard(index) { it.copy(isMatched = true) }
                                if (cards.all { it.isMatched }) {
                                    showCompleteMessage = true
                                    if (!rewardClaimedToday) {
                                        rewardClaimedToday = true
                                        prefs.edit().putString(KEY_MEMORY_COMPLETED_DATE, dateKey).apply()
                                        onEarnPoints(challenge.rewardPoints)
                                    }
                                }
                            } else {
                                updateCard(firstIndex) { it.copy(isFaceUp = false) }
                                updateCard(index) { it.copy(isFaceUp = false) }
                            }
                            firstSelection = null; isResolvingPair = false
                        }
                    }
                })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Button(enabled = !isResolvingPair, onClick = { resetBoard() }) { Text("Shuffle") }
                }
            }
        }
    }
}

@Composable
private fun MemoryGardenBoard(cards: List<MemoryGardenCard>, onCardClick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cards.chunked(4).forEach { rowCards ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCards.forEachIndexed { _, card ->
                    val absoluteIndex = cards.indexOfFirst { it.id == card.id }
                    MemoryGardenCardView(card = card, modifier = Modifier.weight(1f), onClick = { onCardClick(absoluteIndex) })
                }
                repeat(4 - rowCards.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MemoryGardenCardView(card: MemoryGardenCard, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val containerColor = when {
        card.isMatched -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        card.isFaceUp  -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
        else           -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    Surface(modifier = modifier.height(74.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), color = containerColor, tonalElevation = 2.dp, shape = RoundedCornerShape(18.dp)) {
        Box(contentAlignment = Alignment.Center) { Text(if (card.isFaceUp || card.isMatched) card.symbol else "🌿", fontSize = 26.sp, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)).padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusChip(text: String, emphasized: Boolean) {
    Surface(shape = RoundedCornerShape(16.dp), color = if (emphasized) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall, color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

private fun buildMemoryGardenChallenge(dateKey: String): MemoryGardenChallenge {
    val seed = abs(dateKey.hashCode()); val random = Random(seed)
    val themePools = listOf(
        "Moonlight" to listOf("🌙","⭐","☁️","🕯️","🦋","✨","🌌","🫖"),
        "Garden"    to listOf("🌿","🌼","🍃","🪷","🌸","🪴","🍀","🌷"),
        "Still Water" to listOf("🌊","🐚","🐟","🫧","🪨","🍵","🧘","☀️"),
        "Tea House" to listOf("🍵","🫖","📚","🕯️","🌱","🍯","☁️","🌾")
    )
    val adjectives = listOf("Quiet","Soft","Steady","Gentle","Morning","Evening")
    val (themeName, pool) = themePools[random.nextInt(themePools.size)]
    val pairCount = if (random.nextBoolean()) 6 else 8
    val selectedSymbols = pool.shuffled(random).take(pairCount)
    val adjective = adjectives[random.nextInt(adjectives.size)]
    val reward = if (pairCount == 6) 10 + random.nextInt(0, 3) else 12 + random.nextInt(0, 4)
    val moveGoal = if (pairCount == 6) 17 + random.nextInt(0, 4) else 22 + random.nextInt(0, 5)
    return MemoryGardenChallenge("$adjective $themeName Match", "Today's board uses a rotating $themeName-themed symbol set with $pairCount pairs to gently challenge focus, memory, and attention.", selectedSymbols, reward, moveGoal, pairCount)
}

private fun buildShuffledDeck(challenge: MemoryGardenChallenge, dateKey: String, shuffleCount: Int): List<MemoryGardenCard> {
    val random = Random(dateKey.hashCode() + shuffleCount * 31)
    return challenge.symbols.flatMapIndexed { index, symbol ->
        listOf(MemoryGardenCard(id = index * 2, symbol = symbol), MemoryGardenCard(id = index * 2 + 1, symbol = symbol))
    }.shuffled(random)
}

private fun currentDateKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

// ═════════════════════════════════════════════════════════════════════════════
//  CONFLICT MODE
// ═════════════════════════════════════════════════════════════════════════════
private val conflictColorMap = mapOf(
    "RED"    to Color(0xFFE53935),
    "BLUE"   to Color(0xFF1E88E5),
    "GREEN"  to Color(0xFF43A047),
    "YELLOW" to Color(0xFFFDD835)
)
private val conflictColorNames = conflictColorMap.keys.toList()
private val conflictArrows = listOf("←", "→")

private const val CONFLICT_TOTAL_ROUNDS = 15
private const val CONFLICT_TIME_PER_ROUND = 4_000L
private const val CONFLICT_REWARD_POINTS = 20

private fun buildConflictRound(rng: Random): ConflictRound {
    val wordName = conflictColorNames.random(rng)
    val inkName  = conflictColorNames.filter { it != wordName }.random(rng)
    val inkColor = conflictColorMap[inkName]!!
    val arrowDir = conflictArrows.random(rng)
    val mode     = ConflictRuleMode.values().random(rng)
    val correctAnswer = when (mode) {
        ConflictRuleMode.COLOR    -> inkName
        ConflictRuleMode.ARROW    -> if (arrowDir == "←") "→" else "←"
        ConflictRuleMode.CONFLICT -> inkName
    }
    return ConflictRound(wordName, inkColor, arrowDir, mode, correctAnswer)
}

private fun ConflictRuleMode.label() = when (this) {
    ConflictRuleMode.COLOR    -> "TAP THE INK COLOR"
    ConflictRuleMode.ARROW    -> "TAP THE OPPOSITE ARROW"
    ConflictRuleMode.CONFLICT -> "TAP BOTH: INK COLOR + OPPOSITE ARROW"
}

private fun ConflictRuleMode.badgeColor() = when (this) {
    ConflictRuleMode.COLOR    -> Color(0xFF1E88E5)
    ConflictRuleMode.ARROW    -> Color(0xFF8E24AA)
    ConflictRuleMode.CONFLICT -> Color(0xFFE53935)
}

@Composable
fun ConflictModeScreen(onBackToGames: () -> Unit, onEarnPoints: (Int) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(GAMES_PREFS, Context.MODE_PRIVATE) }
    val dateKey = currentDateKey()
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf("INTRO") }
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var lastFeedback by remember { mutableStateOf("") }
    var feedbackColor by remember { mutableStateOf(Color.Green) }
    var showFeedback by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(CONFLICT_TIME_PER_ROUND.toInt()) }
    var timerRunning by remember { mutableStateOf(false) }
    var rewardClaimed by remember { mutableStateOf(prefs.getString(KEY_CONFLICT_COMPLETED_DATE, null) == dateKey) }

    val rng = remember { Random(System.currentTimeMillis()) }
    var currentRound by remember { mutableStateOf(buildConflictRound(rng)) }
    var conflictColorAnswered by remember { mutableStateOf(false) }
    var conflictArrowAnswered by remember { mutableStateOf(false) }

    LaunchedEffect(round, timerRunning) {
        if (!timerRunning) return@LaunchedEffect
        timeLeft = CONFLICT_TIME_PER_ROUND.toInt()
        while (timeLeft > 0 && timerRunning) { delay(100); timeLeft -= 100 }
        if (timerRunning) {
            lastFeedback = "⏱ Too slow!"; feedbackColor = Color(0xFFE53935); showFeedback = true; streak = 0; timerRunning = false
            delay(900); showFeedback = false
            if (round + 1 >= CONFLICT_TOTAL_ROUNDS) { phase = "RESULT" }
            else { round += 1; currentRound = buildConflictRound(rng); conflictColorAnswered = false; conflictArrowAnswered = false; timerRunning = true }
        }
    }

    fun handleAnswer(answer: String) {
        if (!timerRunning || showFeedback) return
        val r = currentRound; val correct: Boolean
        if (r.activeMode == ConflictRuleMode.CONFLICT) {
            val oppositeArrow = if (r.arrowDir == "←") "→" else "←"
            val inkName = conflictColorMap.entries.firstOrNull { it.value == r.wordColor }?.key ?: ""
            if (answer in conflictColorNames && !conflictColorAnswered) conflictColorAnswered = answer == inkName
            else if (answer in conflictArrows && !conflictArrowAnswered) conflictArrowAnswered = answer == oppositeArrow
            if (!conflictColorAnswered || !conflictArrowAnswered) return
            correct = true
        } else { correct = answer == r.correctAnswer }

        timerRunning = false
        if (correct) { score += 1; streak += 1; if (streak > bestStreak) bestStreak = streak; lastFeedback = "✓ Correct!"; feedbackColor = Color(0xFF43A047) }
        else { streak = 0; lastFeedback = "✗ Wrong"; feedbackColor = Color(0xFFE53935) }
        showFeedback = true
        scope.launch {
            delay(800); showFeedback = false
            if (round + 1 >= CONFLICT_TOTAL_ROUNDS) {
                phase = "RESULT"
                if (!rewardClaimed && score >= CONFLICT_TOTAL_ROUNDS / 2) { rewardClaimed = true; prefs.edit().putString(KEY_CONFLICT_COMPLETED_DATE, dateKey).apply(); onEarnPoints(CONFLICT_REWARD_POINTS) }
            } else { round += 1; currentRound = buildConflictRound(rng); conflictColorAnswered = false; conflictArrowAnswered = false; timerRunning = true }
        }
    }

    fun startGame() {
        round = 0; score = 0; streak = 0; bestStreak = 0
        currentRound = buildConflictRound(rng); conflictColorAnswered = false; conflictArrowAnswered = false
        showFeedback = false; phase = "PLAYING"; timerRunning = true
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = { timerRunning = false; onBackToGames() }) { Text("← All games") }
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(ArcadeBlueDeep, ArcadeBlueDark, ArcadeBlue)))
            .border(2.dp, ArcadeGlow.copy(alpha = 0.4f), RoundedCornerShape(24.dp)).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⚡ CONFLICT MODE", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ArcadeCream, letterSpacing = 2.sp)
                Text("Fight your own brain — $CONFLICT_TOTAL_ROUNDS rounds · $CONFLICT_REWARD_POINTS pts reward", fontSize = 13.sp, color = ArcadeCream.copy(alpha = 0.7f))
            }
        }
        when (phase) {
            "INTRO"   -> ConflictIntroCard(onStart = { startGame() })
            "PLAYING" -> ConflictPlayCard(round, CONFLICT_TOTAL_ROUNDS, score, streak, timeLeft, currentRound, showFeedback, lastFeedback, feedbackColor, conflictColorAnswered, conflictArrowAnswered) { handleAnswer(it) }
            "RESULT"  -> ConflictResultCard(score, CONFLICT_TOTAL_ROUNDS, bestStreak, !rewardClaimed || score >= CONFLICT_TOTAL_ROUNDS / 2, { startGame() }, { timerRunning = false; onBackToGames() })
        }
    }
}

@Composable
private fun ConflictIntroCard(onStart: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("How to play", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ConflictRuleRow("COLOR", ConflictRuleMode.COLOR.badgeColor(), "Tap the COLOR of the ink — ignore what the word says.")
            ConflictRuleRow("ARROW", ConflictRuleMode.ARROW.badgeColor(), "Tap the OPPOSITE direction of the arrow shown.")
            ConflictRuleRow("CONFLICT ⚡", ConflictRuleMode.CONFLICT.badgeColor(), "Do BOTH — tap the ink color AND the opposite arrow.")
            Text("You have ${CONFLICT_TIME_PER_ROUND / 1000} seconds per round. Go fast — trust yourself!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ArcadeBlueDeep)) {
                Text("Start Game", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ConflictRuleRow(badge: String, badgeColor: Color, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(badgeColor).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(badge, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Text(desc, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ConflictPlayCard(
    round: Int, totalRounds: Int, score: Int, streak: Int, timeLeft: Int,
    currentRound: ConflictRound, showFeedback: Boolean, feedbackText: String,
    feedbackColor: Color, conflictColorAnswered: Boolean, conflictArrowAnswered: Boolean,
    onAnswer: (String) -> Unit
) {
    val timerProgress = timeLeft.toFloat() / CONFLICT_TIME_PER_ROUND.toFloat()
    val timerBarColor = when { timerProgress > 0.6f -> Color(0xFF43A047); timerProgress > 0.3f -> Color(0xFFFDD835); else -> Color(0xFFE53935) }
    val feedbackScale by animateFloatAsState(if (showFeedback) 1.15f else 1f, tween(200), label = "fs")

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Round ${round + 1}/$totalRounds", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text("Score: $score", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                if (streak >= 2) Text("🔥 $streak", fontWeight = FontWeight.Bold, color = Color(0xFFFF6D00), style = MaterialTheme.typography.bodyMedium)
            }
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxWidth(timerProgress.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(timerBarColor))
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(currentRound.activeMode.badgeColor().copy(alpha = 0.15f)).border(1.dp, currentRound.activeMode.badgeColor().copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text(currentRound.activeMode.label(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = currentRound.activeMode.badgeColor(), letterSpacing = 0.5.sp)
            }
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF0D0D1A)).padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(currentRound.word, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = currentRound.wordColor, letterSpacing = 4.sp)
                    Text(currentRound.arrowDir, fontSize = 40.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
            if (showFeedback) Text(feedbackText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = feedbackColor, modifier = Modifier.scale(feedbackScale), textAlign = TextAlign.Center)
            else Spacer(modifier = Modifier.height(20.dp))
            if (currentRound.activeMode == ConflictRuleMode.COLOR || currentRound.activeMode == ConflictRuleMode.CONFLICT) {
                Text("Ink color:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    conflictColorMap.forEach { (name, color) ->
                        val answered = conflictColorAnswered && currentRound.activeMode == ConflictRuleMode.CONFLICT
                        ColorAnswerButton(name, color, answered, Modifier.weight(1f)) { onAnswer(name) }
                    }
                }
            }
            if (currentRound.activeMode == ConflictRuleMode.ARROW || currentRound.activeMode == ConflictRuleMode.CONFLICT) {
                Text("Opposite arrow:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    conflictArrows.forEach { arrow ->
                        val answered = conflictArrowAnswered && currentRound.activeMode == ConflictRuleMode.CONFLICT
                        ArrowAnswerButton(arrow, answered, Modifier.weight(1f)) { onAnswer(arrow) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorAnswerButton(label: String, color: Color, dimmed: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (dimmed) color.copy(alpha = 0.25f) else color.copy(alpha = 0.85f), label = "colorBtn")
    Box(modifier = modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).background(bg).clickable(enabled = !dimmed, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = if (dimmed) Color.White.copy(0.4f) else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ArrowAnswerButton(label: String, dimmed: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (dimmed) ArcadeBlueDeep.copy(alpha = 0.25f) else ArcadeBlueDeep, label = "arrowBtn")
    Box(modifier = modifier.height(56.dp).clip(RoundedCornerShape(16.dp)).background(bg).clickable(enabled = !dimmed, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 30.sp, color = if (dimmed) Color.White.copy(0.3f) else Color.White)
    }
}

@Composable
private fun ConflictResultCard(score: Int, total: Int, bestStreak: Int, rewardEarned: Boolean, onPlayAgain: () -> Unit, onBack: () -> Unit) {
    val pct = (score.toFloat() / total * 100).toInt()
    val grade = when { pct >= 90 -> "🏆 MASTER"; pct >= 70 -> "⭐ SHARP"; pct >= 50 -> "👍 SOLID"; else -> "💪 KEEP GOING" }
    val gradeColor = when { pct >= 90 -> Color(0xFFFFD700); pct >= 70 -> Color(0xFF43A047); pct >= 50 -> Color(0xFF1E88E5); else -> Color(0xFFFF6D00) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Game Over", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(Brush.radialGradient(listOf(ArcadeBlueDeep, ArcadeBlueDark))), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score/$total", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("$pct%", fontSize = 13.sp, color = Color.White.copy(0.7f))
                }
            }
            Text(grade, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = gradeColor)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip("Correct", "$score", Modifier.weight(1f))
                StatChip("Best Streak", "🔥$bestStreak", Modifier.weight(1f))
                StatChip("Accuracy", "$pct%", Modifier.weight(1f))
            }
            if (rewardEarned) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFD700).copy(alpha = 0.15f)).border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Text("+$CONFLICT_REWARD_POINTS pts earned! 🎉", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700), modifier = Modifier.fillMaxWidth())
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPlayAgain, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ArcadeBlueDeep)) { Text("Play Again") }
                Button(onClick = onBack, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text("All Games", color = MaterialTheme.colorScheme.onSurface) }
            }
        }
    }
}
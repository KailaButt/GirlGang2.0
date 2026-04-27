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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.style.TextOverflow
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
private const val KEY_MEMORY_MAX_LEVEL_COMPLETED = "memory_garden_max_level_completed"
private const val KEY_MEMORY_LEVEL_REWARD_PREFIX = "memory_garden_level_reward_"
private const val KEY_CONFLICT_COMPLETED_DATE = "conflict_mode_completed_date"
private const val KEY_CONFLICT_DAILY_DATE = "conflict_daily_challenge_date"
private const val KEY_CONFLICT_DAILY_SCORE = "conflict_daily_challenge_score"
private const val KEY_CONFLICT_FRIEND_ACTIVITY = "conflict_friend_activity"
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
    val themeName: String,
    val themeEmoji: String,
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

data class MemoryGardenLevel(
    val index: Int,
    val sceneName: String,
    val sceneEmoji: String,
    val levelNumber: Int,
    val title: String,
    val subtitle: String,
    val symbols: List<String>,
    val rewardPoints: Int,
    val suggestedGoal: Int,
    val pairs: Int
)

private sealed interface MemoryGardenRoute {
    data object Hub : MemoryGardenRoute
    data object Daily : MemoryGardenRoute
    data class Level(val levelIndex: Int) : MemoryGardenRoute
}

enum class ConflictRuleMode { COLOR, ARROW, CONFLICT }

// ─── New: Conflict difficulty ─────────────────────────────────────────────────
enum class ConflictDifficulty(
    val label: String,
    val emoji: String,
    val rounds: Int,
    val timePerRound: Long,
    val rewardPoints: Int,
    val description: String,
    val accentColor: Color,
    val glowColor: Color
) {
    EASY(
        label = "Easy", emoji = "🌊", rounds = 10, timePerRound = 6_000L,
        rewardPoints = 5,
        description = "Color only. 6 seconds per round. Perfect warmup.",
        accentColor = Color(0xFF4FC3F7), glowColor = Color(0xFF81D4FA)
    ),
    MEDIUM(
        label = "Medium", emoji = "⚡", rounds = 15, timePerRound = 4_000L,
        rewardPoints = 10,
        description = "Color + Arrow rules. 4 seconds. Faster thinking needed.",
        accentColor = Color(0xFF7C4DFF), glowColor = Color(0xFFB388FF)
    ),
    HARD(
        label = "Hard", emoji = "🔥", rounds = 20, timePerRound = 3_000L,
        rewardPoints = 20,
        description = "All rules including CONFLICT mode. 3 seconds. Pure chaos.",
        accentColor = Color(0xFFFF3D00), glowColor = Color(0xFFFF6E40)
    )
}

data class ConflictRound(
    val word: String,
    val wordColor: Color,
    val arrowDir: String,
    val activeMode: ConflictRuleMode,
    val correctAnswer: String,
    // New: shape challenge for visual variety
    val shapeType: ConflictShapeType = ConflictShapeType.NONE,
    val shapeColor: Color = Color.Transparent
)

enum class ConflictShapeType { NONE, CIRCLE, TRIANGLE, STAR }

// ─── Friend activity entry ────────────────────────────────────────────────────
data class ConflictFriendActivity(
    val name: String,
    val difficulty: String,
    val score: String,
    val dateKey: String,
    val pointsEarned: Int
)

private data class MemoryGardenSceneVisual(
    val label: String,
    val emoji: String,
    val cardBackEmoji: String,
    val decor: List<String>,
    val gradient: List<Color>,
    val accent: Color,
    val accentSoft: Color,
    val boardTint: Color
)

private fun memoryGardenVisual(sceneName: String): MemoryGardenSceneVisual = when {
    sceneName.contains("Meadow", ignoreCase = true) || sceneName.contains("Garden", ignoreCase = true) -> MemoryGardenSceneVisual(
        label = "Morning Meadow",
        emoji = "🌼",
        cardBackEmoji = "🌿",
        decor = listOf("🌼", "🦋", "🌿", "🍃"),
        gradient = listOf(Color(0xFFFFF3C4), Color(0xFFCFECCB), Color(0xFF9FD2A6)),
        accent = Color(0xFF4C8C5A),
        accentSoft = Color(0xFFEAF7DD),
        boardTint = Color(0xFFF6FBEF)
    )
    sceneName.contains("Pond", ignoreCase = true) || sceneName.contains("Water", ignoreCase = true) -> MemoryGardenSceneVisual(
        label = "Lotus Pond",
        emoji = "🪷",
        cardBackEmoji = "🫧",
        decor = listOf("🪷", "🐟", "🫧", "🌊"),
        gradient = listOf(Color(0xFFDFF7FF), Color(0xFFB8E2ED), Color(0xFF8EC8D3)),
        accent = Color(0xFF377D94),
        accentSoft = Color(0xFFE8FAFF),
        boardTint = Color(0xFFF0FBFF)
    )
    sceneName.contains("Tea", ignoreCase = true) -> MemoryGardenSceneVisual(
        label = "Tea House",
        emoji = "🫖",
        cardBackEmoji = "🍵",
        decor = listOf("🫖", "🍵", "🕯️", "📚"),
        gradient = listOf(Color(0xFFF6E7D0), Color(0xFFE5C9A1), Color(0xFFCC9E73)),
        accent = Color(0xFF8A5A37),
        accentSoft = Color(0xFFFFF6EB),
        boardTint = Color(0xFFFFFAF3)
    )
    else -> MemoryGardenSceneVisual(
        label = "Moonlight Grove",
        emoji = "🌙",
        cardBackEmoji = "✨",
        decor = listOf("🌙", "⭐", "✨", "🦋"),
        gradient = listOf(Color(0xFF1F2B5A), Color(0xFF384F87), Color(0xFF6E7BB5)),
        accent = Color(0xFFD7C275),
        accentSoft = Color(0xFF2B356C),
        boardTint = Color(0xFFF8F8FF)
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  TOP-LEVEL NAV
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun GamesScreen(onEarnPoints: (Int) -> Unit) {
    var selectedGameId by remember { mutableStateOf<String?>(null) }
    when (selectedGameId) {
        "memory_garden" -> MemoryGardenContainer(
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

            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ArcadeInk).padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text("✦ DAILY CHALLENGES  ·  EARN POINTS  ·  BRAIN BUILD  ✦  DAILY CHALLENGES  ·  EARN POINTS  ·", fontSize = 10.sp, color = ArcadeGlow.copy(alpha = 0.85f), letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }

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
            Text("✦✦", fontSize = 11.sp, color = ArcadeBlue.copy(alpha = 0.55f), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth())
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
//  WORD RAIN GAME
// ═════════════════════════════════════════════════════════════════════════════

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
    val mascot: String,
    val bgGrad: List<Color>,
    val accentColor: Color,
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
private const val WR_SPAWN_INTERVAL = 750L
private const val WR_FALL_DURATION  = 2_200L

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

private fun DrawScope.drawCategoryMascot(cat: WordRainCategory, cx: Float, cy: Float, t: Float, blink: Boolean = false) {
    val bob = sin(t * 2 * Math.PI.toFloat() * 1.2f) * 3.dp.toPx()
    when (cat.name) {
        "Animals" -> {
            drawCircle(Color(0xFFE8A850), 16.dp.toPx(), Offset(cx + 4.dp.toPx(), cy - 4.dp.toPx() + bob))
            drawCircle(Color(0xFFC07820).copy(alpha = 0.35f), 16.dp.toPx(), Offset(cx + 4.dp.toPx(), cy - 4.dp.toPx() + bob), style = Stroke(1.5.dp.toPx()))
            val spiralPath = Path().apply {
                moveTo(cx + 4.dp.toPx(), cy - 4.dp.toPx() + bob)
                cubicTo(cx + 10.dp.toPx(), cy - 10.dp.toPx() + bob, cx + 18.dp.toPx(), cy - 6.dp.toPx() + bob, cx + 18.dp.toPx(), cy - 2.dp.toPx() + bob)
                cubicTo(cx + 18.dp.toPx(), cy + 6.dp.toPx() + bob, cx + 10.dp.toPx(), cy + 10.dp.toPx() + bob, cx + 2.dp.toPx(), cy + 8.dp.toPx() + bob)
            }
            drawPath(spiralPath, Color(0xFFC07820).copy(alpha = 0.5f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
            val bodyPath = Path().apply {
                moveTo(cx - 18.dp.toPx(), cy + 10.dp.toPx() + bob)
                cubicTo(cx - 18.dp.toPx(), cy + 2.dp.toPx() + bob, cx - 10.dp.toPx(), cy - 2.dp.toPx() + bob, cx - 2.dp.toPx(), cy + 2.dp.toPx() + bob)
                cubicTo(cx + 6.dp.toPx(), cy + 6.dp.toPx() + bob, cx + 16.dp.toPx(), cy + 8.dp.toPx() + bob, cx + 20.dp.toPx(), cy + 14.dp.toPx() + bob)
                cubicTo(cx + 10.dp.toPx(), cy + 18.dp.toPx() + bob, cx - 8.dp.toPx(), cy + 18.dp.toPx() + bob, cx - 18.dp.toPx(), cy + 10.dp.toPx() + bob)
                close()
            }
            drawPath(bodyPath, Color(0xFF88C870))
            drawPath(bodyPath, Color(0xFF5A9A40).copy(alpha = 0.4f), style = Stroke(1.5.dp.toPx()))
            drawCircle(Color(0xFF88C870), 10.dp.toPx(), Offset(cx - 16.dp.toPx(), cy + 4.dp.toPx() + bob))
            drawCircle(Color(0xFF5A9A40).copy(alpha = 0.4f), 10.dp.toPx(), Offset(cx - 16.dp.toPx(), cy + 4.dp.toPx() + bob), style = Stroke(1.5.dp.toPx()))
            drawLine(Color(0xFF5A9A40), Offset(cx - 20.dp.toPx(), cy - 4.dp.toPx() + bob), Offset(cx - 26.dp.toPx(), cy - 14.dp.toPx() + bob), 1.8.dp.toPx())
            drawLine(Color(0xFF5A9A40), Offset(cx - 13.dp.toPx(), cy - 5.dp.toPx() + bob), Offset(cx - 14.dp.toPx(), cy - 15.dp.toPx() + bob), 1.8.dp.toPx())
            drawCircle(Color(0xFF5A9A40), 2.5.dp.toPx(), Offset(cx - 26.dp.toPx(), cy - 14.dp.toPx() + bob))
            drawCircle(Color(0xFF5A9A40), 2.5.dp.toPx(), Offset(cx - 14.dp.toPx(), cy - 15.dp.toPx() + bob))
            val eyeH = if (blink) 1.2.dp.toPx() else 4.5.dp.toPx()
            drawOval(Color.White, Offset(cx - 21.dp.toPx(), cy + 1.dp.toPx() - eyeH / 2 + bob), Size(5.dp.toPx(), eyeH))
            drawOval(Color.White, Offset(cx - 12.dp.toPx(), cy + 1.dp.toPx() - eyeH / 2 + bob), Size(5.dp.toPx(), eyeH))
            if (!blink) {
                drawCircle(Color(0xFF1A3A00), 1.8.dp.toPx(), Offset(cx - 18.5.dp.toPx(), cy + 1.dp.toPx() + bob))
                drawCircle(Color(0xFF1A3A00), 1.8.dp.toPx(), Offset(cx - 9.5.dp.toPx(), cy + 1.dp.toPx() + bob))
            }
            val snailMouth = Path().apply {
                moveTo(cx - 21.dp.toPx(), cy + 7.dp.toPx() + bob)
                quadraticBezierTo(cx - 16.dp.toPx(), cy + 12.dp.toPx() + bob, cx - 11.dp.toPx(), cy + 7.dp.toPx() + bob)
            }
            drawPath(snailMouth, Color(0xFF1A1A1A), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        }
        "Emotions" -> {
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
            drawOval(Color.White.copy(alpha = 0.22f), Offset(cx - 18.dp.toPx(), cy - 14.dp.toPx() + bob), Size(10.dp.toPx(), 7.dp.toPx()))
            val eyeH = if (blink) 1.2.dp.toPx() else 5.dp.toPx()
            drawOval(Color.White, Offset(cx - 10.dp.toPx(), cy - 4.dp.toPx() - eyeH / 2 + bob), Size(6.5.dp.toPx(), eyeH))
            drawOval(Color.White, Offset(cx + 3.dp.toPx(), cy - 4.dp.toPx() - eyeH / 2 + bob), Size(6.5.dp.toPx(), eyeH))
            if (!blink) {
                drawCircle(Color(0xFF4A0030), 2.dp.toPx(), Offset(cx - 6.5.dp.toPx(), cy - 4.dp.toPx() + bob))
                drawCircle(Color(0xFF4A0030), 2.dp.toPx(), Offset(cx + 6.5.dp.toPx(), cy - 4.dp.toPx() + bob))
                drawCircle(Color.White, 0.8.dp.toPx(), Offset(cx - 5.7.dp.toPx(), cy - 4.8.dp.toPx() + bob))
                drawCircle(Color.White, 0.8.dp.toPx(), Offset(cx + 7.3.dp.toPx(), cy - 4.8.dp.toPx() + bob))
            }
            val mouthPath = Path().apply {
                moveTo(cx - 6.dp.toPx(), cy + 5.dp.toPx() + bob)
                quadraticBezierTo(cx, cy + 12.dp.toPx() + bob, cx + 6.dp.toPx(), cy + 5.dp.toPx() + bob)
            }
            drawPath(mouthPath, Color(0xFF1A1A1A), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            drawOval(Color(0xFFFF9ABD).copy(alpha = 0.55f), Offset(cx - 20.dp.toPx(), cy + 2.dp.toPx() + bob), Size(8.dp.toPx(), 5.dp.toPx()))
            drawOval(Color(0xFFFF9ABD).copy(alpha = 0.55f), Offset(cx + 12.dp.toPx(), cy + 2.dp.toPx() + bob), Size(8.dp.toPx(), 5.dp.toPx()))
        }
        "Foods" -> {
            val r2 = 20.dp.toPx()
            drawCircle(Color(0xFFFFAA66), r2, Offset(cx, cy + bob))
            drawCircle(Color(0xFFE07830).copy(alpha = 0.3f), r2, Offset(cx, cy + bob), style = Stroke(1.5.dp.toPx()))
            drawOval(Color(0xFFFF7040).copy(alpha = 0.28f), Offset(cx - r2 * 0.9f, cy + 2.dp.toPx() + bob), Size(r2 * 0.9f, r2 * 0.55f))
            drawOval(Color(0xFFFF7040).copy(alpha = 0.28f), Offset(cx + r2 * 0.1f, cy + 2.dp.toPx() + bob), Size(r2 * 0.9f, r2 * 0.55f))
            val creasePath = Path().apply {
                moveTo(cx, cy - r2 + bob)
                cubicTo(cx - 2.dp.toPx(), cy - r2 * 0.4f + bob, cx - 2.dp.toPx(), cy + r2 * 0.4f + bob, cx, cy + r2 + bob)
            }
            drawPath(creasePath, Color(0xFFE07830).copy(alpha = 0.2f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            drawOval(Color.White.copy(alpha = 0.25f), Offset(cx - r2 * 0.7f, cy - r2 * 0.55f + bob), Size(r2 * 0.55f, r2 * 0.38f))
            drawLine(Color(0xFF8B6914), Offset(cx, cy - r2 + bob), Offset(cx, cy - r2 - 8.dp.toPx() + bob), 2.dp.toPx())
            val leafPath = Path().apply {
                moveTo(cx, cy - r2 - 4.dp.toPx() + bob)
                cubicTo(cx - 8.dp.toPx(), cy - r2 - 12.dp.toPx() + bob, cx - 14.dp.toPx(), cy - r2 - 8.dp.toPx() + bob, cx - 6.dp.toPx(), cy - r2 - 2.dp.toPx() + bob)
                close()
            }
            drawPath(leafPath, Color(0xFF5CB85C))
            val eyeH = if (blink) 1.2.dp.toPx() else 5.dp.toPx()
            drawOval(Color.White, Offset(cx - 8.dp.toPx(), cy - 3.dp.toPx() - eyeH / 2 + bob), Size(6.dp.toPx(), eyeH))
            drawOval(Color.White, Offset(cx + 2.dp.toPx(), cy - 3.dp.toPx() - eyeH / 2 + bob), Size(6.dp.toPx(), eyeH))
            if (!blink) {
                drawCircle(Color(0xFF3A1A00), 2.dp.toPx(), Offset(cx - 5.dp.toPx(), cy - 3.dp.toPx() + bob))
                drawCircle(Color(0xFF3A1A00), 2.dp.toPx(), Offset(cx + 5.dp.toPx(), cy - 3.dp.toPx() + bob))
                drawCircle(Color.White, 0.8.dp.toPx(), Offset(cx - 4.2.dp.toPx(), cy - 3.8.dp.toPx() + bob))
                drawCircle(Color.White, 0.8.dp.toPx(), Offset(cx + 5.8.dp.toPx(), cy - 3.8.dp.toPx() + bob))
            }
            val mouthPath = Path().apply {
                moveTo(cx - 5.dp.toPx(), cy + 4.dp.toPx() + bob)
                quadraticBezierTo(cx, cy + 10.dp.toPx() + bob, cx + 5.dp.toPx(), cy + 4.dp.toPx() + bob)
            }
            drawPath(mouthPath, Color(0xFF1A1A1A), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            drawOval(Color(0xFFFF7040).copy(alpha = 0.45f), Offset(cx - r2 * 0.95f, cy + 3.dp.toPx() + bob), Size(7.dp.toPx(), 4.5.dp.toPx()))
            drawOval(Color(0xFFFF7040).copy(alpha = 0.45f), Offset(cx + r2 * 0.5f, cy + 3.dp.toPx() + bob), Size(7.dp.toPx(), 4.5.dp.toPx()))
        }
        "Space" -> {
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
            val eyeH = if (blink) 1.2.dp.toPx() else 5.dp.toPx()
            drawOval(Color.White, Offset(cx - 8.dp.toPx(), cy - 3.dp.toPx() - eyeH / 2 + bob), Size(6.dp.toPx(), eyeH))
            drawOval(Color.White, Offset(cx + 2.dp.toPx(), cy - 3.dp.toPx() - eyeH / 2 + bob), Size(6.dp.toPx(), eyeH))
            if (!blink) {
                drawCircle(Color(0xFF3A2A00), 2.dp.toPx(), Offset(cx - 5.dp.toPx(), cy - 3.dp.toPx() + bob))
                drawCircle(Color(0xFF3A2A00), 2.dp.toPx(), Offset(cx + 5.dp.toPx(), cy - 3.dp.toPx() + bob))
                drawCircle(Color.White, 0.8.dp.toPx(), Offset(cx - 4.2.dp.toPx(), cy - 3.8.dp.toPx() + bob))
                drawCircle(Color.White, 0.8.dp.toPx(), Offset(cx + 5.8.dp.toPx(), cy - 3.8.dp.toPx() + bob))
            }
            val ssm = Path().apply {
                moveTo(cx - 5.dp.toPx(), cy + 4.dp.toPx() + bob)
                quadraticBezierTo(cx, cy + 9.dp.toPx() + bob, cx + 5.dp.toPx(), cy + 4.dp.toPx() + bob)
            }
            drawPath(ssm, Color(0xFF1A1A1A), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            for (i in 0..2) {
                val spAngle = (t * 2 * Math.PI.toFloat() + i * 2.1f)
                val spX = cx + (or_ + 8.dp.toPx()) * cos(spAngle)
                val spY = cy + bob + (or_ + 8.dp.toPx()) * sin(spAngle)
                drawCircle(Color(0xFFFFEEAA).copy(alpha = 0.6f + 0.3f * sin(spAngle.toFloat())), 2.dp.toPx(), Offset(spX, spY))
            }
        }
        else -> {
            val sunR = 16.dp.toPx()
            for (i in 0..7) {
                val angle = (i.toFloat() / 8f) * 2 * Math.PI.toFloat()
                val innerR = sunR + 4.dp.toPx(); val outerR = sunR + 11.dp.toPx()
                drawLine(Color(0xFFFFD166).copy(alpha = 0.7f), Offset(cx + innerR * cos(angle), cy + bob + innerR * sin(angle)), Offset(cx + outerR * cos(angle), cy + bob + outerR * sin(angle)), 3.dp.toPx())
            }
            drawCircle(Color(0xFFFFD166), sunR, Offset(cx, cy + bob))
            drawCircle(Color(0xFFE0A800).copy(alpha = 0.3f), sunR, Offset(cx, cy + bob), style = Stroke(1.5.dp.toPx()))
            drawOval(Color.White.copy(alpha = 0.25f), Offset(cx - sunR * 0.7f, cy - sunR * 0.55f + bob), Size(sunR * 0.55f, sunR * 0.38f))
            val eyeH = if (blink) 1.2.dp.toPx() else 4.5.dp.toPx()
            drawOval(Color.White, Offset(cx - 7.dp.toPx(), cy - 2.dp.toPx() - eyeH / 2 + bob), Size(5.5.dp.toPx(), eyeH))
            drawOval(Color.White, Offset(cx + 1.5.dp.toPx(), cy - 2.dp.toPx() - eyeH / 2 + bob), Size(5.5.dp.toPx(), eyeH))
            if (!blink) {
                drawCircle(Color(0xFF3A2A00), 1.8.dp.toPx(), Offset(cx - 4.5.dp.toPx(), cy - 2.dp.toPx() + bob))
                drawCircle(Color(0xFF3A2A00), 1.8.dp.toPx(), Offset(cx + 4.5.dp.toPx(), cy - 2.dp.toPx() + bob))
                drawCircle(Color.White, 0.7.dp.toPx(), Offset(cx - 3.8.dp.toPx(), cy - 2.7.dp.toPx() + bob))
                drawCircle(Color.White, 0.7.dp.toPx(), Offset(cx + 5.2.dp.toPx(), cy - 2.7.dp.toPx() + bob))
            }
            val sunMouth = Path().apply {
                moveTo(cx - 5.dp.toPx(), cy + 5.dp.toPx() + bob)
                quadraticBezierTo(cx, cy + 10.dp.toPx() + bob, cx + 5.dp.toPx(), cy + 5.dp.toPx() + bob)
            }
            drawPath(sunMouth, Color(0xFF1A1A1A), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            drawOval(Color(0xFFFFB300).copy(alpha = 0.4f), Offset(cx - sunR * 0.95f, cy + 3.dp.toPx() + bob), Size(7.dp.toPx(), 4.dp.toPx()))
            drawOval(Color(0xFFFFB300).copy(alpha = 0.4f), Offset(cx + sunR * 0.45f, cy + 3.dp.toPx() + bob), Size(7.dp.toPx(), 4.dp.toPx()))
        }
    }
}

@Composable
private fun WordRainIntro(onStart: () -> Unit) {
    val infiniteT = rememberInfiniteTransition(label = "wr_intro")
    val shimmer by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "sh")
    val mascotT by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "mt")

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF091422))))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0..40) {
                val sx = (i * 47 + 13) % size.width; val sy = (i * 61 + 7) % size.height
                val alpha = 0.15f + 0.1f * sin((shimmer * 6f + i * 0.7f).toFloat())
                drawCircle(WRAccentLight.copy(alpha = alpha), (1f + (i % 3) * 0.5f).dp.toPx(), Offset(sx, sy))
            }
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color(0xFF1A3A5C), WRPanelBg))).border(1.dp, WRAccent.copy(alpha = 0.35f), RoundedCornerShape(28.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.fillMaxWidth()) {
                        wordRainCategories.forEach { cat ->
                            Canvas(modifier = Modifier.weight(1f).height(52.dp)) { drawCategoryMascot(cat, size.width / 2f, size.height / 2f, mascotT) }
                        }
                    }
                    Text("WORD RAIN", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = WRTextLight, letterSpacing = 5.sp)
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(WRAccent.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 3.dp)) {
                        Text("QUICK-THINK BRAIN TRAINER", fontSize = 10.sp, color = WRAccentLight, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = WRPanelBg), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().border(1.dp, WRNeutral, RoundedCornerShape(24.dp))) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("How to play", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = WRTextLight)
                    WRRuleRow("🎯", WRAccent, "A cute category mascot appears. Words rain down — tap only the ones that belong!")
                    WRRuleRow("✅", WRCorrect, "Tap matching words before they hit the bottom.")
                    WRRuleRow("❌", WRWrong, "Wrong taps cost a life. So do missed targets.")
                    WRRuleRow("🔥", WRGold, "Chain correct taps for a streak bonus!")
                    WRRuleRow("❤️", WRWrong, "You have 3 lives — don't lose them all!")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WRStatPill("$WR_TOTAL_ROUNDS rounds", "📋", Modifier.weight(1f))
                WRStatPill("30s each", "⏱️", Modifier.weight(1f))
                WRStatPill("reward pts", "🏆", Modifier.weight(1f))
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = WRAccent), shape = RoundedCornerShape(18.dp)) {
                Text("LET IT RAIN  🌧️", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = WRBackground, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WRRuleRow(icon: String, color: Color, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 14.sp) }
        Text(text, style = MaterialTheme.typography.bodySmall, color = WRTextLight.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun WRStatPill(label: String, icon: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(WRNeutral).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(icon, fontSize = 16.sp)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WRTextLight, textAlign = TextAlign.Center)
        }
    }
}

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

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(category.bgGrad + listOf(WRBackground)))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0..18) {
                val px = (i * 61 + 7) % size.width; val py = ((bgT * size.height + i * 83) % size.height)
                val alpha = 0.03f + 0.04f * sin((bgT * 5f + i * 0.9f).toFloat())
                drawCircle(category.accentColor.copy(alpha = alpha), (2f + (i % 3)).dp.toPx(), Offset(px, py))
            }
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, WRWrong.copy(alpha = 0.07f)), size.height * 0.80f, size.height), Offset.Zero, size)
        }
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(WRPanelBg.copy(alpha = 0.97f), Color.Transparent))).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onBack) { Text("← exit", color = WRTextLight.copy(alpha = 0.4f), fontSize = 11.sp) }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { repeat(3) { i -> Text(if (i < lives) "❤️" else "🖤", fontSize = 17.sp) } }
                        if (streak >= 2) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(WRGold.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("🔥 $streak", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = WRGold) }
                        } else { Spacer(Modifier.width(48.dp)) }
                    }
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(category.accentColor.copy(alpha = 0.22f), WRSoft))).border(1.dp, category.accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val blinkAnim by rememberInfiniteTransition(label = "blink_mascot").animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart), label = "bm")
                            Canvas(modifier = Modifier.size(36.dp)) { drawCategoryMascot(category, size.width / 2f, size.height / 2f, mascotT, blink = blinkAnim > 0.92f) }
                            Column {
                                Text("TAP ALL", fontSize = 9.sp, color = category.accentColor, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Text(category.name, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = WRTextLight)
                            }
                            Spacer(Modifier.weight(1f))
                            Text("${roundIndex + 1} / $WR_TOTAL_ROUNDS", fontSize = 11.sp, color = WRTextLight.copy(alpha = 0.45f))
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(WRSoft)) {
                        Box(modifier = Modifier.fillMaxWidth(timerFrac).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Brush.horizontalGradient(listOf(timerColor, timerColor.copy(alpha = 0.5f)))))
                    }
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                words.forEach { word ->
                    val wordAlpha = if (word.tapped) (1f - word.splashProgress).coerceAtLeast(0f) else 1f
                    val wordScale = when { word.tapped && word.correct -> 1f + word.splashProgress * 0.5f; word.tapped -> 1f - word.splashProgress * 0.4f; else -> 1f }
                    val pillBg = when { !word.tapped -> WRSoft; word.correct -> WRCorrect; else -> WRWrong }
                    val pillText = when { !word.tapped -> WRTextLight; else -> Color.White }
                    Surface(modifier = Modifier.align(Alignment.TopCenter).offset(x = ((word.lane - 1.5f) * 86).dp, y = (word.yFraction * 450f).dp).scale(wordScale).alpha(wordAlpha).clickable(enabled = !word.tapped) { onTapWord(word) }, color = pillBg, shape = RoundedCornerShape(20.dp), shadowElevation = if (!word.tapped) 8.dp else 0.dp) {
                        Text(word.text, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pillText, letterSpacing = 0.3.sp)
                    }
                    if (word.tapped && word.splashProgress < 1f) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val lw = size.width / 4f; val cx = word.lane * lw + lw / 2f; val cy = word.yFraction * size.height * 0.88f
                            val sc = if (word.correct) WRCorrect else WRWrong; val count = if (word.correct) 10 else 6
                            for (i in 0 until count) {
                                val angle = (i.toFloat() / count) * 2 * Math.PI.toFloat(); val dist = word.splashProgress * 32.dp.toPx(); val alpha = (1f - word.splashProgress) * 0.9f; val pR = (5f - word.splashProgress * 4f).dp.toPx()
                                drawCircle(sc.copy(alpha = alpha), pR, Offset(cx + dist * cos(angle), cy + dist * sin(angle)))
                            }
                            if (word.correct && word.splashProgress < 0.5f) { val sparkAlpha = (0.5f - word.splashProgress) * 2f * 0.8f; drawCircle(WRGold.copy(alpha = sparkAlpha), (8f - word.splashProgress * 12f).dp.toPx().coerceAtLeast(0f), Offset(cx, cy)) }
                        }
                    }
                }
                if (showFeedback) { Text(feedbackText, modifier = Modifier.align(Alignment.Center).scale(feedbackScale).alpha(feedbackAlpha), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = feedbackColor, textAlign = TextAlign.Center) }
                Canvas(modifier = Modifier.fillMaxSize()) { drawLine(WRWrong.copy(alpha = 0.25f), Offset(0f, size.height * 0.91f), Offset(size.width, size.height * 0.91f), 1.dp.toPx()) }
            }
        }
    }
}

@Composable
private fun WordRainResult(score: Int, bestStreak: Int, missed: Int, wrong: Int, rewardEarned: Boolean, onPlayAgain: () -> Unit, onBack: () -> Unit) {
    val grade = when { score >= 35 -> "🏆 UNSTOPPABLE"; score >= 22 -> "⭐ SHARP MIND"; score >= 10 -> "👍 SOLID RUN"; else -> "💪 KEEP GOING" }
    val gradeColor = when { score >= 35 -> WRGold; score >= 22 -> WRCorrect; score >= 10 -> WRAccent; else -> Color(0xFFFF9A55) }
    val infiniteT = rememberInfiniteTransition(label = "result")
    val shimmer by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "sh")
    val mascotT by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "mt")

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF091422))))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0..35) {
                val sx = (i * 47 + 13) % size.width; val sy = (i * 61 + 7) % size.height
                val alpha = 0.1f + 0.08f * sin((shimmer * 4f + i * 0.7f).toFloat())
                drawCircle(WRAccentLight.copy(alpha = alpha), (1f + (i % 3) * 0.5f).dp.toPx(), Offset(sx, sy))
            }
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            Text("ROUND COMPLETE", fontSize = 11.sp, color = WRAccentLight.copy(alpha = 0.7f), letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(WRPanelBg).border(1.dp, WRNeutral, RoundedCornerShape(24.dp)).padding(vertical = 18.dp, horizontal = 8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        wordRainCategories.forEach { cat -> Canvas(modifier = Modifier.weight(1f).height(56.dp)) { drawCategoryMascot(cat, size.width / 2f, size.height / 2f, mascotT) } }
                    }
                    Text(grade, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = gradeColor, textAlign = TextAlign.Center)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = WRPanelBg), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().border(1.dp, WRNeutral, RoundedCornerShape(24.dp))) {
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
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(WRGold.copy(alpha = 0.12f)).border(1.dp, WRGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp)).padding(14.dp)) {
                    Text("+$WR_REWARD_POINTS pts earned  🎉", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = WRGold, modifier = Modifier.fillMaxWidth(), fontSize = 15.sp)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPlayAgain, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = WRAccent), shape = RoundedCornerShape(16.dp)) { Text("Play Again", fontWeight = FontWeight.ExtraBold, color = WRBackground) }
                Button(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = WRNeutral), shape = RoundedCornerShape(16.dp)) { Text("All Games", fontWeight = FontWeight.Bold, color = WRTextLight) }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun WRStatCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(WRBackground.copy(alpha = 0.55f)).border(1.dp, WRNeutral, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontSize = 10.sp, color = WRTextLight.copy(alpha = 0.45f), fontWeight = FontWeight.Medium)
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  MEMORY GARDEN
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun MemoryGardenContainer(onBackToGames: () -> Unit, onEarnPoints: (Int) -> Unit) {
    val levels = remember { buildMemoryGardenLevels() }
    var route by remember { mutableStateOf<MemoryGardenRoute>(MemoryGardenRoute.Hub) }
    when (val current = route) {
        MemoryGardenRoute.Hub -> MemoryGardenHubScreen(levels = levels, onBackToGames = onBackToGames, onOpenDaily = { route = MemoryGardenRoute.Daily }, onOpenLevel = { route = MemoryGardenRoute.Level(it) })
        MemoryGardenRoute.Daily -> MemoryGardenDailyScreen(onBack = { route = MemoryGardenRoute.Hub }, onEarnPoints = onEarnPoints)
        is MemoryGardenRoute.Level -> MemoryGardenLevelScreen(level = levels.first { it.index == current.levelIndex }, allLevels = levels, onBack = { route = MemoryGardenRoute.Hub }, onOpenLevel = { route = MemoryGardenRoute.Level(it) }, onEarnPoints = onEarnPoints)
    }
}

@Composable
private fun MemoryGardenHubScreen(levels: List<MemoryGardenLevel>, onBackToGames: () -> Unit, onOpenDaily: () -> Unit, onOpenLevel: (Int) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(GAMES_PREFS, Context.MODE_PRIVATE) }
    val dateKey = currentDateKey()
    val challenge = remember(dateKey) { buildMemoryGardenChallenge(dateKey) }
    val completedToday = remember(dateKey) { prefs.getString(KEY_MEMORY_COMPLETED_DATE, null) == dateKey }
    val maxCompletedLevel = remember { prefs.getInt(KEY_MEMORY_MAX_LEVEL_COMPLETED, -1) }
    val completedLevels = remember(maxCompletedLevel) { levels.count { prefs.getBoolean("$KEY_MEMORY_LEVEL_REWARD_PREFIX${it.index}", false) } }
    val currentUnlockIndex = (maxCompletedLevel + 1).coerceIn(0, levels.lastIndex)
    val currentUnlockLevel = levels[currentUnlockIndex]
    val scenes = remember(levels) { levels.groupBy { it.sceneName } }
    val challengeVisual = remember(challenge.themeName) { memoryGardenVisual(challenge.themeName) }
    val journeyVisual = remember(currentUnlockLevel.sceneName) { memoryGardenVisual(currentUnlockLevel.sceneName) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBackToGames) { Text("← All games") }
        Text(text = "Memory Garden Match", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = "Play today's daily challenge or move through the full journey mode to unlock new scenes one level at a time.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors = CardDefaults.cardColors(containerColor = challengeVisual.accentSoft.copy(alpha = 0.92f)), shape = RoundedCornerShape(22.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MemoryGardenSceneHero(sceneName = challenge.themeName, sceneEmoji = challenge.themeEmoji, subtitle = "Today's theme art and symbols rotate by date.", modifier = Modifier.fillMaxWidth().height(156.dp), compact = true)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Daily Challenge", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    StatusChip(text = if (completedToday) "Completed today" else "Ready now", emphasized = true)
                }
                Text(text = challenge.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = "Today's board rotates by date and keeps the challenge completed for the rest of the day once you clear it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(label = "Pairs", value = challenge.pairs.toString(), modifier = Modifier.weight(1f))
                    StatChip(label = "Reward", value = "+${challenge.rewardPoints} pts", modifier = Modifier.weight(1f))
                    StatChip(label = "Target", value = "${challenge.suggestedGoal}", modifier = Modifier.weight(1f))
                }
                FilledTonalButton(onClick = onOpenDaily) { Text(if (completedToday) "Replay today's board" else "Play daily challenge") }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = journeyVisual.boardTint.copy(alpha = 0.98f)), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Garden Journey", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Complete levels in order to unlock new calming scenes and slightly harder boards.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(label = "Cleared", value = "$completedLevels/${levels.size}", modifier = Modifier.weight(1f))
                    StatChip(label = "Scenes", value = scenes.size.toString(), modifier = Modifier.weight(1f))
                    StatChip(label = "Next", value = "L${currentUnlockLevel.levelNumber}", modifier = Modifier.weight(1f))
                }
                LinearProgressIndicator(progress = completedLevels / levels.size.toFloat(), modifier = Modifier.fillMaxWidth())
                Text(text = "Current unlock: ${currentUnlockLevel.sceneName} • Level ${currentUnlockLevel.levelNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        scenes.values.forEach { sceneLevels ->
            val firstLevelIndex = sceneLevels.first().index
            val sceneUnlocked = firstLevelIndex <= maxCompletedLevel + 1
            MemorySceneCard(sceneLevels = sceneLevels, maxCompletedLevel = maxCompletedLevel, onOpenLevel = onOpenLevel, unlocked = sceneUnlocked, prefs = prefs)
        }
    }
}

@Composable
private fun MemorySceneCard(sceneLevels: List<MemoryGardenLevel>, maxCompletedLevel: Int, onOpenLevel: (Int) -> Unit, unlocked: Boolean, prefs: android.content.SharedPreferences) {
    val scene = sceneLevels.first()
    val visual = memoryGardenVisual(scene.sceneName)
    Card(modifier = Modifier.alpha(if (unlocked) 1f else 0.72f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MemoryGardenSceneHero(sceneName = scene.sceneName, sceneEmoji = scene.sceneEmoji, subtitle = "10 unlockable levels with scene-based symbols and a calmer visual style.", modifier = Modifier.fillMaxWidth().height(156.dp), compact = true)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SceneBadge(visual = visual)
                StatusChip(text = if (unlocked) "Unlocked" else "Locked", emphasized = unlocked)
            }
            sceneLevels.chunked(3).forEach { rowLevels ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowLevels.forEach { level ->
                        val rewardClaimed = prefs.getBoolean("$KEY_MEMORY_LEVEL_REWARD_PREFIX${level.index}", false)
                        val levelUnlocked = level.index <= maxCompletedLevel + 1
                        val label = buildString { append("L${level.levelNumber}"); if (rewardClaimed) append(" ✓") else if (!levelUnlocked) append(" 🔒") }
                        if (levelUnlocked) {
                            FilledTonalButton(modifier = Modifier.weight(1f), onClick = { onOpenLevel(level.index) }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = visual.accentSoft, contentColor = visual.accent)) { Text(label, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold) }
                        } else {
                            OutlinedButton(modifier = Modifier.weight(1f), onClick = {}, enabled = false) { Text(label, textAlign = TextAlign.Center) }
                        }
                    }
                    repeat(3 - rowLevels.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun MemoryGardenDailyScreen(onBack: () -> Unit, onEarnPoints: (Int) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(GAMES_PREFS, Context.MODE_PRIVATE) }
    val dateKey = currentDateKey()
    val challenge = remember(dateKey) { buildMemoryGardenChallenge(dateKey) }
    val scope = rememberCoroutineScope()
    var shuffleCount by remember(dateKey) { mutableIntStateOf(0) }
    var cards by remember(dateKey, shuffleCount) { mutableStateOf(buildShuffledDeck(challenge.symbols, dateKey.hashCode() + shuffleCount * 31)) }
    var firstSelection by remember(dateKey, shuffleCount) { mutableStateOf<Int?>(null) }
    var isResolvingPair by remember(dateKey, shuffleCount) { mutableStateOf(false) }
    var moves by remember(dateKey, shuffleCount) { mutableIntStateOf(0) }
    var showCompleteMessage by remember(dateKey, shuffleCount) { mutableStateOf(false) }
    var rewardClaimedToday by remember(dateKey) { mutableStateOf(prefs.getString(KEY_MEMORY_COMPLETED_DATE, null) == dateKey) }

    GameBoardScreen(backLabel = "← Memory Garden", onBack = onBack, title = "Memory Garden Match", subtitle = "Play today's Memory Garden Match daily challenge.", sceneName = challenge.themeName, sceneEmoji = challenge.themeEmoji, headerTitle = challenge.title, headerSubtitle = challenge.subtitle, pairs = challenge.pairs, rewardText = "+${challenge.rewardPoints} pts", targetText = "${challenge.suggestedGoal} moves", statusText = if (rewardClaimedToday) "You already completed today's Memory Garden Match. You can still replay today's board." else "Tap two cards at a time to match the calming symbols and clear today's board.", boardTitle = "Daily board", completionText = if (rewardClaimedToday) "Completed today" else "Challenge cleared", cards = cards, moves = moves, showCompleteMessage = showCompleteMessage, onResetBoard = {
        shuffleCount += 1; cards = buildShuffledDeck(challenge.symbols, dateKey.hashCode() + shuffleCount * 31); firstSelection = null; isResolvingPair = false; moves = 0; showCompleteMessage = false
    }, onCardClick = { index ->
        if (isResolvingPair) return@GameBoardScreen
        val tappedCard = cards[index]
        if (tappedCard.isMatched || tappedCard.isFaceUp) return@GameBoardScreen
        cards = cards.toMutableList().also { it[index] = it[index].copy(isFaceUp = true) }
        val firstIndex = firstSelection
        if (firstIndex == null) { firstSelection = index } else {
            moves += 1; isResolvingPair = true
            scope.launch {
                delay(550)
                val firstCard = cards[firstIndex]; val secondCard = cards[index]
                if (firstCard.symbol == secondCard.symbol) {
                    cards = cards.toMutableList().also { it[firstIndex] = it[firstIndex].copy(isMatched = true); it[index] = it[index].copy(isMatched = true) }
                    if (cards.all { it.isMatched }) { showCompleteMessage = true; if (!rewardClaimedToday) { rewardClaimedToday = true; prefs.edit().putString(KEY_MEMORY_COMPLETED_DATE, dateKey).apply(); onEarnPoints(challenge.rewardPoints) } }
                } else { cards = cards.toMutableList().also { it[firstIndex] = it[firstIndex].copy(isFaceUp = false); it[index] = it[index].copy(isFaceUp = false) } }
                firstSelection = null; isResolvingPair = false
            }
        }
    })
}

@Composable
private fun MemoryGardenLevelScreen(level: MemoryGardenLevel, allLevels: List<MemoryGardenLevel>, onBack: () -> Unit, onOpenLevel: (Int) -> Unit, onEarnPoints: (Int) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(GAMES_PREFS, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val rewardKey = "$KEY_MEMORY_LEVEL_REWARD_PREFIX${level.index}"
    var maxCompletedLevel by remember { mutableIntStateOf(prefs.getInt(KEY_MEMORY_MAX_LEVEL_COMPLETED, -1)) }
    val unlocked = level.index <= maxCompletedLevel + 1
    var shuffleCount by remember(level.index) { mutableIntStateOf(0) }
    var cards by remember(level.index, shuffleCount) { mutableStateOf(buildShuffledDeck(level.symbols, level.index * 997 + shuffleCount * 37)) }
    var firstSelection by remember(level.index, shuffleCount) { mutableStateOf<Int?>(null) }
    var isResolvingPair by remember(level.index, shuffleCount) { mutableStateOf(false) }
    var moves by remember(level.index, shuffleCount) { mutableIntStateOf(0) }
    var showCompleteMessage by remember(level.index, shuffleCount) { mutableStateOf(false) }
    var rewardClaimed by remember(level.index) { mutableStateOf(prefs.getBoolean(rewardKey, false)) }
    var nextUnlockMessage by remember(level.index, shuffleCount) { mutableStateOf<String?>(null) }

    if (!unlocked) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack) { Text("← Memory Garden") }
            Text(text = "Level locked", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = "Clear the previous level to unlock ${level.sceneName} • Level ${level.levelNumber}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val nextLevel = allLevels.getOrNull(level.index + 1)
    GameBoardScreen(backLabel = "← Memory Garden", onBack = onBack, title = "Memory Garden Match", subtitle = "${level.sceneEmoji} ${level.sceneName} • Level ${level.levelNumber}", sceneName = level.sceneName, sceneEmoji = level.sceneEmoji, headerTitle = level.title, headerSubtitle = level.subtitle, pairs = level.pairs, rewardText = "+${level.rewardPoints} pts", targetText = "${level.suggestedGoal} moves", statusText = if (rewardClaimed) "This level stays unlocked once cleared. You can replay it anytime, but first-clear points are only awarded once." else "Clear this board to unlock the next level in the journey.", boardTitle = "Journey level", completionText = when { nextUnlockMessage != null -> nextUnlockMessage!!; rewardClaimed -> "Level cleared"; else -> "Level complete" }, cards = cards, moves = moves, showCompleteMessage = showCompleteMessage, nextButtonText = nextLevel?.let { "Next level" }, onNextLevel = if (nextLevel != null) { { onOpenLevel(nextLevel.index) } } else null, onResetBoard = {
        shuffleCount += 1; cards = buildShuffledDeck(level.symbols, level.index * 997 + shuffleCount * 37); firstSelection = null; isResolvingPair = false; moves = 0; showCompleteMessage = false; nextUnlockMessage = null
    }, onCardClick = { index ->
        if (isResolvingPair) return@GameBoardScreen
        val tappedCard = cards[index]
        if (tappedCard.isMatched || tappedCard.isFaceUp) return@GameBoardScreen
        cards = cards.toMutableList().also { it[index] = it[index].copy(isFaceUp = true) }
        val firstIndex = firstSelection
        if (firstIndex == null) { firstSelection = index } else {
            moves += 1; isResolvingPair = true
            scope.launch {
                delay(550)
                val firstCard = cards[firstIndex]; val secondCard = cards[index]
                if (firstCard.symbol == secondCard.symbol) {
                    cards = cards.toMutableList().also { it[firstIndex] = it[firstIndex].copy(isMatched = true); it[index] = it[index].copy(isMatched = true) }
                    if (cards.all { it.isMatched }) {
                        showCompleteMessage = true
                        if (!rewardClaimed) { rewardClaimed = true; prefs.edit().putBoolean(rewardKey, true).apply(); onEarnPoints(level.rewardPoints) }
                        if (level.index > maxCompletedLevel) { maxCompletedLevel = level.index; prefs.edit().putInt(KEY_MEMORY_MAX_LEVEL_COMPLETED, level.index).apply(); nextUnlockMessage = allLevels.getOrNull(level.index + 1)?.let { "Next unlocked: ${it.sceneName} • L${it.levelNumber}" } ?: "Journey complete" }
                    }
                } else { cards = cards.toMutableList().also { it[firstIndex] = it[firstIndex].copy(isFaceUp = false); it[index] = it[index].copy(isFaceUp = false) } }
                firstSelection = null; isResolvingPair = false
            }
        }
    })
}

@Composable
private fun GameBoardScreen(backLabel: String, onBack: () -> Unit, title: String, subtitle: String, sceneName: String? = null, sceneEmoji: String? = null, headerTitle: String, headerSubtitle: String, pairs: Int, rewardText: String, targetText: String, statusText: String, boardTitle: String, completionText: String, cards: List<MemoryGardenCard>, moves: Int, showCompleteMessage: Boolean, nextButtonText: String? = null, onNextLevel: (() -> Unit)? = null, onResetBoard: () -> Unit, onCardClick: (Int) -> Unit) {
    val matchedPairs = cards.count { it.isMatched } / 2
    val visual = memoryGardenVisual(sceneName ?: "Morning Meadow")
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text(backLabel) }
        Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (sceneName != null && sceneEmoji != null) { MemoryGardenSceneHero(sceneName = sceneName, sceneEmoji = sceneEmoji, subtitle = "Scene art, themed symbols, and polished boards make each journey stop feel distinct.", modifier = Modifier.fillMaxWidth().height(196.dp)) }
        Card(colors = CardDefaults.cardColors(containerColor = visual.accentSoft.copy(alpha = 0.92f)), shape = RoundedCornerShape(22.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = headerTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = headerSubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(label = "Pairs", value = pairs.toString(), modifier = Modifier.weight(1f))
                    StatChip(label = "Reward", value = rewardText, modifier = Modifier.weight(1f))
                    StatChip(label = "Target", value = targetText, modifier = Modifier.weight(1f))
                }
                Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = visual.boardTint.copy(alpha = 0.98f)), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = boardTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = "$matchedPairs / $pairs matched", style = MaterialTheme.typography.bodyMedium, color = visual.accent, fontWeight = FontWeight.Medium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Moves: $moves", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (showCompleteMessage) { Text(text = completionText, style = MaterialTheme.typography.bodyMedium, color = visual.accent, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End) }
                }
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(visual.boardTint, visual.accentSoft.copy(alpha = 0.9f)))).border(1.dp, visual.accent.copy(alpha = 0.16f), RoundedCornerShape(24.dp)).padding(12.dp)) {
                    MemoryGardenBoard(cards = cards, sceneName = sceneName ?: "Morning Meadow", onCardClick = onCardClick)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    if (showCompleteMessage && onNextLevel != null && nextButtonText != null) { FilledTonalButton(onClick = onNextLevel, colors = ButtonDefaults.filledTonalButtonColors(containerColor = visual.accent, contentColor = Color.White)) { Text(nextButtonText) } }
                    Button(onClick = onResetBoard, colors = ButtonDefaults.buttonColors(containerColor = visual.accentSoft, contentColor = visual.accent)) { Text("Shuffle") }
                }
            }
        }
    }
}

@Composable
private fun MemoryGardenBoard(cards: List<MemoryGardenCard>, sceneName: String, onCardClick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cards.chunked(4).forEach { rowCards ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCards.forEach { card ->
                    val absoluteIndex = cards.indexOfFirst { it.id == card.id }
                    MemoryGardenCardView(card = card, sceneName = sceneName, modifier = Modifier.weight(1f), onClick = { onCardClick(absoluteIndex) })
                }
                repeat(4 - rowCards.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MemoryGardenCardView(card: MemoryGardenCard, sceneName: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val visual = memoryGardenVisual(sceneName)
    val frontColor = when { card.isMatched -> visual.accentSoft; else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f) }
    Box(modifier = modifier.height(76.dp).clip(RoundedCornerShape(20.dp)).background(if (card.isFaceUp || card.isMatched) Brush.verticalGradient(listOf(frontColor, Color.White)) else Brush.verticalGradient(visual.gradient)).border(width = 1.5.dp, color = if (card.isMatched) visual.accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.55f), shape = RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(6.dp)) {
        if (card.isFaceUp || card.isMatched) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = card.symbol, fontSize = 28.sp, textAlign = TextAlign.Center)
                if (card.isMatched) { Text(text = "✦", color = visual.accent, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp), fontSize = 12.sp) }
            }
        } else {
            Text(text = visual.cardBackEmoji, fontSize = 22.sp, modifier = Modifier.align(Alignment.Center))
            Text(text = visual.decor.first(), color = Color.White.copy(alpha = 0.85f), modifier = Modifier.align(Alignment.TopStart), fontSize = 11.sp)
            Text(text = visual.decor.last(), color = Color.White.copy(alpha = 0.75f), modifier = Modifier.align(Alignment.BottomEnd), fontSize = 11.sp)
        }
    }
}

@Composable
private fun SceneBadge(visual: MemoryGardenSceneVisual) {
    Surface(shape = RoundedCornerShape(18.dp), color = visual.accentSoft.copy(alpha = 0.95f)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(visual.emoji, fontSize = 16.sp)
            Text(text = visual.label, color = visual.accent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MemoryGardenSceneHero(sceneName: String, sceneEmoji: String, subtitle: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    val visual = memoryGardenVisual(sceneName)
    val isNight = sceneName.contains("Moonlight", ignoreCase = true) || sceneName.contains("Grove", ignoreCase = true)
    val titleText = when { sceneName.contains("Meadow", ignoreCase = true) || sceneName.contains("Garden", ignoreCase = true) -> "Breathe into the meadow"; sceneName.contains("Pond", ignoreCase = true) || sceneName.contains("Water", ignoreCase = true) -> "Let the pond settle your focus"; sceneName.contains("Tea", ignoreCase = true) -> "Cozy up and match mindfully"; else -> "Glow through the grove" }
    val textColor = if (isNight) Color.White else Color(0xFF183126)
    val secondaryTextColor = if (isNight) Color.White.copy(alpha = 0.92f) else Color(0xFF24473A).copy(alpha = 0.92f)
    val panelColor = if (isNight) Color(0xFF1D2B59).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.74f)
    val chipColor = if (isNight) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.55f)
    val borderShape = RoundedCornerShape(if (compact) 22.dp else 26.dp)
    Box(modifier = modifier.clip(borderShape).background(Brush.verticalGradient(visual.gradient)).border(1.dp, Color.White.copy(alpha = 0.55f), borderShape)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when {
                sceneName.contains("Meadow", ignoreCase = true) || sceneName.contains("Garden", ignoreCase = true) -> {
                    drawCircle(Color(0xFFFFE58A), radius = size.minDimension * 0.11f, center = Offset(size.width * 0.82f, size.height * 0.22f))
                    drawCircle(Color(0xFFFFE58A).copy(alpha = 0.18f), radius = size.minDimension * 0.18f, center = Offset(size.width * 0.82f, size.height * 0.22f))
                    drawRoundRect(Color(0xFFCBE6B7), topLeft = Offset(-10f, size.height * 0.56f), size = Size(size.width * 1.1f, size.height * 0.22f), cornerRadius = CornerRadius(140f, 140f))
                    drawRoundRect(Color(0xFF7FB780), topLeft = Offset(-12f, size.height * 0.66f), size = Size(size.width * 0.72f, size.height * 0.26f), cornerRadius = CornerRadius(160f, 160f))
                    drawRoundRect(Color(0xFF5E9E66), topLeft = Offset(size.width * 0.38f, size.height * 0.7f), size = Size(size.width * 0.74f, size.height * 0.24f), cornerRadius = CornerRadius(160f, 160f))
                    repeat(3) { i -> val x = size.width * (0.12f + i * 0.08f); val y = size.height * (0.66f + (i % 2) * 0.03f); drawLine(Color(0xFF5E9E66), Offset(x, y), Offset(x, y - size.height * 0.08f), strokeWidth = 3.dp.toPx()); drawCircle(Color(0xFFFFB65E), radius = 7.dp.toPx(), center = Offset(x, y - size.height * 0.09f)) }
                }
                sceneName.contains("Pond", ignoreCase = true) || sceneName.contains("Water", ignoreCase = true) -> {
                    drawCircle(Color.White.copy(alpha = 0.3f), radius = size.minDimension * 0.09f, center = Offset(size.width * 0.14f, size.height * 0.22f))
                    drawRoundRect(Color(0xFFA5E0ED), topLeft = Offset(size.width * 0.08f, size.height * 0.55f), size = Size(size.width * 0.84f, size.height * 0.22f), cornerRadius = CornerRadius(160f, 160f))
                    drawRoundRect(Color(0xFF7BC4D6), topLeft = Offset(size.width * 0.14f, size.height * 0.62f), size = Size(size.width * 0.72f, size.height * 0.14f), cornerRadius = CornerRadius(160f, 160f))
                    repeat(3) { i -> drawArc(Color.White.copy(alpha = 0.28f), startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(size.width * (0.18f + i * 0.16f), size.height * 0.64f), size = Size(size.width * 0.18f, size.height * 0.08f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)) }
                    drawCircle(Color(0xFFFFAAC8), radius = size.minDimension * 0.06f, center = Offset(size.width * 0.75f, size.height * 0.58f))
                    drawCircle(Color(0xFFFFC2D8), radius = size.minDimension * 0.035f, center = Offset(size.width * 0.68f, size.height * 0.52f))
                }
                sceneName.contains("Tea", ignoreCase = true) -> {
                    drawRoundRect(Color(0xFFA7794F).copy(alpha = 0.25f), topLeft = Offset(size.width * 0.1f, size.height * 0.18f), size = Size(size.width * 0.3f, size.height * 0.34f), cornerRadius = CornerRadius(22f, 22f))
                    drawRoundRect(Color(0xFF8E5F3A), topLeft = Offset(size.width * 0.14f, size.height * 0.24f), size = Size(size.width * 0.22f, size.height * 0.28f), cornerRadius = CornerRadius(18f, 18f))
                    drawLine(Color(0xFF724725), Offset(size.width * 0.25f, size.height * 0.18f), Offset(size.width * 0.25f, size.height * 0.1f), strokeWidth = 3.dp.toPx())
                    drawCircle(Color(0xFFFFEBD3).copy(alpha = 0.75f), radius = size.minDimension * 0.045f, center = Offset(size.width * 0.72f, size.height * 0.42f))
                    drawArc(Color.White.copy(alpha = 0.6f), startAngle = 180f, sweepAngle = 80f, useCenter = false, topLeft = Offset(size.width * 0.67f, size.height * 0.21f), size = Size(size.width * 0.12f, size.height * 0.18f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(Color.White.copy(alpha = 0.42f), startAngle = 180f, sweepAngle = 80f, useCenter = false, topLeft = Offset(size.width * 0.73f, size.height * 0.17f), size = Size(size.width * 0.1f, size.height * 0.16f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    drawRoundRect(Color(0xFFC7986D), topLeft = Offset(-10f, size.height * 0.68f), size = Size(size.width * 1.1f, size.height * 0.18f), cornerRadius = CornerRadius(140f, 140f))
                }
                else -> {
                    drawCircle(Color(0xFFFFF2B0), radius = size.minDimension * 0.1f, center = Offset(size.width * 0.82f, size.height * 0.2f))
                    drawCircle(Color(0xFFFFF2B0).copy(alpha = 0.15f), radius = size.minDimension * 0.17f, center = Offset(size.width * 0.82f, size.height * 0.2f))
                    repeat(9) { i -> drawCircle(Color.White.copy(alpha = 0.55f), radius = 2.4.dp.toPx(), center = Offset(size.width * (0.1f + (i % 5) * 0.12f), size.height * (0.16f + (i / 5) * 0.1f))) }
                    drawRoundRect(Color(0xFF27386B), topLeft = Offset(-10f, size.height * 0.62f), size = Size(size.width * 1.1f, size.height * 0.2f), cornerRadius = CornerRadius(140f, 140f))
                    drawRoundRect(Color(0xFF1A2550), topLeft = Offset(size.width * 0.32f, size.height * 0.7f), size = Size(size.width * 0.84f, size.height * 0.16f), cornerRadius = CornerRadius(140f, 140f))
                }
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(if (compact) 14.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(18.dp), color = chipColor) { Text(text = "$sceneEmoji  ${visual.label}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = textColor, fontWeight = FontWeight.Bold, fontSize = if (compact) 13.sp else 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Surface(shape = RoundedCornerShape(16.dp), color = chipColor.copy(alpha = if (isNight) 0.75f else 0.95f)) { Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { visual.decor.take(3).forEach { deco -> Text(deco, fontSize = if (compact) 15.sp else 17.sp) } } }
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(if (compact) 18.dp else 22.dp), color = panelColor) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = if (compact) 12.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = titleText, color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = if (compact) 16.sp else 22.sp, lineHeight = if (compact) 20.sp else 26.sp, maxLines = if (compact) 2 else 2, overflow = TextOverflow.Ellipsis)
                    Text(text = subtitle, color = secondaryTextColor, fontSize = if (compact) 11.sp else 13.sp, lineHeight = if (compact) 15.sp else 18.sp, maxLines = if (compact) 2 else 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)).padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusChip(text: String, emphasized: Boolean) {
    Surface(shape = RoundedCornerShape(16.dp), color = if (emphasized) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)) {
        Text(text = text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall, color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

private data class MemoryGardenSceneSpec(val name: String, val emoji: String, val pool: List<String>, val subtitles: List<String>, val pairPlan: List<Int>)

private fun buildMemoryGardenLevels(): List<MemoryGardenLevel> {
    val scenes = listOf(
        MemoryGardenSceneSpec("Morning Meadow", "🌼", listOf("🌿", "🌼", "🍃", "🌷", "🍀", "🌱", "🪻", "🌸", "🐝", "🦋", "🌞", "🌾"), listOf("A simple warm-up with soft meadow symbols and a very small board.", "The meadow opens a little wider and asks you to hold a few more cards in mind.", "Fresh leaves and blooms mix together for a slightly longer round.", "A breezier meadow board adds more nature details to track at once.", "The path through the flowers gets busier and steadier attention matters more.", "Small shifts in the meadow make the matches less obvious than before.", "The board grows and the garden starts to reward slower, more careful play.", "A fuller field of symbols now asks for stronger recall across more turns.", "The meadow reaches a denser layout with tighter room for mistakes.", "This final meadow board sets up the next scene with a calm but longer challenge."), listOf(4, 4, 5, 5, 6, 6, 7, 7, 8, 8)),
        MemoryGardenSceneSpec("Lotus Pond", "🪷", listOf("🪷", "🌊", "🐚", "🫧", "🐟", "🪨", "🐸", "☀️", "🦆", "🐠", "🪸", "🌤️"), listOf("Still water reflections introduce a calmer but slightly larger board.", "The pond adds more ripples, shells, and soft details to remember.", "A broader waterline makes the layout longer than the first pond board.", "More symbols drift into view and the pair count begins to climb.", "The pond grows busier with floating details that require steadier focus.", "A deeper pond board rewards careful matching instead of quick guessing.", "The reflections become denser and the route to a clear board gets longer.", "This level stretches the pond into a fuller scene with more pair pressure.", "A packed pond layout pushes memory a little farther than the earlier levels.", "The final pond board closes the scene with one of the longest water rounds yet."), listOf(5, 6, 6, 7, 7, 8, 8, 8, 9, 9)),
        MemoryGardenSceneSpec("Tea House", "🫖", listOf("🫖", "🍵", "📚", "🕯️", "🍯", "🍪", "🪑", "🪟", "🥮", "🧺", "🪴", "🕰️"), listOf("The journey moves indoors with cozy tea house items and a larger board.", "Warm tea house details mix together and ask for steadier recall.", "A calmer room layout still grows more complex as more objects appear.", "Books, candles, and tea tools create a denser memory pattern to clear.", "This level expands the tea house and keeps the board active for longer.", "The room grows fuller and rewards careful tracking across more flips.", "A wider tea room board now asks you to hold many details at once.", "The tea house becomes one of the longer scenes with more repeated-looking items.", "A crowded indoor board tightens the challenge before the final scene opens.", "This last tea house level prepares you for the hardest moonlit boards ahead."), listOf(6, 6, 7, 7, 8, 8, 9, 9, 10, 10)),
        MemoryGardenSceneSpec("Moonlight Grove", "🌙", listOf("🌙", "⭐", "☁️", "🕯️", "🦉", "✨", "🌌", "🌠", "🪐", "🔮", "🌜", "🦋"), listOf("Night settles in and the first moonlit board opens with glowing symbols.", "The grove adds more starlit details and stretches the board a bit farther.", "A deeper moonlit path makes the matches less obvious than before.", "The grove grows wider with more glowing cards to hold in memory.", "Moonlight reflections build a longer board that rewards calm concentration.", "This level asks for steadier recall across a fuller spread of night symbols.", "A larger night board pushes the journey toward its hardest stretch.", "The grove becomes denser and more demanding as more celestial items appear.", "One of the toughest boards in the game, with little room to rush your moves.", "The final grove board completes the full Memory Garden journey with the largest moonlit challenge."), listOf(6, 7, 7, 8, 8, 9, 9, 10, 10, 10))
    )
    val levels = mutableListOf<MemoryGardenLevel>(); var globalIndex = 0
    scenes.forEachIndexed { sceneIndex, scene ->
        scene.pairPlan.forEachIndexed { levelInScene, pairs ->
            val rotation = (levelInScene * 2 + sceneIndex) % scene.pool.size
            val rotatedPool = scene.pool.drop(rotation) + scene.pool.take(rotation)
            val symbols = rotatedPool.take(pairs)
            val target = pairs * 2 + maxOf(2, 5 - sceneIndex) - minOf(2, levelInScene / 4)
            val reward = 6 + sceneIndex * 4 + levelInScene
            levels += MemoryGardenLevel(index = globalIndex, sceneName = scene.name, sceneEmoji = scene.emoji, levelNumber = levelInScene + 1, title = "${scene.name} • Level ${levelInScene + 1}", subtitle = scene.subtitles[levelInScene], symbols = symbols, rewardPoints = reward, suggestedGoal = target, pairs = pairs)
            globalIndex += 1
        }
    }
    return levels
}

private fun buildMemoryGardenChallenge(dateKey: String): MemoryGardenChallenge {
    val seed = abs(dateKey.hashCode()); val random = Random(seed)
    val themePools = listOf(Triple("Moonlight Grove", "🌙", listOf("🌙", "⭐", "☁️", "🕯️", "🦋", "✨", "🌌", "🫖")), Triple("Morning Meadow", "🌼", listOf("🌿", "🌼", "🍃", "🌸", "🍀", "🌷", "🐝", "🦋")), Triple("Lotus Pond", "🪷", listOf("🌊", "🐚", "🐟", "🫧", "🪨", "🐸", "🦆", "☀️")), Triple("Tea House", "🫖", listOf("🍵", "🫖", "📚", "🕯️", "🍯", "🍪", "🥮", "🪴")))
    val adjectives = listOf("Quiet", "Soft", "Steady", "Gentle", "Morning", "Evening")
    val (themeName, themeEmoji, pool) = themePools[random.nextInt(themePools.size)]
    val pairCount = if (random.nextBoolean()) 6 else 8
    val selectedSymbols = pool.shuffled(random).take(pairCount)
    val adjective = adjectives[random.nextInt(adjectives.size)]
    val reward = if (pairCount == 6) 10 + random.nextInt(0, 3) else 12 + random.nextInt(0, 4)
    val moveGoal = if (pairCount == 6) 17 + random.nextInt(0, 4) else 22 + random.nextInt(0, 5)
    return MemoryGardenChallenge(title = "$adjective $themeName Match", subtitle = "Today's board uses a rotating $themeName-themed symbol set with $pairCount pairs to gently challenge focus, memory, and attention.", themeName = themeName, themeEmoji = themeEmoji, symbols = selectedSymbols, rewardPoints = reward, suggestedGoal = moveGoal, pairs = pairCount)
}

private fun buildShuffledDeck(symbols: List<String>, seed: Int): List<MemoryGardenCard> {
    val random = Random(seed)
    return symbols.flatMapIndexed { index, symbol -> listOf(MemoryGardenCard(id = index * 2, symbol = symbol), MemoryGardenCard(id = index * 2 + 1, symbol = symbol)) }.shuffled(random)
}

private fun currentDateKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

// ═════════════════════════════════════════════════════════════════════════════
//  CONFLICT MODE — UPGRADED
// ═════════════════════════════════════════════════════════════════════════════

// ── Conflict color palette ────────────────────────────────────────────────────
private val CFBg          = Color(0xFF070E1A)
private val CFPanel       = Color(0xFF0E1E30)
private val CFBorder      = Color(0xFF1C3650)
private val CFTextLight   = Color(0xFFEAF4FF)
private val CFTextDim     = Color(0xFF5A7A9A)
private val CFGold        = Color(0xFFFFCB47)
private val CFCorrect     = Color(0xFF3DFFA0)
private val CFWrong       = Color(0xFFFF3F6C)

private val conflictColorMap = mapOf(
    "RED"    to Color(0xFFFF3B5E),
    "BLUE"   to Color(0xFF3BA8FF),
    "GREEN"  to Color(0xFF3DEB8A),
    "YELLOW" to Color(0xFFFFD840)
)
private val conflictColorNames = conflictColorMap.keys.toList()
private val conflictArrows = listOf("←", "→")

// ── Daily challenge helper ────────────────────────────────────────────────────
private fun buildConflictDailyConfig(dateKey: String): ConflictDifficulty {
    val seed = abs(dateKey.hashCode())
    return when (seed % 3) {
        0 -> ConflictDifficulty.EASY
        1 -> ConflictDifficulty.MEDIUM
        else -> ConflictDifficulty.HARD
    }
}

// ── Round builder ─────────────────────────────────────────────────────────────
private fun buildConflictRound(rng: Random, difficulty: ConflictDifficulty): ConflictRound {
    val wordName = conflictColorNames.random(rng)
    val inkName  = conflictColorNames.filter { it != wordName }.random(rng)
    val inkColor = conflictColorMap[inkName]!!
    val arrowDir = conflictArrows.random(rng)

    // Difficulty drives which modes appear
    val mode: ConflictRuleMode = when (difficulty) {
        ConflictDifficulty.EASY   -> ConflictRuleMode.COLOR
        ConflictDifficulty.MEDIUM -> if (rng.nextBoolean()) ConflictRuleMode.COLOR else ConflictRuleMode.ARROW
        ConflictDifficulty.HARD   -> ConflictRuleMode.values().random(rng)
    }

    // Shape challenge — only on medium/hard
    val shapeType = if (difficulty != ConflictDifficulty.EASY && rng.nextFloat() < 0.35f) {
        ConflictShapeType.values().filter { it != ConflictShapeType.NONE }.random(rng)
    } else ConflictShapeType.NONE

    val shapeColor = if (shapeType != ConflictShapeType.NONE) {
        conflictColorMap.values.random(rng)
    } else Color.Transparent

    val correctAnswer = when (mode) {
        ConflictRuleMode.COLOR    -> inkName
        ConflictRuleMode.ARROW    -> if (arrowDir == "←") "→" else "←"
        ConflictRuleMode.CONFLICT -> inkName
    }
    return ConflictRound(wordName, inkColor, arrowDir, mode, correctAnswer, shapeType, shapeColor)
}

private fun ConflictRuleMode.label() = when (this) {
    ConflictRuleMode.COLOR    -> "TAP THE INK COLOR"
    ConflictRuleMode.ARROW    -> "TAP THE OPPOSITE ARROW"
    ConflictRuleMode.CONFLICT -> "TAP BOTH: INK COLOR + OPPOSITE ARROW"
}

private fun ConflictRuleMode.badgeColor() = when (this) {
    ConflictRuleMode.COLOR    -> Color(0xFF3BA8FF)
    ConflictRuleMode.ARROW    -> Color(0xFF9B59FF)
    ConflictRuleMode.CONFLICT -> Color(0xFFFF3B5E)
}

// ── Friend activity storage helpers ──────────────────────────────────────────
private fun saveConflictFriendActivity(prefs: android.content.SharedPreferences, difficulty: String, score: String, dateKey: String, pointsEarned: Int) {
    val entry = "You · $difficulty · $score · $dateKey · ${pointsEarned}pts"
    val existing = prefs.getString(KEY_CONFLICT_FRIEND_ACTIVITY, "") ?: ""
    val lines = existing.split("\n").filter { it.isNotBlank() }.takeLast(9)
    prefs.edit().putString(KEY_CONFLICT_FRIEND_ACTIVITY, (lines + entry).joinToString("\n")).apply()
}

private fun loadConflictFriendActivity(prefs: android.content.SharedPreferences): List<String> {
    return (prefs.getString(KEY_CONFLICT_FRIEND_ACTIVITY, "") ?: "")
        .split("\n").filter { it.isNotBlank() }.reversed().take(5)
}

// ── Draw helpers for the conflict arena ──────────────────────────────────────
private fun DrawScope.drawConflictArena(t: Float, accentColor: Color) {
    // Animated scanline grid
    val gridAlpha = 0.04f
    val gapH = 32.dp.toPx()
    val rows = (size.height / gapH).toInt() + 1
    for (r in 0..rows) {
        drawLine(accentColor.copy(alpha = gridAlpha), Offset(0f, r * gapH), Offset(size.width, r * gapH), 1f)
    }
    val gapV = 32.dp.toPx()
    val cols = (size.width / gapV).toInt() + 1
    for (c in 0..cols) {
        drawLine(accentColor.copy(alpha = gridAlpha), Offset(c * gapV, 0f), Offset(c * gapV, size.height), 1f)
    }
    // Corner brackets
    val bLen = 22.dp.toPx(); val bW = 3.dp.toPx(); val pad = 16.dp.toPx()
    val bracketAlpha = 0.5f + 0.3f * sin(t * 2 * Math.PI.toFloat() * 1.5f)
    val bc = accentColor.copy(alpha = bracketAlpha)
    // TL
    drawLine(bc, Offset(pad, pad), Offset(pad + bLen, pad), bW)
    drawLine(bc, Offset(pad, pad), Offset(pad, pad + bLen), bW)
    // TR
    drawLine(bc, Offset(size.width - pad, pad), Offset(size.width - pad - bLen, pad), bW)
    drawLine(bc, Offset(size.width - pad, pad), Offset(size.width - pad, pad + bLen), bW)
    // BL
    drawLine(bc, Offset(pad, size.height - pad), Offset(pad + bLen, size.height - pad), bW)
    drawLine(bc, Offset(pad, size.height - pad), Offset(pad, size.height - pad - bLen), bW)
    // BR
    drawLine(bc, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - bLen, size.height - pad), bW)
    drawLine(bc, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - bLen), bW)
    // Centre crosshair glow
    val cx = size.width / 2f; val cy = size.height / 2f
    val glowR = 120.dp.toPx() * (0.85f + 0.15f * sin(t * 2 * Math.PI.toFloat() * 0.8f))
    drawCircle(accentColor.copy(alpha = 0.04f), glowR, Offset(cx, cy))
    drawCircle(accentColor.copy(alpha = 0.02f), glowR * 1.5f, Offset(cx, cy))
}

private fun DrawScope.drawConflictShape(shapeType: ConflictShapeType, shapeColor: Color, cx: Float, cy: Float, r: Float) {
    when (shapeType) {
        ConflictShapeType.CIRCLE -> {
            drawCircle(shapeColor.copy(alpha = 0.18f), r, Offset(cx, cy))
            drawCircle(shapeColor.copy(alpha = 0.7f), r, Offset(cx, cy), style = Stroke(3.dp.toPx()))
        }
        ConflictShapeType.TRIANGLE -> {
            val triPath = Path().apply {
                moveTo(cx, cy - r); lineTo(cx + r * 0.866f, cy + r * 0.5f); lineTo(cx - r * 0.866f, cy + r * 0.5f); close()
            }
            drawPath(triPath, shapeColor.copy(alpha = 0.18f))
            drawPath(triPath, shapeColor.copy(alpha = 0.7f), style = Stroke(3.dp.toPx()))
        }
        ConflictShapeType.STAR -> {
            val starPath = Path().apply {
                for (i in 0..9) {
                    val angle = (i.toFloat() / 10f * 2 * Math.PI - Math.PI / 2).toFloat()
                    val rad = if (i % 2 == 0) r else r * 0.45f
                    val px = cx + rad * cos(angle); val py = cy + rad * sin(angle)
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }; close()
            }
            drawPath(starPath, shapeColor.copy(alpha = 0.18f))
            drawPath(starPath, shapeColor.copy(alpha = 0.7f), style = Stroke(3.dp.toPx()))
        }
        ConflictShapeType.NONE -> {}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ConflictModeScreen — main entry
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ConflictModeScreen(onBackToGames: () -> Unit, onEarnPoints: (Int) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(GAMES_PREFS, Context.MODE_PRIVATE) }
    val dateKey = currentDateKey()

    // Navigation: hub → difficulty picker → playing → result
    var phase by remember { mutableStateOf("HUB") }   // HUB | INTRO | PLAYING | RESULT
    var selectedDifficulty by remember { mutableStateOf(ConflictDifficulty.MEDIUM) }
    var isDailyRun by remember { mutableStateOf(false) }

    // Game state
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var lastFeedback by remember { mutableStateOf("") }
    var feedbackColor by remember { mutableStateOf(CFCorrect) }
    var showFeedback by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(0) }
    var timerRunning by remember { mutableStateOf(false) }
    var rewardClaimed by remember { mutableStateOf(prefs.getString(KEY_CONFLICT_COMPLETED_DATE, null) == dateKey) }
    var dailyChallengeCompleted by remember { mutableStateOf(prefs.getString(KEY_CONFLICT_DAILY_DATE, null) == dateKey) }

    val rng = remember { Random(System.currentTimeMillis()) }
    var currentRound by remember { mutableStateOf(buildConflictRound(rng, ConflictDifficulty.MEDIUM)) }
    var conflictColorAnswered by remember { mutableStateOf(false) }
    var conflictArrowAnswered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dailyDifficulty = remember(dateKey) { buildConflictDailyConfig(dateKey) }
    val friendActivity = remember { loadConflictFriendActivity(prefs) }

    LaunchedEffect(round, timerRunning) {
        if (!timerRunning) return@LaunchedEffect
        timeLeft = selectedDifficulty.timePerRound.toInt()
        while (timeLeft > 0 && timerRunning) { delay(100); timeLeft -= 100 }
        if (timerRunning) {
            lastFeedback = "⏱ Too slow!"; feedbackColor = CFWrong; showFeedback = true; streak = 0; timerRunning = false
            delay(900); showFeedback = false
            if (round + 1 >= selectedDifficulty.rounds) { phase = "RESULT" } else {
                round += 1; currentRound = buildConflictRound(rng, selectedDifficulty); conflictColorAnswered = false; conflictArrowAnswered = false; timerRunning = true
            }
        }
    }

    fun handleAnswer(answer: String) {
        if (!timerRunning || showFeedback) return
        val r = currentRound
        val correct: Boolean
        if (r.activeMode == ConflictRuleMode.CONFLICT) {
            val oppositeArrow = if (r.arrowDir == "←") "→" else "←"
            val inkName = conflictColorMap.entries.firstOrNull { it.value == r.wordColor }?.key ?: ""
            if (answer in conflictColorNames && !conflictColorAnswered) { conflictColorAnswered = answer == inkName }
            else if (answer in conflictArrows && !conflictArrowAnswered) { conflictArrowAnswered = answer == oppositeArrow }
            if (!conflictColorAnswered || !conflictArrowAnswered) return
            correct = true
        } else { correct = answer == r.correctAnswer }

        timerRunning = false
        if (correct) { score += 1; streak += 1; if (streak > bestStreak) bestStreak = streak; lastFeedback = if (streak >= 5) "🔥 UNSTOPPABLE" else if (streak >= 3) "✨ ON FIRE!" else "✓ CORRECT!"; feedbackColor = CFCorrect }
        else { streak = 0; lastFeedback = "✗ WRONG!"; feedbackColor = CFWrong }
        showFeedback = true
        scope.launch {
            delay(700); showFeedback = false
            if (round + 1 >= selectedDifficulty.rounds) {
                phase = "RESULT"
                val pts = selectedDifficulty.rewardPoints
                if (score >= selectedDifficulty.rounds / 2) {
                    if (!rewardClaimed || isDailyRun) {
                        onEarnPoints(pts)
                        if (!rewardClaimed) { rewardClaimed = true; prefs.edit().putString(KEY_CONFLICT_COMPLETED_DATE, dateKey).apply() }
                    }
                    if (isDailyRun && !dailyChallengeCompleted) {
                        dailyChallengeCompleted = true
                        prefs.edit().putString(KEY_CONFLICT_DAILY_DATE, dateKey).apply()
                        saveConflictFriendActivity(prefs, selectedDifficulty.label, "$score/${selectedDifficulty.rounds}", dateKey, pts)
                    }
                }
            } else {
                round += 1; currentRound = buildConflictRound(rng, selectedDifficulty); conflictColorAnswered = false; conflictArrowAnswered = false; timerRunning = true
            }
        }
    }

    fun startGame(difficulty: ConflictDifficulty, daily: Boolean) {
        selectedDifficulty = difficulty; isDailyRun = daily
        round = 0; score = 0; streak = 0; bestStreak = 0
        currentRound = buildConflictRound(rng, difficulty); conflictColorAnswered = false; conflictArrowAnswered = false
        showFeedback = false; phase = "PLAYING"; timerRunning = true
    }

    Box(modifier = Modifier.fillMaxSize().background(CFBg)) {
        // Ambient background particles
        val infiniteT = rememberInfiniteTransition(label = "cf_bg")
        val bgT by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), label = "bgt")
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0..25) {
                val px = (i * 73 + 11) % size.width; val py = ((bgT * size.height * 0.4f + i * 97) % size.height)
                val alpha = 0.03f + 0.02f * sin((bgT * 4f + i * 0.8f).toFloat())
                drawCircle(ArcadeGlow.copy(alpha = alpha), (1.5f + (i % 3) * 0.8f).dp.toPx(), Offset(px, py))
            }
        }

        when (phase) {
            "HUB" -> ConflictHubScreen(
                dailyDifficulty = dailyDifficulty,
                dailyChallengeCompleted = dailyChallengeCompleted,
                friendActivity = friendActivity,
                onBack = onBackToGames,
                onStartDaily = { startGame(dailyDifficulty, daily = true) },
                onStartDifficulty = { diff -> startGame(diff, daily = false) }
            )
            "PLAYING" -> ConflictPlayScreen(
                round = round, difficulty = selectedDifficulty, score = score, streak = streak,
                timeLeft = timeLeft, currentRound = currentRound,
                showFeedback = showFeedback, feedbackText = lastFeedback, feedbackColor = feedbackColor,
                conflictColorAnswered = conflictColorAnswered, conflictArrowAnswered = conflictArrowAnswered,
                isDailyRun = isDailyRun,
                onAnswer = { handleAnswer(it) },
                onBack = { timerRunning = false; phase = "HUB" }
            )
            "RESULT" -> ConflictResultScreen(
                score = score, difficulty = selectedDifficulty, bestStreak = bestStreak,
                isDailyRun = isDailyRun, dailyChallengeCompleted = dailyChallengeCompleted,
                onPlayAgain = { startGame(selectedDifficulty, false) },
                onHub = { timerRunning = false; phase = "HUB" },
                onBack = { timerRunning = false; onBackToGames() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CONFLICT HUB — daily banner + difficulty cards + friend activity
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ConflictHubScreen(
    dailyDifficulty: ConflictDifficulty,
    dailyChallengeCompleted: Boolean,
    friendActivity: List<String>,
    onBack: () -> Unit,
    onStartDaily: () -> Unit,
    onStartDifficulty: (ConflictDifficulty) -> Unit
) {
    val infiniteT = rememberInfiniteTransition(label = "hub")
    val pulse by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "p")
    val shimmer by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart), label = "sh")

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Back + title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← All games", color = CFTextDim, fontSize = 13.sp) }
        }

        // ── Main header card ──────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0A1828), Color(0xFF0D2540), Color(0xFF0A1828))))
                .border(1.5.dp, ArcadeBlue.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                .padding(22.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                drawConflictArena(pulse, ArcadeGlow)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // Animated lightning bolt icon
                Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF7C4DFF).copy(alpha = 0.3f), Color.Transparent))).border(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                    val boltScale by animateFloatAsState((0.9f + 0.1f * sin(pulse * 2 * Math.PI.toFloat())), tween(100), label = "bs")
                    Text("⚡", fontSize = 26.sp, modifier = Modifier.scale(boltScale))
                }
                Spacer(Modifier.height(10.dp))
                Text("CONFLICT MODE", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = CFTextLight, letterSpacing = 4.sp)
                Spacer(Modifier.height(4.dp))
                Text("Fight your instincts. Train your focus.", fontSize = 12.sp, color = CFTextDim, letterSpacing = 1.sp)
            }
        }

        // ── Daily Challenge banner ────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(
                    if (dailyChallengeCompleted)
                        Brush.linearGradient(listOf(Color(0xFF1A2A18), Color(0xFF0E1E30)))
                    else
                        Brush.linearGradient(listOf(dailyDifficulty.accentColor.copy(alpha = 0.22f), Color(0xFF0E1E30)))
                )
                .border(1.5.dp, if (dailyChallengeCompleted) CFCorrect.copy(alpha = 0.4f) else dailyDifficulty.accentColor.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                .clickable(enabled = !dailyChallengeCompleted) { onStartDaily() }
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // Calendar icon with pulse ring
                Box(contentAlignment = Alignment.Center) {
                    if (!dailyChallengeCompleted) {
                        val ringAlpha = 0.2f + 0.2f * sin(pulse * 2 * Math.PI.toFloat())
                        Canvas(modifier = Modifier.size(56.dp)) {
                            drawCircle(dailyDifficulty.accentColor.copy(alpha = ringAlpha), size.minDimension / 2 * 0.9f, Offset(size.width / 2, size.height / 2), style = Stroke(4.dp.toPx()))
                        }
                    }
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(if (dailyChallengeCompleted) CFCorrect.copy(alpha = 0.15f) else dailyDifficulty.accentColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text(if (dailyChallengeCompleted) "✓" else "📅", fontSize = 22.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("DAILY CHALLENGE", fontSize = 9.sp, color = if (dailyChallengeCompleted) CFCorrect else dailyDifficulty.accentColor, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text(if (dailyChallengeCompleted) "Completed today!" else "${dailyDifficulty.emoji} ${dailyDifficulty.label} Mode — ${dailyDifficulty.rounds} rounds", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CFTextLight)
                    Text(if (dailyChallengeCompleted) "Come back tomorrow for a new challenge." else "+${dailyDifficulty.rewardPoints} calm pts · ${dailyDifficulty.timePerRound / 1000}s per round", fontSize = 11.sp, color = CFTextDim)
                }
                if (!dailyChallengeCompleted) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(dailyDifficulty.accentColor).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("PLAY", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = CFBg, letterSpacing = 1.sp)
                    }
                }
            }
        }

        // ── Difficulty picker label ───────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(CFBorder))
            Text("CHOOSE DIFFICULTY", fontSize = 9.sp, color = CFTextDim, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f).height(1.dp).background(CFBorder))
        }

        // ── Difficulty cards ──────────────────────────────────────────────────
        ConflictDifficulty.values().forEach { diff ->
            ConflictDifficultyCard(difficulty = diff, pulse = pulse) { onStartDifficulty(diff) }
        }

        // ── Friend activity ───────────────────────────────────────────────────
        if (friendActivity.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CFPanel).border(1.dp, CFBorder, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("👥", fontSize = 14.sp)
                    Text("FRIEND ACTIVITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CFTextDim, letterSpacing = 2.sp)
                }
                friendActivity.forEach { entry ->
                    val parts = entry.split(" · ")
                    if (parts.size >= 4) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(ArcadeBlueDeep), contentAlignment = Alignment.Center) { Text("🧠", fontSize = 13.sp) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(parts[0], fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CFTextLight)
                                Text("${parts[1]} · ${parts[2]} rounds · ${parts[3]}", fontSize = 10.sp, color = CFTextDim)
                            }
                            if (parts.size >= 5) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CFGold.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(parts[4], fontSize = 10.sp, color = CFGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ConflictDifficultyCard(difficulty: ConflictDifficulty, pulse: Float, onClick: () -> Unit) {
    val glowAlpha = 0.12f + 0.06f * sin(pulse * 2 * Math.PI.toFloat())
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(difficulty.accentColor.copy(alpha = 0.14f), CFPanel)))
            .border(1.dp, difficulty.accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Glow overlay
        Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            drawCircle(difficulty.accentColor.copy(alpha = glowAlpha), size.width * 0.35f, Offset(size.width * 0.08f, size.height / 2f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Difficulty badge
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(difficulty.accentColor.copy(alpha = 0.2f)).border(1.dp, difficulty.accentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(difficulty.emoji, fontSize = 24.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(difficulty.label.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = difficulty.accentColor, letterSpacing = 2.sp)
                Text(difficulty.description, fontSize = 11.sp, color = CFTextDim, lineHeight = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    CFMiniPill("${difficulty.rounds} rounds")
                    CFMiniPill("${difficulty.timePerRound / 1000}s")
                    CFMiniPill("+${difficulty.rewardPoints}pts", highlight = true, color = CFGold)
                }
            }
            Text("▶", fontSize = 16.sp, color = difficulty.accentColor.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun CFMiniPill(text: String, highlight: Boolean = false, color: Color = CFTextDim) {
    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (highlight) color.copy(alpha = 0.12f) else CFBorder.copy(alpha = 0.5f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, fontSize = 9.sp, color = if (highlight) color else CFTextDim, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal, letterSpacing = 0.5.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CONFLICT PLAY SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ConflictPlayScreen(
    round: Int, difficulty: ConflictDifficulty, score: Int, streak: Int,
    timeLeft: Int, currentRound: ConflictRound,
    showFeedback: Boolean, feedbackText: String, feedbackColor: Color,
    conflictColorAnswered: Boolean, conflictArrowAnswered: Boolean,
    isDailyRun: Boolean,
    onAnswer: (String) -> Unit, onBack: () -> Unit
) {
    val timerProgress = timeLeft.toFloat() / difficulty.timePerRound.toFloat()
    val timerColor by animateColorAsState(when { timerProgress > 0.6f -> difficulty.accentColor; timerProgress > 0.3f -> CFGold; else -> CFWrong }, tween(300), label = "tc")
    val feedbackScale by animateFloatAsState(if (showFeedback) 1.2f else 0.8f, tween(200), label = "fs")
    val feedbackAlpha by animateFloatAsState(if (showFeedback) 1f else 0f, tween(200), label = "fa")
    val infiniteT = rememberInfiniteTransition(label = "play")
    val arenaT by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "at")
    val wordPulse by infiniteT.animateFloat(0.93f, 1.0f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse), label = "wp")

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CFBg, Color(0xFF050C18))))) {
        // Arena background
        Canvas(modifier = Modifier.fillMaxSize()) { drawConflictArena(arenaT, difficulty.accentColor) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // ── Top HUD ───────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← exit", color = CFTextDim, fontSize = 11.sp) }
                // Round indicator dots
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    val totalRounds = difficulty.rounds
                    val displayCount = minOf(totalRounds, 15)
                    val groupSize = if (totalRounds > 15) totalRounds / 15 else 1
                    repeat(displayCount) { i ->
                        val filled = i * groupSize < round + 1
                        Box(modifier = Modifier.size(if (i == (round / groupSize).coerceIn(0, displayCount - 1)) 8.dp else 5.dp).clip(CircleShape).background(if (filled) difficulty.accentColor else CFBorder))
                    }
                }
                // Score + streak
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (streak >= 2) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CFGold.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("🔥$streak", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = CFGold) }
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(difficulty.accentColor.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text("$score", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = difficulty.accentColor) }
                }
            }

            // ── Thin timer bar ────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(CFBorder)) {
                Box(modifier = Modifier.fillMaxWidth(timerProgress.coerceIn(0f, 1f)).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(timerColor, timerColor.copy(alpha = 0.4f)))))
            }

            // ── Mode badge ────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(currentRound.activeMode.badgeColor().copy(alpha = 0.15f)).border(1.dp, currentRound.activeMode.badgeColor().copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 5.dp)) {
                    Text(currentRound.activeMode.label(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = currentRound.activeMode.badgeColor(), letterSpacing = 0.8.sp)
                }
            }

            // ── Main arena card ───────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(CFPanel, Color(0xFF050C18)))).border(1.5.dp, difficulty.accentColor.copy(alpha = 0.3f), RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) { drawConflictArena(arenaT * 0.5f, difficulty.accentColor) }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(20.dp)) {
                    // Shape challenge hint (if active)
                    if (currentRound.shapeType != ConflictShapeType.NONE) {
                        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawConflictShape(currentRound.shapeType, currentRound.shapeColor, size.width / 2f, size.height / 2f, size.minDimension * 0.38f)
                            }
                        }
                    }

                    // The word itself
                    Box(contentAlignment = Alignment.Center) {
                        // Glow behind word
                        Canvas(modifier = Modifier.size(200.dp, 90.dp)) {
                            drawCircle(currentRound.wordColor.copy(alpha = 0.12f * wordPulse), 80.dp.toPx(), Offset(size.width / 2f, size.height / 2f))
                        }
                        Text(
                            text = currentRound.word,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = currentRound.wordColor,
                            letterSpacing = 6.sp,
                            modifier = Modifier.scale(wordPulse)
                        )
                    }

                    // Arrow
                    Text(currentRound.arrowDir, fontSize = 42.sp, color = CFTextLight.copy(alpha = 0.95f), fontWeight = FontWeight.Bold)

                    // Feedback
                    Box(modifier = Modifier.height(36.dp), contentAlignment = Alignment.Center) {
                        if (showFeedback) {
                            Text(feedbackText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = feedbackColor, modifier = Modifier.scale(feedbackScale).alpha(feedbackAlpha), textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // ── Answer buttons ────────────────────────────────────────────────
            if (currentRound.activeMode == ConflictRuleMode.COLOR || currentRound.activeMode == ConflictRuleMode.CONFLICT) {
                Text("Tap the ink color:", fontSize = 10.sp, color = CFTextDim, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    conflictColorMap.forEach { (name, color) ->
                        val answered = conflictColorAnswered && currentRound.activeMode == ConflictRuleMode.CONFLICT
                        ConflictColorButton(name, color, answered, Modifier.weight(1f)) { onAnswer(name) }
                    }
                }
            }
            if (currentRound.activeMode == ConflictRuleMode.ARROW || currentRound.activeMode == ConflictRuleMode.CONFLICT) {
                Text("Tap the opposite arrow:", fontSize = 10.sp, color = CFTextDim, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    conflictArrows.forEach { arrow ->
                        val answered = conflictArrowAnswered && currentRound.activeMode == ConflictRuleMode.CONFLICT
                        ConflictArrowButton(arrow, difficulty.accentColor, answered, Modifier.weight(1f)) { onAnswer(arrow) }
                    }
                }
            }

            // Daily badge
            if (isDailyRun) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CFGold.copy(alpha = 0.1f)).border(1.dp, CFGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                        Text("📅 DAILY CHALLENGE", fontSize = 9.sp, color = CFGold, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConflictColorButton(label: String, color: Color, dimmed: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgAlpha by animateFloatAsState(if (dimmed) 0.18f else 0.88f, tween(200), label = "cba")
    Box(modifier = modifier.height(50.dp).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(color.copy(alpha = bgAlpha), color.copy(alpha = bgAlpha * 0.7f)))).border(1.dp, color.copy(alpha = if (dimmed) 0.2f else 0.6f), RoundedCornerShape(14.dp)).clickable(enabled = !dimmed, onClick = onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (dimmed) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.9f)))
            Text(label, color = if (dimmed) Color.White.copy(0.3f) else Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, letterSpacing = 0.5.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ConflictArrowButton(label: String, accentColor: Color, dimmed: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgColor by animateColorAsState(if (dimmed) CFPanel else Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.25f), CFPanel)).let { accentColor.copy(alpha = 0.2f) }, tween(200), label = "arba")
    Box(modifier = modifier.height(64.dp).clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(accentColor.copy(alpha = if (dimmed) 0.06f else 0.22f), CFPanel))).border(1.5.dp, accentColor.copy(alpha = if (dimmed) 0.15f else 0.55f), RoundedCornerShape(18.dp)).clickable(enabled = !dimmed, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 34.sp, color = if (dimmed) CFTextDim.copy(alpha = 0.3f) else CFTextLight, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CONFLICT RESULT SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ConflictResultScreen(
    score: Int, difficulty: ConflictDifficulty, bestStreak: Int,
    isDailyRun: Boolean, dailyChallengeCompleted: Boolean,
    onPlayAgain: () -> Unit, onHub: () -> Unit, onBack: () -> Unit
) {
    val pct = (score.toFloat() / difficulty.rounds * 100).toInt()
    val grade = when { pct >= 90 -> "🏆 MASTER"; pct >= 70 -> "⭐ SHARP"; pct >= 50 -> "👍 SOLID"; else -> "💪 KEEP GOING" }
    val gradeColor = when { pct >= 90 -> CFGold; pct >= 70 -> CFCorrect; pct >= 50 -> difficulty.accentColor; else -> Color(0xFFFF7A40) }
    val passed = score >= difficulty.rounds / 2

    val infiniteT = rememberInfiniteTransition(label = "result")
    val shimmer by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart), label = "sh")
    val pulse by infiniteT.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "rp")

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CFBg, Color(0xFF050C18))))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawConflictArena(shimmer, difficulty.accentColor)
            // Particle celebration for pass
            if (passed) {
                for (i in 0..20) {
                    val px = (i * 71 + 17) % size.width; val py = (i * 53 + 31) % size.height
                    val alpha = 0.15f + 0.1f * sin((shimmer * 5f + i).toFloat())
                    drawCircle(if (i % 2 == 0) CFGold.copy(alpha = alpha) else CFCorrect.copy(alpha = alpha * 0.7f), (2f + (i % 4)).dp.toPx(), Offset(px, py))
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))

            Text("ROUND COMPLETE", fontSize = 10.sp, color = CFTextDim, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)

            // Score circle
            Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Outer ring
                    drawCircle(CFPanel, size.minDimension / 2f)
                    drawCircle(difficulty.accentColor.copy(alpha = 0.18f), size.minDimension / 2f)
                    drawCircle(difficulty.accentColor.copy(alpha = 0.5f), size.minDimension / 2f, style = Stroke(3.dp.toPx()))
                    // Progress arc
                    val sweep = 360f * (score.toFloat() / difficulty.rounds)
                    drawArc(color = difficulty.accentColor, startAngle = -90f, sweepAngle = sweep, useCenter = false, topLeft = Offset(12.dp.toPx(), 12.dp.toPx()), size = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx()), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
                    // Inner glow
                    val gA = 0.06f + 0.04f * sin(pulse * 2 * Math.PI.toFloat())
                    drawCircle(difficulty.accentColor.copy(alpha = gA), size.minDimension * 0.3f)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score/${difficulty.rounds}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = CFTextLight)
                    Text("$pct%", fontSize = 12.sp, color = CFTextDim)
                }
            }

            Text(grade, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = gradeColor)

            // Difficulty badge
            Row(horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(difficulty.accentColor.copy(alpha = 0.14f)).border(1.dp, difficulty.accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("${difficulty.emoji} ${difficulty.label.uppercase()} MODE", fontSize = 10.sp, color = difficulty.accentColor, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }

            // Stats row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CFResultStat("Correct", "$score", Modifier.weight(1f))
                CFResultStat("Best Streak", "🔥$bestStreak", Modifier.weight(1f))
                CFResultStat("Accuracy", "$pct%", Modifier.weight(1f))
            }

            // Points earned
            if (passed) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CFGold.copy(alpha = 0.1f)).border(1.dp, CFGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("✦  +${difficulty.rewardPoints} calm pts earned  ✦", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CFGold, textAlign = TextAlign.Center)
                    }
                }
            }

            // Daily complete badge
            if (isDailyRun && passed) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CFCorrect.copy(alpha = 0.1f)).border(1.dp, CFCorrect.copy(alpha = 0.4f), RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Text("📅 Daily challenge complete! Saved to friend activity.", fontSize = 12.sp, color = CFCorrect, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                }
            }

            // CTA buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPlayAgain, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = difficulty.accentColor), shape = RoundedCornerShape(16.dp)) {
                    Text("Play Again", fontWeight = FontWeight.ExtraBold, color = CFBg)
                }
                Button(onClick = onHub, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CFPanel), shape = RoundedCornerShape(16.dp)) {
                    Text("Levels", fontWeight = FontWeight.Bold, color = CFTextLight)
                }
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = CFBorder), shape = RoundedCornerShape(12.dp)) {
                Text("All Games", fontWeight = FontWeight.Medium, color = CFTextDim, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CFResultStat(label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(CFPanel).border(1.dp, CFBorder, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CFTextLight, textAlign = TextAlign.Center)
            Text(label, fontSize = 10.sp, color = CFTextDim, textAlign = TextAlign.Center)
        }
    }
}
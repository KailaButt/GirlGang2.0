package com.example.consolicalm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.min

enum class RootCause(val title: String) {
    FATIGUE("Fatigue / low energy"),
    OVERLOAD("Mental overload"),
    AVOIDANCE("Avoidance / anxiety"),
    PERFECTIONISM("Perfectionism"),
    LACK_OF_CLARITY("Lack of clarity"),
    DISTRACTION("Distractions / environment"),
    LOW_MOTIVATION("Low motivation / boredom"),
    TIME_PRESSURE("Time pressure / overwhelm"),
    OTHER("Other / not sure")
}

data class CoachResult(
    val primary: RootCause,
    val secondary: RootCause? = null,
    val confidence: Float, // 0..1
    val explanation: String,
    val microActions: List<String>,
    val recommendedMode: StudyMode? = null
)

object RuleBasedCoachAnalyzer {

    private val phraseRules: Map<RootCause, List<String>> = mapOf(
        RootCause.LACK_OF_CLARITY to listOf(
            "don't know where to start", "dont know where to start",
            "no idea where to start", "i'm confused", "im confused",
            "unclear", "too many steps", "i'm lost", "im lost"
        ),
        RootCause.PERFECTIONISM to listOf(
            "has to be perfect", "needs to be perfect", "not good enough",
            "i'll mess up", "ill mess up", "fear of failing", "scared to fail",
            "what if it's bad", "what if its bad"
        ),
        RootCause.AVOIDANCE to listOf(
            "i'm anxious", "im anxious", "i'm stressed", "im stressed",
            "i'm dreading", "im dreading", "i keep avoiding", "avoid it",
            "i feel overwhelmed", "panic", "nervous"
        ),
        RootCause.DISTRACTION to listOf(
            "can't stop scrolling", "cant stop scrolling", "keep scrolling",
            "stuck on my phone", "distracted", "social media", "notifications"
        ),
        RootCause.FATIGUE to listOf(
            "i'm tired", "im tired", "no energy", "burnt out", "burned out",
            "exhausted", "sleepy"
        ),
        RootCause.OVERLOAD to listOf(
            "too much", "so much to do", "my brain is full", "mental overload",
            "i have a lot", "too many things"
        ),
        RootCause.LOW_MOTIVATION to listOf(
            "i don't care", "i dont care", "boring", "not motivated",
            "no motivation", "i hate this"
        ),
        RootCause.TIME_PRESSURE to listOf(
            "due soon", "running out of time", "deadline", "last minute",
            "i'm behind", "im behind"
        )
    )

    private val keywordRules: Map<RootCause, List<String>> = mapOf(
        RootCause.FATIGUE to listOf("tired", "sleepy", "exhausted", "drained", "burnt", "burned", "fatigue"),
        RootCause.OVERLOAD to listOf("overwhelmed", "overload", "chaos", "stressed"),
        RootCause.AVOIDANCE to listOf("anxious", "anxiety", "scared", "avoid", "dreading", "panic", "nervous"),
        RootCause.PERFECTIONISM to listOf("perfect", "perfection", "fail", "failing", "mistake", "wrong"),
        RootCause.LACK_OF_CLARITY to listOf("confused", "unclear", "lost", "start", "steps"),
        RootCause.DISTRACTION to listOf("scroll", "phone", "tiktok", "instagram", "youtube", "distracted", "notifications"),
        RootCause.LOW_MOTIVATION to listOf("boring", "lazy", "motivation", "dont", "don't", "care", "hate"),
        RootCause.TIME_PRESSURE to listOf("deadline", "due", "soon", "late", "behind")
    )

    fun analyze(raw: String): CoachResult {
        val cleaned = normalize(raw)
        if (cleaned.isBlank()) return fallbackResult()

        val scores = mutableMapOf<RootCause, Int>()
        RootCause.values().forEach { scores[it] = 0 }

        // phrase matches (strong)
        for ((cause, phrases) in phraseRules) {
            for (p in phrases) {
                if (cleaned.contains(p)) scores[cause] = (scores[cause] ?: 0) + 4
            }
        }

        // keyword matches (fuzzy)
        val words = cleaned.split(" ").filter { it.isNotBlank() }
        for ((cause, keys) in keywordRules) {
            for (k in keys) {
                if (containsKeywordFuzzy(words, k)) {
                    scores[cause] = (scores[cause] ?: 0) + 1
                }
            }
        }

        val sorted = scores.entries
            .filter { it.key != RootCause.OTHER }
            .sortedByDescending { it.value }

        val top = sorted.firstOrNull()
        val second = sorted.drop(1).firstOrNull()

        val topScore = top?.value ?: 0
        val secondScore = second?.value ?: 0

        val confidence = when {
            topScore >= 6 -> 0.90f
            topScore >= 4 -> 0.70f
            topScore >= 2 -> 0.48f
            else -> 0.20f
        }

        if (top == null || topScore == 0 || confidence < 0.35f) {
            return uncertainResult()
        }

        val primary = top.key
        val secondary = if (secondScore >= 2 && second?.key != primary) second?.key else null

        return buildResult(primary, secondary, confidence)
    }

    fun buildResult(primary: RootCause, secondary: RootCause?, confidence: Float = 0.70f): CoachResult {
        val (explain, actions, mode) = when (primary) {
            RootCause.FATIGUE -> Triple(
                "Low energy makes your brain choose easy tasks (like scrolling) over hard ones.",
                listOf(
                    "Do Quick Start for 5 minutes only — you can stop after.",
                    "Get water + stand up for 30 seconds, then begin."
                ),
                StudyMode.QUICKSTART_5_1
            )

            RootCause.LACK_OF_CLARITY -> Triple(
                "If the next step isn’t clear, it feels heavy — clarity creates momentum.",
                listOf(
                    "Write the next 2 steps only (not the whole plan).",
                    "Open the assignment and find the rubric / requirements first."
                ),
                StudyMode.POMODORO_25_5
            )

            RootCause.PERFECTIONISM -> Triple(
                "Perfectionism delays starting because the first attempt won’t be perfect.",
                listOf(
                    "Make a deliberately ‘bad first draft’ for 10 minutes.",
                    "Set a timer: quantity first, quality later."
                ),
                StudyMode.POMODORO_25_5
            )

            RootCause.AVOIDANCE -> Triple(
                "Avoidance protects you from discomfort — starting small lowers the threat.",
                listOf(
                    "Pick the easiest 2-minute step and do only that.",
                    "Name the fear in one sentence, then start anyway."
                ),
                StudyMode.QUICKSTART_5_1
            )

            RootCause.OVERLOAD -> Triple(
                "Overload makes choosing hard, so you stall. Simplify the choice.",
                listOf(
                    "Do a 60-second brain dump of everything on your mind.",
                    "Circle ONE item you can do now and start it."
                ),
                StudyMode.POMODORO_25_5
            )

            RootCause.DISTRACTION -> Triple(
                "Distractions win when the environment is set up for interruptions.",
                listOf(
                    "Put your phone out of reach for 5 minutes and start.",
                    "Close extra tabs; keep only the one you need."
                ),
                StudyMode.QUICKSTART_5_1
            )

            RootCause.LOW_MOTIVATION -> Triple(
                "Motivation usually follows action — not the other way around.",
                listOf(
                    "Start with the smallest possible win (first sentence / first problem).",
                    "Tell yourself: ‘I only need to start, not finish.’"
                ),
                StudyMode.QUICKSTART_5_1
            )

            RootCause.TIME_PRESSURE -> Triple(
                "Time pressure can cause freezing. Short sprints help.",
                listOf(
                    "Do one Pomodoro and only aim for progress, not perfection.",
                    "List the 3 most important tasks for the deadline."
                ),
                StudyMode.POMODORO_25_5
            )

            RootCause.OTHER -> Triple(
                "Pick what feels closest and we’ll give you a quick next step.",
                listOf(
                    "Choose one tiny step and do 2 minutes of it.",
                    "Start Quick Start — momentum first."
                ),
                StudyMode.QUICKSTART_5_1
            )
        }

        return CoachResult(
            primary = primary,
            secondary = secondary,
            confidence = confidence,
            explanation = explain,
            microActions = actions,
            recommendedMode = mode
        )
    }

    private fun fallbackResult(): CoachResult = CoachResult(
        primary = RootCause.OTHER,
        confidence = 0.10f,
        explanation = "Type what’s going on (even messy). I’ll try to map it to a common root cause.",
        microActions = listOf(
            "If you’re unsure, tap one of the options below.",
            "Start Quick Start for 5 minutes — just begin."
        ),
        recommendedMode = StudyMode.QUICKSTART_5_1
    )

    private fun uncertainResult(): CoachResult = CoachResult(
        primary = RootCause.OTHER,
        confidence = 0.25f,
        explanation = "I’m not totally sure from that — pick what fits best and I’ll tailor the next step.",
        microActions = listOf(
            "Choose a category chip below.",
            "Then do a 2–5 minute start (momentum first)."
        ),
        recommendedMode = StudyMode.QUICKSTART_5_1
    )

    private fun normalize(s: String): String {
        return s.lowercase()
            .replace(Regex("[^a-z0-9\\s']"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun containsKeywordFuzzy(words: List<String>, keyword: String): Boolean {
        val k = keyword.lowercase()

        // phrases like "too much"
        if (k.contains(" ")) {
            val joined = words.joinToString(" ")
            return joined.contains(k)
        }

        for (w in words) {
            if (w == k) return true

            if (w.length in 3..10 && k.length in 3..10) {
                val d = levenshtein(w, k)
                if (d <= 1) return true
                if (k.length >= 6 && d <= 2) return true
            }
        }
        return false
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}

@Composable
fun ProcrastinationCoachCard(
    modifier: Modifier = Modifier,
    title: String = "Why are you stuck?",
    onRecommendedMode: ((StudyMode) -> Unit)? = null
) {
    var input by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(true) }
    var result by remember { mutableStateOf<CoachResult?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Open")
                }
            }

            if (expanded) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Ex: I'm tired and keep scrolling…") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { result = RuleBasedCoachAnalyzer.analyze(input) }) {
                        Text("Analyze")
                    }
                    OutlinedButton(onClick = {
                        input = ""
                        result = null
                    }) {
                        Text("Clear")
                    }
                }

                val r = result
                if (r != null) {
                    HorizontalDivider()

                    Text(
                        "Likely root cause: ${r.primary.title}" + (r.secondary?.let { " + ${it.title}" } ?: ""),
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(r.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Text("Try this:", fontWeight = FontWeight.SemiBold)
                    r.microActions.take(2).forEach { action ->
                        Text("• $action")
                    }

                    if (r.recommendedMode != null && onRecommendedMode != null) {
                        OutlinedButton(onClick = { onRecommendedMode(r.recommendedMode) }) {
                            Text("Start recommended mode: ${r.recommendedMode.title}")
                        }
                    }

                    if (r.confidence < 0.35f || r.primary == RootCause.OTHER) {
                        Text("Pick what fits best:", fontWeight = FontWeight.SemiBold)

                        FlowRowChips(
                            options = listOf(
                                RootCause.FATIGUE,
                                RootCause.LACK_OF_CLARITY,
                                RootCause.OVERLOAD,
                                RootCause.PERFECTIONISM,
                                RootCause.AVOIDANCE,
                                RootCause.DISTRACTION,
                                RootCause.LOW_MOTIVATION,
                                RootCause.TIME_PRESSURE
                            ),
                            onPick = { picked ->
                                result = RuleBasedCoachAnalyzer.buildResult(
                                    primary = picked,
                                    secondary = null,
                                    confidence = 0.70f
                                )
                            }
                        )
                    }
                }
            } else {
                Text(
                    "Tap Open to type a reason and get a quick plan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FlowRowChips(
    options: List<RootCause>,
    onPick: (RootCause) -> Unit
) {
    val chunks = options.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunks.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { cause ->
                    AssistChip(
                        onClick = { onPick(cause) },
                        label = { Text(cause.title) }
                    )
                }
            }
        }
    }
}

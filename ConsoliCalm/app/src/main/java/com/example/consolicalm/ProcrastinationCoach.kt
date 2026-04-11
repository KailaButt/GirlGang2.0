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
    val confidence: Float,
    val explanation: String,
    val microActions: List<String>,
    val recommendedMode: StudyMode? = null
)

object RuleBasedCoachAnalyzer {

    private val phraseRules: Map<RootCause, List<String>> = mapOf(
        RootCause.LACK_OF_CLARITY to listOf(
            "don't know where to start", "dont know where to start",
            "no idea where to start", "not sure where to start",
            "i'm confused", "im confused", "confused",
            "unclear", "too many steps", "i'm lost", "im lost", "lost",
            "this makes no sense", "not getting it", "i don't get it", "i dont get it",
            "can't figure it out", "cant figure it out", "don't know what to do", "dont know what to do",
            "stuck on this", "stuck on a problem", "not clicking", "nothing is clicking"
        ),

        RootCause.PERFECTIONISM to listOf(
            "has to be perfect", "needs to be perfect",
            "not good enough", "i'll mess up", "ill mess up",
            "fear of failing", "scared to fail", "afraid to fail",
            "what if it's bad", "what if its bad",
            "what if it's wrong", "what if its wrong",
            "afraid of doing it wrong", "afraid to do it wrong",
            "waiting for the perfect time", "waiting until i feel ready",
            "can't start until it's perfect", "cant start until its perfect",
            "i keep overthinking", "overthinking everything"
        ),

        RootCause.AVOIDANCE to listOf(
            "i'm anxious", "im anxious", "anxious",
            "i'm stressed", "im stressed", "stressed",
            "i'm dreading", "im dreading", "dreading it",
            "i keep avoiding", "avoid it", "avoiding it",
            "i feel overwhelmed", "panic", "panicking", "nervous",
            "scared to start", "afraid to start",
            "can't make myself start", "cant make myself start",
            "can't bring myself to do it", "cant bring myself to do it",
            "putting it off", "procrastinating", "stalling", "delaying"
        ),

        RootCause.DISTRACTION to listOf(
            "can't stop scrolling", "cant stop scrolling",
            "keep scrolling", "stuck on my phone", "distracted",
            "social media", "notifications",
            "keep checking my phone", "checking my phone",
            "too many distractions", "i keep zoning out", "zoning out",
            "can't focus", "cant focus", "cannot focus",
            "can't lock in", "cant lock in",
            "my mind keeps wandering", "mind keeps wandering",
            "all over the place", "my brain is everywhere"
        ),

        RootCause.FATIGUE to listOf(
            "i'm tired", "im tired", "so tired", "really tired",
            "no energy", "low energy",
            "burnt out", "burned out", "exhausted", "sleepy",
            "drained", "worn out", "fatigued",
            "half asleep", "mentally tired", "physically tired",
            "too tired to think", "dead tired", "groggy", "sluggish"
        ),

        RootCause.OVERLOAD to listOf(
            "too much", "so much to do", "my brain is full", "mental overload",
            "i have a lot", "too many things", "too much to do",
            "too many assignments", "too much homework", "too much work",
            "everything feels like too much", "i can't do all this", "i cant do all this",
            "swamped", "buried", "drowning", "falling behind on everything",
            "behind on everything", "this is a lot"
        ),

        RootCause.LOW_MOTIVATION to listOf(
            "i don't care", "i dont care", "boring", "not motivated",
            "no motivation", "i hate this",
            "don't feel like it", "dont feel like it",
            "don't want to do it", "dont want to do it",
            "zero motivation", "not in the mood",
            "this is boring", "so boring", "too boring",
            "meh", "feeling lazy", "unmotivated"
        ),

        RootCause.TIME_PRESSURE to listOf(
            "due soon", "running out of time", "deadline", "last minute",
            "i'm behind", "im behind",
            "short on time", "no time", "don't have time", "dont have time",
            "not enough time", "rushing", "in a rush",
            "only have a few minutes", "few minutes",
            "class soon", "work soon", "running late", "packed day"
        )
    )

    private val keywordRules: Map<RootCause, List<String>> = mapOf(
        RootCause.FATIGUE to listOf(
            "tired", "sleepy", "exhausted", "drained", "burnt", "burned",
            "fatigue", "groggy", "sluggish", "worn"
        ),
        RootCause.OVERLOAD to listOf(
            "overwhelmed", "overload", "chaos", "swamped", "buried", "drowning", "pressure"
        ),
        RootCause.AVOIDANCE to listOf(
            "anxious", "anxiety", "scared", "avoid", "dreading", "panic",
            "nervous", "procrastinating", "stalling", "delaying"
        ),
        RootCause.PERFECTIONISM to listOf(
            "perfect", "perfection", "fail", "failing", "mistake", "wrong", "overthinking"
        ),
        RootCause.LACK_OF_CLARITY to listOf(
            "confused", "unclear", "lost", "start", "steps", "stuck", "figure", "understand"
        ),
        RootCause.DISTRACTION to listOf(
            "scroll", "phone", "tiktok", "instagram", "youtube",
            "distracted", "notifications", "focus", "zoning", "wandering"
        ),
        RootCause.LOW_MOTIVATION to listOf(
            "boring", "lazy", "motivation", "care", "hate", "meh", "unmotivated"
        ),
        RootCause.TIME_PRESSURE to listOf(
            "deadline", "due", "soon", "late", "behind", "rush", "time", "minutes"
        )
    )

    fun analyze(raw: String): CoachResult {
        val cleaned = normalize(raw)
        if (cleaned.isBlank()) return fallbackResult()

        val scores = mutableMapOf<RootCause, Int>()
        RootCause.values().forEach { scores[it] = 0 }

        for ((cause, phrases) in phraseRules) {
            for (p in phrases) {
                if (cleaned.contains(p)) {
                    scores[cause] = (scores[cause] ?: 0) + 4
                }
            }
        }

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
            topScore >= 8 -> 0.95f
            topScore >= 6 -> 0.85f
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

    fun buildResult(
        primary: RootCause,
        secondary: RootCause?,
        confidence: Float = 0.70f
    ): CoachResult {
        val (explain, actions, mode) = when (primary) {
            RootCause.FATIGUE -> Triple(
                "Low energy makes your brain choose easy things over demanding study tasks.",
                listOf(
                    "Do Quick Start for 5 minutes only — you can stop after.",
                    "Get water, sit up, and begin with one tiny step."
                ),
                StudyMode.QUICKSTART_5_1
            )

            RootCause.LACK_OF_CLARITY -> Triple(
                "When the next step is unclear, your brain treats the task as heavier than it is.",
                listOf(
                    "Write the next 2 steps only — not the whole plan.",
                    "Open the assignment and find the rubric, prompt, or first question."
                ),
                StudyMode.POMODORO_25_5
            )

            RootCause.PERFECTIONISM -> Triple(
                "Perfectionism can delay starting because the first version won’t feel good enough.",
                listOf(
                    "Make a deliberately messy first draft for 10 minutes.",
                    "Aim for progress first, polishing later."
                ),
                StudyMode.QUICKSTART_5_1
            )

            RootCause.AVOIDANCE -> Triple(
                "Avoidance usually means the task feels threatening, stressful, or emotionally heavy.",
                listOf(
                    "Pick the easiest 2-minute step and do only that.",
                    "Name what feels scary, then shrink the task until it feels safe to begin."
                ),
                StudyMode.QUICKSTART_5_1
            )

            RootCause.OVERLOAD -> Triple(
                "When everything feels important at once, your brain can freeze instead of choosing.",
                listOf(
                    "Do a 60-second brain dump of everything on your mind.",
                    "Circle one thing you can do right now and ignore the rest for one round."
                ),
                StudyMode.QUICKSTART_5_1
            )

            RootCause.DISTRACTION -> Triple(
                "Distractions win more easily when your attention is already split.",
                listOf(
                    "Put your phone out of reach for 5 minutes.",
                    "Close extra tabs and leave only the one you need."
                ),
                StudyMode.POMODORO_25_5
            )

            RootCause.LOW_MOTIVATION -> Triple(
                "Motivation usually shows up after movement, not before it.",
                listOf(
                    "Start with the smallest possible win: one sentence, one problem, one paragraph.",
                    "Tell yourself: I only need to start, not finish."
                ),
                StudyMode.QUICKSTART_5_1
            )

            RootCause.TIME_PRESSURE -> Triple(
                "Time pressure can cause freezing, so short structured sprints work better than waiting.",
                listOf(
                    "Do one focused round and only aim for progress.",
                    "List the 3 most important things for the deadline."
                ),
                StudyMode.POMODORO_25_5
            )

            RootCause.OTHER -> Triple(
                "Even if the cause is unclear, a tiny start can still break the stuck feeling.",
                listOf(
                    "Choose one tiny step and do 2 minutes of it.",
                    "Start Quick Start and let momentum build."
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
        explanation = "Type what’s going on, even if it’s messy. I’ll try to match it to a common reason people get stuck.",
        microActions = listOf(
            "If you’re unsure, tap one of the options below.",
            "Or start Quick Start for 5 minutes and build momentum first."
        ),
        recommendedMode = StudyMode.QUICKSTART_5_1
    )

    private fun uncertainResult(): CoachResult = CoachResult(
        primary = RootCause.OTHER,
        confidence = 0.25f,
        explanation = "I’m not totally sure from that wording, but I can still help you start.",
        microActions = listOf(
            "Pick the category that feels closest.",
            "Then do a 2–5 minute start so the task feels less heavy."
        ),
        recommendedMode = StudyMode.QUICKSTART_5_1
    )

    private fun normalize(s: String): String {
        return s.lowercase()
            .replace("’", "'")
            .replace(Regex("[^a-z0-9\\s']"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun containsKeywordFuzzy(words: List<String>, keyword: String): Boolean {
        val k = keyword.lowercase()

        if (k.contains(" ")) {
            val joined = words.joinToString(" ")
            return joined.contains(k)
        }

        for (w in words) {
            if (w == k) return true

            if (w.length in 3..12 && k.length in 3..12) {
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Text(title, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Open")
                }
            }

            if (expanded) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Ex: I’m tired, overwhelmed, and keep checking my phone…") },
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
                        text = "Likely root cause: ${r.primary.title}" +
                                (r.secondary?.let { " + ${it.title}" } ?: ""),
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = r.explanation,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Try this:",
                        fontWeight = FontWeight.SemiBold
                    )

                    r.microActions.take(2).forEach { action ->
                        Text("• $action")
                    }

                    if (r.recommendedMode != null && onRecommendedMode != null) {
                        OutlinedButton(
                            onClick = { onRecommendedMode(r.recommendedMode) }
                        ) {
                            Text("Start recommended mode: ${r.recommendedMode.title}")
                        }
                    }

                    if (r.confidence < 0.35f || r.primary == RootCause.OTHER) {
                        Text(
                            text = "Pick what fits best:",
                            fontWeight = FontWeight.SemiBold
                        )

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
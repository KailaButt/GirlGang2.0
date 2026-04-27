package com.example.consolicalm

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.URL
import java.util.Locale

// ── Colors ────────────────────────────────────────────────────────────────────
private val TS_NeonYellow = Color(0xFFFFE600)
private val TS_NeonPink   = Color(0xFFFF2D78)
private val TS_NeonBlue   = Color(0xFF00C8FF)
private val TS_NeonGreen  = Color(0xFF39FF14)
private val TS_NeonRed    = Color(0xFFFF3B3B)
private val TS_NightBlack = Color(0xFF0A0A0F)
private val TS_DarkPanel  = Color(0xFF12121C)
private val TS_SubPanel   = Color(0xFF1C1C2E)

// ── Challenge result state ────────────────────────────────────────────────────
private enum class TS_AnswerState { IDLE, CHECKING, CORRECT, WRONG }
private enum class TS_GamePhase  { INTRO, PLAYING, RESULT }

private data class TS_WordChallenge(
    val label: String,
    val hint: String,
    val placeholder: String,
    val checkType: String   // "synonym" | "sentence" | "rhyme" | "definition"
)

private val TS_CHALLENGES = listOf(
    TS_WordChallenge("1. Write a SYNONYM",             "A word that means the same thing",          "e.g. happy → joyful",                    "synonym"),
    TS_WordChallenge("2. Use it in a SENTENCE",        "Make a full sentence using the word",        "e.g. The cat was very happy today.",      "sentence"),
    TS_WordChallenge("3. Write a RHYME",               "A word that sounds similar at the end",      "e.g. cat → bat",                         "rhyme"),
    TS_WordChallenge("4. DEFINE it in your own words", "Explain what it means without a dictionary", "e.g. happy means feeling really good",    "definition")
)

// ══════════════════════════════════════════════════════════════════════════════
//  VALIDATION  using Datamuse API (free, no key)
//  datamuse.com/api
// ══════════════════════════════════════════════════════════════════════════════

private suspend fun ts_fetchRandomWord(): String = withContext(Dispatchers.IO) {
    try {
        val json = URL("https://random-word-api.herokuapp.com/word?number=1").readText()
        JSONArray(json).getString(0).replaceFirstChar { it.uppercase(Locale.getDefault()) }
    } catch (e: Exception) {
        listOf("Serenity", "Bloom", "Zenith", "Radiant", "Tranquil").random()
    }
}

// Returns true if userAnswer is a valid synonym of targetWord
private suspend fun ts_checkSynonym(targetWord: String, userAnswer: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val query = targetWord.lowercase().trim()
            val answer = userAnswer.lowercase().trim()
            if (answer.isBlank() || answer == query) return@withContext false
            // Ask Datamuse for words with similar meaning to targetWord
            val url = "https://api.datamuse.com/words?ml=${encode(query)}&max=50"
            val json = JSONArray(URL(url).readText())
            for (i in 0 until json.length()) {
                val word = json.getJSONObject(i).getString("word").lowercase()
                if (word == answer) return@withContext true
            }
            false
        } catch (e: Exception) {
            // fallback: accept anything non-blank if API fails
            userAnswer.trim().length >= 2
        }
    }

// Returns true if userAnswer rhymes with targetWord
private suspend fun ts_checkRhyme(targetWord: String, userAnswer: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val query  = targetWord.lowercase().trim()
            val answer = userAnswer.lowercase().trim()
            if (answer.isBlank() || answer == query) return@withContext false
            val url = "https://api.datamuse.com/words?rel_rhy=${encode(query)}&max=100"
            val json = JSONArray(URL(url).readText())
            for (i in 0 until json.length()) {
                val word = json.getJSONObject(i).getString("word").lowercase()
                if (word == answer) return@withContext true
            }
            // Also check near-rhymes
            val url2 = "https://api.datamuse.com/words?rel_nry=${encode(query)}&max=100"
            val json2 = JSONArray(URL(url2).readText())
            for (i in 0 until json2.length()) {
                val word = json2.getJSONObject(i).getString("word").lowercase()
                if (word == answer) return@withContext true
            }
            false
        } catch (e: Exception) {
            userAnswer.trim().length >= 2
        }
    }

// Sentence: must contain the word and be >= 5 words
private fun ts_checkSentence(targetWord: String, userAnswer: String): Boolean {
    val answer = userAnswer.trim()
    val wordLower = targetWord.lowercase()
    val answerLower = answer.lowercase()
    val containsWord = answerLower.contains(wordLower)
    val wordCount = answer.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    return containsWord && wordCount >= 5
}

// Definition: must be >= 4 words and NOT just repeat the word
private fun ts_checkDefinition(targetWord: String, userAnswer: String): Boolean {
    val answer = userAnswer.trim()
    val wordLower = targetWord.lowercase()
    val wordCount = answer.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    // Must have at least 4 words, and not just be the word repeated
    return wordCount >= 4 && answer.lowercase() != wordLower
}

private fun encode(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

// Master checker — dispatches to the right check per challenge type
private suspend fun ts_checkAnswer(
    checkType: String,
    targetWord: String,
    userAnswer: String
): Boolean = when (checkType) {
    "synonym"    -> ts_checkSynonym(targetWord, userAnswer)
    "rhyme"      -> ts_checkRhyme(targetWord, userAnswer)
    "sentence"   -> withContext(Dispatchers.Default) { ts_checkSentence(targetWord, userAnswer) }
    "definition" -> withContext(Dispatchers.Default) { ts_checkDefinition(targetWord, userAnswer) }
    else         -> userAnswer.trim().isNotBlank()
}

// ─── New helper functions to fetch example answers ─────────────────────────────
private suspend fun ts_fetchExampleSynonym(word: String): String = withContext(Dispatchers.IO) {
    try {
        val url = "https://api.datamuse.com/words?ml=${encode(word)}&max=1"
        val json = JSONArray(URL(url).readText())
        if (json.length() > 0) json.getJSONObject(0).getString("word") else ""
    } catch (e: Exception) {
        "" // fallback empty
    }
}

private suspend fun ts_fetchExampleRhyme(word: String): String = withContext(Dispatchers.IO) {
    try {
        val url = "https://api.datamuse.com/words?rel_rhy=${encode(word)}&max=1"
        val json = JSONArray(URL(url).readText())
        if (json.length() > 0) json.getJSONObject(0).getString("word") else ""
    } catch (e: Exception) {
        ""
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  PUBLIC ENTRY POINT  — called from GamesScreen.kt
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun TimesSquareWordChallengeScreen(
    onBack: () -> Unit = {},
    onEarnPoints: (Int) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TS_NightBlack)
    ) {
        TS_GameHost(onBack = onBack, onEarnPoints = onEarnPoints)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// GAME HOST
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TS_GameHost(onBack: () -> Unit, onEarnPoints: (Int) -> Unit) {
    var word         by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(true) }
    var phase        by remember { mutableStateOf(TS_GamePhase.INTRO) }
    var timeLeft     by remember { mutableStateOf(90) }
    var score        by remember { mutableStateOf(0) }
    var currentRound by remember { mutableStateOf(1) }

    // ✅ NEW: Example correct answers for each challenge
    var exampleSynonym by remember { mutableStateOf("") }
    var exampleRhyme   by remember { mutableStateOf("") }

    val answers      = remember { mutableStateListOf("", "", "", "") }
    // IDLE = not submitted, CHECKING = API in progress, CORRECT = ✅, WRONG = ❌
    val answerStates = remember { mutableStateListOf(
        TS_AnswerState.IDLE, TS_AnswerState.IDLE, TS_AnswerState.IDLE, TS_AnswerState.IDLE
    )}

    val focusManager = LocalFocusManager.current
    val scope        = rememberCoroutineScope()

    val infiniteAnim = rememberInfiniteTransition(label = "ts_flicker")
    val flickerAlpha by infiniteAnim.animateFloat(
        initialValue  = 0.80f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(280, easing = LinearEasing), RepeatMode.Reverse),
        label         = "ts_alpha"
    )

    // Load word and example answers each round
    LaunchedEffect(currentRound) {
        isLoading = true
        answers.replaceAll { "" }
        answerStates.replaceAll { TS_AnswerState.IDLE }
        timeLeft = 90
        word = ts_fetchRandomWord()

        // ✅ Fetch example synonym and rhyme in parallel
        val synonymJob = scope.async(Dispatchers.IO) {
            ts_fetchExampleSynonym(word)
        }
        val rhymeJob = scope.async(Dispatchers.IO) {
            ts_fetchExampleRhyme(word)
        }
        exampleSynonym = synonymJob.await()
        exampleRhyme   = rhymeJob.await()

        isLoading = false
    }

    // Countdown timer
    LaunchedEffect(phase) {
        if (phase == TS_GamePhase.PLAYING) {
            while (timeLeft > 0 && phase == TS_GamePhase.PLAYING) {
                delay(1000L)
                timeLeft--
            }
            if (timeLeft == 0 && phase == TS_GamePhase.PLAYING) phase = TS_GamePhase.RESULT
        }
    }

    // Called when player hits SUBMIT on a card
    fun submitAnswer(index: Int) {
        if (answers[index].isBlank()) return
        if (answerStates[index] == TS_AnswerState.CHECKING) return
        if (answerStates[index] == TS_AnswerState.CORRECT) return

        answerStates[index] = TS_AnswerState.CHECKING
        focusManager.clearFocus()

        scope.launch {
            val challenge = TS_CHALLENGES[index]
            val isCorrect = ts_checkAnswer(challenge.checkType, word, answers[index])

            answerStates[index] = if (isCorrect) TS_AnswerState.CORRECT else TS_AnswerState.WRONG

            if (isCorrect) {
                // Points: faster = more
                val pts = if (timeLeft > 60) 15 else if (timeLeft > 30) 10 else 5
                score += pts
            }

            // If all 4 have been decided (correct OR wrong) → go to results
            if (answerStates.all { it == TS_AnswerState.CORRECT || it == TS_AnswerState.WRONG }) {
                delay(800)
                onEarnPoints(score)
                phase = TS_GamePhase.RESULT
            }
        }
    }

    when (phase) {
        TS_GamePhase.INTRO   -> TS_IntroScreen(isLoading, word, flickerAlpha,
            onStart = { phase = TS_GamePhase.PLAYING }, onBack)
        TS_GamePhase.PLAYING -> TS_PlayingScreen(
            word, timeLeft, score, answers, answerStates,
            flickerAlpha, ::submitAnswer,
            onGiveUp = { onEarnPoints(score); phase = TS_GamePhase.RESULT },
            exampleSynonym = exampleSynonym,   // ✅ Pass examples
            exampleRhyme   = exampleRhyme
        )
        TS_GamePhase.RESULT  -> TS_ResultScreen(
            word, score, answers, answerStates, currentRound,
            onNextRound = { currentRound++; phase = TS_GamePhase.INTRO },
            onBack = onBack,
            exampleSynonym = exampleSynonym,   // ✅ Pass examples
            exampleRhyme   = exampleRhyme
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// INTRO SCREEN
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TS_IntroScreen(
    isLoading: Boolean, word: String, flickerAlpha: Float,
    onStart: () -> Unit, onBack: () -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) {
                Text("← Back", color = Color.White.copy(0.5f), fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("TIMES SQUARE",   color = TS_NeonYellow, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp)
        Text("WORD CHALLENGE", color = TS_NeonPink,   fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
        Spacer(Modifier.height(28.dp))
        TS_Billboard(word = if (isLoading) "..." else word, alpha = flickerAlpha)
        Spacer(Modifier.height(28.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = TS_SubPanel, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("HOW TO PLAY", color = TS_NeonBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 3.sp)
                TS_CHALLENGES.forEach { c ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("▸", color = TS_NeonYellow, fontSize = 14.sp)
                        Column {
                            Text(c.label, color = Color.White,            fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(c.hint,  color = Color.White.copy(0.5f), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("⏱  90 seconds — correct answers earn points!", color = TS_NeonGreen, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                Text("✅ Right answer = points  ❌ Wrong = no points but keep going!", color = Color.White.copy(0.4f), fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick  = onStart, enabled = !isLoading,
            colors   = ButtonDefaults.buttonColors(containerColor = TS_NeonPink),
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text(
                if (isLoading) "Loading word…" else "START CHALLENGE",
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 2.sp
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PLAYING SCREEN
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TS_PlayingScreen(
    word: String, timeLeft: Int, score: Int,
    answers: MutableList<String>,
    answerStates: MutableList<TS_AnswerState>,
    flickerAlpha: Float,
    onSubmit: (Int) -> Unit,
    onGiveUp: () -> Unit,
    exampleSynonym: String,   // ✅ New parameters
    exampleRhyme: String
) {
    val timerColor = if (timeLeft > 60) TS_NeonGreen else if (timeLeft > 30) TS_NeonYellow else TS_NeonPink

    Column(
        modifier            = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Timer + Score bar
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = TS_DarkPanel) {
                Text("⏱ ${timeLeft}s", color = timerColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
            Surface(shape = RoundedCornerShape(8.dp), color = TS_DarkPanel) {
                Text("★ $score pts", color = TS_NeonYellow, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }

        TS_Billboard(word = word, alpha = flickerAlpha)

        TS_CHALLENGES.forEachIndexed { i, c ->
            TS_ChallengeCard(
                challenge       = c,
                value           = answers[i],
                answerState     = answerStates[i],
                onValueChange   = { answers[i] = it },
                onSubmit        = { onSubmit(i) },
                targetWord      = word,             // ✅ Pass target word for sentence/definition examples
                exampleSynonym  = exampleSynonym,   // ✅ Pass examples
                exampleRhyme    = exampleRhyme
            )
        }

        TextButton(onClick = onGiveUp) {
            Text("See Results", color = Color.White.copy(0.4f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CHALLENGE CARD  — shows ✅ / ❌ / checking spinner
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TS_ChallengeCard(
    challenge: TS_WordChallenge,
    value: String,
    answerState: TS_AnswerState,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    targetWord: String,        // ✅ New: needed for sentence/definition examples
    exampleSynonym: String,    // ✅ New
    exampleRhyme: String       // ✅ New
) {
    val focusRequester = remember { FocusRequester() }
    val isLocked = answerState == TS_AnswerState.CORRECT || answerState == TS_AnswerState.CHECKING

    // Pulse animation for the result badge
    val badgeScale by animateFloatAsState(
        targetValue   = if (answerState == TS_AnswerState.CORRECT || answerState == TS_AnswerState.WRONG) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "badge_scale"
    )

    val borderColor = when (answerState) {
        TS_AnswerState.CORRECT  -> TS_NeonGreen
        TS_AnswerState.WRONG    -> TS_NeonRed
        TS_AnswerState.CHECKING -> TS_NeonBlue
        TS_AnswerState.IDLE     -> TS_SubPanel
    }

    // ✅ Determine the example answer to show when wrong
    val exampleAnswer = when (challenge.checkType) {
        "synonym"    -> exampleSynonym.ifEmpty { "e.g., joyful" }
        "rhyme"      -> exampleRhyme.ifEmpty { "e.g., bat" }
        "sentence"   -> "The weather is very ${targetWord.lowercase()} today."
        "definition" -> "${targetWord} means something pleasant or positive."
        else         -> ""
    }

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = TS_DarkPanel,
        border   = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Label row + result badge
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(challenge.label, color = TS_NeonBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                when (answerState) {
                    TS_AnswerState.CORRECT -> Text(
                        "✅ CORRECT  +pts",
                        color = TS_NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.scale(badgeScale)
                    )
                    TS_AnswerState.WRONG -> Text(
                        "❌ WRONG",
                        color = TS_NeonRed, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.scale(badgeScale)
                    )
                    TS_AnswerState.CHECKING -> Text(
                        "⏳ Checking…",
                        color = TS_NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                    TS_AnswerState.IDLE -> { /* nothing */ }
                }
            }

            Text(challenge.hint, color = Color.White.copy(0.45f), fontSize = 12.sp)

            // Wrong answer tip + example
            if (answerState == TS_AnswerState.WRONG) {
                Surface(shape = RoundedCornerShape(8.dp), color = TS_NeonRed.copy(0.1f)) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            when (challenge.checkType) {
                                "synonym"    -> "Try another word with the same meaning"
                                "rhyme"      -> "The ending sound must match the word"
                                "sentence"   -> "Use the word in a sentence of 5+ words"
                                "definition" -> "Explain the meaning in 4+ words"
                                else         -> "Try again!"
                            },
                            color    = TS_NeonRed.copy(0.85f),
                            fontSize = 11.sp
                        )
                        if (exampleAnswer.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "💡 Example: $exampleAnswer",
                                color = Color.White.copy(0.7f),
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value           = value,
                onValueChange   = onValueChange,
                placeholder     = { Text(challenge.placeholder, color = Color.White.copy(0.3f), fontSize = 13.sp) },
                enabled         = !isLocked,
                singleLine      = false,
                maxLines        = 3,
                modifier        = Modifier.fillMaxWidth().focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White,
                    disabledTextColor       = when (answerState) {
                        TS_AnswerState.CORRECT -> TS_NeonGreen
                        TS_AnswerState.WRONG   -> TS_NeonRed
                        else                   -> Color.White.copy(0.5f)
                    },
                    focusedBorderColor      = TS_NeonPink,
                    unfocusedBorderColor    = Color.White.copy(0.2f),
                    disabledBorderColor     = borderColor.copy(0.3f),
                    focusedContainerColor   = TS_SubPanel,
                    unfocusedContainerColor = TS_SubPanel,
                    disabledContainerColor  = TS_SubPanel
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Show SUBMIT or TRY AGAIN depending on state
            when (answerState) {
                TS_AnswerState.IDLE, TS_AnswerState.WRONG -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick  = onSubmit,
                            enabled  = value.isNotBlank() && answerState != TS_AnswerState.CHECKING,
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = if (answerState == TS_AnswerState.WRONG) TS_NeonRed else TS_NeonPink,
                                disabledContainerColor = TS_SubPanel
                            ),
                            shape    = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                if (answerState == TS_AnswerState.WRONG) "TRY AGAIN" else "SUBMIT",
                                fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp
                            )
                        }
                    }
                }
                TS_AnswerState.CHECKING -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color    = TS_NeonBlue,
                        trackColor = TS_SubPanel
                    )
                }
                TS_AnswerState.CORRECT -> { /* nothing, locked */ }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RESULT SCREEN
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TS_ResultScreen(
    word: String, score: Int,
    answers: List<String>,
    answerStates: List<TS_AnswerState>,
    currentRound: Int,
    onNextRound: () -> Unit,
    onBack: () -> Unit,
    exampleSynonym: String,   // ✅ New
    exampleRhyme: String      // ✅ New
) {
    val correctCount = answerStates.count { it == TS_AnswerState.CORRECT }
    val msg = when {
        correctCount == 4 -> "🔥 PERFECT! Times Square would be proud!"
        correctCount >= 2 -> "💪 Good round! Keep pushing!"
        correctCount == 1 -> "✨ 1 correct — you'll get more next time!"
        else              -> "😤 Tough word! Try the next one!"
    }

    Column(
        modifier            = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("ROUND $currentRound COMPLETE", color = TS_NeonYellow, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
        Text(word.uppercase(), color = TS_NeonPink, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)

        // Score circle
        Surface(shape = RoundedCornerShape(100.dp), color = TS_DarkPanel, border = BorderStroke(3.dp, TS_NeonGreen), modifier = Modifier.size(120.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score", color = TS_NeonGreen, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    Text("pts",   color = Color.White.copy(0.5f), fontSize = 12.sp)
                }
            }
        }

        Text("$correctCount / 4 correct", color = Color.White.copy(0.7f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

        // Answer review
        Surface(shape = RoundedCornerShape(16.dp), color = TS_SubPanel, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("RESULTS", color = TS_NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                TS_CHALLENGES.forEachIndexed { i, c ->
                    val state = answerStates[i]
                    val isCorrect = state == TS_AnswerState.CORRECT
                    val exampleForType = when (c.checkType) {
                        "synonym"    -> exampleSynonym.ifEmpty { "joyful" }
                        "rhyme"      -> exampleRhyme.ifEmpty { "bird" }
                        "sentence"   -> "The ${word.lowercase()} weather is lovely."
                        "definition" -> "${word} means a feeling of great happiness."
                        else         -> ""
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (isCorrect) "✅" else "❌",
                            fontSize = 18.sp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(c.label, color = Color.White.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            // Show user answer
                            Text(
                                if (answers[i].isBlank()) "(no answer given)" else answers[i],
                                color     = if (isCorrect) TS_NeonGreen else TS_NeonRed.copy(0.85f),
                                fontSize  = 13.sp,
                                fontStyle = if (answers[i].isBlank()) FontStyle.Italic else FontStyle.Normal,
                                fontWeight = if (isCorrect) FontWeight.Medium else FontWeight.Normal
                            )
                            // Show expected answer if user got it wrong
                            if (!isCorrect && exampleForType.isNotBlank()) {
                                Text(
                                    "✔ Expected: $exampleForType",
                                    color = Color.White.copy(0.6f),
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                            if (isCorrect) {
                                Text("+ points earned!", color = TS_NeonGreen.copy(0.7f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Text(msg, color = TS_NeonYellow, fontSize = 14.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)

        Button(
            onClick  = onNextRound,
            colors   = ButtonDefaults.buttonColors(containerColor = TS_NeonGreen),
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("NEXT WORD →", color = TS_NightBlack, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 2.sp)
        }
        OutlinedButton(
            onClick  = onBack,
            border   = BorderStroke(1.dp, Color.White.copy(0.3f)),
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Back to Games", color = Color.White.copy(0.6f), fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// BILLBOARD
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TS_Billboard(word: String, alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1A0030), TS_NightBlack)))
            .border(BorderStroke(2.dp, Brush.horizontalGradient(listOf(TS_NeonPink, TS_NeonYellow, TS_NeonBlue))), RoundedCornerShape(20.dp))
            .padding(vertical = 28.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("★  WORD OF THE CHALLENGE  ★", color = TS_NeonYellow.copy(alpha = alpha), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                word.uppercase(), color = Color.White.copy(alpha = alpha), fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                style = LocalTextStyle.current.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(color = TS_NeonPink.copy(0.8f), blurRadius = 24f)
                )
            )
            Spacer(Modifier.height(8.dp))
            Text("Write it 4 different ways!", color = TS_NeonBlue.copy(0.8f), fontSize = 12.sp, fontStyle = FontStyle.Italic)
        }
    }
}
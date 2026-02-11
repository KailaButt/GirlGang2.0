package com.example.consolicalm

import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class MeditationResource(
    val title: String,
    val description: String,
    val durationLabel: String,
    val url: String
)

data class InAppPractice(
    val title: String,
    val summary: String,
    val timeLabel: String,
    val steps: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open links.", Toast.LENGTH_SHORT).show()
        }
    }

    val breathing = listOf(
        MeditationResource(
            title = "Breathing for stress (NHS)",
            description = "Simple guided breathing you can do anywhere.",
            durationLabel = "~5 min",
            url = "https://www.nhs.uk/mental-health/self-help/guides-tools-and-activities/breathing-exercises-for-stress/"
        ),
        MeditationResource(
            title = "4-7-8 relaxing breath (Dr. Weil)",
            description = "A quick technique to calm your nervous system.",
            durationLabel = "~2–3 min",
            url = "https://www.drweil.com/videos-features/videos/breathing-exercises-4-7-8-breath/"
        )
    )

    val shortMeditations = listOf(
        MeditationResource(
            title = "UCLA Mindful: Guided Meditations",
            description = "Stream or download short, beginner-friendly meditations.",
            durationLabel = "3–10 min",
            url = "https://www.uclahealth.org/uclamindful/guided-meditations"
        ),
        MeditationResource(
            title = "Headspace: Let Go of Stress (YouTube)",
            description = "A short guided reset when you feel overwhelmed.",
            durationLabel = "~4 min",
            url = "https://www.youtube.com/watch?v=c1Ndym-IsQg"
        )
    )

    val longerSessions = listOf(
        MeditationResource(
            title = "UCLA Mindful: Weekly Meditations & Talks",
            description = "Longer guided sessions and themes for deeper practice.",
            durationLabel = "10–30+ min",
            url = "https://www.uclahealth.org/uclamindful/weekly-meditations-talks"
        ),
        MeditationResource(
            title = "Headspace channel (YouTube)",
            description = "Browse short meditations, breathing practices, and sleep sounds.",
            durationLabel = "Varies",
            url = "https://www.youtube.com/c/headspace/videos"
        )
    )

    val inAppPractices = listOf(
        InAppPractice(
            title = "5-4-3-2-1 grounding",
            summary = "Use your senses to anchor back to the present moment.",
            timeLabel = "2–4 min",
            steps = "1) Name 5 things you can SEE.\n" +
                    "2) Name 4 things you can FEEL (touch, clothing, feet on the floor).\n" +
                    "3) Name 3 things you can HEAR.\n" +
                    "4) Name 2 things you can SMELL (or two smells you like).\n" +
                    "5) Name 1 thing you can TASTE (or one taste you like).\n\n" +
                    "Finish with one slow breath in and a longer breath out."
        ),
        InAppPractice(
            title = "Box breathing (4-4-4-4)",
            summary = "A steady rhythm that can calm your body fast.",
            timeLabel = "2–3 min",
            steps = "Repeat 3–5 rounds:\n" +
                    "• Inhale through your nose for 4\n" +
                    "• Hold for 4\n" +
                    "• Exhale slowly for 4\n" +
                    "• Hold for 4\n\n" +
                    "Tip: Keep the exhale gentle—don’t force it."
        ),
        InAppPractice(
            title = "STOP skill",
            summary = "A quick reset when your mind is racing.",
            timeLabel = "1–2 min",
            steps = "S — Stop. Pause for a moment.\n" +
                    "T — Take a breath. One slow inhale + longer exhale.\n" +
                    "O — Observe. What am I feeling in my body? What thoughts are here?\n" +
                    "P — Proceed. Choose one small next step."
        ),
        InAppPractice(
            title = "4-7-8 relaxing breath",
            summary = "A calming pattern that can help with stress or winding down.",
            timeLabel = "2–3 min",
            steps = "Repeat 3–4 times:\n" +
                    "• Inhale through your nose for 4\n" +
                    "• Hold for 7\n" +
                    "• Exhale through your mouth for 8\n\n" +
                    "If you feel lightheaded, return to normal breathing."
        ),
        InAppPractice(
            title = "Mini body scan",
            summary = "Release tension by checking in from head to toe.",
            timeLabel = "3–5 min",
            steps = "Start at the top of your head and move down slowly:\n" +
                    "• Forehead/jaw: unclench\n" +
                    "• Shoulders: drop them down\n" +
                    "• Chest/belly: soften as you exhale\n" +
                    "• Hands: relax your grip\n" +
                    "• Legs/feet: feel them supported\n\n" +
                    "Name one spot that feels calmer than before."
        ),
        InAppPractice(
            title = "Progressive muscle relax (quick)",
            summary = "Tense then release to signal safety to your body.",
            timeLabel = "4–6 min",
            steps = "For each area: tense 5 seconds → release 10 seconds.\n" +
                    "1) Hands (make fists)\n" +
                    "2) Shoulders (shrug up)\n" +
                    "3) Face (scrunch)\n" +
                    "4) Legs (tighten thighs)\n" +
                    "5) Feet (curl toes)\n\n" +
                    "End with a slow breath out and notice the difference."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meditation & Breathing", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Take a small pause 🌿", fontWeight = FontWeight.Bold)
                    Text("Pick something short. Even one minute counts.")
                }
            }

            SectionTitle("In-app grounding & mindfulness")
            Text("Tap a card to read the steps.")
            inAppPractices.forEach { practice ->
                PracticeCard(practice = practice)
            }

            SectionTitle("Quick breathing")
            breathing.forEach { res ->
                ResourceCard(
                    resource = res,
                    onOpen = { openUrl(res.url) }
                )
            }

            SectionTitle("Short meditations")
            shortMeditations.forEach { res ->
                ResourceCard(
                    resource = res,
                    onOpen = { openUrl(res.url) }
                )
            }

            SectionTitle("Longer sessions")
            longerSessions.forEach { res ->
                ResourceCard(
                    resource = res,
                    onOpen = { openUrl(res.url) }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun ResourceCard(
    resource: MeditationResource,
    onOpen: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(resource.title, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(resource.description)
            }

            Spacer(Modifier.width(12.dp))

            AssistChip(
                onClick = onOpen,
                label = { Text(resource.durationLabel) }
            )
        }
    }
}

@Composable
private fun PracticeCard(
    practice: InAppPractice
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(practice.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                AssistChip(onClick = { expanded = !expanded }, label = { Text(practice.timeLabel) })
            }

            Text(practice.summary)

            if (expanded) {
                Divider()
                Text(practice.steps)
            }

            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide" else "Show steps")
            }
        }
    }
}

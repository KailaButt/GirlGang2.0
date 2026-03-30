package com.example.consolicalm.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.consolicalm.R

val PacificoFont = FontFamily(
    Font(R.font.pacifico_regular)
)

@Composable
fun HomeScreen(
    calmPoints: Int,
    nextRewardGoal: Int,
    onProfileClick: () -> Unit,
    onRewardsClick: () -> Unit,
    onTodoClick: () -> Unit,
    onMeditationClick: () -> Unit,
    onStudyClick: () -> Unit,
    onEarnPoints: (Int) -> Unit
) {

    val progress = if (nextRewardGoal > 0)
        calmPoints.toFloat() / nextRewardGoal.toFloat()
    else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "ConsoliCalm",
            fontFamily = PacificoFont,
            fontSize = 28.sp,
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    text = "Calm Points",
                    fontFamily = PacificoFont,
                    fontSize = 20.sp,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "$calmPoints pts",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Next Reward at $nextRewardGoal pts",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = onStudyClick, modifier = Modifier.fillMaxWidth()) {
            Text("Start Study Session")
        }

        Button(onClick = onTodoClick, modifier = Modifier.fillMaxWidth()) {
            Text("Open To-Do List")
        }

        Button(onClick = onMeditationClick, modifier = Modifier.fillMaxWidth()) {
            Text("Meditation")
        }

        Button(onClick = onRewardsClick, modifier = Modifier.fillMaxWidth()) {
            Text("View Rewards")
        }

        OutlinedButton(onClick = onProfileClick, modifier = Modifier.fillMaxWidth()) {
            Text("Profile")
        }
    }
}
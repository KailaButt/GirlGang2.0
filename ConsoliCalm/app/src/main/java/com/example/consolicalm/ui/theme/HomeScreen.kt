package com.example.consolicalm.com.example.consolicalm.ui.theme



import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "ConsoliCalm",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text("Calm Points", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$calmPoints pts",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = calmPoints.toFloat() / nextRewardGoal.toFloat(),
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
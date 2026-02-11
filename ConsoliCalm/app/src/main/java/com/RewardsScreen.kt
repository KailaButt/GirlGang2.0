package com.example.consolicalm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class RewardOption(
    val title: String,
    val pointsCost: Int,
    val description: String
)

@Composable
fun RewardsScreen(
    calmPoints: Int,
    onBack: () -> Unit
) {
    val rewards = listOf(
        RewardOption("Amazon Gift Card", 500, "Shop anything"),
        RewardOption("Starbucks Gift Card", 900, "Coffee reward ☕"),
        RewardOption("Campus Bookstore Credit", 700, "Books & supplies 📚"),
        RewardOption("Dunkin Gift Card", 800, "Snacks + coffee")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Rewards 🎁", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.height(8.dp))
        Text("You have $calmPoints Calm Points 🌿")
        Spacer(Modifier.height(16.dp))

        rewards.forEach { reward ->
            val canRedeem = calmPoints >= reward.pointsCost

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable(enabled = canRedeem) {},
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(reward.title, fontWeight = FontWeight.Bold)
                        Text("${reward.pointsCost} pts")
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(reward.description)

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {},
                        enabled = canRedeem,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (canRedeem) "Redeem"
                            else "Need ${reward.pointsCost - calmPoints} more points"
                        )
                    }
                }
            }
        }
    }
}


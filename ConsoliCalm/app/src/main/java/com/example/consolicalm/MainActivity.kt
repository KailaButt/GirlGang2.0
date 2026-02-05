package com.example.consolicalm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import com.example.consolicalm.ui.theme.ConsoliCalmTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConsoliCalmTheme {

                var calmPoints by remember { mutableIntStateOf(240) }
                var showRewards by remember { mutableStateOf(false) }

                Scaffold { _ ->

                    if (showRewards) {
                        RewardsScreen(
                            calmPoints = calmPoints,
                            onBack = { showRewards = false }
                        )
                    } else {
                        HomeScreen(
                            calmPoints = calmPoints,
                            nextRewardGoal = 500,
                            onRewardsClick = { showRewards = true }
                        )
                    }

                }
            }
        }
    }
}


package com.example.consolicalm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WeeklyInsightsRoute(
    onBack: () -> Unit,
    viewModel: WeeklyInsightsViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val myInsight = uiState.myInsight

    val myStats = WeeklyInsightStats(
        pointsEarned = myInsight?.pointsEarned ?: 0,
        sessionsCompleted = myInsight?.studySessions ?: 0,
        focusMinutes = myInsight?.calmMinutes ?: 0,
        streakDays = myInsight?.moodCheckIns ?: 0,
        bestDay = "This Week"
    )

    val leaderboardEntries = uiState.leaderboard.map { insight ->
        LeaderboardEntry(
            name = if (insight.uid == myInsight?.uid) {
                if (insight.nickname.isBlank()) "You" else insight.nickname
            } else {
                if (insight.nickname.isBlank()) "Friend" else insight.nickname
            },
            points = insight.pointsEarned,
            sessions = insight.studySessions,
            isYou = insight.uid == myInsight?.uid
        )
    }

    WeeklyInsightsScreen(
        myStats = myStats,
        leaderboard = leaderboardEntries,
        onBack = onBack
    )
}
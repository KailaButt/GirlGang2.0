package com.example.consolicalm

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WeeklyInsightsUiState(
    val myInsight: WeeklyInsight? = null,
    val leaderboard: List<WeeklyInsight> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class WeeklyInsightsViewModel : ViewModel() {

    private val repository = WeeklyInsightsRepository()

    private val _uiState = MutableStateFlow(WeeklyInsightsUiState())
    val uiState: StateFlow<WeeklyInsightsUiState> = _uiState.asStateFlow()

    private var myInsightListener: ListenerRegistration? = null
    private var leaderboardListener: ListenerRegistration? = null

    init {
        startListening()
    }

    private fun startListening() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )

        myInsightListener?.remove()
        leaderboardListener?.remove()

        myInsightListener = repository.listenToMyWeeklyInsight(
            onUpdate = { insight ->
                _uiState.value = _uiState.value.copy(
                    myInsight = insight,
                    isLoading = false
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load your weekly insight",
                    isLoading = false
                )
            }
        )

        leaderboardListener = repository.listenToLeaderboard(
            onUpdate = { list ->
                _uiState.value = _uiState.value.copy(
                    leaderboard = list,
                    isLoading = false
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load leaderboard",
                    isLoading = false
                )
            }
        )
    }

    fun saveWeeklyInsight(
        nickname: String,
        pointsEarned: Int,
        studySessions: Int,
        tasksCompleted: Int,
        calmMinutes: Int,
        moodCheckIns: Int
    ) {
        repository.saveMyWeeklyInsight(
            nickname = nickname,
            pointsEarned = pointsEarned,
            studySessions = studySessions,
            tasksCompleted = tasksCompleted,
            calmMinutes = calmMinutes,
            moodCheckIns = moodCheckIns,
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to save weekly insight"
                )
            }
        )
    }

    fun incrementWeeklyStats(
        nickname: String,
        pointsToAdd: Int = 0,
        sessionsToAdd: Int = 0,
        tasksToAdd: Int = 0,
        calmMinutesToAdd: Int = 0,
        moodCheckInsToAdd: Int = 0
    ) {
        repository.incrementWeeklyStats(
            nickname = nickname,
            pointsToAdd = pointsToAdd,
            sessionsToAdd = sessionsToAdd,
            tasksToAdd = tasksToAdd,
            calmMinutesToAdd = calmMinutesToAdd,
            moodCheckInsToAdd = moodCheckInsToAdd,
            onSuccess = {
                // no-op, snapshot listeners will update UI automatically
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update weekly stats"
                )
            }
        )
    }

    fun refresh() {
        startListening()
    }

    override fun onCleared() {
        myInsightListener?.remove()
        leaderboardListener?.remove()
        super.onCleared()
    }
}
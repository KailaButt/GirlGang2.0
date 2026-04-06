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
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        myInsightListener = repository.listenToMyWeeklyInsight(
            onUpdate = { insight ->
                _uiState.value = _uiState.value.copy(
                    myInsight = insight,
                    isLoading = false
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    error = e.message,
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
                    error = e.message,
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
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        )
    }

    override fun onCleared() {
        myInsightListener?.remove()
        leaderboardListener?.remove()
        super.onCleared()
    }
}
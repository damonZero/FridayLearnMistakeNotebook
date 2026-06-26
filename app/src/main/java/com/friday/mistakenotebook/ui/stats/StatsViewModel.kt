package com.friday.mistakenotebook.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val totalQuestions: Int = 0,
    val masteredQuestions: Int = 0,
    val todayReviewCount: Int = 0,
    val masteryPercentage: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            // 总题目数
            questionRepository.getTotalQuestionCount().collect { total ->
                _uiState.update { it.copy(totalQuestions = total) }
                calculateMastery()
            }
        }

        viewModelScope.launch {
            // 已掌握数量
            questionRepository.getMasteredCount().collect { mastered ->
                _uiState.update { it.copy(masteredQuestions = mastered) }
                calculateMastery()
            }
        }

        viewModelScope.launch {
            // 今日复习数量
            questionRepository.getTodayReviewCount().collect { count ->
                _uiState.update { it.copy(todayReviewCount = count, isLoading = false) }
            }
        }
    }

    private fun calculateMastery() {
        val state = _uiState.value
        val percentage = if (state.totalQuestions > 0) {
            (state.masteredQuestions * 100 / state.totalQuestions)
        } else {
            0
        }
        _uiState.update { it.copy(masteryPercentage = percentage) }
    }
}

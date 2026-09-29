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
    val distribution: MasteryDistribution = MasteryDistribution(),
    val isLoading: Boolean = true
)

/**
 * 掌握程度分布（按莱特纳盒子统计，三档百分比相加等于 100%）
 */
data class MasteryDistribution(
    val newPercentage: Int = 0,       // 盒1：新题/答错
    val learningPercentage: Int = 0,  // 盒2-4：学习中
    val masteredPercentage: Int = 0   // 盒5：已掌握
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

        viewModelScope.launch {
            // 莱特纳盒子分布：盒1=新题/答错，盒2-4=学习中，盒5=已掌握
            questionRepository.getBoxCounts().collect { boxCounts ->
                _uiState.update { it.copy(distribution = calculateDistribution(boxCounts)) }
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

    private fun calculateDistribution(boxCounts: Map<Int, Int>): MasteryDistribution {
        val counts = listOf(
            boxCounts[1] ?: 0,                    // 新题/答错
            (2..4).sumOf { boxCounts[it] ?: 0 },  // 学习中
            boxCounts[5] ?: 0                     // 已掌握
        )
        val total = counts.sum()
        if (total == 0) return MasteryDistribution()

        val percentages = counts.map { it * 100 / total }.toMutableList()
        // 整数除法向下取整，把余数补给题数最多的一档，保证三档相加等于 100%
        val maxIndex = counts.indexOf(counts.max())
        percentages[maxIndex] += 100 - percentages.sum()

        return MasteryDistribution(
            newPercentage = percentages[0],
            learningPercentage = percentages[1],
            masteredPercentage = percentages[2]
        )
    }
}

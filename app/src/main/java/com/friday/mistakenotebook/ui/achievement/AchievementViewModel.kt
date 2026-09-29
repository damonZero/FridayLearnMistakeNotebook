package com.friday.mistakenotebook.ui.achievement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.domain.model.Question
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val progress: Int,
    val target: Int
)

data class AchievementUiState(
    val achievements: List<Achievement> = emptyList(),
    val totalQuestions: Int = 0,
    val masteredQuestions: Int = 0,
    val streakDays: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class AchievementViewModel @Inject constructor(
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementUiState())
    val uiState: StateFlow<AchievementUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            questionRepository.getTotalQuestionCount().collect { total ->
                _uiState.update { it.copy(totalQuestions = total) }
                updateAchievements()
            }
        }

        viewModelScope.launch {
            questionRepository.getMasteredCount().collect { mastered ->
                _uiState.update { it.copy(masteredQuestions = mastered) }
                updateAchievements()
            }
        }

        viewModelScope.launch {
            // 连续复习天数
            questionRepository.getAllQuestions().collect { questions ->
                val streak = calculateStreakDays(questions)
                _uiState.update { it.copy(streakDays = streak) }
                updateAchievements()
            }
        }
    }

    /**
     * 连续复习天数：把所有题目的最近复习日期按本地时区截断到天并去重，
     * 从今天往回数连续天数（今天还没复习不打断记录，从昨天起算）。
     */
    private fun calculateStreakDays(questions: List<Question>): Int {
        val reviewDates = questions.mapNotNull { question ->
            question.lastReviewDate?.let { timestamp ->
                Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
        }.toSet()
        if (reviewDates.isEmpty()) return 0

        var day = LocalDate.now()
        if (day !in reviewDates) {
            day = day.minusDays(1)
        }
        var streak = 0
        while (day in reviewDates) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    private fun updateAchievements() {
        val state = _uiState.value
        val achievements = listOf(
            Achievement(
                id = "first_question",
                title = "初学者",
                description = "添加第一道错题",
                icon = "📚",
                isUnlocked = state.totalQuestions >= 1,
                progress = minOf(state.totalQuestions, 1),
                target = 1
            ),
            Achievement(
                id = "ten_questions",
                title = "勤奋学习",
                description = "添加 10 道错题",
                icon = "📝",
                isUnlocked = state.totalQuestions >= 10,
                progress = minOf(state.totalQuestions, 10),
                target = 10
            ),
            Achievement(
                id = "fifty_questions",
                title = "错题收集家",
                description = "添加 50 道错题",
                icon = "📊",
                isUnlocked = state.totalQuestions >= 50,
                progress = minOf(state.totalQuestions, 50),
                target = 50
            ),
            Achievement(
                id = "first_mastered",
                title = "小有成就",
                description = "掌握第一道错题",
                icon = "⭐",
                isUnlocked = state.masteredQuestions >= 1,
                progress = minOf(state.masteredQuestions, 1),
                target = 1
            ),
            Achievement(
                id = "ten_mastered",
                title = "知识达人",
                description = "掌握 10 道错题",
                icon = "🌟",
                isUnlocked = state.masteredQuestions >= 10,
                progress = minOf(state.masteredQuestions, 10),
                target = 10
            ),
            Achievement(
                id = "fifty_mastered",
                title = "学霸",
                description = "掌握 50 道错题",
                icon = "👑",
                isUnlocked = state.masteredQuestions >= 50,
                progress = minOf(state.masteredQuestions, 50),
                target = 50
            ),
            Achievement(
                id = "streak_3",
                title = "坚持三天",
                description = "连续复习 3 天",
                icon = "🔥",
                isUnlocked = state.streakDays >= 3,
                progress = minOf(state.streakDays, 3),
                target = 3
            ),
            Achievement(
                id = "streak_7",
                title = "一周坚持",
                description = "连续复习 7 天",
                icon = "💪",
                isUnlocked = state.streakDays >= 7,
                progress = minOf(state.streakDays, 7),
                target = 7
            ),
            Achievement(
                id = "streak_30",
                title = "月度之星",
                description = "连续复习 30 天",
                icon = "🏆",
                isUnlocked = state.streakDays >= 30,
                progress = minOf(state.streakDays, 30),
                target = 30
            )
        )

        _uiState.update { it.copy(achievements = achievements, isLoading = false) }
    }
}

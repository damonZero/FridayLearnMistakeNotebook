package com.friday.mistakenotebook.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.domain.model.Question
import com.friday.mistakenotebook.domain.model.ReviewResult
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val isAnswerShown: Boolean = false,
    // 答完全部题目（会话内推进到末尾）
    val isCompleted: Boolean = false,
    // 正在提交答案（防双击重复计数）
    val isSubmitting: Boolean = false,
    // 正在做一次性快照加载
    val isLoading: Boolean = true
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        loadReviewQuestions()
    }

    private fun loadReviewQuestions() {
        viewModelScope.launch {
            // 进入会话时对题目列表做一次性快照，之后不再由数据库重发驱动列表，
            // 避免答题落库导致列表变短与手动推进 index 互相踩踏（崩溃/跳题）
            val questions = questionRepository.getQuestionsForReview().first()
            _uiState.update {
                it.copy(
                    questions = questions,
                    isLoading = false,
                    // 快照为空属于 loadedEmpty，由 questions.isEmpty() 分支呈现；
                    // isCompleted 只表示“答完全部”，两者必须区分
                    isCompleted = false
                )
            }
        }
    }

    fun showAnswer() {
        _uiState.update { it.copy(isAnswerShown = true) }
    }

    fun markCorrect() {
        submitResult(isCorrect = true, score = 5)
    }

    fun markIncorrect() {
        submitResult(isCorrect = false, score = 2)
    }

    private fun submitResult(isCorrect: Boolean, score: Int) {
        val state = _uiState.value
        if (state.isSubmitting || state.isCompleted) return
        val question = state.questions.getOrNull(state.currentIndex) ?: return

        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                questionRepository.processReviewResult(
                    ReviewResult(questionId = question.id, isCorrect = isCorrect, score = score)
                )
            } finally {
                moveToNext()
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    private fun moveToNext() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1

        if (nextIndex >= state.questions.size) {
            _uiState.update { it.copy(isCompleted = true) }
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    isAnswerShown = false
                )
            }
        }
    }
}

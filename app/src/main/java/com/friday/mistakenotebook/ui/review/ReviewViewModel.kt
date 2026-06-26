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
    val isCompleted: Boolean = false,
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
            questionRepository.getQuestionsForReview().collect { questions ->
                _uiState.update {
                    it.copy(
                        questions = questions,
                        isLoading = false,
                        isCompleted = questions.isEmpty()
                    )
                }
            }
        }
    }

    fun showAnswer() {
        _uiState.update { it.copy(isAnswerShown = true) }
    }

    fun markCorrect() {
        val state = _uiState.value
        val question = state.questions.getOrNull(state.currentIndex) ?: return

        viewModelScope.launch {
            questionRepository.processReviewResult(
                ReviewResult(questionId = question.id, isCorrect = true, score = 5)
            )
            moveToNext()
        }
    }

    fun markIncorrect() {
        val state = _uiState.value
        val question = state.questions.getOrNull(state.currentIndex) ?: return

        viewModelScope.launch {
            questionRepository.processReviewResult(
                ReviewResult(questionId = question.id, isCorrect = false, score = 2)
            )
            moveToNext()
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

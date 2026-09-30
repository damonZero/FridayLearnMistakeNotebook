package com.friday.mistakenotebook.ui.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.remote.AiChatService
import com.friday.mistakenotebook.data.remote.GeneratedQuestion
import com.friday.mistakenotebook.domain.model.Question
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PracticeUiState(
    // 原错题（生成相似题的依据）
    val sourceQuestion: Question? = null,
    // 相似题生成中
    val isLoading: Boolean = true,
    // 生成失败信息（展示重试按钮）
    val error: String? = null,
    val questions: List<GeneratedQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val isAnswerShown: Boolean = false,
    // 自评"会了"的题数（仅本地计数，不写复习系统）
    val knownCount: Int = 0,
    // 全部答完
    val isCompleted: Boolean = false
)

@HiltViewModel
class PracticeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val questionRepository: QuestionRepository,
    private val aiChatService: AiChatService
) : ViewModel() {

    private val questionId: Long = savedStateHandle.get<Long>("questionId") ?: -1L

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        generateQuestions()
    }

    /**
     * 生成一组相似题：进入页面时自动触发，失败重试、"再练一组"都走这里。
     * 生成中界面不会出现重试入口，因此无需防重入
     */
    fun generateQuestions() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val source = questionRepository.getQuestionById(questionId)
            if (source == null) {
                _uiState.update { it.copy(isLoading = false, error = "找不到这道错题") }
                return@launch
            }

            aiChatService.generateSimilarQuestions(source.content)
                .onSuccess { questions ->
                    _uiState.update {
                        it.copy(
                            sourceQuestion = source,
                            isLoading = false,
                            error = null,
                            questions = questions,
                            currentIndex = 0,
                            isAnswerShown = false,
                            knownCount = 0,
                            isCompleted = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            sourceQuestion = source,
                            isLoading = false,
                            error = e.message ?: "生成失败，请重试"
                        )
                    }
                }
        }
    }

    fun showAnswer() {
        _uiState.update { it.copy(isAnswerShown = true) }
    }

    /** 自评"会了"（仅本地计数） */
    fun markKnown() = submit(isKnown = true)

    /** 自评"不会"（仅本地计数） */
    fun markUnknown() = submit(isKnown = false)

    private fun submit(isKnown: Boolean) {
        val state = _uiState.value
        // 没看答案不能自评，答完不再计数
        if (!state.isAnswerShown || state.isCompleted) return

        val newKnownCount = if (isKnown) state.knownCount + 1 else state.knownCount
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.questions.size) {
            _uiState.update {
                it.copy(isCompleted = true, knownCount = newKnownCount)
            }
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    isAnswerShown = false,
                    knownCount = newKnownCount
                )
            }
        }
    }
}

package com.friday.mistakenotebook.ui.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.remote.AiChatService
import com.friday.mistakenotebook.data.remote.GeneratedQuestion
import com.friday.mistakenotebook.data.remote.SimilarQuestionCodec
import com.friday.mistakenotebook.domain.model.Question
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PracticeUiState(
    // 原错题（生成相似题的依据）
    val sourceQuestion: Question? = null,
    // 生成/刷新中
    val isLoading: Boolean = true,
    // 生成失败信息（仅在没有任何题目时展示死端错误页；
    // 会话中刷新失败保留会话，用 refreshError 提示）
    val error: String? = null,
    val refreshError: String? = null,
    val questions: List<GeneratedQuestion> = emptyList(),
    // 当前题目来自缓存（true）还是本次新生成（false）
    val fromCache: Boolean = false,
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
        loadOrGenerate()
    }

    /**
     * 优先使用上次生成并保存的举一反三（秒开、可反复看）；
     * 没有缓存才调 AI 生成，生成成功后落库供下次复用
     */
    private fun loadOrGenerate() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val source = questionRepository.getQuestionById(questionId)
            if (source == null) {
                _uiState.update { it.copy(isLoading = false, error = "找不到这道错题") }
                return@launch
            }
            val cached = source.similarQuestionsJson
                ?.takeIf { it.isNotBlank() }
                ?.let { SimilarQuestionCodec.decode(it) }
                .orEmpty()
            if (cached.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        sourceQuestion = source,
                        isLoading = false,
                        error = null,
                        questions = cached,
                        fromCache = true
                    ).resetSession()
                }
            } else {
                generateInternal(source)
            }
        }
    }

    /**
     * 重新生成（刷新按钮）：调 AI 出新的一组，成功后覆盖保存。
     * 会话中刷新失败不销毁当前练习，改用 refreshError 提示
     */
    fun refreshQuestions() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null, refreshError = null) }
        viewModelScope.launch {
            val source = _uiState.value.sourceQuestion
                ?: questionRepository.getQuestionById(questionId)
            if (source == null) {
                _uiState.update { it.copy(isLoading = false, error = "找不到这道错题") }
                return@launch
            }
            val result = aiChatService.generateAndCacheSimilarQuestions(questionRepository, source)
            if (result.errorMessage == null) {
                _uiState.update {
                    it.copy(
                        sourceQuestion = source.copy(
                            similarQuestionsJson = SimilarQuestionCodec.encode(result.questions)
                        ),
                        isLoading = false,
                        error = null,
                        refreshError = null,
                        questions = result.questions
                    ).resetSession()
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        refreshError = if (it.questions.isNotEmpty()) {
                            "刷新失败：${result.errorMessage}，已保留当前练习"
                        } else {
                            null
                        },
                        error = if (it.questions.isEmpty()) result.errorMessage else it.error
                    )
                }
            }
        }
    }

    /** 调 AI 生成 → 落库 → 进入会话；失败展示重试入口 */
    private suspend fun generateInternal(source: Question) {
        val result = aiChatService.generateAndCacheSimilarQuestions(questionRepository, source)
        if (result.errorMessage != null) {
            _uiState.update {
                it.copy(
                    sourceQuestion = source,
                    isLoading = false,
                    error = result.errorMessage
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                sourceQuestion = source.copy(
                    similarQuestionsJson = SimilarQuestionCodec.encode(result.questions)
                ),
                isLoading = false,
                error = null,
                questions = result.questions,
                fromCache = false
            ).resetSession()
        }
    }

    /** "再练一组"：同一批题从头再来（不重新生成） */
    fun restart() {
        _uiState.update { it.resetSession() }
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

    private fun PracticeUiState.resetSession(): PracticeUiState = copy(
        currentIndex = 0,
        isAnswerShown = false,
        knownCount = 0,
        isCompleted = false
    )
}

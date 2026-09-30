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
    // 生成失败信息（展示重试按钮）
    val error: String? = null,
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
     * 重新生成（刷新按钮）：调 AI 出新的一组，成功后覆盖保存
     */
    fun refreshQuestions() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val source = _uiState.value.sourceQuestion
                ?: questionRepository.getQuestionById(questionId)
            if (source == null) {
                _uiState.update { it.copy(isLoading = false, error = "找不到这道错题") }
                return@launch
            }
            generateInternal(source)
        }
    }

    /** 调 AI 生成 → 落库 → 进入会话；失败展示重试入口 */
    private suspend fun generateInternal(source: Question) {
        aiChatService.generateSimilarQuestions(source.content)
            .onSuccess { questions ->
                // 持久化：下次进入直接复用，打印也用它
                if (questions.isNotEmpty()) {
                    runCatching {
                        questionRepository.updateSimilarQuestions(
                            source.id,
                            SimilarQuestionCodec.encode(questions)
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        sourceQuestion = source.copy(similarQuestionsJson = SimilarQuestionCodec.encode(questions)),
                        isLoading = false,
                        error = null,
                        questions = questions,
                        fromCache = false
                    ).resetSession()
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

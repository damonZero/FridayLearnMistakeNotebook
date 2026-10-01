package com.friday.mistakenotebook.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.remote.SimilarQuestionCodec
import com.friday.mistakenotebook.domain.model.Question
import com.friday.mistakenotebook.domain.model.ReviewResult
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import com.friday.mistakenotebook.print.PracticeSheetPdfGenerator
import com.friday.mistakenotebook.print.SheetGenerateState
import com.friday.mistakenotebook.print.SheetItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private val questionRepository: QuestionRepository,
    private val pdfGenerator: PracticeSheetPdfGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _sheetState = MutableStateFlow<SheetGenerateState>(SheetGenerateState.Idle)
    val sheetState: StateFlow<SheetGenerateState> = _sheetState.asStateFlow()

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

    /**
     * 打印今日待复习卷：全部到期错题（含已保存的举一反三）一卷打尽，
     * 题卷孩子做、答案卷家长留存。做完后在会话里逐题标"会了/还错"回录
     */
    fun printDueSheet() {
        if (_sheetState.value is SheetGenerateState.Generating) return
        _sheetState.value = SheetGenerateState.Generating("正在整理今日待复习…")
        viewModelScope.launch {
            try {
                val due = questionRepository.getQuestionsForReview().first()
                if (due.isEmpty()) {
                    _sheetState.value = SheetGenerateState.Failed("今天没有待复习的错题")
                    return@launch
                }
                _sheetState.value = SheetGenerateState.Generating("正在排版生成 PDF…")
                val items = due.map { q ->
                    SheetItem(q, SimilarQuestionCodec.decode(q.similarQuestionsJson.orEmpty()))
                }
                val missing = items.count { it.similar.isEmpty() }
                val title = "今日待复习练习卷 · " +
                    SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date())
                val files = pdfGenerator.generate(items, includeSimilar = true, title = title)
                _sheetState.value = SheetGenerateState.Ready(
                    files,
                    note = if (missing > 0) "注意：$missing 题尚无已保存的举一反三，本卷仅含原错题" else null
                )
            } catch (e: Exception) {
                _sheetState.value = SheetGenerateState.Failed("生成失败：${e.message ?: "未知错误"}")
            }
        }
    }

    fun consumeSheetState() {
        _sheetState.value = SheetGenerateState.Idle
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

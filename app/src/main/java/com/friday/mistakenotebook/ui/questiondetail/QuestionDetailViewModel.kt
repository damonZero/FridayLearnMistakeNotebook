package com.friday.mistakenotebook.ui.questiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.remote.AiChatService
import com.friday.mistakenotebook.data.remote.KnowledgeAnalysis
import com.friday.mistakenotebook.domain.model.Question
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionDetailUiState(
    val question: Question? = null,
    // 题目加载中
    val isLoading: Boolean = true,
    // AI 分析进行中
    val isAnalyzing: Boolean = false,
    // 本次会话内拿到的分析结果（比已存摘要信息更全：带知识点 chips）
    val analysis: KnowledgeAnalysis? = null,
    // AI 分析失败信息
    val analysisError: String? = null,
    // 删除成功后通知界面返回
    val isDeleted: Boolean = false
)

@HiltViewModel
class QuestionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val questionRepository: QuestionRepository,
    private val aiChatService: AiChatService
) : ViewModel() {

    private val questionId: Long = savedStateHandle.get<Long>("questionId") ?: -1L

    private val _uiState = MutableStateFlow(QuestionDetailUiState())
    val uiState: StateFlow<QuestionDetailUiState> = _uiState.asStateFlow()

    init {
        loadQuestion()
    }

    fun loadQuestion() {
        viewModelScope.launch {
            val question = questionRepository.getQuestionById(questionId)
            _uiState.update { it.copy(question = question, isLoading = false) }
        }
    }

    /**
     * AI 分析知识点：成功后把摘要存进题目的 aiAnalysis 列，下次进入直接展示
     */
    fun analyzeKnowledge() {
        val question = _uiState.value.question ?: return
        if (_uiState.value.isAnalyzing) return

        _uiState.update { it.copy(isAnalyzing = true, analysisError = null) }
        viewModelScope.launch {
            aiChatService.analyzeKnowledge(
                questionContent = question.content,
                userAnswer = question.userAnswer.takeIf { it.isNotBlank() },
                correctAnswer = question.answer.takeIf { it.isNotBlank() }
            ).onSuccess { analysis ->
                val updated = question.copy(aiAnalysis = buildAnalysisSummary(analysis))
                questionRepository.updateQuestion(updated)
                _uiState.update {
                    it.copy(question = updated, analysis = analysis, isAnalyzing = false)
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isAnalyzing = false, analysisError = e.message ?: "分析失败，请重试")
                }
            }
        }
    }

    /**
     * 删除错题（确认弹窗之后调用），完成后通知界面 popBackStack
     */
    fun deleteQuestion() {
        if (_uiState.value.isDeleted) return
        viewModelScope.launch {
            questionRepository.deleteQuestion(questionId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    companion object {
        /**
         * 分析结果摘要：存库并展示的纯文本形式
         */
        fun buildAnalysisSummary(analysis: KnowledgeAnalysis): String {
            return buildString {
                if (analysis.knowledgePoints.isNotEmpty()) {
                    appendLine("知识点：${analysis.knowledgePoints.joinToString("、")}")
                }
                if (analysis.errorTypeGuess.isNotBlank()) {
                    appendLine("错因：${analysis.errorTypeGuess}")
                }
                if (analysis.analysis.isNotBlank()) {
                    appendLine("分析：${analysis.analysis}")
                }
            }.trim()
        }
    }
}

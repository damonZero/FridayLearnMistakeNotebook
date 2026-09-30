package com.friday.mistakenotebook.ui.addquestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.local.entity.ErrorType
import com.friday.mistakenotebook.domain.model.Subject
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import com.friday.mistakenotebook.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddQuestionUiState(
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectId: Long? = null,
    val content: String = "",
    val answer: String = "",
    val userAnswer: String = "",
    val errorType: ErrorType = ErrorType.UNKNOWN,
    // 拍照识别时随 OCR 结果回传的原图绝对路径；手动录入为 null
    val imagePath: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val ocrApplied: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddQuestionViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddQuestionUiState())
    val uiState: StateFlow<AddQuestionUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            subjectRepository.getAllSubjects().collect { subjects ->
                _uiState.update {
                    it.copy(
                        subjects = subjects,
                        selectedSubjectId = it.selectedSubjectId ?: subjects.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun selectSubject(subjectId: Long) {
        _uiState.update { it.copy(selectedSubjectId = subjectId) }
    }

    fun updateContent(content: String) {
        if (content.length > MAX_CONTENT_LENGTH) {
            _uiState.update {
                it.copy(
                    content = content.take(MAX_CONTENT_LENGTH),
                    errorMessage = "题目内容不能超过 2000 字"
                )
            }
            return
        }
        _uiState.update { it.copy(content = content) }
    }

    fun updateAnswer(answer: String) {
        _uiState.update { it.copy(answer = answer) }
    }

    fun updateUserAnswer(userAnswer: String) {
        _uiState.update { it.copy(userAnswer = userAnswer) }
    }

    fun updateErrorType(errorType: ErrorType) {
        _uiState.update { it.copy(errorType = errorType) }
    }

    fun applyOcrResult(content: String, imagePath: String? = null) {
        _uiState.update {
            it.copy(
                content = content.trim(),
                imagePath = imagePath,
                ocrApplied = true,
                isSaved = false
            )
        }
    }

    fun createQuestionDraftFromOcr(content: String, imagePath: String? = null) {
        val normalizedContent = content.trim()
        if (normalizedContent.isBlank()) return
        applyOcrResult(normalizedContent, imagePath)
    }

    fun saveQuestion() {
        saveQuestionInternal(_uiState.value)
    }

    private fun saveQuestionInternal(state: AddQuestionUiState) {
        // 防止重复提交：正在保存时直接忽略
        if (state.isLoading) return

        if (state.content.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入题目内容") }
            return
        }

        if (state.content.length > MAX_CONTENT_LENGTH) {
            _uiState.update { it.copy(errorMessage = "题目内容不能超过 2000 字") }
            return
        }

        val subjectId = state.selectedSubjectId ?: state.subjects.firstOrNull()?.id
        if (subjectId == null) {
            _uiState.update { it.copy(errorMessage = "请选择科目") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                questionRepository.addQuestion(
                    subjectId = subjectId,
                    content = state.content,
                    answer = state.answer,
                    userAnswer = state.userAnswer,
                    errorType = state.errorType,
                    imagePath = state.imagePath
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true,
                        selectedSubjectId = subjectId,
                        ocrApplied = false,
                        imagePath = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "保存失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val MAX_CONTENT_LENGTH = 2000
    }
}

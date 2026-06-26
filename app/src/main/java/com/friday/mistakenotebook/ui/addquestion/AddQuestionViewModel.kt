package com.friday.mistakenotebook.ui.addquestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.local.entity.ErrorType
import com.friday.mistakenotebook.domain.model.Subject
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import com.friday.mistakenotebook.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddQuestionUiState(
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectId: Long? = null,
    val content: String = "",
    val answer: String = "",
    val userAnswer: String = "",
    val errorType: ErrorType = ErrorType.UNKNOWN,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
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

    fun saveQuestion() {
        val state = _uiState.value

        if (state.content.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入题目内容") }
            return
        }

        if (state.selectedSubjectId == null) {
            _uiState.update { it.copy(errorMessage = "请选择科目") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                questionRepository.addQuestion(
                    subjectId = state.selectedSubjectId,
                    content = state.content,
                    answer = state.answer,
                    userAnswer = state.userAnswer,
                    errorType = state.errorType
                )
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
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
}

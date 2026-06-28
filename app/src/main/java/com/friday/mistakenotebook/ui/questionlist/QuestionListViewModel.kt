package com.friday.mistakenotebook.ui.questionlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.domain.model.Question
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

data class QuestionListUiState(
    val questions: List<Question> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedSubjectId: Long? = null
)

@HiltViewModel
class QuestionListViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionListUiState())
    val uiState: StateFlow<QuestionListUiState> = _uiState.asStateFlow()

    private var allQuestions: List<Question> = emptyList()

    init {
        loadSubjects()
        loadQuestions()
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
                filterQuestions()
            }
        }
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            questionRepository.getAllQuestions().collect { questions ->
                allQuestions = questions
                filterQuestions()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterQuestions()
    }

    fun selectSubject(subjectId: Long?) {
        _uiState.update { it.copy(selectedSubjectId = subjectId) }
        filterQuestions()
    }

    fun clearFilters() {
        _uiState.update { it.copy(searchQuery = "", selectedSubjectId = null) }
        filterQuestions()
    }

    private fun filterQuestions() {
        val state = _uiState.value
        val filtered = allQuestions.filter { question ->
            val matchesSubject = state.selectedSubjectId == null ||
                question.subjectId == state.selectedSubjectId
            val matchesSearch = state.searchQuery.isBlank() ||
                question.content.contains(state.searchQuery, ignoreCase = true) ||
                question.answer.contains(state.searchQuery, ignoreCase = true)
            matchesSubject && matchesSearch
        }
        _uiState.update { it.copy(questions = filtered, isLoading = false) }
    }

    fun deleteQuestion(id: Long) {
        viewModelScope.launch {
            questionRepository.deleteQuestion(id)
        }
    }
}

package com.friday.mistakenotebook.ui.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.domain.model.Subject
import com.friday.mistakenotebook.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectUiState(
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val newSubjectName: String = "",
    val newSubjectIcon: String = "📚",
    val newSubjectColor: String = "#4CAF50",
    val errorMessage: String? = null
)

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectUiState())
    val uiState: StateFlow<SubjectUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            subjectRepository.getAllSubjects().collect { subjects ->
                _uiState.update { it.copy(subjects = subjects, isLoading = false) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun hideAddDialog() {
        _uiState.update {
            it.copy(showAddDialog = false, newSubjectName = "", errorMessage = null)
        }
    }

    fun updateNewSubjectName(name: String) {
        _uiState.update { it.copy(newSubjectName = name, errorMessage = null) }
    }

    fun addSubject() {
        val name = _uiState.value.newSubjectName.trim()
        if (name.isBlank()) return

        // 与现有科目（含预设科目）重名时不保存，提示用户
        val isDuplicate = _uiState.value.subjects.any {
            it.name.trim().equals(name, ignoreCase = true)
        }
        if (isDuplicate) {
            _uiState.update { it.copy(errorMessage = "该科目已存在") }
            return
        }

        viewModelScope.launch {
            subjectRepository.addSubject(
                name = name,
                icon = _uiState.value.newSubjectIcon,
                color = _uiState.value.newSubjectColor
            )
            hideAddDialog()
        }
    }

    fun deleteSubject(id: Long) {
        viewModelScope.launch {
            subjectRepository.deleteSubject(id)
        }
    }
}

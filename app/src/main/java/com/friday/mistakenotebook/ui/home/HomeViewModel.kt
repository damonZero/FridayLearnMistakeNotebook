package com.friday.mistakenotebook.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import com.friday.mistakenotebook.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HomeUiState(
    val todayReviewCount: Int = 0,
    val totalQuestionCount: Int = 0,
    val masteredCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // 初始化预设科目
        viewModelScope.launch {
            subjectRepository.initPresetSubjects()
        }

        // 监听今日复习数量
        viewModelScope.launch {
            questionRepository.getTodayReviewCount().collect { count ->
                _uiState.update { it.copy(todayReviewCount = count) }
            }
        }

        // 监听错题总数
        viewModelScope.launch {
            questionRepository.getTotalQuestionCount().collect { count ->
                _uiState.update { it.copy(totalQuestionCount = count, isLoading = false) }
            }
        }

        // 监听已掌握数量
        viewModelScope.launch {
            questionRepository.getMasteredCount().collect { count ->
                _uiState.update { it.copy(masteredCount = count) }
            }
        }
    }
}

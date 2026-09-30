package com.friday.mistakenotebook.ui.subjectdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.domain.model.Question
import com.friday.mistakenotebook.domain.model.Subject
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import com.friday.mistakenotebook.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SubjectDetailUiState(
    val subject: Subject? = null,
    val total: Int = 0,
    val newCount: Int = 0,
    val learningCount: Int = 0,
    val masteredCount: Int = 0,
    val todayDue: Int = 0,
    val recentQuestions: List<Question> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    subjectRepository: SubjectRepository,
    questionRepository: QuestionRepository
) : ViewModel() {

    private val subjectId: Long = savedStateHandle.get<Long>("subjectId") ?: -1L

    private val subjectFlow = flow {
        emit(subjectRepository.getSubjectById(subjectId))
    }

    val uiState = combine(
        subjectFlow,
        questionRepository.getBoxCountsBySubject(subjectId),
        questionRepository.getTodayDueCountBySubject(subjectId),
        questionRepository.getQuestionsBySubject(subjectId)
    ) { subject, boxCounts, todayDue, questions ->
        SubjectDetailUiState(
            subject = subject,
            total = boxCounts.values.sum(),
            newCount = boxCounts[1] ?: 0,
            learningCount = (boxCounts[2] ?: 0) + (boxCounts[3] ?: 0) + (boxCounts[4] ?: 0),
            masteredCount = boxCounts[5] ?: 0,
            todayDue = todayDue,
            recentQuestions = questions.take(5),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubjectDetailUiState())
}

package com.friday.mistakenotebook.ui.questionlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.local.entity.ErrorType
import com.friday.mistakenotebook.data.remote.AiChatService
import com.friday.mistakenotebook.domain.model.Question
import com.friday.mistakenotebook.domain.model.Subject
import com.friday.mistakenotebook.domain.repository.QuestionRepository
import com.friday.mistakenotebook.domain.repository.SubjectRepository
import com.friday.mistakenotebook.print.PracticeSheetPdfGenerator
import com.friday.mistakenotebook.print.SheetGenerateState
import com.friday.mistakenotebook.print.SheetItem
import com.friday.mistakenotebook.ui.addquestion.getErrorTypeName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** 列表排序方式 */
enum class QuestionSort(val label: String) {
    NEWEST("最新"),
    OLDEST("最早"),
    DUE_FIRST("待复习优先")
}

/** 列表分组方式 */
enum class QuestionGroup(val label: String) {
    NONE("不分组"),
    BY_STATUS("按掌握状态"),
    BY_ERROR_TYPE("按错误类型"),
    BY_DATE("按录入日期"),
    BY_KNOWLEDGE_POINT("按知识点")
}

/** 分组后的一个段落（title 为空表示不分组渲染） */
data class QuestionSection(
    val title: String,
    val questions: List<Question>
)

data class QuestionListUiState(
    val questions: List<Question> = emptyList(),
    val sections: List<QuestionSection> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedSubjectId: Long? = null,
    val sortMode: QuestionSort = QuestionSort.NEWEST,
    val groupMode: QuestionGroup = QuestionGroup.NONE,
    // 多选打印模式
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
)

@HiltViewModel
class QuestionListViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val subjectRepository: SubjectRepository,
    private val aiChatService: AiChatService,
    private val pdfGenerator: PracticeSheetPdfGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionListUiState())
    val uiState: StateFlow<QuestionListUiState> = _uiState.asStateFlow()

    private val _sheetState = MutableStateFlow<SheetGenerateState>(SheetGenerateState.Idle)
    val sheetState: StateFlow<SheetGenerateState> = _sheetState.asStateFlow()

    private var allQuestions: List<Question> = emptyList()
    private var searchDebounceJob: Job? = null

    init {
        loadSubjects()
        loadQuestions()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            subjectRepository.getAllSubjects().collect { subjects ->
                // 注意：selectedSubjectId 为 null 表示"全部"，是合法筛选状态，
                // 不能在这里改写成第一个科目，否则从首页"查看全部错题"进入时会被覆盖
                _uiState.update { it.copy(subjects = subjects) }
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
        // 300ms 防抖：输入停止后再过滤，避免每敲一个字都全量过滤
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            filterQuestions()
        }
    }

    fun selectSubject(subjectId: Long?) {
        _uiState.update { it.copy(selectedSubjectId = subjectId) }
        filterQuestions()
    }

    fun setSortMode(mode: QuestionSort) {
        _uiState.update { it.copy(sortMode = mode) }
        filterQuestions()
    }

    fun setGroupMode(mode: QuestionGroup) {
        _uiState.update { it.copy(groupMode = mode) }
        filterQuestions()
    }

    fun clearFilters() {
        searchDebounceJob?.cancel()
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
        }.let { list ->
            when (state.sortMode) {
                QuestionSort.NEWEST -> list.sortedByDescending { it.createdAt }
                QuestionSort.OLDEST -> list.sortedBy { it.createdAt }
                // 越紧急（nextReviewDate 越早）越靠前
                QuestionSort.DUE_FIRST -> list.sortedBy { it.nextReviewDate }
            }
        }
        val sections = buildSections(filtered, state.groupMode)
        _uiState.update { it.copy(questions = filtered, sections = sections, isLoading = false) }
    }

    private fun buildSections(list: List<Question>, mode: QuestionGroup): List<QuestionSection> {
        return when (mode) {
            QuestionGroup.NONE -> listOf(QuestionSection("", list))
            QuestionGroup.BY_STATUS -> listOf(
                "🔴 待巩固（盒1）" to list.filter { it.leitnerBox <= 1 },
                "🟡 学习中（盒2-4）" to list.filter { it.leitnerBox in 2..4 },
                "🟢 已掌握（盒5）" to list.filter { it.leitnerBox >= 5 }
            ).filter { it.second.isNotEmpty() }.map { QuestionSection(it.first, it.second) }
            QuestionGroup.BY_ERROR_TYPE -> ErrorType.entries.mapNotNull { type ->
                val group = list.filter { it.errorType == type }
                if (group.isEmpty()) {
                    null
                } else {
                    // 统一用 getErrorTypeName，与卡片 chip 文案同源
                    QuestionSection(getErrorTypeName(type), group)
                }
            }
            // groupBy 保留首次出现顺序，与当前排序一致：日期自然从新到旧
            QuestionGroup.BY_DATE -> list.groupBy { dateLabel(it.createdAt) }
                .map { QuestionSection(it.key, it.value) }
            // 按考点分组：考点多的排前面，未标注的沉底
            QuestionGroup.BY_KNOWLEDGE_POINT -> list
                .groupBy { it.knowledgePoint?.trim().takeUnless { t -> t.isNullOrBlank() } ?: "未标注" }
                .map { QuestionSection(it.key, it.value) }
                .sortedByDescending { it.questions.size }
        }
    }

    private fun dateLabel(timestamp: Long): String {
        val date = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        return when {
            date == today -> "今天"
            date == today.minusDays(1) -> "昨天"
            date.isAfter(today.minusDays(7)) -> "近 7 天"
            else -> "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
        }
    }

    fun deleteQuestion(id: Long) {
        viewModelScope.launch {
            questionRepository.deleteQuestion(id)
        }
    }

    // ---------- 多选打印 ----------

    fun enterSelectionMode() {
        _uiState.update { it.copy(selectionMode = true, selectedIds = emptySet()) }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    fun toggleSelection(id: Long) {
        _uiState.update {
            val next = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id
            it.copy(selectedIds = next)
        }
    }

    fun selectAllVisible() {
        _uiState.update { it.copy(selectedIds = it.questions.map { q -> q.id }.toSet()) }
    }

    /**
     * 批量生成练习卷：默认只印原错题+作答区；includeSimilar 时逐题调 AI 生成举一反三
     */
    fun generateSheet(includeSimilar: Boolean) {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return
        if (_sheetState.value is SheetGenerateState.Generating) return
        _sheetState.value = SheetGenerateState.Generating("正在整理题目…")
        viewModelScope.launch {
            try {
                val selected = allQuestions.filter { it.id in ids }
                if (selected.isEmpty()) {
                    _sheetState.value = SheetGenerateState.Failed("所选题目不在当前列表中")
                    return@launch
                }
                var failedCount = 0
                val items = selected.mapIndexed { index, q ->
                    val similar = if (includeSimilar) {
                        _sheetState.value = SheetGenerateState.Generating(
                            "正在生成第 ${index + 1}/${selected.size} 题的举一反三…"
                        )
                        aiChatService.generateSimilarQuestions(q.content)
                            .onFailure { failedCount++ }
                            .getOrElse { emptyList() }
                    } else {
                        emptyList()
                    }
                    SheetItem(q, similar)
                }
                _sheetState.value = SheetGenerateState.Generating("正在排版生成 PDF…")
                val title = "错题练习卷 · ${SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date())}"
                val files = pdfGenerator.generate(items, includeSimilar, title)
                _sheetState.value = SheetGenerateState.Ready(
                    files,
                    note = if (failedCount > 0) "注意：$failedCount 题的举一反三生成失败，已跳过" else null
                )
            } catch (e: Exception) {
                _sheetState.value = SheetGenerateState.Failed("生成失败：${e.message ?: "未知错误"}")
            }
        }
    }

    fun consumeSheetState() {
        _sheetState.value = SheetGenerateState.Idle
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}

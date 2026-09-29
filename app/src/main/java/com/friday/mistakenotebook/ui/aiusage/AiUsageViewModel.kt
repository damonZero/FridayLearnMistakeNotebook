package com.friday.mistakenotebook.ui.aiusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.local.dao.AiUsageLogDao
import com.friday.mistakenotebook.data.local.entity.AiUsageLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiUsageUiState(
    val logs: List<AiUsageLogEntity> = emptyList(),
    val totalCost: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class AiUsageViewModel @Inject constructor(
    private val aiUsageLogDao: AiUsageLogDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUsageUiState())
    val uiState: StateFlow<AiUsageUiState> = _uiState.asStateFlow()

    /** 清理完成等一次性提示，由页面用 Snackbar 展示 */
    private val _cleanupEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val cleanupEvent: SharedFlow<String> = _cleanupEvent.asSharedFlow()

    init {
        loadUsageData()
    }

    private fun loadUsageData() {
        viewModelScope.launch {
            aiUsageLogDao.getAllUsageLogs().collect { logs ->
                _uiState.update { it.copy(logs = logs, isLoading = false) }
            }
        }

        viewModelScope.launch {
            aiUsageLogDao.getTotalCost().collect { cost ->
                _uiState.update { it.copy(totalCost = cost ?: 0.0) }
            }
        }
    }

    fun clearOldLogs() {
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
            aiUsageLogDao.deleteOldLogs(thirtyDaysAgo)
            _cleanupEvent.emit("已清理")
        }
    }
}

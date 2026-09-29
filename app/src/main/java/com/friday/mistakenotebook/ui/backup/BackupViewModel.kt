package com.friday.mistakenotebook.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BackupUiState(
    val backupFiles: List<File> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        loadBackupFiles()
    }

    private fun loadBackupFiles() {
        _uiState.update {
            it.copy(backupFiles = backupManager.getBackupFiles())
        }
    }

    /**
     * 导出备份到用户选择的位置（SAF Uri）
     */
    fun exportToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val success = backupManager.exportToUri(uri)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    message = if (success) "备份已导出到所选位置" else "导出失败，请重试",
                    isError = !success
                )
            }
        }
    }

    /**
     * 从所选文件导入备份（合并覆盖现有数据），展示真实结果与统计
     */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val result = backupManager.importFromFile(uri)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    message = result.message,
                    isError = !result.success
                )
            }
        }
    }

    fun deleteBackup(file: File) {
        backupManager.deleteBackup(file)
        loadBackupFiles()
        _uiState.update { it.copy(message = "已删除备份") }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * 手动触发自动备份（保存到应用内部备份目录），如实展示结果
     */
    fun autoBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val success = backupManager.autoBackup()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    message = if (success) "自动备份完成，已保存到应用内备份目录" else "自动备份失败，请稍后重试",
                    isError = !success
                )
            }
            loadBackupFiles()
        }
    }
}

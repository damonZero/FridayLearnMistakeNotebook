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
import java.text.SimpleDateFormat
import java.util.*
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

    fun exportBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val file = backupManager.exportToFile()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "备份成功: ${file.name}",
                        isError = false
                    )
                }
                loadBackupFiles()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "备份失败: ${e.message}",
                        isError = true
                    )
                }
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val success = backupManager.importFromFile(uri)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = if (success) "导入成功" else "导入失败",
                        isError = !success
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "导入失败: ${e.message}",
                        isError = true
                    )
                }
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

    fun autoBackup() {
        viewModelScope.launch {
            backupManager.autoBackup()
            _uiState.update { it.copy(message = "自动备份完成") }
            loadBackupFiles()
        }
    }
}

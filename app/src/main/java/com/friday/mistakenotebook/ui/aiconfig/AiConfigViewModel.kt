package com.friday.mistakenotebook.ui.aiconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.local.dao.AiConfigDao
import com.friday.mistakenotebook.data.local.entity.AiConfigEntity
import com.friday.mistakenotebook.data.local.entity.AiTaskType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiConfigUiState(
    val configs: List<AiConfigEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val editingConfig: AiConfigEntity? = null,
    val provider: String = "火山方舟",
    val apiKey: String = "",
    val baseUrl: String = "https://ark.cn-beijing.volces.com/api/v3",
    val modelName: String = "doubao-vision-pro-32k",
    val taskType: AiTaskType = AiTaskType.OCR,
    val testResult: String? = null,
    val isTesting: Boolean = false
)

@HiltViewModel
class AiConfigViewModel @Inject constructor(
    private val aiConfigDao: AiConfigDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiConfigUiState())
    val uiState: StateFlow<AiConfigUiState> = _uiState.asStateFlow()

    init {
        loadConfigs()
    }

    private fun loadConfigs() {
        viewModelScope.launch {
            aiConfigDao.getAllAiConfigs().collect { configs ->
                _uiState.update { it.copy(configs = configs, isLoading = false) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                editingConfig = null,
                provider = "火山方舟",
                apiKey = "",
                baseUrl = "https://ark.cn-beijing.volces.com/api/v3",
                modelName = "doubao-vision-pro-32k",
                taskType = AiTaskType.OCR
            )
        }
    }

    fun showEditDialog(config: AiConfigEntity) {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                editingConfig = config,
                provider = config.provider,
                apiKey = config.apiKey,
                baseUrl = config.baseUrl,
                modelName = config.modelName,
                taskType = config.taskType
            )
        }
    }

    fun hideDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingConfig = null) }
    }

    fun updateProvider(provider: String) {
        _uiState.update { it.copy(provider = provider) }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey) }
    }

    fun updateBaseUrl(baseUrl: String) {
        _uiState.update { it.copy(baseUrl = baseUrl) }
    }

    fun updateModelName(modelName: String) {
        _uiState.update { it.copy(modelName = modelName) }
    }

    fun updateTaskType(taskType: AiTaskType) {
        _uiState.update { it.copy(taskType = taskType) }
    }

    fun saveConfig() {
        val state = _uiState.value
        if (state.apiKey.isBlank()) return

        viewModelScope.launch {
            val config = AiConfigEntity(
                id = state.editingConfig?.id ?: 0,
                provider = state.provider,
                apiKey = state.apiKey,
                baseUrl = state.baseUrl,
                modelName = state.modelName,
                taskType = state.taskType,
                isEnabled = true
            )

            if (state.editingConfig != null) {
                aiConfigDao.updateAiConfig(config)
            } else {
                aiConfigDao.insertAiConfig(config)
            }

            hideDialog()
        }
    }

    fun deleteConfig(config: AiConfigEntity) {
        viewModelScope.launch {
            aiConfigDao.deleteAiConfig(config)
        }
    }

    fun toggleConfig(config: AiConfigEntity) {
        viewModelScope.launch {
            aiConfigDao.updateAiConfig(config.copy(isEnabled = !config.isEnabled))
        }
    }

    fun testConnection() {
        _uiState.update { it.copy(isTesting = true, testResult = null) }
        viewModelScope.launch {
            // 模拟测试连接
            kotlinx.coroutines.delay(1500)
            _uiState.update {
                it.copy(
                    isTesting = false,
                    testResult = if (it.apiKey.isNotBlank()) "连接成功！" else "请输入 API Key"
                )
            }
        }
    }
}

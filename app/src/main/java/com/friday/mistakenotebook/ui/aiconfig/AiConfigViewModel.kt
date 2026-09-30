package com.friday.mistakenotebook.ui.aiconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.local.dao.AiConfigDao
import com.friday.mistakenotebook.data.local.entity.AiConfigEntity
import com.friday.mistakenotebook.data.local.entity.AiTaskType
import com.friday.mistakenotebook.data.remote.AiConnectivityTester
import com.friday.mistakenotebook.data.remote.TestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 供应商快捷模板：一键预填 Base URL 与各任务的推荐模型 */
data class ProviderTemplate(
    val name: String,
    val baseUrl: String,
    val visionModel: String,
    val textModel: String
)

data class AiConfigUiState(
    val configs: List<AiConfigEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val editingConfig: AiConfigEntity? = null,
    val provider: String = "DeepSeek",
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com",
    val modelName: String = "deepseek-v4-flash-vision-exp",
    val taskType: AiTaskType = AiTaskType.OCR,
    val testResult: TestResult? = null,
    val isTesting: Boolean = false
) {
    /** 保存所需的三项必填信息是否都已填写 */
    val canSave: Boolean
        get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && modelName.isNotBlank()
}

@HiltViewModel
class AiConfigViewModel @Inject constructor(
    private val aiConfigDao: AiConfigDao,
    private val aiConnectivityTester: AiConnectivityTester
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
                provider = "DeepSeek",
                apiKey = "",
                baseUrl = "https://api.deepseek.com",
                modelName = "deepseek-v4-flash-vision-exp",
                taskType = AiTaskType.OCR,
                testResult = null,
                isTesting = false
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
                taskType = config.taskType,
                testResult = null,
                isTesting = false
            )
        }
    }

    fun hideDialog() {
        _uiState.update {
            it.copy(
                showAddDialog = false,
                editingConfig = null,
                testResult = null,
                isTesting = false
            )
        }
    }

    fun updateProvider(provider: String) {
        _uiState.update { it.copy(provider = provider) }
    }

    /** 供应商模板：一键预填 Base URL 与当前任务的推荐模型 */
    fun applyProviderTemplate(provider: String) {
        val template = providerTemplates.firstOrNull { it.name == provider } ?: return
        _uiState.update { st ->
            st.copy(
                provider = template.name,
                baseUrl = template.baseUrl,
                modelName = if (st.taskType == AiTaskType.OCR) template.visionModel else template.textModel,
                testResult = null
            )
        }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey, testResult = null) }
    }

    fun updateBaseUrl(baseUrl: String) {
        _uiState.update { it.copy(baseUrl = baseUrl, testResult = null) }
    }

    fun updateModelName(modelName: String) {
        _uiState.update { it.copy(modelName = modelName, testResult = null) }
    }

    fun updateTaskType(taskType: AiTaskType) {
        _uiState.update { st ->
            val template = providerTemplates.firstOrNull { it.name == st.provider }
            if (template == null || st.editingConfig != null) {
                return@update st.copy(taskType = taskType)
            }
            // 仅当模型名还是模板默认值时跟随任务类型切换，不覆盖用户手输的模型
            val currentIsTemplateDefault =
                st.modelName == template.visionModel || st.modelName == template.textModel
            val suggested = if (taskType == AiTaskType.OCR) template.visionModel else template.textModel
            st.copy(
                taskType = taskType,
                modelName = if (currentIsTemplateDefault) suggested else st.modelName
            )
        }
    }

    fun saveConfig() {
        val state = _uiState.value
        val provider = state.provider.trim().ifBlank { "DeepSeek" }
        val apiKey = state.apiKey.trim()
        val baseUrl = state.baseUrl.trim()
        val modelName = state.modelName.trim()

        if (apiKey.isBlank()) {
            _uiState.update { it.copy(testResult = TestResult.Failure("请填写 API Key")) }
            return
        }
        if (baseUrl.isBlank()) {
            _uiState.update { it.copy(testResult = TestResult.Failure("请填写 Base URL")) }
            return
        }
        if (modelName.isBlank()) {
            _uiState.update { it.copy(testResult = TestResult.Failure("请填写模型名称")) }
            return
        }

        viewModelScope.launch {
            val config = AiConfigEntity(
                id = state.editingConfig?.id ?: 0,
                provider = provider,
                apiKey = apiKey,
                baseUrl = baseUrl,
                modelName = modelName,
                taskType = state.taskType,
                // 编辑时保留原来的启用状态，不能把关掉的配置悄悄重新启用开始计费
                isEnabled = state.editingConfig?.isEnabled ?: true,
                createdAt = state.editingConfig?.createdAt ?: System.currentTimeMillis()
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
        val state = _uiState.value
        val apiKey = state.apiKey.trim()
        val baseUrl = state.baseUrl.trim()
        val modelName = state.modelName.trim()

        if (apiKey.isBlank() || baseUrl.isBlank() || modelName.isBlank()) {
            _uiState.update {
                it.copy(testResult = TestResult.Failure("请先填写 API Key、Base URL 和模型名称"))
            }
            return
        }

        _uiState.update { it.copy(isTesting = true, testResult = null) }
        viewModelScope.launch {
            val result = aiConnectivityTester.testConnection(baseUrl, apiKey, modelName)
            _uiState.update {
                // 对话框已关闭则只复位状态，避免下次打开时残留旧结果
                if (it.showAddDialog) {
                    it.copy(isTesting = false, testResult = result)
                } else {
                    it.copy(isTesting = false)
                }
            }
        }
    }

    companion object {
        /**
         * 2026-09-30 起默认供应商切为 DeepSeek：
         * 识图用 deepseek-v4-flash-vision-exp（实验版视觉模型，价格约为豆包的 1/10），
         * 推理/出题用 deepseek-v4-flash；火山方舟保留为备选模板
         */
        val providerTemplates = listOf(
            ProviderTemplate(
                name = "DeepSeek",
                baseUrl = "https://api.deepseek.com",
                visionModel = "deepseek-v4-flash-vision-exp",
                textModel = "deepseek-v4-flash"
            ),
            ProviderTemplate(
                name = "火山方舟",
                baseUrl = "https://ark.cn-beijing.volces.com/api/v3",
                visionModel = "doubao-vision-pro-32k",
                textModel = "doubao-pro-32k"
            )
        )
    }
}

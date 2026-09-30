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
    val provider: String = AiConfigViewModel.defaultTemplate.name,
    val apiKey: String = "",
    val baseUrl: String = AiConfigViewModel.defaultTemplate.baseUrl,
    val modelName: String = AiConfigViewModel.defaultTemplate.visionModel,
    val taskType: AiTaskType = AiTaskType.OCR,
    // 最近一次应用的供应商模板；手改 provider/baseUrl 后清除，chip 选中态只认它而非自由文本
    val appliedTemplate: String? = AiConfigViewModel.defaultTemplate.name,
    // 用户是否手改过模型名（改过则任务切换不再自动跟随）
    val modelCustomized: Boolean = false,
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
                provider = AiConfigViewModel.defaultTemplate.name,
                apiKey = "",
                baseUrl = AiConfigViewModel.defaultTemplate.baseUrl,
                modelName = AiConfigViewModel.defaultModelFor(
                    AiConfigViewModel.defaultTemplate, AiTaskType.OCR
                ),
                taskType = AiTaskType.OCR,
                appliedTemplate = AiConfigViewModel.defaultTemplate.name,
                modelCustomized = false,
                testResult = null,
                isTesting = false
            )
        }
    }

    fun showEditDialog(config: AiConfigEntity) {
        // 仅当 provider+baseUrl 与模板完全一致时才点亮对应 chip，避免误导
        val matchedTemplate = AiConfigViewModel.providerTemplates
            .firstOrNull { it.name == config.provider && it.baseUrl == config.baseUrl }?.name
        _uiState.update {
            it.copy(
                showAddDialog = true,
                editingConfig = config,
                provider = config.provider,
                apiKey = config.apiKey,
                baseUrl = config.baseUrl,
                modelName = config.modelName,
                taskType = config.taskType,
                appliedTemplate = matchedTemplate,
                modelCustomized = true,
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
        // 手改提供商文本 = 脱离模板，chip 选中态随之熄灭
        _uiState.update { it.copy(provider = provider, appliedTemplate = null) }
    }

    /** 供应商模板：一键预填 Base URL 与当前任务的推荐模型（chip 点击为显式动作） */
    fun applyProviderTemplate(provider: String) {
        val template = AiConfigViewModel.providerTemplates.firstOrNull { it.name == provider } ?: return
        _uiState.update { st ->
            st.copy(
                provider = template.name,
                baseUrl = template.baseUrl,
                modelName = AiConfigViewModel.defaultModelFor(template, st.taskType),
                appliedTemplate = template.name,
                modelCustomized = false,
                testResult = null
            )
        }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey, testResult = null) }
    }

    fun updateBaseUrl(baseUrl: String) {
        _uiState.update { it.copy(baseUrl = baseUrl, appliedTemplate = null, testResult = null) }
    }

    fun updateModelName(modelName: String) {
        _uiState.update { it.copy(modelName = modelName, modelCustomized = true, testResult = null) }
    }

    fun updateTaskType(taskType: AiTaskType) {
        _uiState.update { st ->
            val template = AiConfigViewModel.providerTemplates
                .firstOrNull { it.name == st.appliedTemplate }
            if (template == null || st.modelCustomized) {
                // 未应用模板或用户手改过模型名：只切任务，不动模型
                return@update st.copy(taskType = taskType)
            }
            st.copy(
                taskType = taskType,
                modelName = AiConfigViewModel.defaultModelFor(template, taskType)
            )
        }
    }

    fun saveConfig() {
        val state = _uiState.value
        val provider = state.provider.trim().ifBlank { AiConfigViewModel.defaultTemplate.name }
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

        /** 默认模板：新增配置与兜底都以它为基准，单一来源 */
        val defaultTemplate: ProviderTemplate get() = providerTemplates.first()

        fun defaultModelFor(template: ProviderTemplate, taskType: AiTaskType): String =
            if (taskType == AiTaskType.OCR) template.visionModel else template.textModel
    }
}

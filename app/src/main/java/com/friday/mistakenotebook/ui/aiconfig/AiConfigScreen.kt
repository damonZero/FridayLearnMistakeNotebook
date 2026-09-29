package com.friday.mistakenotebook.ui.aiconfig

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friday.mistakenotebook.data.local.entity.AiConfigEntity
import com.friday.mistakenotebook.data.local.entity.AiTaskType
import com.friday.mistakenotebook.data.remote.TestResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConfigScreen(
    navController: NavController,
    viewModel: AiConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 配置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加配置")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.configs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🤖", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "暂无 AI 配置",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击右下角按钮添加",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.configs) { config ->
                    AiConfigCard(
                        config = config,
                        onEdit = { viewModel.showEditDialog(config) },
                        onDelete = { viewModel.deleteConfig(config) },
                        onToggle = { viewModel.toggleConfig(config) }
                    )
                }
            }
        }

        // 添加/编辑对话框
        if (uiState.showAddDialog) {
            AiConfigDialog(
                uiState = uiState,
                onProviderChange = { viewModel.updateProvider(it) },
                onApiKeyChange = { viewModel.updateApiKey(it) },
                onBaseUrlChange = { viewModel.updateBaseUrl(it) },
                onModelNameChange = { viewModel.updateModelName(it) },
                onTaskTypeChange = { viewModel.updateTaskType(it) },
                onTest = { viewModel.testConnection() },
                onSave = { viewModel.saveConfig() },
                onDismiss = { viewModel.hideDialog() }
            )
        }
    }
}

@Composable
fun AiConfigCard(
    config: AiConfigEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = config.provider,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = getTaskTypeName(config.taskType),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "模型: ${config.modelName}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Text(
                text = "API Key: ${maskApiKey(config.apiKey)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Text("编辑")
                }
                TextButton(onClick = { showDeleteDialog = true }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个 AI 配置吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConfigDialog(
    uiState: AiConfigUiState,
    onProviderChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelNameChange: (String) -> Unit,
    onTaskTypeChange: (AiTaskType) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (uiState.editingConfig != null) "编辑配置" else "添加配置")
        },
        text = {
            Column {
                // 提供商
                OutlinedTextField(
                    value = uiState.provider,
                    onValueChange = onProviderChange,
                    label = { Text("AI 提供商") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // API Key（默认遮蔽，点眼睛图标切换明文）
                var showApiKey by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = uiState.apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showApiKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "隐藏 API Key" else "显示 API Key"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Base URL
                OutlinedTextField(
                    value = uiState.baseUrl,
                    onValueChange = onBaseUrlChange,
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 模型名称
                OutlinedTextField(
                    value = uiState.modelName,
                    onValueChange = onModelNameChange,
                    label = { Text("模型名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 任务类型
                Text(
                    text = "任务类型",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiTaskType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.taskType == type,
                            onClick = { onTaskTypeChange(type) },
                            label = { Text(getTaskTypeName(type), fontSize = 12.sp) }
                        )
                    }
                }

                // 测试结果
                uiState.testResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val (message, color) = when (result) {
                        is TestResult.Success -> result.message to MaterialTheme.colorScheme.primary
                        is TestResult.Failure -> result.message to MaterialTheme.colorScheme.error
                    }
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = color
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = onTest,
                    enabled = !uiState.isTesting
                ) {
                    if (uiState.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("测试连接")
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onSave,
                    enabled = !uiState.isTesting && uiState.canSave
                ) {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

fun getTaskTypeName(type: AiTaskType): String {
    return when (type) {
        AiTaskType.OCR -> "文字识别"
        AiTaskType.ANALYSIS -> "错题分析"
        AiTaskType.GENERATE -> "生成相似题"
    }
}

/**
 * 遮蔽 API Key：长度足够时只露出末 4 位，否则全部遮蔽
 */
private fun maskApiKey(apiKey: String): String {
    return if (apiKey.length >= 8) "••••${apiKey.takeLast(4)}" else "••••"
}

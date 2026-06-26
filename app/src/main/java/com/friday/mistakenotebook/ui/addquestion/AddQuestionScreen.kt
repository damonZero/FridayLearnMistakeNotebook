package com.friday.mistakenotebook.ui.addquestion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friday.mistakenotebook.data.local.entity.ErrorType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestionScreen(
    navController: NavController,
    subjectId: Long? = null,
    viewModel: AddQuestionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(subjectId) {
        if (subjectId != null && subjectId > 0) {
            viewModel.selectSubject(subjectId)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加错题", fontWeight = FontWeight.Bold) },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 科目选择
            Text(
                text = "选择科目",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.subjects.forEach { subject ->
                    FilterChip(
                        selected = uiState.selectedSubjectId == subject.id,
                        onClick = { viewModel.selectSubject(subject.id) },
                        label = { Text("${subject.icon} ${subject.name}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 题目内容
            OutlinedTextField(
                value = uiState.content,
                onValueChange = { viewModel.updateContent(it) },
                label = { Text("题目内容 *") },
                placeholder = { Text("请输入题目内容") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 正确答案
            OutlinedTextField(
                value = uiState.answer,
                onValueChange = { viewModel.updateAnswer(it) },
                label = { Text("正确答案") },
                placeholder = { Text("请输入正确答案") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 我的答案
            OutlinedTextField(
                value = uiState.userAnswer,
                onValueChange = { viewModel.updateUserAnswer(it) },
                label = { Text("我的答案") },
                placeholder = { Text("请输入你的答案") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 错误类型
            Text(
                text = "错误类型",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ErrorType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.errorType == type,
                        onClick = { viewModel.updateErrorType(type) },
                        label = { Text(getErrorTypeName(type)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 保存按钮
            Button(
                onClick = { viewModel.saveQuestion() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("保存错题", fontSize = 16.sp)
                }
            }
        }

        // 错误提示
        uiState.errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("提示") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

fun getErrorTypeName(type: ErrorType): String {
    return when (type) {
        ErrorType.CARELESS -> "粗心"
        ErrorType.CONCEPTUAL -> "概念错"
        ErrorType.METHOD -> "方法错"
        ErrorType.CALCULATION -> "计算错"
        ErrorType.UNKNOWN -> "未知"
    }
}

package com.friday.mistakenotebook.ui.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friday.mistakenotebook.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    navController: NavController,
    viewModel: PracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRefreshDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("相似题练习", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.questions.isNotEmpty()) {
                        IconButton(
                            onClick = { showRefreshDialog = true },
                            enabled = !uiState.isLoading
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新举一反三")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在生成相似题…",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            uiState.error != null -> {
                PracticeErrorContent(
                    modifier = Modifier.padding(padding),
                    error = uiState.error!!,
                    onRetry = { viewModel.refreshQuestions() }
                )
            }
            uiState.isCompleted -> {
                PracticeCompleteContent(
                    modifier = Modifier.padding(padding),
                    totalCount = uiState.questions.size,
                    knownCount = uiState.knownCount,
                    onPracticeAgain = { viewModel.restart() },
                    onBack = { navController.popBackStack() }
                )
            }
            // 防御：没有可练习的题目时不渲染答题区，避免越界
            uiState.questions.isEmpty() -> {
                PracticeErrorContent(
                    modifier = Modifier.padding(padding),
                    error = "没有可练习的题目",
                    onRetry = { viewModel.refreshQuestions() }
                )
            }
            else -> {
                PracticeContent(
                    modifier = Modifier.padding(padding),
                    uiState = uiState,
                    onShowAnswer = { viewModel.showAnswer() },
                    onKnown = { viewModel.markKnown() },
                    onUnknown = { viewModel.markUnknown() }
                )
            }
        }
    }

    if (showRefreshDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshDialog = false },
            title = { Text("刷新举一反三？") },
            text = { Text("将重新生成一组新的题目，并覆盖当前保存的内容。") },
            confirmButton = {
                TextButton(onClick = {
                    showRefreshDialog = false
                    viewModel.refreshQuestions()
                }) { Text("刷新") }
            },
            dismissButton = {
                TextButton(onClick = { showRefreshDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun PracticeContent(
    modifier: Modifier = Modifier,
    uiState: PracticeUiState,
    onShowAnswer: () -> Unit,
    onKnown: () -> Unit,
    onUnknown: () -> Unit
) {
    val question = uiState.questions[uiState.currentIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LinearProgressIndicator(
            progress = (uiState.currentIndex + 1).toFloat() / uiState.questions.size,
            modifier = Modifier.fillMaxWidth(),
            color = Secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "第 ${uiState.currentIndex + 1} / ${uiState.questions.size} 题",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // 变化梯度标签：让学生知道这题在从哪个角度考同一个知识点
                    if (question.variation.isNotBlank()) {
                        Text(
                            text = when (question.variation) {
                                "同型巩固" -> "🟩 同型巩固"
                                "情境变换" -> "🟨 情境变换"
                                "逆向综合" -> "🟦 逆向综合"
                                else -> question.variation
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = "题目",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = question.content,
                        fontSize = 18.sp,
                        lineHeight = 28.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isAnswerShown) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MasteredGreen.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "答案",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MasteredGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.answer.ifBlank { "暂无答案" },
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = onShowAnswer,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                ) {
                    Text("看答案", fontSize = 16.sp)
                }
            }
        }

        Text(
            text = "练习不计入复习计划",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isAnswerShown) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onUnknown,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeedReviewRed)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("不会", fontSize = 16.sp)
                }
                Button(
                    onClick = onKnown,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MasteredGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("会了", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun PracticeErrorContent(
    modifier: Modifier = Modifier,
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "😵", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "相似题生成失败",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
fun PracticeCompleteContent(
    modifier: Modifier = Modifier,
    totalCount: Int,
    knownCount: Int,
    onPracticeAgain: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🎉", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "练习完成！",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "共 $totalCount 题，会了 $knownCount 题",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "练习不计入复习计划",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) {
                Text("返回")
            }
            Button(onClick = onPracticeAgain) {
                Text("再练一组")
            }
        }
    }
}

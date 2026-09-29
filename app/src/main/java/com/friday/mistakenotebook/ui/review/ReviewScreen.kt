package com.friday.mistakenotebook.ui.review

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friday.mistakenotebook.ui.navigation.Screen
import com.friday.mistakenotebook.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    // 会话进行中且未完成时，拦截系统返回键
    val inSession = !uiState.isLoading && !uiState.isCompleted && uiState.questions.isNotEmpty()
    BackHandler(enabled = inSession) {
        showExitDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("复习", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.isCompleted -> {
                ReviewCompleteContent(
                    modifier = Modifier.padding(padding),
                    onAddQuestion = { navController.navigate(Screen.AddQuestion.createRoute()) },
                    onGoToList = { navController.navigate(Screen.QuestionList.createRoute()) }
                )
            }
            uiState.questions.isEmpty() -> {
                EmptyReviewContent(
                    modifier = Modifier.padding(padding),
                    onAddQuestion = { navController.navigate(Screen.AddQuestion.createRoute()) },
                    onGoToList = { navController.navigate(Screen.QuestionList.createRoute()) }
                )
            }
            else -> {
                ReviewContent(
                    modifier = Modifier.padding(padding),
                    uiState = uiState,
                    onShowAnswer = { viewModel.showAnswer() },
                    onCorrect = { viewModel.markCorrect() },
                    onIncorrect = { viewModel.markIncorrect() }
                )
            }
        }
    }

    if (showExitDialog && inSession) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("退出复习？") },
            text = { Text("已作答的题目已保存，退出将结束本次复习。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("继续复习")
                }
            }
        )
    }
}

@Composable
fun ReviewContent(
    modifier: Modifier = Modifier,
    uiState: ReviewUiState,
    onShowAnswer: () -> Unit,
    onCorrect: () -> Unit,
    onIncorrect: () -> Unit
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

        // 内容区域可滚动：长题目不被裁切，底部按钮永远可达
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

                    if (question.userAnswer.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "我的答案",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeedReviewRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = question.userAnswer,
                            fontSize = 16.sp,
                            color = NeedReviewRed
                        )
                    }
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
                            text = "正确答案",
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
                    Text("显示答案", fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isAnswerShown) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onIncorrect,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = NeedReviewRed)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("还错", fontSize = 16.sp)
                }
                Button(
                    onClick = onCorrect,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSubmitting,
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
fun EmptyReviewContent(
    modifier: Modifier = Modifier,
    onAddQuestion: () -> Unit,
    onGoToList: () -> Unit
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
            text = "今天没有需要复习的题目",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "先添加错题，系统才会生成复习任务。",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onGoToList) {
                Text("查看错题")
            }
            Button(onClick = onAddQuestion) {
                Text("添加错题")
            }
        }
    }
}

@Composable
fun ReviewCompleteContent(
    modifier: Modifier = Modifier,
    onAddQuestion: () -> Unit,
    onGoToList: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🎊", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "复习完成！",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "今天的复习已经结束，可以继续补充错题。",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onGoToList) {
                Text("查看错题")
            }
            Button(onClick = onAddQuestion) {
                Text("继续添加")
            }
        }
    }
}

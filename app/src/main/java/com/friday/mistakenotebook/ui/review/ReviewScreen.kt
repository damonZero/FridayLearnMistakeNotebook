package com.friday.mistakenotebook.ui.review

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
import com.friday.mistakenotebook.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                ReviewCompleteContent(modifier = Modifier.padding(padding))
            }
            uiState.questions.isEmpty() -> {
                EmptyReviewContent(modifier = Modifier.padding(padding))
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
        // 进度指示
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

        // 题目卡片
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

        // 答案区域
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

        Spacer(modifier = Modifier.weight(1f))

        // 操作按钮
        if (uiState.isAnswerShown) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onIncorrect,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeedReviewRed)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("还错", fontSize = 16.sp)
                }
                Button(
                    onClick = onCorrect,
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
fun EmptyReviewContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🎉", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无待复习题目",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "快去添加错题吧！",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ReviewCompleteContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
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
            text = "太棒了，继续保持！",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

package com.friday.mistakenotebook.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.friday.mistakenotebook.print.SheetGenerateState
import com.friday.mistakenotebook.print.printExerciseSheet
import com.friday.mistakenotebook.print.shareSheetFile
import com.friday.mistakenotebook.ui.navigation.Screen
import com.friday.mistakenotebook.ui.theme.*
import java.io.File
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState by viewModel.sheetState.collectAsState()
    val context = LocalContext.current
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
                actions = {
                    // 打印今日待复习卷：全部到期错题（含已保存举一反三）一卷打尽
                    IconButton(
                        onClick = { viewModel.printDueSheet() },
                        enabled = !uiState.isLoading && !uiState.isSubmitting
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "打印今日待复习卷")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                    onIncorrect = { viewModel.markIncorrect() },
                    onPractice = {
                        uiState.questions.getOrNull(uiState.currentIndex)?.let { question ->
                            navController.navigate(Screen.Practice.createRoute(question.id))
                        }
                    }
                )
            }
        }
    }

    // 今日待复习卷生成状态弹窗
    when (val st = sheetState) {
        is SheetGenerateState.Generating -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("正在生成练习卷") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(st.progress, fontSize = 14.sp)
                    }
                },
                confirmButton = {}
            )
        }
        is SheetGenerateState.Ready -> {
            AlertDialog(
                onDismissRequest = { viewModel.consumeSheetState() },
                title = { Text("今日待复习卷已生成") },
                text = {
                    Column {
                        Text(
                            "全部到期错题（含已保存的举一反三）已生成练习卷与答案卷。\n分享到微信打印；孩子做完后在会话里逐题标\"会了/还错\"回录。",
                            fontSize = 14.sp
                        )
                        st.note?.let {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        shareSheetFile(context, st.files.exerciseSheet)
                    }) { Text("发练习卷") }
                },
                dismissButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(onClick = {
                            shareSheetFile(context, st.files.answerSheet)
                        }) { Text("发答案卷") }
                        Row {
                            TextButton(onClick = {
                                printExerciseSheet(context, st.files.exerciseSheet)
                                viewModel.consumeSheetState()
                            }) { Text("直接打印") }
                            TextButton(onClick = { viewModel.consumeSheetState() }) { Text("完成") }
                        }
                    }
                }
            )
        }
        is SheetGenerateState.Failed -> {
            AlertDialog(
                onDismissRequest = { viewModel.consumeSheetState() },
                title = { Text("生成失败") },
                text = { Text(st.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.consumeSheetState() }) { Text("知道了") }
                }
            )
        }
        SheetGenerateState.Idle -> {}
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
    onIncorrect: () -> Unit,
    onPractice: () -> Unit
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

                    // 拍照识别保存的题目原图，放在题面文字上方；无图或文件已不存在（如换机恢复）不占位
                    question.imagePath?.let { path ->
                        val imageFile = remember(path) { File(path) }
                        if (imageFile.exists()) {
                            Image(
                                painter = rememberAsyncImagePainter(imageFile),
                                contentDescription = "题目原图",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

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
            // 举一反三：看完答案顺手练相似题，巩固后再自评
            OutlinedButton(
                onClick = onPractice,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("举一反三 · 练相似题", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

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

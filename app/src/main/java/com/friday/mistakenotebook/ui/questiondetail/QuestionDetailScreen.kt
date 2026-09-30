package com.friday.mistakenotebook.ui.questiondetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.friday.mistakenotebook.data.remote.KnowledgeAnalysis
import com.friday.mistakenotebook.domain.algorithm.SpacedRepetitionAlgorithm
import com.friday.mistakenotebook.print.SheetGenerateState
import com.friday.mistakenotebook.print.printExerciseSheet
import com.friday.mistakenotebook.print.shareSheetFile
import com.friday.mistakenotebook.ui.addquestion.getErrorTypeName
import com.friday.mistakenotebook.ui.navigation.Screen
import com.friday.mistakenotebook.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    navController: NavController,
    viewModel: QuestionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState by viewModel.sheetState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // 删除完成后返回上一页
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("错题详情", fontWeight = FontWeight.Bold) },
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
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.question == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "找不到这道错题",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            else -> {
                QuestionDetailContent(
                    modifier = Modifier.padding(padding),
                    uiState = uiState,
                    dateFormat = dateFormat,
                    onAnalyze = { viewModel.analyzeKnowledge() },
                    onStartPractice = {
                        navController.navigate(Screen.Practice.createRoute(uiState.question!!.id))
                    },
                    onGenerateSheet = { viewModel.generatePracticeSheet() },
                    onRequestDelete = { showDeleteDialog = true }
                )
            }
        }
    }

    if (showDeleteDialog && uiState.question != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("删除后无法恢复，确定要删除这道错题吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteQuestion()
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

    // 练习卷生成状态弹窗
    val sheetContext = LocalContext.current
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
                title = { Text("练习卷已生成") },
                text = {
                    Column {
                        Text(
                            "已生成练习卷（孩子做）与答案卷（家长留存）两个 PDF。\n分别分享到微信发送电脑/打印 APP 即可打印。",
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
                        shareSheetFile(sheetContext, st.files.exerciseSheet)
                    }) { Text("发练习卷") }
                },
                dismissButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(onClick = {
                            shareSheetFile(sheetContext, st.files.answerSheet)
                        }) { Text("发答案卷") }
                        Row {
                            TextButton(onClick = {
                                printExerciseSheet(sheetContext, st.files.exerciseSheet)
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
}

@Composable
fun QuestionDetailContent(
    modifier: Modifier = Modifier,
    uiState: QuestionDetailUiState,
    dateFormat: SimpleDateFormat,
    onAnalyze: () -> Unit,
    onStartPractice: () -> Unit,
    onGenerateSheet: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val question = uiState.question ?: return
    val masteryColor = when {
        question.leitnerBox >= 4 -> MasteredGreen
        question.leitnerBox >= 2 -> LearningYellow
        else -> NeedReviewRed
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 题目原图
        question.imagePath?.let { path ->
            val imageFile = remember(path) { File(path) }
            if (imageFile.exists()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = rememberAsyncImagePainter(imageFile),
                        contentDescription = "题目原图",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 题目内容
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
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

        if (question.userAnswer.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailInfoCard(
                title = "我的答案",
                content = question.userAnswer,
                titleColor = NeedReviewRed,
                contentColor = NeedReviewRed
            )
        }

        if (question.answer.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailInfoCard(
                title = "正确答案",
                content = question.answer,
                titleColor = MasteredGreen,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 复习状态
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "复习状态",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = getErrorTypeName(question.errorType),
                        fontSize = 12.sp,
                        color = masteryColor
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "莱特纳盒",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "第 ${question.leitnerBox} 盒（${SpacedRepetitionAlgorithm.getBoxDescription(question.leitnerBox)}）",
                        fontSize = 14.sp,
                        color = masteryColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "掌握程度",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${SpacedRepetitionAlgorithm.getMasteryPercentage(question.leitnerBox)}%",
                        fontSize = 14.sp,
                        color = masteryColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "复习次数",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${question.reviewCount} 次",
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "下次复习时间",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = dateFormat.format(Date(question.nextReviewDate)),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AI 知识点分析
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "AI 知识点分析",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                when {
                    uiState.isAnalyzing -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "正在分析…",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    uiState.analysisError != null -> {
                        Text(
                            text = uiState.analysisError,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    // 本次会话内分析过：展示完整结果（知识点 chips + 错因 + 分析）
                    uiState.analysis != null -> {
                        KnowledgeAnalysisResult(analysis = uiState.analysis!!)
                    }
                    // 否则展示上次已保存的分析摘要
                    question.aiAnalysis != null -> {
                        Text(
                            text = question.aiAnalysis,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                    else -> {
                        Text(
                            text = "还没有分析过，让 AI 帮你找出这道题考查的知识点和错误原因吧。",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onAnalyze,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isAnalyzing
                ) {
                    Text(
                        text = if (uiState.analysis == null && question.aiAnalysis == null) {
                            "AI 分析知识点"
                        } else {
                            "重新分析"
                        },
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 生成相似题练习
        Button(
            onClick = onStartPractice,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Secondary)
        ) {
            Text("生成相似题练习", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 生成纸质练习卷
        OutlinedButton(
            onClick = onGenerateSheet,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🖨 生成纸质练习卷（含举一反三）", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 删除错题
        TextButton(
            onClick = onRequestDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "删除错题",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DetailInfoCard(
    title: String,
    content: String,
    titleColor: Color,
    contentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = titleColor.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = contentColor
            )
        }
    }
}

@Composable
fun KnowledgeAnalysisResult(analysis: KnowledgeAnalysis) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (analysis.knowledgePoints.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(analysis.knowledgePoints) { point ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(point, fontSize = 12.sp) }
                    )
                }
            }
        }
        if (analysis.errorTypeGuess.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "错因猜测：${analysis.errorTypeGuess}",
                fontSize = 14.sp,
                color = NeedReviewRed
            )
        }
        if (analysis.analysis.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = analysis.analysis,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

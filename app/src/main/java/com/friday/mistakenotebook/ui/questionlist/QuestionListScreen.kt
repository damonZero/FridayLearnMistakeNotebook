package com.friday.mistakenotebook.ui.questionlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friday.mistakenotebook.print.printExerciseSheet
import com.friday.mistakenotebook.print.shareSheetFile
import com.friday.mistakenotebook.print.SheetGenerateState
import com.friday.mistakenotebook.domain.algorithm.SpacedRepetitionAlgorithm
import com.friday.mistakenotebook.domain.model.Question
import com.friday.mistakenotebook.ui.addquestion.getErrorTypeName
import com.friday.mistakenotebook.ui.navigation.Screen
import com.friday.mistakenotebook.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionListScreen(
    navController: NavController,
    subjectId: Long? = null,
    viewModel: QuestionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState by viewModel.sheetState.collectAsState()
    val context = LocalContext.current
    var showSheetConfig by remember { mutableStateOf(false) }
    var includeSimilar by remember { mutableStateOf(false) }
    var forceRefresh by remember { mutableStateOf(false) }
    // 多选操作栏选择的动作：生成完成后自动触发（share / print）
    var pendingAction by remember { mutableStateOf<String?>(null) }

    // 生成完成 → 自动执行选定的动作（分享练习卷 / 直接打印）
    LaunchedEffect(sheetState, pendingAction) {
        val st = sheetState
        if (st is SheetGenerateState.Ready && pendingAction != null) {
            when (pendingAction) {
                "share" -> shareSheetFile(context, st.files.exerciseSheet)
                "print" -> printExerciseSheet(context, st.files.exerciseSheet)
            }
            pendingAction = null
        }
    }

    LaunchedEffect(subjectId) {
        viewModel.selectSubject(if (subjectId == -1L) null else subjectId)
    }

    Scaffold(
        bottomBar = {
            if (uiState.selectionMode) {
                // 多选操作栏：全选（当前筛选/分类结果）、分享、打印
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.selectAllVisible() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("全选")
                    }
                    Button(
                        onClick = {
                            pendingAction = "share"
                            // 每次打开配置弹窗重置默认，防止上次的"强制刷新"静默覆写缓存
                            includeSimilar = false
                            forceRefresh = false
                            showSheetConfig = true
                        },
                        enabled = uiState.selectedIds.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("分享")
                    }
                    Button(
                        onClick = {
                            pendingAction = "print"
                            includeSimilar = false
                            forceRefresh = false
                            showSheetConfig = true
                        },
                        enabled = uiState.selectedIds.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("打印")
                    }
                }
            }
        },
        topBar = {
            if (uiState.selectionMode) {
                TopAppBar(
                    title = { Text("已选 ${uiState.selectedIds.size} 题", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "退出多选")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("错题列表", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.enterSelectionMode() }) {
                            Icon(Icons.Default.Checklist, contentDescription = "批量选择打印")
                        }
                        if (uiState.searchQuery.isNotBlank() || uiState.selectedSubjectId != null) {
                            TextButton(onClick = { viewModel.clearFilters() }) {
                                Text("清除筛选")
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Text(
                text = "科目筛选",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedSubjectId == null,
                        onClick = { viewModel.selectSubject(null) },
                        label = { Text("全部") }
                    )
                }
                items(uiState.subjects) { subject ->
                    FilterChip(
                        selected = uiState.selectedSubjectId == subject.id,
                        onClick = { viewModel.selectSubject(subject.id) },
                        label = { Text(subject.name) }
                    )
                }
            }

            // 排序与分组：一行横向可滑，标签短、触控友好
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "排序",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                QuestionSort.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.sortMode == mode,
                        onClick = { viewModel.setSortMode(mode) },
                        label = { Text(mode.label, fontSize = 12.sp) }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "分组",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                QuestionGroup.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.groupMode == mode,
                        onClick = { viewModel.setGroupMode(mode) },
                        label = { Text(mode.label, fontSize = 12.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("搜索题目或答案") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.questions.isEmpty()) {
                EmptyQuestionList(
                    hasFilter = uiState.searchQuery.isNotBlank() || uiState.selectedSubjectId != null,
                    onAddQuestion = { navController.navigate(Screen.AddQuestion.createRoute()) },
                    onClearFilters = { viewModel.clearFilters() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.sections.forEach { section ->
                        if (section.title.isNotBlank()) {
                            item(key = "header_${section.title}") {
                                Text(
                                    text = "${section.title} · ${section.questions.size} 题",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        items(
                            section.questions,
                            key = { "q_${section.title}_${it.id}" }
                        ) { question ->
                            if (uiState.selectionMode) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = question.id in uiState.selectedIds,
                                        onCheckedChange = { viewModel.toggleSelection(question.id) }
                                    )
                                    QuestionCard(
                                        question = question,
                                        onClick = { viewModel.toggleSelection(question.id) },
                                        onDelete = { viewModel.deleteQuestion(question.id) }
                                    )
                                }
                            } else {
                                QuestionCard(
                                    question = question,
                                    onClick = {
                                        navController.navigate(Screen.QuestionDetail.createRoute(question.id))
                                    },
                                    onDelete = { viewModel.deleteQuestion(question.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 练习卷生成配置
    if (showSheetConfig) {
        AlertDialog(
            onDismissRequest = { showSheetConfig = false },
            title = { Text(if (pendingAction == "print") "生成并打印（${uiState.selectedIds.size} 题）" else "生成并分享（${uiState.selectedIds.size} 题）") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includeSimilar,
                            onCheckedChange = {
                                includeSimilar = it
                                if (!it) forceRefresh = false
                            }
                        )
                        Text("含举一反三（无缓存的题现场生成）", fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = forceRefresh,
                            onCheckedChange = { forceRefresh = it },
                            enabled = includeSimilar
                        )
                        Text("强制重新生成（忽略已保存）", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "答案卷（家长留存）会同时生成，含 AI 分析。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSheetConfig = false
                    viewModel.generateSheet(includeSimilar, forceRefresh)
                }) { Text("开始生成") }
            },
            dismissButton = {
                TextButton(onClick = { showSheetConfig = false }) { Text("取消") }
            }
        )
    }

    // 练习卷生成状态弹窗
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
}

@Composable
fun EmptyQuestionList(
    hasFilter: Boolean,
    onAddQuestion: () -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = if (hasFilter) "🔎" else "📝", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (hasFilter) "没有符合条件的错题" else "暂无错题",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasFilter) "试试清除筛选，或者换个关键词" else "先添加一道错题开始使用",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (hasFilter) {
                OutlinedButton(onClick = onClearFilters) {
                    Text("清除筛选")
                }
            }
            Button(onClick = onAddQuestion) {
                Text("添加错题")
            }
        }
    }
}

@Composable
fun QuestionCard(
    question: Question,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val masteryColor = when {
        question.leitnerBox >= 4 -> MasteredGreen
        question.leitnerBox >= 2 -> LearningYellow
        else -> NeedReviewRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(getErrorTypeName(question.errorType), fontSize = 12.sp) }
                    )
                    if (!question.knowledgePoint.isNullOrBlank()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("📚 ${question.knowledgePoint}", fontSize = 12.sp) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = SpacedRepetitionAlgorithm.getBoxDescription(question.leitnerBox),
                        fontSize = 12.sp,
                        color = masteryColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Circle,
                        contentDescription = null,
                        tint = masteryColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.content,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (question.answer.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "答案: ${question.answer}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "复习 ${question.reviewCount} 次",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这道错题吗？") },
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


package com.friday.mistakenotebook.ui.stats

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friday.mistakenotebook.ui.home.StatCard
import com.friday.mistakenotebook.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学习统计", fontWeight = FontWeight.Bold) },
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 掌握率卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Primary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "整体掌握率",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.masteryPercentage}%",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = uiState.masteryPercentage / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 统计卡片
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "错题总数",
                        value = uiState.totalQuestions.toString(),
                        icon = "📊",
                        color = SubjectMath
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "已掌握",
                        value = uiState.masteredQuestions.toString(),
                        icon = "✅",
                        color = MasteredGreen
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "待复习",
                        value = uiState.todayReviewCount.toString(),
                        icon = "📝",
                        color = NeedReviewRed
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 掌握程度分布
                Text(
                    text = "掌握程度分布",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 亮黄在浅色主题白底上对比度不足，浅色主题改用深琥珀色
                val learningColor = if (isSystemInDarkTheme()) LearningYellow else LearningAmber

                MasteryLevelItem(
                    level = "新题/答错",
                    color = NeedReviewRed,
                    percentage = uiState.distribution.newPercentage
                )
                MasteryLevelItem(
                    level = "学习中",
                    color = learningColor,
                    percentage = uiState.distribution.learningPercentage
                )
                MasteryLevelItem(
                    level = "已掌握",
                    color = MasteredGreen,
                    percentage = uiState.distribution.masteredPercentage
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 学习建议
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💡 学习建议",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                uiState.masteryPercentage >= 80 -> "太棒了！继续保持，定期复习巩固知识。"
                                uiState.masteryPercentage >= 50 -> "做得不错！建议每天坚持复习，提高掌握率。"
                                uiState.totalQuestions == 0 -> "快去添加错题开始学习吧！"
                                else -> "加油！每天复习几道题，进步会很明显。"
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MasteryLevelItem(
    level: String,
    color: Color,
    percentage: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Circle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = level,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$percentage%",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

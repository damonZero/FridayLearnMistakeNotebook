package com.friday.mistakenotebook.ui.home

import androidx.compose.foundation.clickable
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
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "周周错题本",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
            // 欢迎卡片：点击直达今日复习
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.Review.route) },
                colors = CardDefaults.cardColors(containerColor = Primary)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "📚 今日学习",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "今日待复习 ${uiState.todayReviewCount} 道 · 点我开始复习",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        fontSize = 14.sp
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
                    title = "待复习",
                    value = uiState.todayReviewCount.toString(),
                    icon = "📝",
                    color = NeedReviewRed,
                    onClick = { navController.navigate(Screen.Review.route) }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "错题总数",
                    value = uiState.totalQuestionCount.toString(),
                    icon = "📊",
                    color = SubjectMath,
                    onClick = { navController.navigate(Screen.QuestionList.createRoute()) }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "已掌握",
                    value = uiState.masteredCount.toString(),
                    icon = "✅",
                    color = MasteredGreen,
                    onClick = { navController.navigate(Screen.Stats.route) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 快速操作按钮
            Text(
                text = "快速操作",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    text = "开始复习",
                    icon = "🔄",
                    color = Secondary
                ) {
                    navController.navigate(Screen.Review.route)
                }
                ActionButton(
                    modifier = Modifier.weight(1f),
                    text = "添加错题",
                    icon = "➕",
                    color = Primary
                ) {
                    navController.navigate(Screen.AddQuestion.createRoute())
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    text = "错题列表",
                    icon = "📋",
                    color = SubjectMath
                ) {
                    navController.navigate(Screen.QuestionList.createRoute())
                }
                ActionButton(
                    modifier = Modifier.weight(1f),
                    text = "学习统计",
                    icon = "📈",
                    color = SubjectEnglish
                ) {
                    navController.navigate(Screen.Stats.route)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 学习提示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 学习小贴士",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "每天坚持复习错题，根据遗忘曲线安排复习时间，学习效果会更好哦！",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text = "$icon $text", fontSize = 16.sp)
    }
}

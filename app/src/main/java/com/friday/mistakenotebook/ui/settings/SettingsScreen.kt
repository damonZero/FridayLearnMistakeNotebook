package com.friday.mistakenotebook.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.friday.mistakenotebook.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
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
        ) {
            // AI 配置部分
            SettingsSection(title = "AI 配置") {
                SettingsItem(
                    icon = Icons.Default.SmartToy,
                    title = "AI 模型配置",
                    subtitle = "配置 OCR、分析、出题的 AI 模型",
                    onClick = { navController.navigate(Screen.AiConfig.route) }
                )
                SettingsItem(
                    icon = Icons.Default.Analytics,
                    title = "AI 使用统计",
                    subtitle = "查看 Token 消耗和费用",
                    onClick = { navController.navigate(Screen.AiUsage.route) }
                )
            }

            // 数据管理部分
            SettingsSection(title = "数据管理") {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "数据备份",
                    subtitle = "备份和恢复错题数据",
                    onClick = { navController.navigate(Screen.Backup.route) }
                )
                SettingsItem(
                    icon = Icons.Default.EmojiEvents,
                    title = "成就系统",
                    subtitle = "查看学习成就",
                    onClick = { navController.navigate(Screen.Achievement.route) }
                )
            }

            // 提醒设置部分
            SettingsSection(title = "提醒设置") {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "复习提醒",
                    subtitle = "设置每日复习提醒时间",
                    onClick = { /* TODO: 提醒设置 */ }
                )
            }

            // 关于部分
            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "关于周周错题本",
                    subtitle = "版本 1.0.0",
                    onClick = { /* TODO: 关于页面 */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                content()
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
    if (title != "关于周周错题本") {
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

package com.friday.mistakenotebook.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.friday.mistakenotebook.data.reminder.ReminderNotifier
import com.friday.mistakenotebook.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val reminderState by viewModel.reminderSettings.collectAsState()

    var showReminderDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.setReminderEnabled(true)
        // 无论授权与否都打开配置弹窗；未授权时弹窗内有提示
        showReminderDialog = true
    }

    fun openReminderSettings() {
        if (ReminderNotifier.needsPermissionRequest(context)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showReminderDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    subtitle = if (reminderState.enabled) {
                        "已开启 · 每天 %02d:%02d 提醒当天到期的错题".format(
                            reminderState.hour,
                            reminderState.minute
                        )
                    } else {
                        "未开启，点按开启每日复习提醒"
                    },
                    onClick = { openReminderSettings() }
                )
            }

            // 关于部分
            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "关于周周错题本",
                    subtitle = "版本 1.0.0",
                    onClick = {
                        scope.launch { snackbarHostState.showSnackbar("功能开发中") }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 复习提醒配置弹窗
    if (showReminderDialog) {
        val permissionMissing = ReminderNotifier.needsPermissionRequest(context)
        val timePickerState = rememberTimePickerState(
            initialHour = reminderState.hour,
            initialMinute = reminderState.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("每日复习提醒") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = reminderState.enabled,
                            onCheckedChange = { viewModel.setReminderEnabled(it) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (reminderState.enabled) "已开启" else "已关闭",
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TimePicker(state = timePickerState)
                    if (permissionMissing) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "尚未授予通知权限，开启后将看不到提醒，请点「去授权」",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // 保存时间并按需重排调度
                    viewModel.setReminderTime(timePickerState.hour, timePickerState.minute)
                    if (reminderState.enabled && permissionMissing) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    showReminderDialog = false
                }) { Text(if (permissionMissing) "保存并去授权" else "保存") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) { Text("取消") }
            }
        )
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

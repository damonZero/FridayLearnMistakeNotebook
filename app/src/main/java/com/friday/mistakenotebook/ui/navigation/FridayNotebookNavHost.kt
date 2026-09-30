package com.friday.mistakenotebook.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.friday.mistakenotebook.ui.home.HomeScreen
import com.friday.mistakenotebook.ui.subject.SubjectsScreen
import com.friday.mistakenotebook.ui.review.ReviewScreen
import com.friday.mistakenotebook.ui.settings.SettingsScreen
import com.friday.mistakenotebook.ui.addquestion.AddQuestionScreen
import com.friday.mistakenotebook.ui.camera.CameraScreen
import com.friday.mistakenotebook.ui.questiondetail.QuestionDetailScreen
import com.friday.mistakenotebook.ui.questionlist.QuestionListScreen
import com.friday.mistakenotebook.ui.practice.PracticeScreen
import com.friday.mistakenotebook.ui.stats.StatsScreen
import com.friday.mistakenotebook.ui.aiconfig.AiConfigScreen
import com.friday.mistakenotebook.ui.aiusage.AiUsageScreen
import com.friday.mistakenotebook.ui.backup.BackupScreen
import com.friday.mistakenotebook.ui.achievement.AchievementScreen

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridayNotebookNavHost() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home, "首页", Icons.Default.Home),
        BottomNavItem(Screen.Subjects, "科目", Icons.Default.MenuBook),
        BottomNavItem(Screen.Review, "复习", Icons.Default.Replay),
        BottomNavItem(Screen.Settings, "设置", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(Screen.Subjects.route) {
                SubjectsScreen(navController = navController)
            }
            composable(Screen.Review.route) {
                ReviewScreen(navController = navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
            composable(Screen.AddQuestion.route) { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getString("subjectId")?.toLongOrNull()
                AddQuestionScreen(
                    navController = navController,
                    subjectId = if (subjectId == -1L) null else subjectId,
                    backStackEntry = backStackEntry
                )
            }
            composable(Screen.Camera.route) {
                CameraScreen(
                    navController = navController,
                    onOcrComplete = { text ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("ocr_result", text)
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.QuestionList.route) { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getString("subjectId")?.toLongOrNull()
                QuestionListScreen(
                    navController = navController,
                    subjectId = if (subjectId == -1L) null else subjectId
                )
            }
            composable(
                Screen.QuestionDetail.route,
                arguments = listOf(navArgument("questionId") { type = NavType.LongType })
            ) {
                QuestionDetailScreen(navController = navController)
            }
            composable(
                Screen.Practice.route,
                arguments = listOf(navArgument("questionId") { type = NavType.LongType })
            ) {
                PracticeScreen(navController = navController)
            }
            composable(Screen.Stats.route) {
                StatsScreen(navController = navController)
            }
            composable(Screen.AiConfig.route) {
                AiConfigScreen(navController = navController)
            }
            composable(Screen.AiUsage.route) {
                AiUsageScreen(navController = navController)
            }
            composable(Screen.Backup.route) {
                BackupScreen(navController = navController)
            }
            composable(Screen.Achievement.route) {
                AchievementScreen(navController = navController)
            }
        }
    }
}



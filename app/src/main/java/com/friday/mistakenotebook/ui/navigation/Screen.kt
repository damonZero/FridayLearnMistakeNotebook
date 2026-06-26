package com.friday.mistakenotebook.ui.navigation

/**
 * 屏幕路由定义
 */
sealed class Screen(val route: String) {
    // 主页面
    object Home : Screen("home")
    object Subjects : Screen("subjects")
    object Review : Screen("review")
    object Settings : Screen("settings")

    // 详情页面
    object SubjectDetail : Screen("subject/{subjectId}") {
        fun createRoute(subjectId: Long) = "subject/$subjectId"
    }

    object QuestionDetail : Screen("question/{questionId}") {
        fun createRoute(questionId: Long) = "question/$questionId"
    }

    object AddQuestion : Screen("add_question?subjectId={subjectId}") {
        fun createRoute(subjectId: Long? = null): String {
            return if (subjectId != null) {
                "add_question?subjectId=$subjectId"
            } else {
                "add_question?subjectId=-1"
            }
        }
    }

    object ReviewSession : Screen("review_session")

    // 设置页面
    object AiConfig : Screen("ai_config")
    object DataBackup : Screen("data_backup")

    // 拍照识别
    object Camera : Screen("camera")

    // 错题列表
    object QuestionList : Screen("question_list?subjectId={subjectId}") {
        fun createRoute(subjectId: Long? = null): String {
            return if (subjectId != null) {
                "question_list?subjectId=$subjectId"
            } else {
                "question_list?subjectId=-1"
            }
        }
    }

    // 学习统计
    object Stats : Screen("stats")

    // AI 配置
    object AiConfig : Screen("ai_config")

    // AI 使用统计
    object AiUsage : Screen("ai_usage")

    // 数据备份
    object Backup : Screen("backup")

    // 成就系统
    object Achievement : Screen("achievement")
}

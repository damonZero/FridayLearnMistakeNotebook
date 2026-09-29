package com.friday.mistakenotebook.ui.theme

import androidx.compose.ui.graphics.Color

// 主题色彩 - 卡通风格，适合小学生
val Primary = Color(0xFFFF6B6B)        // 珊瑚红
val PrimaryVariant = Color(0xFFFF5252)
val Secondary = Color(0xFF4ECDC4)      // 薄荷绿
val SecondaryVariant = Color(0xFF26A69A)
val Background = Color(0xFFF8F9FA)     // 浅灰白
val Surface = Color(0xFFFFFFFF)
val Error = Color(0xFFE53935)

// 科目颜色
val SubjectChinese = Color(0xFFFF7043)  // 语文 - 橙色
val SubjectMath = Color(0xFF42A5F5)     // 数学 - 蓝色
val SubjectEnglish = Color(0xFF66BB6A)  // 英语 - 绿色

// 掌握程度颜色
val MasteredGreen = Color(0xFF4CAF50)
val LearningYellow = Color(0xFFFFC107)
val NeedReviewRed = Color(0xFFFF5722)
val LearningAmber = Color(0xFFB7791F)   // 学习中 - 深琥珀（浅色主题白底上比亮黄更清晰）

// primaryContainer 配色（亮红底配深字对比度不足，按深浅主题区分）
val PrimaryContainerLight = Color(0xFFFFE3E2)   // 浅色主题 - 低饱和浅红容器
val OnPrimaryContainerLight = Color(0xFF7A2E2E) // 浅色主题 - 深红文字
val PrimaryContainerDark = Color(0xFF5C2323)    // 深色主题 - 暗红容器
val OnPrimaryContainerDark = Color(0xFFFFDAD9)  // 深色主题 - 浅红文字

// 卡片颜色
val CardColors = listOf(
    Color(0xFFFFCDD2),
    Color(0xFFFFE0B2),
    Color(0xFFFFF9C4),
    Color(0xFFC8E6C9),
    Color(0xFFBBDEFB),
    Color(0xFFD1C4E9),
    Color(0xFFB2DFDB),
    Color(0xFFF8BBD0)
)

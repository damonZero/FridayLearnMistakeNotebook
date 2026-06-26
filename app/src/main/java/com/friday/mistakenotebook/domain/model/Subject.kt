package com.friday.mistakenotebook.domain.model

data class Subject(
    val id: Long = 0,
    val name: String,
    val icon: String = "📚",
    val color: String = "#4CAF50",
    val isPreset: Boolean = false,
    val questionCount: Int = 0
)

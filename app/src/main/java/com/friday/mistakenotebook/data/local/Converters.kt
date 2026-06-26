package com.friday.mistakenotebook.data.local

import androidx.room.TypeConverter
import com.friday.mistakenotebook.data.local.entity.AiTaskType
import com.friday.mistakenotebook.data.local.entity.ErrorType

class Converters {

    @TypeConverter
    fun fromErrorType(errorType: ErrorType): String {
        return errorType.name
    }

    @TypeConverter
    fun toErrorType(value: String): ErrorType {
        return try {
            ErrorType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ErrorType.UNKNOWN
        }
    }

    @TypeConverter
    fun fromAiTaskType(taskType: AiTaskType): String {
        return taskType.name
    }

    @TypeConverter
    fun toAiTaskType(value: String): AiTaskType {
        return try {
            AiTaskType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            AiTaskType.OCR
        }
    }
}

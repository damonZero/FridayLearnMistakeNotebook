package com.friday.mistakenotebook.data.backup

import android.content.Context
import android.net.Uri
import com.friday.mistakenotebook.data.local.MistakeNotebookDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val subjects: List<Any> = emptyList(),
    val questions: List<Any> = emptyList(),
    val aiConfigs: List<Any> = emptyList()
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MistakeNotebookDatabase,
    private val gson: Gson
) {
    private val backupDir = File(context.filesDir, "backups")

    init {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }

    /**
     * 导出数据为 JSON
     */
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val subjects = database.subjectDao().getAllSubjectsList()
        val questions = database.questionDao().getAllQuestionsList()
        val aiConfigs = database.aiConfigDao().getAllAiConfigsList()

        val backupData = BackupData(
            subjects = subjects,
            questions = questions,
            aiConfigs = aiConfigs
        )

        gson.toJson(backupData)
    }

    /**
     * 导出到文件
     */
    suspend fun exportToFile(): File = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val filename = "backup_${dateFormat.format(Date())}.json"
        val file = File(backupDir, filename)

        val json = exportToJson()
        file.writeText(json)

        file
    }

    /**
     * 从 JSON 导入
     */
    suspend fun importFromJson(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupData = gson.fromJson(json, BackupData::class.java)
            // TODO: 实现数据导入逻辑
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从文件导入
     */
    suspend fun importFromFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val json = inputStream?.bufferedReader()?.readText()
            inputStream?.close()

            if (json != null) {
                importFromJson(json)
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 自动备份
     */
    suspend fun autoBackup() {
        try {
            val file = exportToFile()
            // 清理旧备份（保留最近 7 个）
            cleanOldBackups(7)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 清理旧备份
     */
    private fun cleanOldBackups(keepCount: Int) {
        val files = backupDir.listFiles()
            ?.filter { it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }

        if (files != null && files.size > keepCount) {
            files.drop(keepCount).forEach { it.delete() }
        }
    }

    /**
     * 获取备份列表
     */
    fun getBackupFiles(): List<File> {
        return backupDir.listFiles()
            ?.filter { it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * 删除备份
     */
    fun deleteBackup(file: File): Boolean {
        return file.delete()
    }
}

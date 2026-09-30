package com.friday.mistakenotebook.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.friday.mistakenotebook.data.local.MistakeNotebookDatabase
import com.friday.mistakenotebook.data.local.entity.AiConfigEntity
import com.friday.mistakenotebook.data.local.entity.AiUsageLogEntity
import com.friday.mistakenotebook.data.local.entity.ChapterEntity
import com.friday.mistakenotebook.data.local.entity.KnowledgePointEntity
import com.friday.mistakenotebook.data.local.entity.QuestionEntity
import com.friday.mistakenotebook.data.local.entity.SubjectEntity
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** 备份文件格式版本（v1 旧格式已废弃，不做兼容） */
private const val BACKUP_VERSION = 2

/** 自动备份保留数量 */
private const val KEEP_BACKUP_COUNT = 7

private const val TAG = "BackupManager"

/**
 * 备份数据结构（v2，强类型，包含全部 6 张表）
 */
data class BackupData(
    val version: Int = BACKUP_VERSION,
    val timestamp: Long = System.currentTimeMillis(),
    val subjects: List<SubjectEntity> = emptyList(),
    val chapters: List<ChapterEntity> = emptyList(),
    val knowledgePoints: List<KnowledgePointEntity> = emptyList(),
    val questions: List<QuestionEntity> = emptyList(),
    val aiConfigs: List<AiConfigEntity> = emptyList(),
    val aiUsageLogs: List<AiUsageLogEntity> = emptyList()
)

/**
 * 导入结果（结构化，供 UI 展示成败与统计信息）
 */
data class ImportResult(
    val success: Boolean,
    val message: String
)

/** 备份文件格式/内容不符合要求 */
private class BackupFormatException(message: String) : Exception(message)

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
        gson.toJson(readAllTables())
    }

    /**
     * 在单个事务中读取全部 6 张表，保证备份内容的一致性
     */
    private suspend fun readAllTables(): BackupData = database.withTransaction {
        val subjects = database.subjectDao().getAllSubjectsList()
        val chapters = database.chapterDao().getAllChaptersList()
        val knowledgePoints = database.knowledgePointDao().getAllKnowledgePointsList()

        BackupData(
            version = BACKUP_VERSION,
            subjects = subjects,
            chapters = chapters,
            knowledgePoints = knowledgePoints,
            questions = database.questionDao().getAllQuestionsList(),
            // 隐私安全：导出的备份一律不携带 apiKey
            aiConfigs = database.aiConfigDao().getAllAiConfigsList().map { it.copy(apiKey = "") },
            aiUsageLogs = database.aiUsageLogDao().getAllUsageLogsList()
        )
    }

    /**
     * 导出到应用内部备份目录
     */
    suspend fun exportToFile(): File = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val filename = "backup_${dateFormat.format(Date())}.json"
        val file = File(backupDir, filename)

        file.writeText(exportToJson())

        file
    }

    /**
     * 导出到用户选择的外部位置（SAF Uri）
     */
    suspend fun exportToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = exportToJson()
            val output = context.contentResolver.openOutputStream(uri)
                ?: return@withContext false
            output.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "导出备份到外部位置失败", e)
            false
        }
    }

    /**
     * 从 JSON 导入
     *
     * 按科目 → 章节 → 知识点 → 错题 → AI 配置 → 使用记录的顺序，
     * 逐表按 id 合并覆盖（已有同 id 记录则更新，否则新增），整个过程在一个事务中完成。
     */
    suspend fun importFromJson(json: String): ImportResult = withContext(Dispatchers.IO) {
        val backupData = try {
            gson.fromJson(json, BackupData::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "解析备份文件失败", e)
            return@withContext ImportResult(false, "导入失败：备份文件格式不正确")
        }

        try {
            if (backupData == null) {
                throw BackupFormatException("备份文件内容为空")
            }
            if (backupData.version != BACKUP_VERSION) {
                throw BackupFormatException(
                    "备份文件版本不支持（需要 v$BACKUP_VERSION，实际是 v${backupData.version}）"
                )
            }
            // Gson 通过反射填充字段，JSON 缺字段时即使声明为非空也可能是 null，统一校验
            val subjects = requiredList(backupData.subjects, "subjects")
            val chapters = requiredList(backupData.chapters, "chapters")
            val knowledgePoints = requiredList(backupData.knowledgePoints, "knowledgePoints")
            val questions = requiredList(backupData.questions, "questions")
            val aiConfigs = requiredList(backupData.aiConfigs, "aiConfigs")
            val aiUsageLogs = requiredList(backupData.aiUsageLogs, "aiUsageLogs")

            database.withTransaction {
                subjects.forEach { subject ->
                    if (database.subjectDao().getSubjectById(subject.id) != null) {
                        database.subjectDao().updateSubject(subject)
                    } else {
                        database.subjectDao().insertSubject(subject)
                    }
                }
                chapters.forEach { chapter ->
                    if (database.chapterDao().getChapterById(chapter.id) != null) {
                        database.chapterDao().updateChapter(chapter)
                    } else {
                        database.chapterDao().insertChapter(chapter)
                    }
                }
                knowledgePoints.forEach { knowledgePoint ->
                    if (database.knowledgePointDao().getKnowledgePointById(knowledgePoint.id) != null) {
                        database.knowledgePointDao().updateKnowledgePoint(knowledgePoint)
                    } else {
                        database.knowledgePointDao().insertKnowledgePoint(knowledgePoint)
                    }
                }
                questions.forEach { question ->
                    if (database.questionDao().getQuestionById(question.id) != null) {
                        database.questionDao().updateQuestion(question)
                    } else {
                        database.questionDao().insertQuestion(question)
                    }
                }
                aiConfigs.forEach { config ->
                    val merged = if (config.apiKey.isBlank()) {
                        // 导出的备份不携带 apiKey，若本地已有同 id 配置则保留本地密钥
                        val local = database.aiConfigDao().getAiConfigById(config.id)
                        if (local != null) config.copy(apiKey = local.apiKey) else config
                    } else {
                        config
                    }
                    database.aiConfigDao().insertAiConfig(merged)
                }
                aiUsageLogs.forEach { log ->
                    database.aiUsageLogDao().insertUsageLog(log)
                }
            }

            ImportResult(
                success = true,
                message = "导入成功：${subjects.size} 个科目、${chapters.size} 个章节、" +
                    "${knowledgePoints.size} 个知识点、${questions.size} 道错题、" +
                    "${aiConfigs.size} 个 AI 配置、${aiUsageLogs.size} 条使用记录"
            )
        } catch (e: BackupFormatException) {
            Log.e(TAG, "备份文件校验失败", e)
            ImportResult(false, "导入失败：${e.message}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "导入备份失败", e)
            ImportResult(false, "导入失败：${e.message ?: "未知错误"}")
        }
    }

    /**
     * 校验备份中的列表字段是否存在（缺失即视为文件损坏）
     */
    private fun <T : Any> requiredList(value: List<T>?, name: String): List<T> =
        value ?: throw BackupFormatException("备份文件缺少字段：$name")

    /**
     * 从文件导入
     */
    suspend fun importFromFile(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: return@withContext ImportResult(false, "导入失败：无法读取所选文件")
            importFromJson(json)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "读取备份文件失败", e)
            ImportResult(false, "导入失败：读取文件出错（${e.message ?: "未知错误"}）")
        }
    }

    /**
     * 自动备份到应用内部目录
     *
     * 幂等：重复调用只会生成新的备份文件并清理旧备份，不影响业务数据。
     * 返回是否成功，失败原因记日志。
     */
    suspend fun autoBackup(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = exportToFile()
            cleanOldBackups(KEEP_BACKUP_COUNT)
            Log.i(TAG, "自动备份完成：${file.name}")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "自动备份失败", e)
            false
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

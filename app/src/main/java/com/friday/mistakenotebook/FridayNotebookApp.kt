package com.friday.mistakenotebook

import android.app.Application
import android.util.Log
import com.friday.mistakenotebook.data.local.dao.QuestionDao
import com.friday.mistakenotebook.domain.repository.SubjectRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class FridayNotebookApp : Application() {

    @Inject
    lateinit var questionDao: QuestionDao

    @Inject
    lateinit var subjectRepository: SubjectRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        seedPresetSubjects()
        cleanupOrphanImages()
    }

    /**
     * 预设科目播种必须在应用启动时完成：启动页是"复习"，
     * 用户可能从不经过首页，若播种挂在首页 ViewModel 会导致录入页无科目可选
     */
    private fun seedPresetSubjects() {
        appScope.launch {
            runCatching { subjectRepository.initPresetSubjects() }
                .onFailure { Log.w("FridayApp", "预设科目播种失败: ${it.message}") }
        }
    }

    /**
     * 启动时清理 filesDir/images 下不再被任何错题引用的图片
     * （弃拍、清数据、换机恢复等产生的孤儿文件，防止无限累积）
     */
    private fun cleanupOrphanImages() {
        appScope.launch {
            runCatching {
                val referenced = questionDao.getAllQuestionsList()
                    .mapNotNull { it.imagePath?.let { path -> File(path).canonicalPath } }
                    .toSet()
                val imageDir = File(filesDir, "images")
                imageDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("question_") && file.canonicalPath !in referenced) {
                        file.delete()
                    }
                }
            }
        }
    }
}

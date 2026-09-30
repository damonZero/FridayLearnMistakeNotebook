package com.friday.mistakenotebook

import android.app.Application
import com.friday.mistakenotebook.data.local.dao.QuestionDao
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

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        cleanupOrphanImages()
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

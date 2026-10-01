package com.friday.mistakenotebook.data.repository

import com.friday.mistakenotebook.data.local.dao.SubjectDao
import com.friday.mistakenotebook.data.local.entity.SubjectEntity
import com.friday.mistakenotebook.domain.model.Subject
import com.friday.mistakenotebook.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepositoryImpl @Inject constructor(
    private val subjectDao: SubjectDao
) : SubjectRepository {

    override fun getAllSubjects(): Flow<List<Subject>> {
        return subjectDao.getAllSubjects().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSubjectById(id: Long): Subject? {
        return subjectDao.getSubjectById(id)?.toDomain()
    }

    override suspend fun addSubject(name: String, icon: String, color: String): Long {
        val entity = SubjectEntity(
            name = name,
            icon = icon,
            color = color,
            isPreset = false
        )
        return subjectDao.insertSubject(entity)
    }

    override suspend fun updateSubject(subject: Subject) {
        val entity = subject.toEntity()
        subjectDao.updateSubject(entity)
    }

    override suspend fun deleteSubject(id: Long): Boolean {
        val deletedRows = subjectDao.deleteSubjectById(id)
        return deletedRows > 0
    }

    // 进程内防重入：播种逻辑现由 Application 启动触发（不再依赖用户经过首页），
    // 加护栏避免 Home 等入口并发重复调用
    private val presetSeeded = java.util.concurrent.atomic.AtomicBoolean(false)

    override suspend fun initPresetSubjects() {
        if (!presetSeeded.compareAndSet(false, true)) return
        val count = subjectDao.getSubjectCount()
        if (count == 0) {
            val presets = listOf(
                SubjectEntity(name = "语文", icon = "📖", color = "#FF5722", isPreset = true),
                SubjectEntity(name = "数学", icon = "🔢", color = "#2196F3", isPreset = true),
                SubjectEntity(name = "英语", icon = "🔤", color = "#4CAF50", isPreset = true)
            )
            presets.forEach { subjectDao.insertSubject(it) }
        }
    }

    private fun SubjectEntity.toDomain(): Subject {
        return Subject(
            id = id,
            name = name,
            icon = icon,
            color = color,
            isPreset = isPreset
        )
    }

    private fun Subject.toEntity(): SubjectEntity {
        return SubjectEntity(
            id = id,
            name = name,
            icon = icon,
            color = color,
            isPreset = isPreset
        )
    }
}

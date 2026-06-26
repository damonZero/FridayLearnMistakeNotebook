package com.friday.mistakenotebook.domain.repository

import com.friday.mistakenotebook.domain.model.Subject
import kotlinx.coroutines.flow.Flow

interface SubjectRepository {

    fun getAllSubjects(): Flow<List<Subject>>

    suspend fun getSubjectById(id: Long): Subject?

    suspend fun addSubject(name: String, icon: String, color: String): Long

    suspend fun updateSubject(subject: Subject)

    suspend fun deleteSubject(id: Long): Boolean

    suspend fun initPresetSubjects()
}

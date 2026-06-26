package com.friday.mistakenotebook.data.local.dao

import androidx.room.*
import com.friday.mistakenotebook.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY isPreset DESC, name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE name = :name LIMIT 1")
    suspend fun getSubjectByName(name: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id AND isPreset = 0")
    suspend fun deleteSubjectById(id: Long): Int

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int
}

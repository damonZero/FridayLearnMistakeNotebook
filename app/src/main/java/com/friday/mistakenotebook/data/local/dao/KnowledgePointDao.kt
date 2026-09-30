package com.friday.mistakenotebook.data.local.dao

import androidx.room.*
import com.friday.mistakenotebook.data.local.entity.KnowledgePointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgePointDao {

    @Query("SELECT * FROM knowledge_points WHERE chapterId = :chapterId ORDER BY name ASC")
    fun getKnowledgePointsByChapter(chapterId: Long): Flow<List<KnowledgePointEntity>>

    @Query("SELECT * FROM knowledge_points ORDER BY chapterId ASC, name ASC")
    suspend fun getAllKnowledgePointsList(): List<KnowledgePointEntity>

    @Query("SELECT * FROM knowledge_points WHERE id = :id")
    suspend fun getKnowledgePointById(id: Long): KnowledgePointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgePoint(knowledgePoint: KnowledgePointEntity): Long

    @Update
    suspend fun updateKnowledgePoint(knowledgePoint: KnowledgePointEntity)

    @Delete
    suspend fun deleteKnowledgePoint(knowledgePoint: KnowledgePointEntity)
}

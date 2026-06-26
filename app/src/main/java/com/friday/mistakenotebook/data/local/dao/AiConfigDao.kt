package com.friday.mistakenotebook.data.local.dao

import androidx.room.*
import com.friday.mistakenotebook.data.local.entity.AiConfigEntity
import com.friday.mistakenotebook.data.local.entity.AiTaskType
import kotlinx.coroutines.flow.Flow

@Dao
interface AiConfigDao {

    @Query("SELECT * FROM ai_configs ORDER BY taskType ASC")
    fun getAllAiConfigs(): Flow<List<AiConfigEntity>>

    @Query("SELECT * FROM ai_configs WHERE taskType = :taskType AND isEnabled = 1 LIMIT 1")
    suspend fun getEnabledConfigByTaskType(taskType: AiTaskType): AiConfigEntity?

    @Query("SELECT * FROM ai_configs WHERE id = :id")
    suspend fun getAiConfigById(id: Long): AiConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiConfig(config: AiConfigEntity): Long

    @Update
    suspend fun updateAiConfig(config: AiConfigEntity)

    @Delete
    suspend fun deleteAiConfig(config: AiConfigEntity)
}

package com.friday.mistakenotebook.data.local.dao

import androidx.room.*
import com.friday.mistakenotebook.data.local.entity.AiUsageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiUsageLogDao {

    @Query("SELECT * FROM ai_usage_logs ORDER BY createdAt DESC")
    fun getAllUsageLogs(): Flow<List<AiUsageLogEntity>>

    @Query("SELECT * FROM ai_usage_logs ORDER BY createdAt DESC")
    suspend fun getAllUsageLogsList(): List<AiUsageLogEntity>

    @Query("SELECT * FROM ai_usage_logs WHERE createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    fun getUsageLogsByTimeRange(startTime: Long, endTime: Long): Flow<List<AiUsageLogEntity>>

    @Query("SELECT SUM(estimatedCost) FROM ai_usage_logs")
    fun getTotalCost(): Flow<Double?>

    @Query("SELECT SUM(estimatedCost) FROM ai_usage_logs WHERE createdAt BETWEEN :startTime AND :endTime")
    fun getCostByTimeRange(startTime: Long, endTime: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageLog(log: AiUsageLogEntity): Long

    @Query("DELETE FROM ai_usage_logs WHERE createdAt < :before")
    suspend fun deleteOldLogs(before: Long)
}

package com.friday.mistakenotebook.data.local.dao

import androidx.room.*
import com.friday.mistakenotebook.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

/** 莱特纳盒子分布统计行 */
data class BoxCount(
    val leitnerBox: Int,
    val count: Int
)

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    suspend fun getAllQuestionsList(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getQuestionsBySubject(subjectId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: Long): QuestionEntity?

    // dueUntil 语义：当天到期即算待复习，传"明天 0 点"的时间戳
    @Query("SELECT * FROM questions WHERE nextReviewDate <= :dueUntil ORDER BY nextReviewDate ASC")
    fun getQuestionsForReview(dueUntil: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE nextReviewDate <= :dueUntil AND subjectId = :subjectId ORDER BY nextReviewDate ASC")
    fun getQuestionsForReviewBySubject(dueUntil: Long, subjectId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT COUNT(*) FROM questions")
    fun getTotalQuestionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM questions WHERE nextReviewDate <= :dueUntil")
    fun getTodayReviewCount(dueUntil: Long): Flow<Int>

    // 已掌握 = 莱特纳盒 5（需求 §5.2），与统计页分布口径一致
    @Query("SELECT COUNT(*) FROM questions WHERE leitnerBox = 5")
    fun getMasteredCount(): Flow<Int>

    @Query("SELECT leitnerBox, COUNT(*) AS count FROM questions GROUP BY leitnerBox")
    fun getBoxCounts(): Flow<List<BoxCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity): Long

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteQuestionById(id: Long)

    @Query("""
        SELECT * FROM questions
        WHERE content LIKE '%' || :query || '%'
        OR answer LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchQuestions(query: String): Flow<List<QuestionEntity>>
}

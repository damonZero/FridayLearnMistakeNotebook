package com.friday.mistakenotebook.data.local.dao

import androidx.room.*
import com.friday.mistakenotebook.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getQuestionsBySubject(subjectId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: Long): QuestionEntity?

    @Query("SELECT * FROM questions WHERE nextReviewDate <= :currentTime ORDER BY nextReviewDate ASC")
    fun getQuestionsForReview(currentTime: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE nextReviewDate <= :currentTime AND subjectId = :subjectId ORDER BY nextReviewDate ASC")
    fun getQuestionsForReviewBySubject(currentTime: Long, subjectId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT COUNT(*) FROM questions")
    fun getTotalQuestionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM questions WHERE nextReviewDate <= :currentTime")
    fun getTodayReviewCount(currentTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM questions WHERE leitnerBox >= 4")
    fun getMasteredCount(): Flow<Int>

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

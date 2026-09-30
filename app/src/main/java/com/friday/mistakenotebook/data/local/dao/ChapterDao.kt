package com.friday.mistakenotebook.data.local.dao

import androidx.room.*
import com.friday.mistakenotebook.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY name ASC")
    fun getChaptersBySubject(subjectId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters ORDER BY subjectId ASC, name ASC")
    suspend fun getAllChaptersList(): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapterById(id: Long)
}

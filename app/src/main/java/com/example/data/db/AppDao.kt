package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Bookmark
import com.example.data.model.ReadingProgress
import com.example.data.model.StudyStats
import com.example.data.model.UserNote
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- User Notes ---
    @Query("SELECT * FROM user_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<UserNote>>

    @Query("SELECT * FROM user_notes WHERE chapterId = :chapterId ORDER BY timestamp DESC")
    fun getNotesForChapter(chapterId: String): Flow<List<UserNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: UserNote)

    @Delete
    suspend fun deleteNote(note: UserNote)

    @Query("DELETE FROM user_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Int)

    // --- Bookmarks ---
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE chapterId = :chapterId")
    suspend fun getBookmarkById(chapterId: String): Bookmark?

    @Query("SELECT * FROM bookmarks WHERE chapterId = :chapterId")
    fun getBookmarkFlowById(chapterId: String): Flow<Bookmark?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmarkObj(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE chapterId = :chapterId")
    suspend fun deleteBookmark(chapterId: String)

    // --- Reading Progress ---
    @Query("SELECT * FROM reading_progress ORDER BY lastReadTimestamp DESC")
    fun getAllProgress(): Flow<List<ReadingProgress>>

    @Query("SELECT * FROM reading_progress WHERE chapterId = :chapterId")
    suspend fun getProgressById(chapterId: String): ReadingProgress?

    @Query("SELECT * FROM reading_progress WHERE chapterId = :chapterId")
    fun getProgressFlowById(chapterId: String): Flow<ReadingProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: ReadingProgress)

    @Query("SELECT COUNT(*) FROM reading_progress WHERE isCompleted = 1")
    fun getCompletedLessonsCount(): Flow<Int>

    // --- Study Stats ---
    @Query("SELECT * FROM study_stats WHERE dateString = :dateString")
    suspend fun getStatsForDate(dateString: String = "overall"): StudyStats?

    @Query("SELECT * FROM study_stats WHERE dateString = :dateString")
    fun getStatsFlowForDate(dateString: String = "overall"): Flow<StudyStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: StudyStats)
}

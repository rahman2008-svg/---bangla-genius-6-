package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_notes")
data class UserNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chapterId: String,
    val chapterTitle: String,
    val noteText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val chapterId: String,
    val chapterTitle: String,
    val categoryId: String,
    val progressPercentage: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_progress")
data class ReadingProgress(
    @PrimaryKey val chapterId: String,
    val percentage: Int = 0,
    val isCompleted: Boolean = false,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_stats")
data class StudyStats(
    @PrimaryKey val dateString: String = "overall", // e.g., "2026-07-27" or "overall"
    val lessonsCompletedToday: Int = 0,
    val minutesReadToday: Int = 0,
    val dailyGoalLessons: Int = 3,
    val lessonsCompletedTotal: Int = 0,
    val quizzesPassed: Int = 0,
    val totalPoints: Int = 0,
    val currentStreakDays: Int = 1,
    val lastStudyDateMillis: Long = System.currentTimeMillis()
)

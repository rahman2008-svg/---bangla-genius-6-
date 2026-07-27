package com.example.data.model

data class Chapter(
    val id: String,
    val categoryId: String, // "grammar" or "language"
    val chapterNumber: Int,
    val title: String,
    val subtitle: String,
    val content: String,
    val readingTimeMinutes: Int = 10,
    val difficulty: String = "Easy", // Easy, Medium, Hard
    val lastUpdated: String = "জুলাই ২০২৬",
    val tags: List<String> = emptyList()
)

data class QuizQuestion(
    val id: String,
    val chapterId: String,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val requiredLessons: Int,
    val iconName: String, // e.g. "beginner", "learner", "expert", "genius"
    val isUnlocked: Boolean = false
)

data class MockTestResult(
    val examName: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val totalScore: Float,
    val timeTakenSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)

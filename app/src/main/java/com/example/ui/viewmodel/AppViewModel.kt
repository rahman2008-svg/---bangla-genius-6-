package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Achievement
import com.example.data.model.Bookmark
import com.example.data.model.Chapter
import com.example.data.model.MockTestResult
import com.example.data.model.QuizQuestion
import com.example.data.model.ReadingProgress
import com.example.data.model.StudyStats
import com.example.data.model.UserNote
import com.example.data.repository.GrammarChaptersData
import com.example.data.repository.LanguageChaptersData
import com.example.data.repository.QuizAndMockData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.appDao()
    private val prefs = application.getSharedPreferences("bangla_genius_prefs", Context.MODE_PRIVATE)

    // All static chapters
    val allGrammarChapters: List<Chapter> = GrammarChaptersData.grammarChapters.sortedBy { it.chapterNumber }
    val allLanguageChapters: List<Chapter> = LanguageChaptersData.languageChapters.sortedBy { it.chapterNumber }
    val allChapters: List<Chapter> = (allGrammarChapters + allLanguageChapters).sortedBy { it.chapterNumber }

    // App Preferences State
    private val _isFirstLaunch = MutableStateFlow(prefs.getBoolean("is_first_launch", true))
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _fontSizeScale = MutableStateFlow(prefs.getFloat("font_size_scale", 1.0f))
    val fontSizeScale: StateFlow<Float> = _fontSizeScale.asStateFlow()

    // Search & Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredChapters: StateFlow<List<Chapter>> = _searchQuery.map { query ->
        if (query.isBlank()) {
            allChapters
        } else {
            val q = query.trim().lowercase()
            allChapters.filter { ch ->
                ch.title.lowercase().contains(q) ||
                ch.subtitle.lowercase().contains(q) ||
                ch.content.lowercase().contains(q) ||
                ch.tags.any { it.lowercase().contains(q) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), allChapters)

    // Database Streams
    val allBookmarks: StateFlow<List<Bookmark>> = dao.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<UserNote>> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProgress: StateFlow<List<ReadingProgress>> = dao.getAllProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyStats: StateFlow<StudyStats> = dao.getStatsFlowForDate("overall")
        .map { it ?: StudyStats(dateString = "overall") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudyStats(dateString = "overall"))

    // Recent Chapter ID
    private val _lastReadChapterId = MutableStateFlow(prefs.getString("last_read_chapter_id", "grammar_1") ?: "grammar_1")
    val lastReadChapterId: StateFlow<String> = _lastReadChapterId.asStateFlow()

    val lastReadChapter: StateFlow<Chapter?> = _lastReadChapterId.map { id ->
        allChapters.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), allChapters.firstOrNull())

    // Bookmarked Chapters list combined with static Chapter data
    val bookmarkedChapters: StateFlow<List<Chapter>> = combine(allBookmarks, _searchQuery) { bookmarks, _ ->
        val bookmarkedIds = bookmarks.map { it.chapterId }.toSet()
        allChapters.filter { bookmarkedIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Achievements state calculated from completed lessons and quizzes
    val achievements: StateFlow<List<Achievement>> = combine(allProgress, studyStats) { progressList, stats ->
        val completedCount = progressList.count { it.isCompleted }
        LanguageChaptersData.achievementBadges.map { badge ->
            val unlocked = completedCount >= badge.requiredLessons ||
                    (badge.id == "badge_expert" && stats.quizzesPassed >= 5) ||
                    (badge.id == "badge_genius" && completedCount >= 13)
            badge.copy(isUnlocked = unlocked)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LanguageChaptersData.achievementBadges)

    // Quiz & Mock Test State
    private val _currentQuizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val currentQuizQuestions: StateFlow<List<QuizQuestion>> = _currentQuizQuestions.asStateFlow()

    private val _mockTestResults = MutableStateFlow<List<MockTestResult>>(emptyList())
    val mockTestResults: StateFlow<List<MockTestResult>> = _mockTestResults.asStateFlow()

    init {
        // Initialize stats if empty
        viewModelScope.launch {
            if (dao.getStatsForDate("overall") == null) {
                dao.insertOrUpdateStats(StudyStats(dateString = "overall"))
            }
            updateStreak()
        }
    }

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean("is_first_launch", false).apply()
        _isFirstLaunch.value = false
    }

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        prefs.edit().putBoolean("is_dark_mode", next).apply()
        _isDarkMode.value = next
    }

    fun increaseFontSize() {
        if (_fontSizeScale.value < 1.4f) {
            val next = (_fontSizeScale.value + 0.1f).coerceAtMost(1.4f)
            prefs.edit().putFloat("font_size_scale", next).apply()
            _fontSizeScale.value = next
        }
    }

    fun decreaseFontSize() {
        if (_fontSizeScale.value > 0.8f) {
            val next = (_fontSizeScale.value - 0.1f).coerceAtLeast(0.8f)
            prefs.edit().putFloat("font_size_scale", next).apply()
            _fontSizeScale.value = next
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setLastReadChapter(chapterId: String) {
        prefs.edit().putString("last_read_chapter_id", chapterId).apply()
        _lastReadChapterId.value = chapterId
    }

    fun toggleBookmark(chapter: Chapter) {
        viewModelScope.launch {
            val existing = dao.getBookmarkById(chapter.id)
            if (existing != null) {
                dao.deleteBookmarkObj(existing)
            } else {
                dao.insertBookmark(
                    Bookmark(
                        chapterId = chapter.id,
                        chapterTitle = chapter.title,
                        categoryId = if (chapter.categoryId == "grammar") "বাংলা ব্যাকরণ" else "ভাষা ও নির্মিতি"
                    )
                )
            }
        }
    }

    fun isChapterBookmarkedFlow(chapterId: String): StateFlow<Boolean> {
        return dao.getBookmarkFlowById(chapterId).map { it != null }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    }

    fun getChapterProgressFlow(chapterId: String): StateFlow<ReadingProgress?> {
        return dao.getProgressFlowById(chapterId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun getNotesForChapterFlow(chapterId: String): StateFlow<List<UserNote>> {
        return dao.getNotesForChapter(chapterId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun markChapterCompleted(chapterId: String) {
        viewModelScope.launch {
            val existing = dao.getProgressById(chapterId)
            val now = System.currentTimeMillis()
            if (existing != null) {
                if (!existing.isCompleted) {
                    dao.insertOrUpdateProgress(existing.copy(isCompleted = true, lastReadTimestamp = now))
                    incrementCompletedLessons()
                } else {
                    dao.insertOrUpdateProgress(existing.copy(lastReadTimestamp = now))
                }
            } else {
                dao.insertOrUpdateProgress(
                    ReadingProgress(
                        chapterId = chapterId,
                        percentage = 100,
                        isCompleted = true,
                        lastReadTimestamp = now
                    )
                )
                incrementCompletedLessons()
            }
            setLastReadChapter(chapterId)
            updateStreak()
        }
    }

    private suspend fun incrementCompletedLessons() {
        val stats = dao.getStatsForDate("overall") ?: StudyStats(dateString = "overall")
        dao.insertOrUpdateStats(
            stats.copy(
                lessonsCompletedTotal = stats.lessonsCompletedTotal + 1,
                lessonsCompletedToday = stats.lessonsCompletedToday + 1,
                lastStudyDateMillis = System.currentTimeMillis()
            )
        )
    }

    fun recordQuizScore(chapterId: String, total: Int, correct: Int) {
        viewModelScope.launch {
            val stats = dao.getStatsForDate("overall") ?: StudyStats(dateString = "overall")
            val passed = correct >= (total * 0.6f)
            dao.insertOrUpdateStats(
                stats.copy(
                    quizzesPassed = if (passed) stats.quizzesPassed + 1 else stats.quizzesPassed,
                    totalPoints = stats.totalPoints + (correct * 10),
                    lastStudyDateMillis = System.currentTimeMillis()
                )
            )
            updateStreak()
        }
    }

    fun recordMockTestResult(result: MockTestResult) {
        viewModelScope.launch {
            _mockTestResults.value = listOf(result) + _mockTestResults.value
            val stats = dao.getStatsForDate("overall") ?: StudyStats(dateString = "overall")
            dao.insertOrUpdateStats(
                stats.copy(
                    totalPoints = stats.totalPoints + (result.correctAnswers * 15),
                    lastStudyDateMillis = System.currentTimeMillis()
                )
            )
            updateStreak()
        }
    }

    private suspend fun updateStreak() {
        val stats = dao.getStatsForDate("overall") ?: StudyStats(dateString = "overall")
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date(now))
        val lastDateStr = dateFormat.format(Date(stats.lastStudyDateMillis))

        if (todayStr != lastDateStr) {
            val diffDays = (now - stats.lastStudyDateMillis) / (1000 * 60 * 60 * 24)
            if (diffDays <= 1) {
                dao.insertOrUpdateStats(stats.copy(currentStreakDays = stats.currentStreakDays + 1, lastStudyDateMillis = now))
            } else if (diffDays > 1 && stats.lastStudyDateMillis > 0) {
                dao.insertOrUpdateStats(stats.copy(currentStreakDays = 1, lastStudyDateMillis = now))
            } else {
                dao.insertOrUpdateStats(stats.copy(currentStreakDays = 1, lastStudyDateMillis = now))
            }
        } else if (stats.currentStreakDays == 0) {
            dao.insertOrUpdateStats(stats.copy(currentStreakDays = 1, lastStudyDateMillis = now))
        }
    }

    fun addNote(chapterId: String, title: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            dao.insertNote(
                UserNote(
                    chapterId = chapterId,
                    chapterTitle = allChapters.find { it.id == chapterId }?.title ?: "সাধারণ নোট",
                    noteText = if (title.isBlank()) content.take(30) + "..." else title + "\n\n" + content,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteNote(note: UserNote) {
        viewModelScope.launch {
            dao.deleteNote(note)
        }
    }

    fun loadQuizForChapter(chapterId: String) {
        _currentQuizQuestions.value = QuizAndMockData.chapterQuizzes[chapterId] ?: emptyList()
    }

    fun loadMockTest(examName: String): List<QuizQuestion> {
        return QuizAndMockData.mockTestQuestionSets[examName] ?: emptyList()
    }
}

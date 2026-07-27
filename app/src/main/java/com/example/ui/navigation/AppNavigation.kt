package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.ChapterListScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MockTestSelectScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.ReadingScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.viewmodel.AppViewModel

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val HOME = "home"
    const val CHAPTER_LIST = "chapter_list/{category}"
    const val READING = "reading/{chapterId}"
    const val QUIZ = "quiz/{chapterId}/{isMock}"
    const val MOCK_SELECT = "mock_test_select"
    const val BOOKMARKS = "bookmarks"
    const val NOTES = "notes"
    const val STATS = "stats"
    const val ABOUT = "about"

    fun createChapterListRoute(category: String) = "chapter_list/$category"
    fun createReadingRoute(chapterId: String) = "reading/$chapterId"
    fun createQuizRoute(id: String, isMock: Boolean = false) = "quiz/$id/$isMock"
}

@Composable
fun AppNavigation(
    viewModel: AppViewModel,
    navController: NavHostController = rememberNavController()
) {
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onTimeout = {
                    val nextRoute = if (isFirstLaunch) Routes.WELCOME else Routes.HOME
                    navController.navigate(nextRoute) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onStartClicked = {
                    viewModel.setFirstLaunchCompleted()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToChapterList = { cat ->
                    navController.navigate(Routes.createChapterListRoute(cat))
                },
                onNavigateToReading = { chId ->
                    viewModel.setLastReadChapter(chId)
                    navController.navigate(Routes.createReadingRoute(chId))
                },
                onNavigateToMockSelect = {
                    navController.navigate(Routes.MOCK_SELECT)
                },
                onNavigateToBookmarks = {
                    navController.navigate(Routes.BOOKMARKS)
                },
                onNavigateToNotes = {
                    navController.navigate(Routes.NOTES)
                },
                onNavigateToStats = {
                    navController.navigate(Routes.STATS)
                },
                onNavigateToAbout = {
                    navController.navigate(Routes.ABOUT)
                }
            )
        }

        composable(
            route = Routes.CHAPTER_LIST,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "all"
            ChapterListScreen(
                viewModel = viewModel,
                category = category,
                onNavigateBack = { navController.popBackStack() },
                onChapterClick = { chId ->
                    viewModel.setLastReadChapter(chId)
                    navController.navigate(Routes.createReadingRoute(chId))
                }
            )
        }

        composable(
            route = Routes.READING,
            arguments = listOf(navArgument("chapterId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: "grammar_1"
            ReadingScreen(
                viewModel = viewModel,
                chapterId = chapterId,
                onNavigateBack = { navController.popBackStack() },
                onTakeQuiz = { chId ->
                    navController.navigate(Routes.createQuizRoute(chId, false))
                },
                onNavigateToNotes = {
                    navController.navigate(Routes.NOTES)
                }
            )
        }

        composable(
            route = Routes.QUIZ,
            arguments = listOf(
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("isMock") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("chapterId") ?: "grammar_1"
            val isMock = backStackEntry.arguments?.getBoolean("isMock") ?: false
            QuizScreen(
                viewModel = viewModel,
                targetId = id,
                isMockTest = isMock,
                onNavigateBack = { navController.popBackStack() },
                onFinishQuiz = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.MOCK_SELECT) {
            MockTestSelectScreen(
                onNavigateBack = { navController.popBackStack() },
                onSelectMockTest = { examName ->
                    navController.navigate(Routes.createQuizRoute(examName, true))
                }
            )
        }

        composable(Routes.BOOKMARKS) {
            BookmarksScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onChapterClick = { chId ->
                    viewModel.setLastReadChapter(chId)
                    navController.navigate(Routes.createReadingRoute(chId))
                }
            )
        }

        composable(Routes.NOTES) {
            NotesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChapter = { chId ->
                    viewModel.setLastReadChapter(chId)
                    navController.navigate(Routes.createReadingRoute(chId))
                }
            )
        }

        composable(Routes.STATS) {
            StatsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

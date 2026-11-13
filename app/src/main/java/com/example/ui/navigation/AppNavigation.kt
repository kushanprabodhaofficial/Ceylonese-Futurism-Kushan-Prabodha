package com.example.ui.navigation

import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.SciFiSubGenre
import com.example.ui.auth.attemptSilentGoogleSignIn
import com.example.ui.components.AuthorSideDrawerContent
import kotlinx.coroutines.launch
import com.example.ui.screens.ArticleDetailScreen
import com.example.ui.screens.BlogUniverseScreen
import com.example.ui.screens.CategoryPostsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PageDetailScreen
import com.example.ui.screens.ScoreboardScreen
import com.example.ui.screens.WebViewerScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.viewmodel.BlogViewModel
import com.example.ui.viewmodel.ScoreboardViewModel
import java.nio.charset.StandardCharsets

object AppRoutes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val LIBRARY = "library"
    const val SCOREBOARD = "scoreboard"
    const val UNIVERSE = "universe"
    const val ARTICLE_DETAIL = "article/{postId}"
    const val PAGE_DETAIL = "page/{pageId}"
    const val CATEGORY_POSTS = "category/{categoryName}"
    const val SUBGENRE_POSTS = "subgenre/{subGenreName}"
    const val WEB_VIEWER = "web/{url}/{title}"

    fun articleRoute(postId: String): String = "article/${encodeArg(postId)}"
    fun pageRoute(pageId: String): String = "page/${encodeArg(pageId)}"
    fun categoryRoute(categoryName: String): String = "category/${encodeArg(categoryName)}"
    fun subGenreRoute(subGenre: SciFiSubGenre): String = "subgenre/${subGenre.name}"
    fun webRoute(url: String, title: String): String = "web/${encodeArg(url)}/${encodeArg(title)}"

    fun encodeArg(value: String): String {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    fun decodeArg(encoded: String): String {
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP)
            String(bytes, StandardCharsets.UTF_8)
        }.getOrDefault("")
    }
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopLevelTabs = listOf(
    BottomNavTab(
        route = AppRoutes.HOME,
        label = "ලිපි",
        selectedIcon = Icons.Filled.AutoStories,
        unselectedIcon = Icons.Outlined.AutoStories,
        testTag = "nav_tab_home"
    ),
    BottomNavTab(
        route = AppRoutes.EXPLORE,
        label = "වර්ග",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_tab_explore"
    ),
    BottomNavTab(
        route = AppRoutes.LIBRARY,
        label = "සුරැකි",
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder,
        testTag = "nav_tab_library"
    ),
    BottomNavTab(
        route = AppRoutes.SCOREBOARD,
        label = "ලකුණු",
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents,
        testTag = "nav_tab_scoreboard"
    ),
    BottomNavTab(
        route = AppRoutes.UNIVERSE,
        label = "බ්ලොග්",
        selectedIcon = Icons.Filled.Public,
        unselectedIcon = Icons.Outlined.Public,
        testTag = "nav_tab_universe"
    )
)

@Composable
fun CeyloneseFuturismApp(viewModel: BlogViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val postCommentsMap by viewModel.postComments.collectAsStateWithLifecycle()
    val loadingCommentsPostId by viewModel.loadingCommentsForPost.collectAsStateWithLifecycle()
    val scoreboardViewModel: ScoreboardViewModel = viewModel(factory = ScoreboardViewModel.provideFactory())
    val scoreboardUiState by scoreboardViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        attemptSilentGoogleSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = this,
            onAuthSuccess = { scoreboardViewModel.onUserAuthenticated() },
            onUnauthenticated = {}
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevelDestination = TopLevelTabs.any { it.route == currentRoute }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        drawerScope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isTopLevelDestination,
        drawerContent = {
            AuthorSideDrawerContent(
                blogMeta = uiState.blogMeta,
                currentRoute = currentRoute,
                onNavigateToTab = navigateToTab,
                onOpenInAppWeb = { url, title ->
                    navController.navigate(AppRoutes.webRoute(url, title))
                },
                onCloseDrawer = {
                    drawerScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (isTopLevelDestination && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        TopLevelTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelDestination && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        TopLevelTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.HOME
                    ) {
                        composable(AppRoutes.HOME) {
                            HomeScreen(
                                uiState = uiState,
                                onOpenDrawer = {
                                    drawerScope.launch { drawerState.open() }
                                },
                                onRefresh = { viewModel.refreshBlog(isUserInitiated = true) },
                                onDismissStatus = { viewModel.dismissStatusBanner() },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onSelectSubGenre = { viewModel.setSelectedSubGenre(it) },
                                onSelectCategory = { viewModel.setSelectedCategory(it) },
                                onToggleTheme = {
                                    val next = if (uiState.readerPreferences.appThemeMode == AppThemeMode.LIGHT) {
                                        AppThemeMode.DARK
                                    } else {
                                        AppThemeMode.LIGHT
                                    }
                                    viewModel.setAppThemeMode(next)
                                },
                                onPostClick = { post ->
                                    navController.navigate(AppRoutes.articleRoute(post.id))
                                },
                                onToggleBookmark = { post ->
                                    viewModel.toggleBookmark(post)
                                }
                            )
                        }

                        composable(AppRoutes.EXPLORE) {
                            ExploreScreen(
                                uiState = uiState,
                                onSubGenreClick = { subGenre ->
                                    navController.navigate(AppRoutes.subGenreRoute(subGenre))
                                },
                                onCategoryClick = { category ->
                                    navController.navigate(AppRoutes.categoryRoute(category))
                                },
                                onPostClick = { post ->
                                    navController.navigate(AppRoutes.articleRoute(post.id))
                                },
                                onToggleBookmark = { post ->
                                    viewModel.toggleBookmark(post)
                                }
                            )
                        }

                        composable(AppRoutes.LIBRARY) {
                            LibraryScreen(
                                uiState = uiState,
                                onPostClick = { post ->
                                    navController.navigate(AppRoutes.articleRoute(post.id))
                                },
                                onToggleBookmark = { post ->
                                    viewModel.toggleBookmark(post)
                                },
                                onBrowseStoriesClick = { navigateToTab(AppRoutes.HOME) }
                            )
                        }

                        composable(AppRoutes.SCOREBOARD) {
                            ScoreboardScreen(
                                uiState = scoreboardUiState,
                                onSaveDisplayName = { scoreboardViewModel.saveDisplayName(it) },
                                onAuthSuccess = { scoreboardViewModel.onUserAuthenticated() },
                                onSyncNow = { scoreboardViewModel.syncAllScoresToCloud() },
                                onAuthMessage = { scoreboardViewModel.showStatusMessage(it) },
                                onClearStatusMessage = { scoreboardViewModel.clearStatusMessage() },
                                onOpenPost = { post ->
                                    navController.navigate(AppRoutes.articleRoute(post.id))
                                },
                                onOpenPostById = { postId ->
                                    navController.navigate(AppRoutes.articleRoute(postId))
                                }
                            )
                        }

                        composable(AppRoutes.UNIVERSE) {
                            BlogUniverseScreen(
                                uiState = uiState,
                                onPageClick = { page ->
                                    navController.navigate(AppRoutes.pageRoute(page.id))
                                },
                                onOpenWebUrl = { url, title ->
                                    navController.navigate(AppRoutes.webRoute(url, title))
                                },
                                onOpenPostFromComment = { postId ->
                                    navController.navigate(AppRoutes.articleRoute(postId))
                                },
                                onSetThemeMode = { viewModel.setAppThemeMode(it) },
                                onRefreshAll = { viewModel.refreshBlog(isUserInitiated = true) }
                            )
                        }

                        composable(
                            route = AppRoutes.ARTICLE_DETAIL,
                            arguments = listOf(navArgument("postId") { type = NavType.StringType })
                        ) { entry ->
                            val encodedId = entry.arguments?.getString("postId").orEmpty()
                            val postId = remember(encodedId) { AppRoutes.decodeArg(encodedId) }
                            val post by viewModel.observePost(postId).collectAsStateWithLifecycle(initialValue = null)
                            val comments = post?.let { postCommentsMap[it.numericPostId] }.orEmpty()
                            val isLoadingComments = post?.numericPostId == loadingCommentsPostId

                            ArticleDetailScreen(
                                post = post,
                                blogMeta = uiState.blogMeta,
                                allPosts = uiState.allPosts,
                                comments = comments,
                                isLoadingComments = isLoadingComments,
                                preferences = uiState.readerPreferences,
                                onBack = { navController.popBackStack() },
                                onEnsureFullContent = { viewModel.ensureFullPostLoaded(it) },
                                onLoadComments = { numId, title -> viewModel.loadCommentsForPost(numId, title) },
                                onToggleBookmark = { viewModel.toggleBookmark(it) },
                                onUpdateProgress = { id, progress -> viewModel.updateReadingProgress(id, progress) },
                                onSelectPost = { nextPost ->
                                    navController.navigate(AppRoutes.articleRoute(nextPost.id)) {
                                        popUpTo(AppRoutes.ARTICLE_DETAIL) { inclusive = true }
                                    }
                                },
                                onSubGenreClick = { subGenre ->
                                    navController.navigate(AppRoutes.subGenreRoute(subGenre))
                                },
                                onCategoryClick = { category ->
                                    navController.navigate(AppRoutes.categoryRoute(category))
                                },
                                onOpenWebUrl = { url, title ->
                                    navController.navigate(AppRoutes.webRoute(url, title))
                                },
                                onFontSizeChange = { viewModel.setFontSize(it) },
                                onLineHeightChange = { viewModel.setLineHeightMultiplier(it) },
                                onUseSerifChange = { viewModel.setUseSerifFont(it) },
                                onReaderSurfaceChange = { viewModel.setReaderSurfaceMode(it) },
                                onAmbientMusicEnabledChange = { viewModel.setAmbientMusicEnabled(it) },
                                onAmbientMusicVolumeChange = { viewModel.setAmbientMusicVolume(it) },
                                onSelectTrackSlot = { postIdArg, slotIdx ->
                                    viewModel.setStoryTrackSlot(postIdArg, slotIdx)
                                },
                                onStartStoryMusic = { track, volume ->
                                    viewModel.startStoryMusic(track, volume)
                                },
                                onStopStoryMusic = { viewModel.stopStoryAmbientMusic() },
                                onNavigateToScoreboard = {
                                    navigateToTab(AppRoutes.SCOREBOARD)
                                }
                            )
                        }

                        composable(
                            route = AppRoutes.PAGE_DETAIL,
                            arguments = listOf(navArgument("pageId") { type = NavType.StringType })
                        ) { entry ->
                            val encodedId = entry.arguments?.getString("pageId").orEmpty()
                            val pageId = remember(encodedId) { AppRoutes.decodeArg(encodedId) }
                            val page = remember(pageId, uiState.pages) {
                                uiState.pages.firstOrNull { it.id == pageId }
                            }
                            PageDetailScreen(
                                page = page,
                                preferences = uiState.readerPreferences,
                                onBack = { navController.popBackStack() },
                                onOpenWebUrl = { url, title ->
                                    navController.navigate(AppRoutes.webRoute(url, title))
                                }
                            )
                        }

                        composable(
                            route = AppRoutes.CATEGORY_POSTS,
                            arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                        ) { entry ->
                            val encodedCat = entry.arguments?.getString("categoryName").orEmpty()
                            val categoryName = remember(encodedCat) { AppRoutes.decodeArg(encodedCat) }
                            CategoryPostsScreen(
                                categoryName = categoryName,
                                allPosts = uiState.allPosts,
                                onBack = { navController.popBackStack() },
                                onPostClick = { post ->
                                    navController.navigate(AppRoutes.articleRoute(post.id))
                                },
                                onToggleBookmark = { viewModel.toggleBookmark(it) }
                            )
                        }

                        composable(
                            route = AppRoutes.SUBGENRE_POSTS,
                            arguments = listOf(navArgument("subGenreName") { type = NavType.StringType })
                        ) { entry ->
                            val rawName = entry.arguments?.getString("subGenreName").orEmpty()
                            val subGenre = remember(rawName) {
                                runCatching { SciFiSubGenre.valueOf(rawName) }.getOrNull()
                            }
                            CategoryPostsScreen(
                                categoryName = subGenre?.titleSinhala ?: rawName,
                                subGenre = subGenre,
                                allPosts = uiState.allPosts,
                                onBack = { navController.popBackStack() },
                                onPostClick = { post ->
                                    navController.navigate(AppRoutes.articleRoute(post.id))
                                },
                                onToggleBookmark = { viewModel.toggleBookmark(it) }
                            )
                        }

                        composable(
                            route = AppRoutes.WEB_VIEWER,
                            arguments = listOf(
                                navArgument("url") { type = NavType.StringType },
                                navArgument("title") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val url = remember(entry) {
                                AppRoutes.decodeArg(entry.arguments?.getString("url").orEmpty())
                            }
                            val title = remember(entry) {
                                AppRoutes.decodeArg(entry.arguments?.getString("title").orEmpty())
                            }
                            WebViewerScreen(
                                url = url,
                                title = title,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

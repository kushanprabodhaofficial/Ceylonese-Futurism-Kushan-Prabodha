package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.StoryAmbientAudioEngine
import com.example.audio.StorySoundscape
import com.example.data.local.BlogMetaEntity
import com.example.data.local.BlogPageEntity
import com.example.data.local.BlogPostEntity
import com.example.data.model.ArchiveGroup
import com.example.data.model.BlogComment
import com.example.data.model.CategoryInfo
import com.example.data.model.ReaderSurfaceMode
import com.example.data.preferences.ReaderPreferences
import com.example.data.preferences.ReaderPreferencesRepository
import com.example.data.remote.BloggerJsonParser
import com.example.data.repository.BlogRepository
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class BlogUiState(
    val allPosts: List<BlogPostEntity> = emptyList(),
    val filteredPosts: List<BlogPostEntity> = emptyList(),
    val bookmarkedPosts: List<BlogPostEntity> = emptyList(),
    val readingHistory: List<BlogPostEntity> = emptyList(),
    val continueReadingPost: BlogPostEntity? = null,
    val pages: List<BlogPageEntity> = emptyList(),
    val blogMeta: BlogMetaEntity? = null,
    val categories: List<CategoryInfo> = emptyList(),
    val archives: List<ArchiveGroup> = emptyList(),
    val recentComments: List<BlogComment> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val isInitialLoading: Boolean = true,
    val statusBannerMessage: String? = null,
    val isStatusError: Boolean = false,
    val readerPreferences: ReaderPreferences = ReaderPreferences()
)

class BlogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BlogRepository(application)
    private val prefsRepository = ReaderPreferencesRepository(application)
    private val audioEngine = StoryAmbientAudioEngine()

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCategoryFlow = MutableStateFlow<String?>(null)
    private val isRefreshingFlow = MutableStateFlow(false)
    private val hasAttemptedInitialSync = MutableStateFlow(false)
    private val statusMessageFlow = MutableStateFlow<Pair<String?, Boolean>>(null to false)
    private val recentCommentsFlow = MutableStateFlow<List<BlogComment>>(emptyList())

    private val _postComments = MutableStateFlow<Map<String, List<BlogComment>>>(emptyMap())
    val postComments: StateFlow<Map<String, List<BlogComment>>> = _postComments

    private val _loadingCommentsForPost = MutableStateFlow<String?>(null)
    val loadingCommentsForPost: StateFlow<String?> = _loadingCommentsForPost

    private data class CoreDataBundle(
        val posts: List<BlogPostEntity>,
        val bookmarks: List<BlogPostEntity>,
        val history: List<BlogPostEntity>,
        val pages: List<BlogPageEntity>,
        val meta: BlogMetaEntity?
    )

    private data class FilterStateBundle(
        val query: String,
        val category: String?,
        val isRefreshing: Boolean,
        val hasAttemptedSync: Boolean,
        val status: Pair<String?, Boolean>
    )

    private val coreDataFlow = combine(
        repository.allPostsFlow,
        repository.bookmarkedPostsFlow,
        repository.readingHistoryFlow,
        repository.allPagesFlow,
        repository.blogMetaFlow
    ) { posts, bookmarks, history, pages, meta ->
        CoreDataBundle(posts, bookmarks, history, pages, meta)
    }

    private val filterStateFlow = combine(
        searchQueryFlow,
        selectedCategoryFlow,
        isRefreshingFlow,
        hasAttemptedInitialSync,
        statusMessageFlow
    ) { query, category, refreshing, attempted, status ->
        FilterStateBundle(query, category, refreshing, attempted, status)
    }

    val uiState: StateFlow<BlogUiState> = combine(
        coreDataFlow,
        filterStateFlow,
        prefsRepository.preferencesFlow,
        recentCommentsFlow
    ) { core, filter, prefs, comments ->
        val posts = core.posts
        val filtered = filterPosts(posts, filter.query, filter.category)
        val categories = computeCategories(posts)
        val archives = computeArchives(posts)
        val continueReading = core.history.firstOrNull { !it.isRead && it.lastScrollProgress > 0.03f }
            ?: core.history.firstOrNull { it.lastScrollProgress > 0.03f && it.lastScrollProgress < 0.95f }

        BlogUiState(
            allPosts = posts,
            filteredPosts = filtered,
            bookmarkedPosts = core.bookmarks,
            readingHistory = core.history,
            continueReadingPost = continueReading,
            pages = core.pages,
            blogMeta = core.meta,
            categories = categories,
            archives = archives,
            recentComments = comments,
            selectedCategory = filter.category,
            searchQuery = filter.query,
            isRefreshing = filter.isRefreshing,
            isInitialLoading = posts.isEmpty() && !filter.hasAttemptedSync,
            statusBannerMessage = filter.status.first,
            isStatusError = filter.status.second,
            readerPreferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BlogUiState()
    )

    init {
        refreshBlog(isUserInitiated = false)
    }

    fun refreshBlog(isUserInitiated: Boolean = true) {
        if (isRefreshingFlow.value) return
        viewModelScope.launch {
            isRefreshingFlow.value = true
            if (isUserInitiated) {
                statusMessageFlow.value = null to false
            }
            val result = repository.refreshBlog(forceNetwork = isUserInitiated)
            hasAttemptedInitialSync.value = true
            isRefreshingFlow.value = false

            if (result.isSuccess) {
                launch {
                    recentCommentsFlow.value = repository.fetchRecentComments()
                }
                if (isUserInitiated) {
                    val msg = when {
                        result.newPostsCount > 0 ->
                            "නව ලිපි ${result.newPostsCount} ක් අලුතින් එක් විය! (මුළු ලිපි ${result.totalPostsCount})"
                        else ->
                            "බ්ලොග් අඩවිය යාවත්කාලීනයි — ලිපි ${result.totalPostsCount} ක් පෙන්වයි."
                    }
                    statusMessageFlow.value = msg to false
                }
            } else {
                statusMessageFlow.value = (result.errorMessage ?: "දෝෂයක් ඇතිවිය.") to true
            }
        }
    }

    fun dismissStatusBanner() {
        statusMessageFlow.value = null to false
    }

    fun setSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun setSelectedCategory(category: String?) {
        selectedCategoryFlow.value = category
    }

    fun observePost(postId: String): Flow<BlogPostEntity?> = repository.observePost(postId)

    fun ensureFullPostLoaded(post: BlogPostEntity) {
        viewModelScope.launch {
            repository.ensureFullPostContentIfTruncated(post)
        }
    }

    fun loadCommentsForPost(numericPostId: String, postTitle: String) {
        if (numericPostId.isBlank()) return
        viewModelScope.launch {
            _loadingCommentsForPost.value = numericPostId
            val comments = repository.fetchPostComments(numericPostId, postTitle)
            _postComments.value = _postComments.value + (numericPostId to comments)
            _loadingCommentsForPost.value = null
        }
    }

    fun toggleBookmark(post: BlogPostEntity) {
        viewModelScope.launch {
            repository.toggleBookmark(post.id, post.isBookmarked)
        }
    }

    fun updateReadingProgress(postId: String, progress: Float) {
        viewModelScope.launch {
            repository.updateReadingProgress(postId, progress)
        }
    }

    fun setFontSize(sizeSp: Float) {
        viewModelScope.launch { prefsRepository.setFontSize(sizeSp) }
    }

    fun setLineHeightMultiplier(multiplier: Float) {
        viewModelScope.launch { prefsRepository.setLineHeightMultiplier(multiplier) }
    }

    fun setUseSerifFont(useSerif: Boolean) {
        viewModelScope.launch { prefsRepository.setUseSerifFont(useSerif) }
    }

    fun setReaderSurfaceMode(mode: ReaderSurfaceMode) {
        viewModelScope.launch { prefsRepository.setReaderSurfaceMode(mode) }
    }

    fun setAppThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { prefsRepository.setAppThemeMode(mode) }
    }

    fun setAmbientMusicEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setAmbientMusicEnabled(enabled) }
        if (!enabled) {
            audioEngine.stop()
        }
    }

    fun setAmbientMusicVolume(volume: Float) {
        audioEngine.setVolume(volume)
        viewModelScope.launch { prefsRepository.setAmbientMusicVolume(volume) }
    }

    fun startStoryAmbientMusic(soundscape: StorySoundscape, volume: Float) {
        audioEngine.start(soundscape, volume)
    }

    fun stopStoryAmbientMusic() {
        audioEngine.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }

    private fun filterPosts(
        posts: List<BlogPostEntity>,
        query: String,
        category: String?
    ): List<BlogPostEntity> {
        val q = query.trim().lowercase()
        return posts.filter { post ->
            val matchesCategory = category == null || post.labelsList().any {
                it.equals(category, ignoreCase = true)
            }
            val matchesQuery = q.isEmpty() ||
                post.title.lowercase().contains(q) ||
                post.excerpt.lowercase().contains(q) ||
                post.labelsDelimited.lowercase().contains(q)
            matchesCategory && matchesQuery
        }
    }

    private fun computeCategories(posts: List<BlogPostEntity>): List<CategoryInfo> {
        val grouped = linkedMapOf<String, MutableList<BlogPostEntity>>()
        for (post in posts) {
            for (label in post.labelsList()) {
                grouped.getOrPut(label) { mutableListOf() }.add(post)
            }
        }
        return grouped.entries.map { (label, labelPosts) ->
            CategoryInfo(
                name = label,
                count = labelPosts.size,
                latestPostTitle = labelPosts.firstOrNull()?.title.orEmpty()
            )
        }.sortedByDescending { it.count }
    }

    private fun computeArchives(posts: List<BlogPostEntity>): List<ArchiveGroup> {
        val cal = Calendar.getInstance()
        val grouped = linkedMapOf<Pair<Int, Int>, MutableList<BlogPostEntity>>()
        for (post in posts) {
            if (post.publishedMillis <= 0L) continue
            cal.timeInMillis = post.publishedMillis
            val key = cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
            grouped.getOrPut(key) { mutableListOf() }.add(post)
        }
        return grouped.entries.map { (ym, monthPosts) ->
            val (year, month) = ym
            ArchiveGroup(
                year = year,
                month = month,
                labelSinhala = "$year ${BloggerJsonParser.sinhalaMonthName(month)}",
                postCount = monthPosts.size,
                postIds = monthPosts.map { it.id }
            )
        }
    }
}

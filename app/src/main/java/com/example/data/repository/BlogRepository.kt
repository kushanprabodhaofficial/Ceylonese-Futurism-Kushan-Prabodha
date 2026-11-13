package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.BlogMetaEntity
import com.example.data.local.BlogPageEntity
import com.example.data.local.BlogPostEntity
import com.example.data.model.BlogComment
import com.example.data.model.ExternalBlogLink
import com.example.data.model.RefreshResult
import com.example.data.remote.BloggerApiService
import com.example.data.remote.BloggerJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class BlogRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).blogDao()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val api: BloggerApiService = Retrofit.Builder()
        .baseUrl("https://kushanprabodha.blogspot.com/")
        .client(okHttpClient)
        .build()
        .create(BloggerApiService::class.java)

    val allPostsFlow: Flow<List<BlogPostEntity>> = dao.observeAllPosts()
    val bookmarkedPostsFlow: Flow<List<BlogPostEntity>> = dao.observeBookmarkedPosts()
    val readingHistoryFlow: Flow<List<BlogPostEntity>> = dao.observeReadingHistoryPosts()
    val allPagesFlow: Flow<List<BlogPageEntity>> = dao.observeAllPages()
    val blogMetaFlow: Flow<BlogMetaEntity?> = dao.observeBlogMeta()

    fun observePost(postId: String): Flow<BlogPostEntity?> = dao.observePostById(postId)

    suspend fun refreshBlog(forceNetwork: Boolean = true): RefreshResult = withContext(Dispatchers.IO) {
        val cacheHeader = if (forceNetwork) "no-cache, no-store, must-revalidate" else null
        try {
            val allFetchedPosts = mutableListOf<BlogPostEntity>()
            var startIndex = 1
            val pageSize = 150
            var totalResults = 0
            var blogTitle = "Ceylonese Futurism"
            var blogSubtitle = ""
            var authorName = "Kushan Prabodha"
            var authorAvatarUrl: String? = null
            var blogUrl = BloggerJsonParser.BLOG_BASE_URL

            // Paginate until every post on the blog is collected
            var pageSafety = 0
            while (pageSafety < 15) {
                pageSafety++
                val response = api.getPostsFeed(
                    startIndex = startIndex,
                    maxResults = pageSize,
                    cacheControl = cacheHeader
                )
                if (!response.isSuccessful) {
                    val code = response.code()
                    return@withContext RefreshResult(
                        isSuccess = false,
                        newPostsCount = 0,
                        totalPostsCount = 0,
                        pagesCount = 0,
                        errorMessage = "බ්ලොග් අඩවියට සම්බන්ධ වීමට නොහැකි විය (HTTP $code)."
                    )
                }
                val bodyString = response.body()?.string().orEmpty()
                val parsed = BloggerJsonParser.parsePostsFeed(bodyString)
                if (pageSafety == 1) {
                    totalResults = parsed.totalResults
                    blogTitle = parsed.blogTitle
                    blogSubtitle = parsed.blogSubtitle
                    authorName = parsed.authorName
                    authorAvatarUrl = parsed.authorAvatarUrl
                    blogUrl = parsed.blogUrl
                }
                if (parsed.posts.isEmpty()) break
                allFetchedPosts.addAll(parsed.posts)

                if (allFetchedPosts.size >= totalResults || parsed.posts.size < pageSize) {
                    break
                }
                startIndex += parsed.posts.size
            }

            val distinctPosts = allFetchedPosts.distinctBy { it.id }
            val newPostsCount = dao.syncPostsPreservingUserState(distinctPosts)

            // Fetch Blogger static pages
            val pagesList = runCatching {
                val pagesResp = api.getPagesFeed(cacheControl = cacheHeader)
                if (pagesResp.isSuccessful) {
                    BloggerJsonParser.parsePagesFeed(pagesResp.body()?.string().orEmpty())
                } else emptyList()
            }.getOrDefault(emptyList())

            dao.replaceAllPages(pagesList)

            // Fetch blog homepage HTML for extra navigation links and meta description
            var navLinks: List<ExternalBlogLink> = emptyList()
            var htmlMetaDescription = ""
            runCatching {
                val homeResp = api.fetchRawUrl(BloggerJsonParser.BLOG_BASE_URL, cacheControl = cacheHeader)
                if (homeResp.isSuccessful) {
                    val html = homeResp.body()?.string().orEmpty()
                    navLinks = BloggerJsonParser.extractNavLinksFromBlogHtml(html)
                    htmlMetaDescription = BloggerJsonParser.extractMetaDescriptionFromHtml(html)
                }
            }

            val resolvedSubtitle = blogSubtitle.ifBlank { htmlMetaDescription }
                .ifBlank { "සිංහල විද්‍යා ප්‍රබන්ධ සහ අනාගතවාදී ලේඛන" }

            dao.upsertBlogMeta(
                BlogMetaEntity(
                    id = 1,
                    blogTitle = blogTitle,
                    blogSubtitle = resolvedSubtitle,
                    authorName = authorName,
                    authorAvatarUrl = authorAvatarUrl,
                    blogUrl = blogUrl,
                    navLinksDelimited = BloggerJsonParser.encodeNavLinks(navLinks),
                    lastSyncedMillis = System.currentTimeMillis(),
                    totalPostsCount = distinctPosts.size
                )
            )

            RefreshResult(
                isSuccess = true,
                newPostsCount = newPostsCount,
                totalPostsCount = distinctPosts.size,
                pagesCount = pagesList.size
            )
        } catch (e: Exception) {
            RefreshResult(
                isSuccess = false,
                newPostsCount = 0,
                totalPostsCount = 0,
                pagesCount = 0,
                errorMessage = "අන්තර්ජාල සම්බන්ධතාවය පරීක්ෂා කර නැවත උත්සාහ කරන්න."
            )
        }
    }

    suspend fun ensureFullPostContentIfTruncated(post: BlogPostEntity) = withContext(Dispatchers.IO) {
        if (!post.isSummaryOnly && post.contentHtml.length > 150) return@withContext
        runCatching {
            val response = api.fetchRawUrl(post.postUrl)
            if (response.isSuccessful) {
                val html = response.body()?.string().orEmpty()
                val fullBody = BloggerJsonParser.extractFullPostBodyFromWebHtml(html)
                if (!fullBody.isNullOrBlank() && fullBody.length > post.contentHtml.length) {
                    val plain = BloggerJsonParser.stripHtmlToPlainText(fullBody)
                    dao.updatePostFullContent(
                        postId = post.id,
                        fullHtml = fullBody,
                        excerpt = BloggerJsonParser.buildExcerpt(plain),
                        readingMinutes = BloggerJsonParser.estimateReadingMinutes(plain)
                    )
                }
            }
        }
    }

    suspend fun fetchPostComments(numericPostId: String, postTitle: String): List<BlogComment> =
        withContext(Dispatchers.IO) {
            if (numericPostId.isBlank()) return@withContext emptyList()
            runCatching {
                val resp = api.getPostCommentsFeed(
                    postId = numericPostId,
                    cacheControl = "no-cache"
                )
                if (resp.isSuccessful) {
                    BloggerJsonParser.parseCommentsFeed(
                        jsonText = resp.body()?.string().orEmpty(),
                        fallbackPostId = numericPostId,
                        fallbackPostTitle = postTitle
                    )
                } else emptyList()
            }.getOrDefault(emptyList())
        }

    suspend fun fetchRecentComments(): List<BlogComment> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = api.getRecentCommentsFeed(cacheControl = "no-cache")
            if (resp.isSuccessful) {
                BloggerJsonParser.parseCommentsFeed(resp.body()?.string().orEmpty())
            } else emptyList()
        }.getOrDefault(emptyList())
    }

    suspend fun toggleBookmark(postId: String, currentState: Boolean) = withContext(Dispatchers.IO) {
        dao.setBookmarked(postId, !currentState)
    }

    suspend fun updateReadingProgress(postId: String, progress: Float) = withContext(Dispatchers.IO) {
        val clamped = progress.coerceIn(0f, 1f)
        val existing = dao.getPostById(postId)
        val highestProgress = maxOf(existing?.lastScrollProgress ?: 0f, clamped)
        val markedRead = (existing?.isRead == true) || highestProgress >= 0.85f
        dao.updateReadingProgress(
            postId = postId,
            progress = highestProgress,
            isRead = markedRead,
            readAtMillis = System.currentTimeMillis()
        )
    }
}

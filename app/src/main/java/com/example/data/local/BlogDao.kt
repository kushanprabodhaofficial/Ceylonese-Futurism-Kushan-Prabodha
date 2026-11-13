package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BlogDao {

    @Query("SELECT * FROM blog_posts ORDER BY publishedMillis DESC")
    fun observeAllPosts(): Flow<List<BlogPostEntity>>

    @Query("SELECT * FROM blog_posts WHERE isBookmarked = 1 ORDER BY publishedMillis DESC")
    fun observeBookmarkedPosts(): Flow<List<BlogPostEntity>>

    @Query("SELECT * FROM blog_posts WHERE lastReadAtMillis > 0 ORDER BY lastReadAtMillis DESC")
    fun observeReadingHistoryPosts(): Flow<List<BlogPostEntity>>

    @Query("SELECT * FROM blog_posts WHERE id = :postId LIMIT 1")
    fun observePostById(postId: String): Flow<BlogPostEntity?>

    @Query("SELECT * FROM blog_posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): BlogPostEntity?

    @Query("SELECT * FROM blog_posts")
    suspend fun getAllPostsOnce(): List<BlogPostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<BlogPostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: BlogPostEntity)

    @Query("DELETE FROM blog_posts WHERE id NOT IN (:activeIds)")
    suspend fun deleteRemovedPosts(activeIds: List<String>)

    @Query("UPDATE blog_posts SET isBookmarked = :bookmarked WHERE id = :postId")
    suspend fun setBookmarked(postId: String, bookmarked: Boolean)

    @Query(
        "UPDATE blog_posts SET lastScrollProgress = :progress, isRead = :isRead, lastReadAtMillis = :readAtMillis WHERE id = :postId"
    )
    suspend fun updateReadingProgress(
        postId: String,
        progress: Float,
        isRead: Boolean,
        readAtMillis: Long
    )

    @Query("UPDATE blog_posts SET contentHtml = :fullHtml, isSummaryOnly = 0, excerpt = :excerpt, readingTimeMinutes = :readingMinutes WHERE id = :postId")
    suspend fun updatePostFullContent(
        postId: String,
        fullHtml: String,
        excerpt: String,
        readingMinutes: Int
    )

    @Transaction
    suspend fun syncPostsPreservingUserState(freshPosts: List<BlogPostEntity>): Int {
        if (freshPosts.isEmpty()) return 0
        val existingMap = getAllPostsOnce().associateBy { it.id }
        var newPostsCount = 0
        val merged = freshPosts.map { fresh ->
            val old = existingMap[fresh.id]
            if (old == null) {
                newPostsCount++
                fresh
            } else {
                val keepOldFullContent = !old.isSummaryOnly && fresh.isSummaryOnly
                fresh.copy(
                    contentHtml = if (keepOldFullContent) old.contentHtml else fresh.contentHtml,
                    isSummaryOnly = if (keepOldFullContent) false else fresh.isSummaryOnly,
                    excerpt = if (keepOldFullContent) old.excerpt else fresh.excerpt,
                    readingTimeMinutes = if (keepOldFullContent) old.readingTimeMinutes else fresh.readingTimeMinutes,
                    isBookmarked = old.isBookmarked,
                    isRead = old.isRead,
                    lastScrollProgress = old.lastScrollProgress,
                    lastReadAtMillis = old.lastReadAtMillis
                )
            }
        }
        insertPosts(merged)
        deleteRemovedPosts(freshPosts.map { it.id })
        return newPostsCount
    }

    @Query("SELECT * FROM blog_pages ORDER BY orderIndex ASC")
    fun observeAllPages(): Flow<List<BlogPageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<BlogPageEntity>)

    @Query("DELETE FROM blog_pages")
    suspend fun clearPages()

    @Transaction
    suspend fun replaceAllPages(pages: List<BlogPageEntity>) {
        clearPages()
        if (pages.isNotEmpty()) {
            insertPages(pages)
        }
    }

    @Query("SELECT * FROM blog_meta WHERE id = 1 LIMIT 1")
    fun observeBlogMeta(): Flow<BlogMetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlogMeta(meta: BlogMetaEntity)
}

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blog_posts")
data class BlogPostEntity(
    @PrimaryKey val id: String,
    val numericPostId: String,
    val title: String,
    val contentHtml: String,
    val isSummaryOnly: Boolean,
    val excerpt: String,
    val publishedIso: String,
    val publishedMillis: Long,
    val updatedIso: String,
    val authorName: String,
    val authorImageUrl: String?,
    val thumbnailUrl: String?,
    val postUrl: String,
    val labelsDelimited: String,
    val commentCount: Int,
    val readingTimeMinutes: Int,
    val isBookmarked: Boolean = false,
    val isRead: Boolean = false,
    val lastScrollProgress: Float = 0f,
    val lastReadAtMillis: Long = 0L
) {
    fun labelsList(): List<String> =
        if (labelsDelimited.isBlank()) emptyList()
        else labelsDelimited.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
}

@Entity(tableName = "blog_pages")
data class BlogPageEntity(
    @PrimaryKey val id: String,
    val title: String,
    val contentHtml: String,
    val excerpt: String,
    val updatedIso: String,
    val pageUrl: String,
    val orderIndex: Int
)

@Entity(tableName = "blog_meta")
data class BlogMetaEntity(
    @PrimaryKey val id: Int = 1,
    val blogTitle: String,
    val blogSubtitle: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val blogUrl: String,
    val navLinksDelimited: String,
    val lastSyncedMillis: Long,
    val totalPostsCount: Int
)

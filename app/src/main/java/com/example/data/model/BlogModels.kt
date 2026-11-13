package com.example.data.model

data class BlogComment(
    val id: String,
    val postId: String,
    val postTitle: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val publishedIso: String,
    val publishedMillis: Long,
    val contentText: String,
    val commentUrl: String
)

data class CategoryInfo(
    val name: String,
    val count: Int,
    val latestPostTitle: String
)

data class ArchiveGroup(
    val year: Int,
    val month: Int,
    val labelSinhala: String,
    val postCount: Int,
    val postIds: List<String>
)

data class ExternalBlogLink(
    val title: String,
    val url: String
)

data class RefreshResult(
    val isSuccess: Boolean,
    val newPostsCount: Int,
    val totalPostsCount: Int,
    val pagesCount: Int,
    val errorMessage: String? = null
)

enum class ReaderSurfaceMode {
    FOLLOW_APP,
    COSMIC_DARK,
    WARM_PAPER,
    SEPIA
}

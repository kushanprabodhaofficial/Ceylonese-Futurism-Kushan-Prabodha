package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val totalScore: Long = 0L,
    val storiesCompleted: Long = 0L,
    val lastStoryTitle: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudStoryScore(
    @DocumentId val id: String = "",
    val userId: String = "",
    val storyId: String = "",
    val postId: String = "",
    val postTitle: String = "",
    val score: Long = 0L,
    val maxScore: Long = 0L,
    val correctAnswers: Long = 0L,
    val totalQuestions: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class StoryQuizQuestion(
    val questionSinhala: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationSinhala: String
)

data class StoryQuiz(
    val postId: String,
    val postTitle: String,
    val questions: List<StoryQuizQuestion>,
    val visualScenePrompt: String
)

object StoryIdMapper {
    private val validRegex = Regex("^[a-zA-Z0-9_\\-]+$")

    fun safeFirestoreStoryId(postId: String, numericPostId: String): String {
        val numericCleaned = numericPostId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        if (numericCleaned.isNotBlank() && validRegex.matches(numericCleaned)) {
            return "story_${numericCleaned.take(100)}"
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(postId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(32)
        return "story_$digest"
    }
}

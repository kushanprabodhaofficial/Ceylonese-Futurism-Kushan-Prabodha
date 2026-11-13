package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.StoryScoreEntity
import com.example.data.model.CloudStoryScore
import com.example.data.model.LeaderboardEntry
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LeaderboardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Google Sign-In required before accessing Firestore leaderboard.")
    }

    fun observeLeaderboard(limitCount: Long = 50L): Flow<List<LeaderboardEntry>> {
        val path = COLLECTION_LEADERBOARD
        return db.collection(COLLECTION_LEADERBOARD)
            .orderBy("totalScore", Query.Direction.DESCENDING)
            .limit(limitCount)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LeaderboardEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                }
                throw error
            }
    }

    fun observeUserStoryScores(userId: String): Flow<List<CloudStoryScore>> {
        val path = "$COLLECTION_USERS/$userId/$COLLECTION_STORY_SCORES"
        return db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_STORY_SCORES)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudStoryScore::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                }
                throw error
            }
    }

    suspend fun upsertStoryScore(score: StoryScoreEntity): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_STORY_SCORES)
            .document(score.storyId)

        val safeTitle = score.postTitle.trim().ifBlank { "Story" }.take(200)
        val safePostId = score.postId.trim().ifBlank { score.storyId }.take(200)
        val clampedMax = score.maxScore.coerceIn(1, 1000)
        val clampedScore = score.score.coerceIn(0, clampedMax)
        val clampedTotalQ = score.totalQuestions.coerceIn(1, 20)
        val clampedCorrect = score.correctAnswers.coerceIn(0, clampedTotalQ)

        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "postTitle" to safeTitle,
                        "score" to clampedScore,
                        "maxScore" to clampedMax,
                        "correctAnswers" to clampedCorrect,
                        "totalQuestions" to clampedTotalQ,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "storyId" to score.storyId,
                        "postId" to safePostId,
                        "postTitle" to safeTitle,
                        "score" to clampedScore,
                        "maxScore" to clampedMax,
                        "correctAnswers" to clampedCorrect,
                        "totalQuestions" to clampedTotalQ,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = OperationType.WRITE
            handleFirestoreError(e, op, docRef.path, auth)
            throw e
        }
    }

    suspend fun publishLeaderboardSummary(
        displayName: String,
        totalScore: Int,
        storiesCompleted: Int,
        lastStoryTitle: String?
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_LEADERBOARD).document(uid)
        val cleanName = displayName.trim().ifBlank { "Ceylon Reader" }.take(40)
        val cleanScore = totalScore.coerceIn(0, 1_000_000)
        val cleanCount = storiesCompleted.coerceIn(0, 1_000)
        val cleanLastTitle = lastStoryTitle?.trim()?.take(160)?.takeIf { it.isNotBlank() }

        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = buildMap<String, Any> {
                    put("displayName", cleanName)
                    put("totalScore", cleanScore)
                    put("storiesCompleted", cleanCount)
                    if (cleanLastTitle != null) {
                        put("lastStoryTitle", cleanLastTitle)
                    }
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(updates).await()
            } else {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", cleanName)
                    put("totalScore", cleanScore)
                    put("storiesCompleted", cleanCount)
                    if (cleanLastTitle != null) {
                        put("lastStoryTitle", cleanLastTitle)
                    }
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            throw e
        }
    }

    companion object {
        const val COLLECTION_LEADERBOARD = "leaderboard"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_STORY_SCORES = "story_scores"
    }
}

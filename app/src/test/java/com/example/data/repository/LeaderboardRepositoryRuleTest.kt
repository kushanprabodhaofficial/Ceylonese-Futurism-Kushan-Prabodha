package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.StoryScoreEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveStoryScoreAndLeaderboard_authenticatedOwner_succeedsAndStreams() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = LeaderboardRepository(firestore, auth)

        val scoreEntity = StoryScoreEntity(
            postId = "tag:blogger.com,1999:blog-1.post-777",
            storyId = "story_777",
            postTitle = "නියෝන් සඳ",
            score = 75,
            maxScore = 100,
            correctAnswers = 3,
            totalQuestions = 4,
            earnedAtMillis = System.currentTimeMillis()
        )

        val scoreSaveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertStoryScore(scoreEntity)
        }
        assertTrue("Story score save failed: ${scoreSaveResult.exceptionOrNull()}", scoreSaveResult.isSuccess)

        val leaderboardResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.publishLeaderboardSummary(
                displayName = "Alice Explorer",
                totalScore = 75,
                storiesCompleted = 1,
                lastStoryTitle = "නියෝන් සඳ"
            )
        }
        assertTrue("Leaderboard publish failed: ${leaderboardResult.exceptionOrNull()}", leaderboardResult.isSuccess)

        val scores = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserStoryScores(aliceUid).first { list ->
                list.any { it.storyId == "story_777" }
            }
        }
        assertEquals(75L, scores.first { it.storyId == "story_777" }.score)

        val leaderboard = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeLeaderboard().first { list ->
                list.any { it.userId == aliceUid && it.displayName == "Alice Explorer" }
            }
        }
        assertEquals(75L, leaderboard.first { it.userId == aliceUid }.totalScore)
    }

    @Test
    fun observeUserStoryScores_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = LeaderboardRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertStoryScore(
                StoryScoreEntity(
                    postId = "post-888",
                    storyId = "story_888",
                    postTitle = "කාල තරණය",
                    score = 100,
                    maxScore = 100,
                    correctAnswers = 4,
                    totalQuestions = 4,
                    earnedAtMillis = System.currentTimeMillis()
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = LeaderboardRepository(firestore, auth)
        var captured: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeUserStoryScores(aliceUid).first()
            }
        } catch (t: Throwable) {
            captured = t
        }
        val firestoreException = captured?.unwrapFirestoreException()
        assertNotNull("Expected FirestoreException, got $captured", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeLeaderboard_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = LeaderboardRepository(firestore, auth)

        var captured: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeLeaderboard().first()
            }
        } catch (t: Throwable) {
            captured = t
        }
        val firestoreException = captured?.unwrapFirestoreException()
        assertNotNull("Expected FirestoreException, got $captured", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_cf@test.com"
        const val BOB_EMAIL = "bob_cf@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}

package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.local.BlogPostEntity
import com.example.data.local.StoryScoreEntity
import com.example.data.model.LeaderboardEntry
import com.example.data.preferences.ReaderPreferencesRepository
import com.example.data.repository.BlogRepository
import com.example.data.repository.LeaderboardRepository
import com.example.ui.auth.authStateFlow
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "ScoreboardVM"

sealed interface LeaderboardCloudState {
    data object Unauthenticated : LeaderboardCloudState
    data object Loading : LeaderboardCloudState
    data class Success(val entries: List<LeaderboardEntry>) : LeaderboardCloudState
    data class Error(val message: String) : LeaderboardCloudState
}

data class ScoreboardUiState(
    val totalScore: Int = 0,
    val totalMaxScore: Int = 0,
    val storiesCompletedCount: Int = 0,
    val totalStoriesCount: Int = 0,
    val storyScores: List<StoryScoreEntity> = emptyList(),
    val unattemptedPosts: List<BlogPostEntity> = emptyList(),
    val savedDisplayName: String = "",
    val currentUser: FirebaseUser? = null,
    val cloudLeaderboardState: LeaderboardCloudState = LeaderboardCloudState.Unauthenticated,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ScoreboardViewModel(
    private val blogRepository: BlogRepository,
    private val leaderboardRepository: LeaderboardRepository,
    private val prefsRepository: ReaderPreferencesRepository,
    private val auth: FirebaseAuth = Firebase.auth
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val currentUserFlow = auth.authStateFlow()

    // Strictly auth-gated: zero Firestore queries when currentUser == null
    private val cloudLeaderboardFlow: StateFlow<LeaderboardCloudState> = currentUserFlow
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(LeaderboardCloudState.Unauthenticated)
            } else {
                leaderboardRepository.observeLeaderboard(limitCount = 50L)
                    .map<List<LeaderboardEntry>, LeaderboardCloudState> { entries ->
                        LeaderboardCloudState.Success(entries)
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing leaderboard", rootError)
                        emit(
                            LeaderboardCloudState.Error(
                                rootError.localizedMessage ?: "Leaderboard දත්ත ලබාගැනීමට නොහැකි විය."
                            )
                        )
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = LeaderboardCloudState.Unauthenticated
        )

    private val coreScoreFlow = combine(
        blogRepository.allStoryScoresFlow,
        blogRepository.allPostsFlow,
        prefsRepository.preferencesFlow
    ) { scores, posts, prefs ->
        Triple(scores, posts, prefs.readerDisplayName)
    }

    val uiState: StateFlow<ScoreboardUiState> = combine(
        coreScoreFlow,
        currentUserFlow,
        cloudLeaderboardFlow,
        _isSyncing,
        _statusMessage
    ) { core, user, cloudState, syncing, status ->
        val scores = core.first
        val posts = core.second
        val savedName = core.third
        val completedIds = scores.map { it.postId }.toSet()
        val unattempted = posts.filter { it.id !in completedIds }

        ScoreboardUiState(
            totalScore = scores.sumOf { it.score },
            totalMaxScore = scores.sumOf { it.maxScore },
            storiesCompletedCount = scores.size,
            totalStoriesCount = posts.size,
            storyScores = scores,
            unattemptedPosts = unattempted,
            savedDisplayName = savedName,
            currentUser = user,
            cloudLeaderboardState = cloudState,
            isSyncing = syncing,
            statusMessage = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ScoreboardUiState()
    )

    fun saveDisplayName(newName: String) {
        val cleaned = newName.trim().take(40)
        viewModelScope.launch {
            prefsRepository.setReaderDisplayName(cleaned)
            val user = auth.currentUser
            if (user != null) {
                syncAllScoresToCloud(explicitName = cleaned)
            } else {
                _statusMessage.value = "ඔබගේ නම සුරැකිණි. Leaderboard වෙත එක්වීමට Google ගිණුමෙන් සම්බන්ධ වන්න."
            }
        }
    }

    fun onUserAuthenticated() {
        viewModelScope.launch {
            syncAllScoresToCloud()
        }
    }

    fun syncAllScoresToCloud(explicitName: String? = null) {
        val user = auth.currentUser ?: return
        if (_isSyncing.value) return

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val localScores = blogRepository.getAllStoryScoresOnce()
                val syncedIds = mutableListOf<String>()
                for (score in localScores) {
                    if (!score.syncedToCloud) {
                        val res = leaderboardRepository.upsertStoryScore(score)
                        if (res.isSuccess) {
                            syncedIds.add(score.postId)
                        }
                    }
                }
                if (syncedIds.isNotEmpty()) {
                    blogRepository.markScoresSynced(syncedIds)
                }

                val currentPrefName = explicitName
                    ?: uiState.value.savedDisplayName.takeIf { it.isNotBlank() }
                    ?: user.displayName?.takeIf { it.isNotBlank() }
                    ?: "Ceylon Reader"

                if (uiState.value.savedDisplayName.isBlank() && currentPrefName.isNotBlank()) {
                    prefsRepository.setReaderDisplayName(currentPrefName)
                }

                val totalScore = localScores.sumOf { it.score }
                val latestTitle = localScores.maxByOrNull { it.earnedAtMillis }?.postTitle

                val publishRes = leaderboardRepository.publishLeaderboardSummary(
                    displayName = currentPrefName,
                    totalScore = totalScore,
                    storiesCompleted = localScores.size,
                    lastStoryTitle = latestTitle
                )
                _statusMessage.value = if (publishRes.isSuccess) {
                    "Leaderboard සහ ලකුණු සාර්ථකව සමමුහුර්ත විය!"
                } else {
                    "Leaderboard යාවත්කාලීන කිරීම අසාර්ථක විය: ${publishRes.exceptionOrNull()?.localizedMessage}"
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun showStatusMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                ScoreboardViewModel(
                    blogRepository = BlogRepository(app),
                    leaderboardRepository = LeaderboardRepository(db),
                    prefsRepository = ReaderPreferencesRepository(app)
                )
            }
        }
    }
}

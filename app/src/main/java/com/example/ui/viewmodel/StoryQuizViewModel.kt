package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
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
import com.example.data.model.StoryIdMapper
import com.example.data.model.StoryQuiz
import com.example.data.preferences.ReaderPreferencesRepository
import com.example.data.repository.AiStoryRepository
import com.example.data.repository.BlogRepository
import com.example.data.repository.LeaderboardRepository
import com.example.ui.util.ImageDownloadHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "StoryQuizVM"
private const val POINTS_PER_CORRECT_ANSWER = 25

sealed interface QuizStageState {
    data object Idle : QuizStageState
    data object Loading : QuizStageState
    data class InProgress(
        val quiz: StoryQuiz,
        val selectedAnswers: Map<Int, Int> = emptyMap()
    ) : QuizStageState
    data class Completed(
        val quiz: StoryQuiz,
        val selectedAnswers: Map<Int, Int>,
        val score: Int,
        val maxScore: Int,
        val correctCount: Int
    ) : QuizStageState
    data class Error(val message: String) : QuizStageState
}

sealed interface SceneImageState {
    data object Idle : SceneImageState
    data object Loading : SceneImageState
    data class Success(val bitmap: Bitmap, val promptUsed: String) : SceneImageState
    data class Error(val message: String) : SceneImageState
}

class StoryQuizViewModel(
    private val postId: String,
    private val aiRepository: AiStoryRepository,
    private val blogRepository: BlogRepository,
    private val leaderboardRepository: LeaderboardRepository,
    private val prefsRepository: ReaderPreferencesRepository
) : ViewModel() {

    private val _quizState = MutableStateFlow<QuizStageState>(QuizStageState.Idle)
    val quizState: StateFlow<QuizStageState> = _quizState.asStateFlow()

    private val _imageState = MutableStateFlow<SceneImageState>(SceneImageState.Idle)
    val imageState: StateFlow<SceneImageState> = _imageState.asStateFlow()

    private val _editableScenePrompt = MutableStateFlow("")
    val editableScenePrompt: StateFlow<String> = _editableScenePrompt.asStateFlow()

    private val _downloadMessage = MutableStateFlow<String?>(null)
    val downloadMessage: StateFlow<String?> = _downloadMessage.asStateFlow()

    private val _isSavingImage = MutableStateFlow(false)
    val isSavingImage: StateFlow<Boolean> = _isSavingImage.asStateFlow()

    val savedStoryScore = blogRepository.observeStoryScore(postId)

    fun startQuiz(post: BlogPostEntity) {
        viewModelScope.launch {
            _quizState.value = QuizStageState.Loading
            try {
                val quiz = aiRepository.generateStoryQuiz(
                    postId = post.id,
                    postTitle = post.title,
                    storyHtml = post.contentHtml,
                    storyExcerpt = post.excerpt
                )
                _editableScenePrompt.value = quiz.visualScenePrompt
                _quizState.value = QuizStageState.InProgress(quiz = quiz)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating quiz", e)
                _quizState.value = QuizStageState.Error(
                    e.localizedMessage ?: "ප්‍රශ්නාවලිය සැකසීම අසාර්ථක විය."
                )
            }
        }
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        val current = _quizState.value as? QuizStageState.InProgress ?: return
        val updated = current.selectedAnswers.toMutableMap().apply {
            this[questionIndex] = optionIndex
        }
        _quizState.value = current.copy(selectedAnswers = updated)
    }

    fun submitQuiz(post: BlogPostEntity) {
        val current = _quizState.value as? QuizStageState.InProgress ?: return
        val quiz = current.quiz
        if (current.selectedAnswers.size < quiz.questions.size) return

        var correctCount = 0
        quiz.questions.forEachIndexed { idx, q ->
            if (current.selectedAnswers[idx] == q.correctIndex) {
                correctCount++
            }
        }
        val totalQ = quiz.questions.size
        val score = correctCount * POINTS_PER_CORRECT_ANSWER
        val maxScore = totalQ * POINTS_PER_CORRECT_ANSWER

        _quizState.value = QuizStageState.Completed(
            quiz = quiz,
            selectedAnswers = current.selectedAnswers,
            score = score,
            maxScore = maxScore,
            correctCount = correctCount
        )

        val eventHighlights = quiz.questions
            .take(2)
            .joinToString("; ") { q ->
                val ans = q.options.getOrNull(q.correctIndex).orEmpty()
                if (ans.isNotBlank()) ans else q.questionSinhala
            }

        viewModelScope.launch {
            val entity = StoryScoreEntity(
                postId = post.id,
                storyId = StoryIdMapper.safeFirestoreStoryId(post.id, post.numericPostId),
                postTitle = post.title,
                score = score,
                maxScore = maxScore,
                correctAnswers = correctCount,
                totalQuestions = totalQ,
                earnedAtMillis = System.currentTimeMillis(),
                syncedToCloud = false
            )
            blogRepository.saveStoryScoreLocal(entity)

            // If reader has authenticated with Google, sync score & leaderboard immediately
            val user = Firebase.auth.currentUser
            if (user != null) {
                val prefs = prefsRepository.preferencesFlow.first()
                val chosenName = prefs.readerDisplayName.ifBlank {
                    user.displayName?.takeIf { it.isNotBlank() } ?: "Ceylon Reader"
                }
                val upsertResult = leaderboardRepository.upsertStoryScore(entity)
                if (upsertResult.isSuccess) {
                    blogRepository.markScoresSynced(listOf(entity.postId))
                }
                val allScores = blogRepository.getAllStoryScoresOnce()
                val totalScore = allScores.sumOf { it.score }
                leaderboardRepository.publishLeaderboardSummary(
                    displayName = chosenName,
                    totalScore = totalScore,
                    storiesCompleted = allScores.size,
                    lastStoryTitle = post.title
                )
            }
        }

        // Automatically generate a story & quiz event illustration via Gemini Nano Banana 2.1
        if (_imageState.value is SceneImageState.Idle) {
            generateStorySceneImage(
                storyTitle = post.title,
                promptOverride = _editableScenePrompt.value.ifBlank { quiz.visualScenePrompt },
                quizHighlights = eventHighlights
            )
        }
    }

    fun updateScenePrompt(newPrompt: String) {
        _editableScenePrompt.value = newPrompt
    }

    fun generateStorySceneImage(
        storyTitle: String,
        promptOverride: String? = null,
        quizHighlights: String = ""
    ) {
        val promptToUse = (promptOverride ?: _editableScenePrompt.value).trim()
        if (promptToUse.isBlank()) {
            _imageState.value = SceneImageState.Error("රූපය නිර්මාණය කිරීම සඳහා විස්තරයක් ඇතුළත් කරන්න.")
            return
        }
        viewModelScope.launch {
            _imageState.value = SceneImageState.Loading
            try {
                val bitmap = aiRepository.generateStorySceneImage(
                    storyTitle = storyTitle,
                    userPrompt = promptToUse,
                    quizHighlights = quizHighlights
                )
                _imageState.value = SceneImageState.Success(bitmap = bitmap, promptUsed = promptToUse)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating scene image", e)
                _imageState.value = SceneImageState.Error(
                    e.localizedMessage ?: "රූපය ජනනය කිරීම අසාර්ථක විය."
                )
            }
        }
    }

    fun editCurrentSceneImage(editInstruction: String) {
        val currentSuccess = _imageState.value as? SceneImageState.Success ?: return
        val instruction = editInstruction.trim()
        if (instruction.isBlank()) return

        viewModelScope.launch {
            _imageState.value = SceneImageState.Loading
            try {
                val editedBitmap = aiRepository.editStorySceneImage(
                    currentBitmap = currentSuccess.bitmap,
                    editInstruction = instruction
                )
                _imageState.value = SceneImageState.Success(
                    bitmap = editedBitmap,
                    promptUsed = instruction
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error editing scene image", e)
                _imageState.value = SceneImageState.Error(
                    e.localizedMessage ?: "රූපය සංස්කරණය කිරීම අසාර්ථක විය."
                )
            }
        }
    }

    fun downloadGeneratedImage(context: Context, storyTitle: String) {
        val currentSuccess = _imageState.value as? SceneImageState.Success ?: return
        if (_isSavingImage.value) return
        viewModelScope.launch {
            _isSavingImage.value = true
            val result = ImageDownloadHelper.saveBitmapToPictures(
                context = context,
                bitmap = currentSuccess.bitmap,
                storyTitle = storyTitle
            )
            _isSavingImage.value = false
            _downloadMessage.value = if (result.isSuccess) {
                "රූපය ගැලරියට සුරැකිණි (${result.getOrNull()})"
            } else {
                "රූපය සුරැකීම අසාර්ථක විය: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearDownloadMessage() {
        _downloadMessage.value = null
    }

    companion object {
        fun provideFactory(postId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                StoryQuizViewModel(
                    postId = postId,
                    aiRepository = AiStoryRepository(),
                    blogRepository = BlogRepository(app),
                    leaderboardRepository = LeaderboardRepository(db),
                    prefsRepository = ReaderPreferencesRepository(app)
                )
            }
        }
    }
}

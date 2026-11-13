package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.StoryQuiz
import com.example.data.model.StoryQuizQuestion
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiStoryRepository"

class AiStoryRepository {

    private val quizSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(description = "Comprehension question in Sinhala script about key events or characters in the story."),
                        "options" to Schema.array(
                            items = Schema.string(description = "One of four multiple-choice options in Sinhala script."),
                            description = "Exactly 4 answer choices in Sinhala."
                        ),
                        "correctIndex" to Schema.integer(description = "Zero-based index (0 to 3) of the correct option."),
                        "explanation" to Schema.string(description = "Brief explanation in Sinhala referencing what happened in the story.")
                    )
                ),
                description = "Exactly 4 comprehension questions in Sinhala."
            ),
            "visualScenePrompt" to Schema.string(
                description = "Vivid English illustration prompt capturing the story's signature sci-fi events, Ceylonese futurism setting, characters, and atmosphere."
            )
        )
    )

    private val quizModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
            }
        )
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    suspend fun generateStoryQuiz(
        postId: String,
        postTitle: String,
        storyHtml: String,
        storyExcerpt: String
    ): StoryQuiz {
        val plainStory = stripHtmlForPrompt(storyHtml).ifBlank { storyExcerpt }.take(6500)
        val prompt = """
            You are creating an engaging end-of-story comprehension quiz for readers of a Sinhala science fiction blog ("Ceylonese Futurism" by Kushan Prabodha).
            Story Title: $postTitle
            Story Text:
            $plainStory

            Tasks:
            1. Write exactly 4 comprehension questions in natural Sinhala (සිංහල) based strictly on specific events, technology, characters, or plot points from this story.
            2. Provide exactly 4 Sinhala options per question and set correctIndex (0..3).
            3. Provide a concise 1-sentence Sinhala explanation for each answer.
            4. Write visualScenePrompt in English (2-3 descriptive sentences) depicting a dramatic, cinematic Ceylonese Futurism sci-fi illustration of this story's pivotal events (do not include text overlays in the scene).
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                quizModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI ආකෘතියෙන් ප්‍රතිචාරයක් නොලැබුණි.")
            val root = JSONObject(rawJson)
            val questionsArr = root.getJSONArray("questions")
            val parsedQuestions = buildList {
                for (i in 0 until questionsArr.length()) {
                    val item = questionsArr.getJSONObject(i)
                    val optionsJson = item.getJSONArray("options")
                    val options = (0 until optionsJson.length()).map { optionsJson.getString(it) }
                    if (options.size >= 2) {
                        val correctIdx = item.getInt("correctIndex").coerceIn(0, options.lastIndex)
                        add(
                            StoryQuizQuestion(
                                questionSinhala = item.getString("question"),
                                options = options,
                                correctIndex = correctIdx,
                                explanationSinhala = item.optString("explanation", "")
                            )
                        )
                    }
                }
            }
            if (parsedQuestions.isEmpty()) {
                throw IllegalStateException("ප්‍රශ්නාවලිය සකස් කිරීමට නොහැකි විය. නැවත උත්සාහ කරන්න.")
            }
            val scenePrompt = root.optString(
                "visualScenePrompt",
                "Cinematic Ceylonese futurism sci-fi illustration inspired by $postTitle, tropical cyberpunk horizon, glowing golden Sri Lankan geometric motifs, dramatic lighting"
            )
            return StoryQuiz(
                postId = postId,
                postTitle = postTitle,
                questions = parsedQuestions,
                visualScenePrompt = scenePrompt
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Quiz generation timed out", e)
            throw IllegalStateException("ප්‍රශ්නාවලිය සැකසීමට වැඩි වේලාවක් ගතවිය. කරුණාකර නැවත උත්සාහ කරන්න.", e)
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Quiz generation quota exceeded", e)
            throw IllegalStateException("AI සේවා සීමාව ඉක්මවා ඇත. මඳ වේලාවකින් නැවත උත්සාහ කරන්න.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Quiz generation failed", e)
            throw e
        }
    }

    suspend fun generateStorySceneImage(
        storyTitle: String,
        userPrompt: String,
        quizHighlights: String
    ): Bitmap {
        val fullPrompt = buildString {
            append("Create a vivid cinematic sci-fi book illustration in Ceylonese Futurism style (no text, no watermarks). ")
            append("Story context ($storyTitle): ")
            append(userPrompt.trim())
            if (quizHighlights.isNotBlank()) {
                append(". Key story events highlighted: ")
                append(quizHighlights.trim())
            }
        }
        try {
            val response = withTimeout(60_000L) {
                imageModel.generateContent(fullPrompt)
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            return imagePart?.image
                ?: throw IllegalStateException("රූපය ජනනය කිරීමේදී ප්‍රතිදාන රූපයක් නොලැබුණි.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            throw IllegalStateException("රූපය ජනනය කිරීමට තත්පර 60 ඉක්මවීය. කරුණාකර නැවත උත්සාහ කරන්න.", e)
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image generation quota exceeded", e)
            throw IllegalStateException("AI රූප ජනන සීමාව ඉක්මවා ඇත (Blaze/Quota). මඳ වේලාවකින් උත්සාහ කරන්න.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            throw e
        }
    }

    suspend fun editStorySceneImage(
        currentBitmap: Bitmap,
        editInstruction: String
    ): Bitmap {
        try {
            val response = withTimeout(60_000L) {
                imageModel.generateContent(
                    content {
                        image(currentBitmap)
                        text(editInstruction.trim())
                    }
                )
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            return imagePart?.image
                ?: throw IllegalStateException("රූපය සංස්කරණය කිරීමේදී ප්‍රතිදාන රූපයක් නොලැබුණි.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image edit timed out", e)
            throw IllegalStateException("රූපය සංස්කරණය කිරීමට වැඩි වේලාවක් ගතවිය.", e)
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image edit quota exceeded", e)
            throw IllegalStateException("AI රූප සංස්කරණ සීමාව ඉක්මවා ඇත.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image edit failed", e)
            throw e
        }
    }

    private fun stripHtmlForPrompt(html: String): String {
        return html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

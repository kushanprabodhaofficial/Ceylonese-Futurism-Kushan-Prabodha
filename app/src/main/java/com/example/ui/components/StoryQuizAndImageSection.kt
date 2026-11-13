package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.BlogPostEntity
import com.example.data.local.StoryScoreEntity
import com.example.ui.theme.NotoSerifSinhalaFamily
import com.example.ui.viewmodel.QuizStageState
import com.example.ui.viewmodel.SceneImageState

@Composable
fun StoryQuizAndImageSection(
    post: BlogPostEntity,
    savedScore: StoryScoreEntity?,
    quizState: QuizStageState,
    imageState: SceneImageState,
    editableScenePrompt: String,
    isSavingImage: Boolean,
    downloadMessage: String?,
    accentColor: Color,
    surfaceColor: Color,
    textColor: Color,
    secondaryColor: Color,
    onStartQuiz: () -> Unit,
    onSelectAnswer: (Int, Int) -> Unit,
    onSubmitQuiz: () -> Unit,
    onScenePromptChange: (String) -> Unit,
    onGenerateImage: () -> Unit,
    onEditImage: (String) -> Unit,
    onDownloadImage: () -> Unit,
    onNavigateToScoreboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var editInstructionInput by remember { mutableStateOf("") }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("story_quiz_section_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.16f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Quiz,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "කතාව අවසන් — ප්‍රශ්නාවලිය සහ AI දර්ශන රූපය",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NotoSerifSinhalaFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor
                    )
                    Text(
                        text = "ප්‍රශ්නවලට පිළිතුරු දී ලකුණු රැස් කර, කතාවේ සිදුවීම් ඇසුරෙන් රූපයක් නිර්මාණය කර බාගන්න.",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor
                    )
                }
            }

            if (savedScore != null && quizState !is QuizStageState.Completed) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = accentColor.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ඔබ ලබාගත් ලකුණු: ${savedScore.score} / ${savedScore.maxScore} (${savedScore.correctAnswers}/${savedScore.totalQuestions})",
                                style = MaterialTheme.typography.labelLarge,
                                color = textColor
                            )
                        }
                    }
                }
            }

            when (quizState) {
                is QuizStageState.Idle -> {
                    Button(
                        onClick = onStartQuiz,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("start_story_quiz_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (savedScore == null) {
                                "ප්‍රශ්නාවලිය අරඹන්න"
                            } else {
                                "නැවත ප්‍රශ්නාවලියට පිළිතුරු දෙන්න"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is QuizStageState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = accentColor)
                        Text(
                            text = "කතාව ඇසුරෙන් ප්‍රශ්න 4ක් සකසමින් පවතී...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryColor
                        )
                    }
                }

                is QuizStageState.Error -> {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = quizState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            OutlinedButton(
                                onClick = onStartQuiz,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("නැවත උත්සාහ කරන්න")
                            }
                        }
                    }
                }

                is QuizStageState.InProgress -> {
                    val totalQ = quizState.quiz.questions.size
                    val answeredCount = quizState.selectedAnswers.size

                    Text(
                        text = "පිළිතුරු සැපයූ ප්‍රශ්න: $answeredCount / $totalQ",
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor
                    )

                    quizState.quiz.questions.forEachIndexed { qIdx, question ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${qIdx + 1}. ${question.questionSinhala}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = textColor
                                )

                                question.options.forEachIndexed { optIdx, optText ->
                                    val selected = quizState.selectedAnswers[qIdx] == optIdx
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) accentColor.copy(alpha = 0.16f) else Color.Transparent,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) accentColor else secondaryColor.copy(alpha = 0.25f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onSelectAnswer(qIdx, optIdx) }
                                            .testTag("quiz_q_${qIdx}_opt_$optIdx")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selected,
                                                onClick = { onSelectAnswer(qIdx, optIdx) },
                                                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = optText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onSubmitQuiz,
                        enabled = answeredCount == totalQ,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("submit_story_quiz_button")
                    ) {
                        Text(
                            text = if (answeredCount == totalQ) {
                                "ලකුණු ලබාගෙන සිදුවීම් රූපය සාදන්න"
                            } else {
                                "සියලු ප්‍රශ්නවලට පිළිතුරු තෝරන්න ($answeredCount/$totalQ)"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is QuizStageState.Completed -> {
                    // Score Summary Banner
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = accentColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "ඔබ ලබාගත් ලකුණු: ${quizState.score} / ${quizState.maxScore}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = textColor
                                        )
                                        Text(
                                            text = "නිවැරදි පිළිතුරු ${quizState.correctCount} / ${quizState.quiz.questions.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = secondaryColor
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = onStartQuiz,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("නැවත කරන්න")
                                }
                                OutlinedButton(
                                    onClick = onNavigateToScoreboard,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("quiz_to_scoreboard_button")
                                ) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Leaderboard")
                                }
                            }
                        }
                    }

                    // Question-by-Question Explanations
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        quizState.quiz.questions.forEachIndexed { idx, question ->
                            val userAnswer = quizState.selectedAnswers[idx] ?: -1
                            val isCorrect = userAnswer == question.correctIndex
                            val statusColor = if (isCorrect) Color(0xFF22C55E) else Color(0xFFEF4444)

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = statusColor.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${idx + 1}. ${question.questionSinhala}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = textColor
                                        )
                                    }
                                    Text(
                                        text = "නිවැරදි පිළිතුර: ${question.options.getOrNull(question.correctIndex).orEmpty()}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = statusColor
                                    )
                                    if (question.explanationSinhala.isNotBlank()) {
                                        Text(
                                            text = question.explanationSinhala,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = secondaryColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = secondaryColor.copy(alpha = 0.25f))

                    // AI Story & Quiz Scene Image Generator + Editor + Downloader
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "කතාවේ සිදුවීම් AI රූපය (Story Event Scene)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = NotoSerifSinhalaFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = textColor
                            )
                        }

                        OutlinedTextField(
                            value = editableScenePrompt,
                            onValueChange = onScenePromptChange,
                            label = { Text("දර්ශන විස්තරය (Prompt)") },
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scene_prompt_input")
                        )

                        Button(
                            onClick = onGenerateImage,
                            enabled = imageState !is SceneImageState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("generate_scene_image_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (imageState is SceneImageState.Success) {
                                    "නව රූපයක් ජනනය කරන්න"
                                } else {
                                    "සිදුවීම් රූපය ජනනය කරන්න"
                                }
                            )
                        }

                        when (imageState) {
                            is SceneImageState.Idle -> Unit

                            is SceneImageState.Loading -> {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(color = accentColor)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Gemini AI මගින් කතාවේ රූපය අඳිමින් පවතී (තත්පර 30–60ක් ගතවිය හැක)...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = secondaryColor
                                        )
                                    }
                                }
                            }

                            is SceneImageState.Error -> {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = imageState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        OutlinedButton(onClick = onGenerateImage) {
                                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("නැවත උත්සාහ කරන්න")
                                        }
                                    }
                                }
                            }

                            is SceneImageState.Success -> {
                                Image(
                                    bitmap = imageState.bitmap.asImageBitmap(),
                                    contentDescription = "${post.title} AI illustration",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(MaterialTheme.shapes.medium)
                                        .testTag("generated_story_image")
                                )

                                Button(
                                    onClick = onDownloadImage,
                                    enabled = !isSavingImage,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("download_generated_image_button")
                                ) {
                                    if (isSavingImage) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSavingImage) "සුරකිමින් පවතී..." else "රූපය ඩවුන්ලෝඩ් කරන්න (Download)",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Interactive Image Editing with Prompt
                                OutlinedTextField(
                                    value = editInstructionInput,
                                    onValueChange = { editInstructionInput = it },
                                    label = { Text("රූපය වෙනස් කිරීමට උපදෙසක් දෙන්න (Edit Prompt)") },
                                    placeholder = { Text("උදා: Add glowing golden Sinhala holograms in the sky") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("edit_image_prompt_input")
                                )
                                OutlinedButton(
                                    onClick = {
                                        if (editInstructionInput.isNotBlank()) {
                                            onEditImage(editInstructionInput)
                                        }
                                    },
                                    enabled = editInstructionInput.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("edit_generated_image_button")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("රූපය සංස්කරණය කරන්න (AI Edit)")
                                }
                            }
                        }

                        if (!downloadMessage.isNullOrBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = accentColor.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = downloadMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

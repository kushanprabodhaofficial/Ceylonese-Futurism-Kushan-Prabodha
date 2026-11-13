package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.local.BlogPostEntity
import com.example.data.local.StoryScoreEntity
import com.example.data.model.LeaderboardEntry
import com.example.ui.auth.launchInteractiveGoogleSignIn
import com.example.ui.auth.signOutGoogleAccount
import com.example.ui.theme.NotoSerifSinhalaFamily
import com.example.ui.viewmodel.LeaderboardCloudState
import com.example.ui.viewmodel.ScoreboardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreboardScreen(
    uiState: ScoreboardUiState,
    onSaveDisplayName: (String) -> Unit,
    onAuthSuccess: () -> Unit,
    onSyncNow: () -> Unit,
    onAuthMessage: (String) -> Unit,
    onClearStatusMessage: () -> Unit,
    onOpenPost: (BlogPostEntity) -> Unit,
    onOpenPostById: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var nameInput by remember(uiState.savedDisplayName) {
        mutableStateOf(
            uiState.savedDisplayName.ifBlank {
                uiState.currentUser?.displayName.orEmpty()
            }
        )
    }
    var isSigningIn by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedDisplayName) {
        if (uiState.savedDisplayName.isNotBlank() && nameInput.isBlank()) {
            nameInput = uiState.savedDisplayName
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ලකුණු සහ Leaderboard",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = NotoSerifSinhalaFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "කතා ප්‍රශ්නාවලි ප්‍රගතිය හා ඉහළම පාඨකයින්",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (uiState.currentUser != null) {
                        IconButton(
                            onClick = onSyncNow,
                            enabled = !uiState.isSyncing,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "ලකුණු Sync කරන්න"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .testTag("scoreboard_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status message banner
                if (!uiState.statusMessage.isNullOrBlank()) {
                    item(key = "status_banner") {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onClearStatusMessage)
                        ) {
                            Text(
                                text = uiState.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // 1. Total Score Summary Hero Card
                item(key = "total_score_hero") {
                    TotalScoreHeroCard(uiState = uiState)
                }

                // 2. Reader Name & Optional Cloud Leaderboard Auth Card
                item(key = "reader_identity_card") {
                    ReaderIdentityCard(
                        nameInput = nameInput,
                        onNameChange = { nameInput = it },
                        onSaveName = { onSaveDisplayName(nameInput) },
                        isSignedIn = uiState.currentUser != null,
                        isSigningIn = isSigningIn,
                        isSyncing = uiState.isSyncing,
                        onSignInClick = {
                            isSigningIn = true
                            launchInteractiveGoogleSignIn(
                                context = context,
                                credentialManager = credentialManager,
                                scope = scope,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isSigningIn = false
                                    onAuthMessage(msg)
                                },
                                onAuthCancelled = {
                                    isSigningIn = false
                                }
                            )
                        },
                        onSignOutClick = {
                            signOutGoogleAccount(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = {
                                    onAuthMessage("Google ගිණුමෙන් ඉවත් විය. ඔබගේ ලකුණු උපාංගයේ සුරැකී ඇත.")
                                }
                            )
                        }
                    )
                }

                // 3. Top Readers Leaderboard Header
                item(key = "leaderboard_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Leaderboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "වැඩිම ලකුණු ගත් පාඨකයින් (Leaderboard)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NotoSerifSinhalaFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                when (val cloudState = uiState.cloudLeaderboardState) {
                    is LeaderboardCloudState.Unauthenticated -> {
                        item(key = "leaderboard_auth_prompt") {
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Leaderboard හි ඔබගේ නම සහ ලකුණු පෙන්වීමට කැමති නම් ඉහත නම සකසා Google ගිණුමෙන් සම්බන්ධ වන්න.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = {
                                            isSigningIn = true
                                            launchInteractiveGoogleSignIn(
                                                context = context,
                                                credentialManager = credentialManager,
                                                scope = scope,
                                                onAuthSuccess = {
                                                    isSigningIn = false
                                                    onAuthSuccess()
                                                },
                                                onAuthError = { msg ->
                                                    isSigningIn = false
                                                    onAuthMessage(msg)
                                                },
                                                onAuthCancelled = {
                                                    isSigningIn = false
                                                }
                                            )
                                        },
                                        enabled = !isSigningIn,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .testTag("leaderboard_google_signin_button")
                                    ) {
                                        if (isSigningIn) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text("Sign in with Google")
                                    }
                                }
                            }
                        }
                    }

                    is LeaderboardCloudState.Loading -> {
                        item(key = "leaderboard_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    is LeaderboardCloudState.Error -> {
                        item(key = "leaderboard_error") {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = cloudState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    is LeaderboardCloudState.Success -> {
                        if (cloudState.entries.isEmpty()) {
                            item(key = "leaderboard_empty") {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Text(
                                        text = "තවම Leaderboard හි ලකුණු සටහන් නැත. කතාවක් කියවා ප්‍රශ්නාවලියට පිළිතුරු දී පළමුවැන්නා වන්න!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = cloudState.entries,
                                key = { _, item -> "lb_${item.id}" }
                            ) { index, entry ->
                                LeaderboardRowCard(
                                    rank = index + 1,
                                    entry = entry,
                                    isCurrentUser = entry.userId == uiState.currentUser?.uid
                                )
                            }
                        }
                    }
                }

                // 4. Per-Story Individual Score Breakdown
                item(key = "story_breakdown_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Quiz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "කතාවෙන් කතාවට ලබාගත් ලකුණු (${uiState.storyScores.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NotoSerifSinhalaFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (uiState.storyScores.isEmpty()) {
                    item(key = "no_story_scores") {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Text(
                                text = "ඔබ තවම කිසිදු කතාවක ප්‍රශ්නාවලිය සම්පූර්ණ කර නැත. පහත කතාවක් තෝරා කියවා අවසානයේ ප්‍රශ්නවලට පිළිතුරු දෙන්න!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.storyScores,
                        key = { score -> "score_${score.postId}" }
                    ) { score ->
                        StoryScoreBreakdownCard(
                            score = score,
                            onClick = { onOpenPostById(score.postId) }
                        )
                    }
                }

                // 5. Stories remaining to attempt
                if (uiState.unattemptedPosts.isNotEmpty()) {
                    item(key = "unattempted_header") {
                        Text(
                            text = "ලකුණු ලබාගැනීමට ඉතිරි කතා (${uiState.unattemptedPosts.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = NotoSerifSinhalaFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(
                        items = uiState.unattemptedPosts.take(6),
                        key = { post -> "pending_${post.id}" }
                    ) { post ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onOpenPost(post) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = post.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = NotoSerifSinhalaFamily
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "කතාව විවෘත කරන්න",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TotalScoreHeroCard(uiState: ScoreboardUiState) {
    val ratio = if (uiState.totalMaxScore > 0) {
        (uiState.totalScore.toFloat() / uiState.totalMaxScore.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "මුළු ලකුණු එකතුව",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${uiState.totalScore}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "සම්පූර්ණ කළ කතා: ${uiState.storiesCompletedCount} / ${uiState.totalStoriesCount}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (uiState.totalMaxScore > 0) {
                        Text(
                            text = "නිවැරදිතාවය ${(ratio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
private fun ReaderIdentityCard(
    nameInput: String,
    onNameChange: (String) -> Unit,
    onSaveName: () -> Unit,
    isSignedIn: Boolean,
    isSigningIn: Boolean,
    isSyncing: Boolean,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "පාඨක නම (Leaderboard Display Name)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = onNameChange,
                    label = { Text("ඔබ කැමති නම ඇතුළත් කරන්න") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("leaderboard_display_name_input")
                )
                Button(
                    onClick = onSaveName,
                    enabled = nameInput.isNotBlank() && !isSyncing,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("save_display_name_button")
                ) {
                    Text("සුරකින්න")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isSignedIn) {
                        "Google ගිණුම සම්බන්ධයි • Leaderboard Sync සක්‍රියයි"
                    } else {
                        "කැමති නම් නම යොදා Leaderboard වෙත එක්වීමට සම්බන්ධ වන්න"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (isSignedIn) {
                    OutlinedButton(
                        onClick = onSignOutClick,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ඉවත් වන්න")
                    }
                } else {
                    OutlinedButton(
                        onClick = onSignInClick,
                        enabled = !isSigningIn,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(if (isSigningIn) "..." else "Sign in with Google")
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRowCard(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFF59E0B)
        2 -> Color(0xFF94A3B8)
        3 -> Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        border = if (isCurrentUser) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = rankColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#$rank",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = rankColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCurrentUser) "${entry.displayName} (ඔබ)" else entry.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "සම්පූර්ණ කතා ${entry.storiesCompleted}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "${entry.totalScore} pts",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StoryScoreBreakdownCard(
    score: StoryScoreEntity,
    onClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = score.postTitle,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = NotoSerifSinhalaFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "නිවැරදි පිළිතුරු ${score.correctAnswers}/${score.totalQuestions}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (score.syncedToCloud) Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
                        contentDescription = if (score.syncedToCloud) "Synced" else "Local only",
                        tint = if (score.syncedToCloud) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${score.score}/${score.maxScore}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

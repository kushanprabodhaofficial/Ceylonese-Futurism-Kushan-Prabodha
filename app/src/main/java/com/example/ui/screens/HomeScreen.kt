package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BlogPostEntity
import com.example.ui.components.ContinueReadingBanner
import com.example.ui.components.CosmicOrbitalBanner
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FeaturedStoryHeroCard
import com.example.ui.components.StoryListCard
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.viewmodel.BlogUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: BlogUiState,
    onRefresh: () -> Unit,
    onDismissStatus: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onToggleTheme: () -> Unit,
    onPostClick: (BlogPostEntity) -> Unit,
    onToggleBookmark: (BlogPostEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val refreshRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val showFeaturedHero = uiState.searchQuery.isBlank() &&
        uiState.selectedCategory == null &&
        uiState.filteredPosts.isNotEmpty()
    val featuredPost = if (showFeaturedHero) uiState.filteredPosts.first() else null
    val listPosts = if (featuredPost != null) {
        uiState.filteredPosts.drop(1)
    } else {
        uiState.filteredPosts
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .testTag("home_stories_list"),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Branded Ceylonese Futurism Header
                item(key = "header") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    ) {
                        CosmicOrbitalBanner(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(end = 8.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CEYLONESE FUTURISM",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = SpaceGroteskFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = uiState.blogMeta?.blogTitle ?: "Ceylonese Futurism",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "සිංහල විද්‍යා ප්‍රබන්ධ • කුෂාන් ප්‍රබෝධ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = onToggleTheme,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("toggle_theme_button")
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.readerPreferences.appThemeMode == AppThemeMode.LIGHT) {
                                                Icons.Filled.DarkMode
                                            } else {
                                                Icons.Filled.LightMode
                                            },
                                            contentDescription = "තේමාව මාරු කරන්න",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = onRefresh,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("refresh_blog_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "බ්ලොග් අඩවිය යාවත්කාලීන කරන්න",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = if (uiState.isRefreshing) {
                                                Modifier.rotate(refreshRotation)
                                            } else Modifier
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Search Field
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = {
                                    Text(
                                        text = "විද්‍යා ප්‍රබන්ධ හෝ මාතෘකා සොයන්න...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { onSearchQueryChange("") },
                                            modifier = Modifier.testTag("clear_search_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "සෙවුම මකන්න"
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_input")
                            )
                        }
                    }
                }

                // Refresh / Error Status Banner
                item(key = "status_banner") {
                    AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                        val msg = uiState.statusBannerMessage.orEmpty()
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (uiState.isStatusError) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("status_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.isStatusError) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = if (uiState.isStatusError) onRefresh else onDismissStatus) {
                                    Text(text = if (uiState.isStatusError) "නැවත" else "හරි")
                                }
                            }
                        }
                    }
                }

                // Quick Category Filter Chips
                if (uiState.categories.isNotEmpty()) {
                    item(key = "category_chips") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedCategory == null,
                                    onClick = { onSelectCategory(null) },
                                    label = { Text("සියල්ල (${uiState.allPosts.size})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("chip_all_posts")
                                )
                            }
                            items(uiState.categories, key = { it.name }) { cat ->
                                FilterChip(
                                    selected = uiState.selectedCategory == cat.name,
                                    onClick = {
                                        onSelectCategory(
                                            if (uiState.selectedCategory == cat.name) null else cat.name
                                        )
                                    },
                                    label = { Text("${cat.name} (${cat.count})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // Continue Reading Banner
                if (uiState.continueReadingPost != null && uiState.searchQuery.isBlank()) {
                    item(key = "continue_reading") {
                        ContinueReadingBanner(
                            post = uiState.continueReadingPost,
                            onClick = { onPostClick(uiState.continueReadingPost) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Loading / Empty States
                if (uiState.isInitialLoading) {
                    item(key = "initial_loading") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Ceylonese Futurism ලිපි පූරණය වෙමින් පවතී...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (uiState.allPosts.isEmpty()) {
                    item(key = "empty_network") {
                        EmptyStateView(
                            icon = Icons.Outlined.WifiOff,
                            title = "ලිපි පූරණය කළ නොහැකි විය",
                            subtitle = "බ්ලොග් අඩවියෙන් නවතම විද්‍යා ප්‍රබන්ධ ලබාගැනීමට අන්තර්ජාල සම්බන්ධතාවය පරීක්ෂා කර නැවත උත්සාහ කරන්න.",
                            actionLabel = "නැවත උත්සාහ කරන්න",
                            onAction = onRefresh
                        )
                    }
                } else if (uiState.filteredPosts.isEmpty()) {
                    item(key = "empty_filter") {
                        EmptyStateView(
                            icon = Icons.Outlined.AutoStories,
                            title = "ගැලපෙන ලිපි හමු නොවීය",
                            subtitle = "වෙනත් සෙවුම් පදයක් හෝ වර්ගයක් උත්සාහ කරන්න.",
                            actionLabel = "සියලු ලිපි පෙන්වන්න",
                            onAction = {
                                onSearchQueryChange("")
                                onSelectCategory(null)
                            }
                        )
                    }
                }

                // Featured Latest Story
                if (featuredPost != null) {
                    item(key = "featured_${featuredPost.id}") {
                        FeaturedStoryHeroCard(
                            post = featuredPost,
                            onReadClick = { onPostClick(featuredPost) },
                            onBookmarkClick = { onToggleBookmark(featuredPost) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                if (listPosts.isNotEmpty()) {
                    item(key = "section_label") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.selectedCategory != null) {
                                    "${uiState.selectedCategory} (${uiState.filteredPosts.size})"
                                } else if (uiState.searchQuery.isNotBlank()) {
                                    "සෙවුම් ප්‍රතිඵල (${uiState.filteredPosts.size})"
                                } else {
                                    "සියලුම විද්‍යා ප්‍රබන්ධ සහ ලිපි"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "පහළට ඇද Refresh කරන්න",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(listPosts, key = { it.id }) { post ->
                        StoryListCard(
                            post = post,
                            onClick = { onPostClick(post) },
                            onBookmarkClick = { onToggleBookmark(post) },
                            onLabelClick = { label -> onSelectCategory(label) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

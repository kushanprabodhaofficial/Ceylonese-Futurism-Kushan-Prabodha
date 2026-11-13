package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.BlogPostEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StoryListCard
import com.example.ui.viewmodel.BlogUiState

@Composable
fun LibraryScreen(
    uiState: BlogUiState,
    onPostClick: (BlogPostEntity) -> Unit,
    onToggleBookmark: (BlogPostEntity) -> Unit,
    onBrowseStoriesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "ඔබේ පුස්තකාලය",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "අන්තර්ජාලය නොමැතිව වුවද කියවීමට සුරැකි ලිපි සහ කියවූ ඉතිහාසය",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_segmented_tabs")
                ) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text("සුරැකි (${uiState.bookmarkedPosts.size})")
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text("කියවූ ඉතිහාසය (${uiState.readingHistory.size})")
                    }
                }
            }

            val itemsList = if (selectedTab == 0) uiState.bookmarkedPosts else uiState.readingHistory
            if (itemsList.isEmpty()) {
                EmptyStateView(
                    icon = if (selectedTab == 0) Icons.Outlined.BookmarkBorder else Icons.Outlined.History,
                    title = if (selectedTab == 0) "තවම ලිපි සුරැක නැත" else "කියවූ ලිපි තවම නැත",
                    subtitle = if (selectedTab == 0) {
                        "ඕනෑම විද්‍යා ප්‍රබන්ධයක Bookmark සලකුණ ඔබා පසුව කියවීමට මෙහි එකතු කරගන්න."
                    } else {
                        "ඔබ කියවන ලිපි සහ කියවූ ප්‍රමාණය ස්වයංක්‍රීයව මෙහි සටහන් වේ."
                    },
                    actionLabel = "ලිපි කියවන්න",
                    onAction = onBrowseStoriesClick
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(itemsList, key = { it.id }) { post ->
                        StoryListCard(
                            post = post,
                            onClick = { onPostClick(post) },
                            onBookmarkClick = { onToggleBookmark(post) }
                        )
                    }
                }
            }
        }
    }
}

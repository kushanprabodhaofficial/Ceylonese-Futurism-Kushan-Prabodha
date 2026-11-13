package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BlogPostEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StoryListCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPostsScreen(
    categoryName: String,
    allPosts: List<BlogPostEntity>,
    onBack: () -> Unit,
    onPostClick: (BlogPostEntity) -> Unit,
    onToggleBookmark: (BlogPostEntity) -> Unit
) {
    BackHandler(onBack = onBack)

    val matchingPosts = remember(categoryName, allPosts) {
        allPosts.filter { post ->
            post.labelsList().any { it.equals(categoryName, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "$categoryName (${matchingPosts.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ආපසු"
                        )
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
            if (matchingPosts.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.AutoStories,
                    title = "මෙම වර්ගයේ ලිපි නැත",
                    subtitle = "වෙනත් වර්ගයක් තෝරා බලන්න."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(matchingPosts, key = { it.id }) { post ->
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

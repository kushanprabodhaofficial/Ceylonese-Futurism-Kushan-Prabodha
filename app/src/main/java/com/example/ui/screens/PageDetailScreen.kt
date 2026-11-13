package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BlogPageEntity
import com.example.data.preferences.ReaderPreferences
import com.example.data.remote.HtmlArticleParser
import com.example.ui.components.ArticleBlockView
import com.example.ui.theme.NotoSerifSinhalaFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageDetailScreen(
    page: BlogPageEntity?,
    preferences: ReaderPreferences,
    onBack: () -> Unit,
    onOpenWebUrl: (String, String) -> Unit
) {
    BackHandler(onBack = onBack)

    if (page == null) {
        onBack()
        return
    }

    val blocks = remember(page.id, page.contentHtml) {
        HtmlArticleParser.parse(page.contentHtml)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = page.title,
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
                actions = {
                    IconButton(onClick = { onOpenWebUrl(page.pageUrl, page.title) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = "වෙබ් අඩවියෙන් විවෘත කරන්න"
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)
            ) {
                item(key = "page_header") {
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = NotoSerifSinhalaFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                items(
                    items = blocks.withIndex().toList(),
                    key = { "page_block_${it.index}" }
                ) { (_, block) ->
                    ArticleBlockView(
                        block = block,
                        textColor = MaterialTheme.colorScheme.onBackground,
                        secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        accentColor = MaterialTheme.colorScheme.primary,
                        surfaceHighlightColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        fontSizeSp = preferences.fontSizeSp,
                        lineHeightMultiplier = preferences.lineHeightMultiplier,
                        useSerifFont = preferences.useSerifFont,
                        onOpenUrl = { url -> onOpenWebUrl(url, page.title) }
                    )
                }
            }
        }
    }
}

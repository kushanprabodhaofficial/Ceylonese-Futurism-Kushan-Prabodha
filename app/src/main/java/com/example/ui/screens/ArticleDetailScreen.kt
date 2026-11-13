package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.audio.StoryMoodAnalyzer
import com.example.audio.StorySoundscape
import com.example.data.local.BlogPostEntity
import com.example.data.model.BlogComment
import com.example.data.model.ReaderSurfaceMode
import com.example.data.preferences.ReaderPreferences
import com.example.data.remote.BloggerJsonParser
import com.example.data.remote.HtmlArticleParser
import com.example.ui.components.ArticleBlockView
import com.example.ui.theme.CosmicBackground
import com.example.ui.theme.CosmicCyan
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicTextPrimary
import com.example.ui.theme.CosmicTextSecondary
import com.example.ui.theme.NotoSerifSinhalaFamily
import com.example.ui.theme.PaperAccent
import com.example.ui.theme.PaperBackground
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.PaperTextPrimary
import com.example.ui.theme.PaperTextSecondary
import com.example.ui.theme.SepiaAccent
import com.example.ui.theme.SepiaBackground
import com.example.ui.theme.SepiaSurface
import com.example.ui.theme.SepiaTextPrimary
import com.example.ui.theme.SepiaTextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArticleDetailScreen(
    post: BlogPostEntity?,
    allPosts: List<BlogPostEntity>,
    comments: List<BlogComment>,
    isLoadingComments: Boolean,
    preferences: ReaderPreferences,
    onBack: () -> Unit,
    onEnsureFullContent: (BlogPostEntity) -> Unit,
    onLoadComments: (String, String) -> Unit,
    onToggleBookmark: (BlogPostEntity) -> Unit,
    onUpdateProgress: (String, Float) -> Unit,
    onSelectPost: (BlogPostEntity) -> Unit,
    onCategoryClick: (String) -> Unit,
    onOpenWebUrl: (String, String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onUseSerifChange: (Boolean) -> Unit,
    onReaderSurfaceChange: (ReaderSurfaceMode) -> Unit,
    onAmbientMusicEnabledChange: (Boolean) -> Unit,
    onAmbientMusicVolumeChange: (Float) -> Unit,
    onStartAmbientMusic: (StorySoundscape, Float) -> Unit,
    onStopAmbientMusic: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showReaderSettings by remember { mutableStateOf(false) }

    if (post == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ලිපිය") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ආපසු")
                        }
                    }
                )
            }
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val matchedSoundscape = remember(post.id, post.title, post.contentHtml, post.labelsDelimited) {
        StoryMoodAnalyzer.analyzePost(post)
    }
    var overrideSoundscape by remember(post.id) { mutableStateOf<StorySoundscape?>(null) }
    val activeSoundscape = overrideSoundscape ?: matchedSoundscape

    // Lifecycle-aware ambient music playback
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> onStopAmbientMusic()
                Lifecycle.Event.ON_RESUME -> {
                    if (preferences.ambientMusicEnabled) {
                        onStartAmbientMusic(activeSoundscape, preferences.ambientMusicVolume)
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onStopAmbientMusic()
        }
    }

    LaunchedEffect(post.id, activeSoundscape, preferences.ambientMusicEnabled) {
        if (preferences.ambientMusicEnabled) {
            onStartAmbientMusic(activeSoundscape, preferences.ambientMusicVolume)
        } else {
            onStopAmbientMusic()
        }
    }

    LaunchedEffect(post.id) {
        onEnsureFullContent(post)
        onLoadComments(post.numericPostId, post.title)
    }

    val blocks = remember(post.id, post.contentHtml) {
        HtmlArticleParser.parse(post.contentHtml)
    }

    val currentIndex = remember(post.id, allPosts) {
        allPosts.indexOfFirst { it.id == post.id }
    }
    val newerPost = if (currentIndex > 0) allPosts[currentIndex - 1] else null
    val olderPost = if (currentIndex in 0 until allPosts.lastIndex) allPosts[currentIndex + 1] else null

    // Resolve dedicated Reader Colors
    val bgColor: Color
    val textColor: Color
    val secondaryColor: Color
    val accentColor: Color
    val highlightSurface: Color

    when (preferences.readerSurfaceMode) {
        ReaderSurfaceMode.COSMIC_DARK -> {
            bgColor = CosmicBackground
            textColor = CosmicTextPrimary
            secondaryColor = CosmicTextSecondary
            accentColor = CosmicCyan
            highlightSurface = CosmicSurface
        }
        ReaderSurfaceMode.WARM_PAPER -> {
            bgColor = PaperBackground
            textColor = PaperTextPrimary
            secondaryColor = PaperTextSecondary
            accentColor = PaperAccent
            highlightSurface = PaperSurface
        }
        ReaderSurfaceMode.SEPIA -> {
            bgColor = SepiaBackground
            textColor = SepiaTextPrimary
            secondaryColor = SepiaTextSecondary
            accentColor = SepiaAccent
            highlightSurface = SepiaSurface
        }
        ReaderSurfaceMode.FOLLOW_APP -> {
            bgColor = MaterialTheme.colorScheme.background
            textColor = MaterialTheme.colorScheme.onBackground
            secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
            accentColor = MaterialTheme.colorScheme.primary
            highlightSurface = MaterialTheme.colorScheme.surfaceContainerHigh
        }
    }

    val listState = rememberLazyListState()
    val scrollProgress by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total <= 1) 1f
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                ((lastVisible + 1).toFloat() / total.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    LaunchedEffect(post.id) {
        snapshotFlow { scrollProgress }
            .distinctUntilChanged()
            .collect { progress ->
                if (progress > 0.05f) {
                    onUpdateProgress(post.id, progress)
                }
            }
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = post.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("reader_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ආපසු",
                                tint = textColor
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { onAmbientMusicEnabledChange(!preferences.ambientMusicEnabled) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("music_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (preferences.ambientMusicEnabled) {
                                    Icons.Filled.MusicNote
                                } else {
                                    Icons.Filled.MusicOff
                                },
                                contentDescription = if (preferences.ambientMusicEnabled) {
                                    "පසුබිම් සංගීතය නිවා දමන්න"
                                } else {
                                    "පසුබිම් සංගීතය ක්‍රියාත්මක කරන්න"
                                },
                                tint = if (preferences.ambientMusicEnabled) accentColor else secondaryColor
                            )
                        }
                        IconButton(
                            onClick = { showReaderSettings = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reader_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TextFields,
                                contentDescription = "කියවීමේ සැකසුම්",
                                tint = accentColor
                            )
                        }
                        IconButton(
                            onClick = { onToggleBookmark(post) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reader_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "සුරකින්න",
                                tint = if (post.isBookmarked) MaterialTheme.colorScheme.secondary else textColor
                            )
                        }
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, post.title)
                                    putExtra(Intent.EXTRA_TEXT, "${post.title}\n\n${post.postUrl}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "ලිපිය බෙදාගන්න"))
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "බෙදාගන්න",
                                tint = textColor
                            )
                        }
                        IconButton(
                            onClick = { onOpenWebUrl(post.postUrl, post.title) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = "වෙබ් අඩවියෙන් විවෘත කරන්න",
                                tint = textColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
                )
                LinearProgressIndicator(
                    progress = { scrollProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = accentColor,
                    trackColor = secondaryColor.copy(alpha = 0.15f)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp)
                    .testTag("article_reader_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Article Title, Metadata & Story-Matched Music Bar
                item(key = "article_header") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val labels = post.labelsList()
                        if (labels.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                labels.forEach { label ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accentColor.copy(alpha = 0.16f),
                                        modifier = Modifier.clickable { onCategoryClick(label) }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = NotoSerifSinhalaFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!post.authorImageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = post.authorImageUrl,
                                    contentDescription = post.authorName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                )
                            }
                            Column {
                                Text(
                                    text = post.authorName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = textColor
                                )
                                Text(
                                    text = "${BloggerJsonParser.formatSinhalaDate(post.publishedMillis, post.publishedIso)} • මිනිත්තු ${post.readingTimeMinutes} ක කියවීම",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Story-Matched Ambient Background Music Control Card
                        StoryAmbientMusicBar(
                            soundscape = activeSoundscape,
                            isPlaying = preferences.ambientMusicEnabled,
                            accentColor = accentColor,
                            surfaceColor = highlightSurface,
                            textColor = textColor,
                            secondaryTextColor = secondaryColor,
                            onToggle = { onAmbientMusicEnabledChange(!preferences.ambientMusicEnabled) },
                            onOpenOptions = { showReaderSettings = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 18.dp),
                            color = secondaryColor.copy(alpha = 0.22f)
                        )
                    }
                }

                // Rich HTML Content Blocks
                items(
                    items = blocks.withIndex().toList(),
                    key = { "block_${it.index}" }
                ) { (_, block) ->
                    ArticleBlockView(
                        block = block,
                        textColor = textColor,
                        secondaryTextColor = secondaryColor,
                        accentColor = accentColor,
                        surfaceHighlightColor = highlightSurface,
                        fontSizeSp = preferences.fontSizeSp,
                        lineHeightMultiplier = preferences.lineHeightMultiplier,
                        useSerifFont = preferences.useSerifFont,
                        onOpenUrl = { url -> onOpenWebUrl(url, post.title) }
                    )
                }

                // Previous / Next Article Navigation
                item(key = "prev_next_nav") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 24.dp),
                            color = secondaryColor.copy(alpha = 0.25f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (olderPost != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = highlightSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectPost(olderPost) }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = accentColor
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "පෙර ලිපිය",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accentColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = olderPost.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = NotoSerifSinhalaFamily
                                            ),
                                            color = textColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (newerPost != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = highlightSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectPost(newerPost) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "ඊළඟ ලිපිය",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accentColor
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = accentColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = newerPost.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = NotoSerifSinhalaFamily
                                            ),
                                            color = textColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Comments Section
                item(key = "comments_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 32.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Comment,
                                    contentDescription = null,
                                    tint = accentColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "පාඨක අදහස් (${comments.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = textColor
                                )
                            }

                            OutlinedButton(
                                onClick = { onOpenWebUrl("${post.postUrl}#comments", post.title) }
                            ) {
                                Text("අදහසක් එක් කරන්න")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isLoadingComments) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = accentColor
                            )
                        } else if (comments.isEmpty()) {
                            Text(
                                text = "මෙම ලිපියට තවම අදහස් පළ කර නැත. වෙබ් පිටුව හරහා පළමු අදහස එක් කරන්න!",
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryColor
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                comments.forEach { comment ->
                                    Card(
                                        shape = MaterialTheme.shapes.small,
                                        colors = CardDefaults.cardColors(containerColor = highlightSurface)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = comment.authorName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = textColor
                                                )
                                                Text(
                                                    text = BloggerJsonParser.formatSinhalaDate(
                                                        comment.publishedMillis,
                                                        comment.publishedIso
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = secondaryColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = comment.contentText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = textColor.copy(alpha = 0.88f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReaderSettings) {
        ModalBottomSheet(onDismissRequest = { showReaderSettings = false }) {
            ReaderSettingsSheetContent(
                preferences = preferences,
                activeSoundscape = activeSoundscape,
                onFontSizeChange = onFontSizeChange,
                onLineHeightChange = onLineHeightChange,
                onUseSerifChange = onUseSerifChange,
                onReaderSurfaceChange = onReaderSurfaceChange,
                onAmbientMusicEnabledChange = onAmbientMusicEnabledChange,
                onAmbientMusicVolumeChange = onAmbientMusicVolumeChange,
                onSelectSoundscape = { chosen ->
                    overrideSoundscape = chosen
                    if (!preferences.ambientMusicEnabled) {
                        onAmbientMusicEnabledChange(true)
                    } else {
                        onStartAmbientMusic(chosen, preferences.ambientMusicVolume)
                    }
                }
            )
        }
    }
}

@Composable
private fun StoryAmbientMusicBar(
    soundscape: StorySoundscape,
    isPlaying: Boolean,
    accentColor: Color,
    surfaceColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    onToggle: () -> Unit,
    onOpenOptions: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = surfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpenOptions)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isPlaying) accentColor.copy(alpha = 0.18f) else secondaryTextColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isPlaying) {
                            AnimatedEqualizerBars(color = accentColor)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.MusicOff,
                                contentDescription = null,
                                tint = secondaryTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isPlaying) {
                            "පසුබිම් සංගීතය • ${soundscape.displayNameSinhala}"
                        } else {
                            "පසුබිම් සංගීතය නිහඬයි"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isPlaying) {
                            "${soundscape.subtitleSinhala} • සැකසීමට තට්ටු කරන්න"
                        } else {
                            "${soundscape.displayNameSinhala} සක්‍රිය කිරීමට ON කරන්න"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
                checked = isPlaying,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("music_card_switch")
            )
        }
    }
}

@Composable
private fun AnimatedEqualizerBars(color: Color) {
    val transition = rememberInfiniteTransition(label = "eq")
    val bar1 by transition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 14f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 8f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = Modifier.height(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar1.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
    }
}

@Composable
fun ReaderSettingsSheetContent(
    preferences: ReaderPreferences,
    activeSoundscape: StorySoundscape,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onUseSerifChange: (Boolean) -> Unit,
    onReaderSurfaceChange: (ReaderSurfaceMode) -> Unit,
    onAmbientMusicEnabledChange: (Boolean) -> Unit,
    onAmbientMusicVolumeChange: (Float) -> Unit,
    onSelectSoundscape: (StorySoundscape) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "කියවීමේ සහ සංගීත සැකසුම්",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Ambient Background Music Controls
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "පසුබිම් සංගීතය (Story Audio)",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = activeSoundscape.displayNameSinhala,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Switch(
                        checked = preferences.ambientMusicEnabled,
                        onCheckedChange = onAmbientMusicEnabledChange,
                        modifier = Modifier.testTag("sheet_music_switch")
                    )
                }

                if (preferences.ambientMusicEnabled) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "සංගීත ශබ්දය (Volume)",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${(preferences.ambientMusicVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = preferences.ambientMusicVolume,
                            onValueChange = onAmbientMusicVolumeChange,
                            valueRange = 0.05f..1.0f
                        )
                    }

                    Text(
                        text = "සංගීත රටාව තෝරන්න",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StorySoundscape.entries) { soundscape ->
                            FilterChip(
                                selected = activeSoundscape == soundscape,
                                onClick = { onSelectSoundscape(soundscape) },
                                label = { Text(soundscape.displayNameSinhala) }
                            )
                        }
                    }
                }
            }
        }

        // Font Size
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "අකුරු ප්‍රමාණය",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "${preferences.fontSizeSp.toInt()} sp",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = preferences.fontSizeSp,
                onValueChange = onFontSizeChange,
                valueRange = 14f..26f,
                steps = 5
            )
        }

        // Font Family Toggle
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "අකුරු රටාව (Sinhala Font)",
                style = MaterialTheme.typography.labelLarge
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = preferences.useSerifFont,
                    onClick = { onUseSerifChange(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Serif (සාහිත්‍ය)")
                }
                SegmentedButton(
                    selected = !preferences.useSerifFont,
                    onClick = { onUseSerifChange(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Sans (සරල)")
                }
            }
        }

        // Line Spacing
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "පේළි අතර පරතරය",
                style = MaterialTheme.typography.labelLarge
            )
            val spacings = listOf(1.45f to "සංයුක්ත", 1.65f to "සුවපහසු", 1.85f to "විශාල")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                spacings.forEachIndexed { idx, (mult, label) ->
                    SegmentedButton(
                        selected = kotlin.math.abs(preferences.lineHeightMultiplier - mult) < 0.08f,
                        onClick = { onLineHeightChange(mult) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = spacings.size)
                    ) {
                        Text(label)
                    }
                }
            }
        }

        // Reading Background Surface
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "පසුබිම් වර්ණය (Reading Surface)",
                style = MaterialTheme.typography.labelLarge
            )
            val surfaces = listOf(
                ReaderSurfaceMode.FOLLOW_APP to "ඇප් තේමාව",
                ReaderSurfaceMode.COSMIC_DARK to "අඳුරු",
                ReaderSurfaceMode.WARM_PAPER to "කඩදාසි",
                ReaderSurfaceMode.SEPIA to "සේපියා"
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                surfaces.forEachIndexed { idx, (mode, label) ->
                    SegmentedButton(
                        selected = preferences.readerSurfaceMode == mode,
                        onClick = { onReaderSurfaceChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = surfaces.size)
                    ) {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

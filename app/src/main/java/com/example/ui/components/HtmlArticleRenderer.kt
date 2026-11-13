package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.remote.ArticleBlock
import com.example.data.remote.InlineSegment
import com.example.ui.theme.NotoSansSinhalaFamily
import com.example.ui.theme.NotoSerifSinhalaFamily

@Composable
fun ArticleBlockView(
    block: ArticleBlock,
    textColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    surfaceHighlightColor: Color,
    fontSizeSp: Float,
    lineHeightMultiplier: Float,
    useSerifFont: Boolean,
    onOpenUrl: (String) -> Unit,
    fallbackAdClient: String? = null,
    fallbackAdSlot: String? = null
) {
    val context = LocalContext.current
    val bodyFontFamily: FontFamily = if (useSerifFont) NotoSerifSinhalaFamily else NotoSansSinhalaFamily
    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }

    when (block) {
        is ArticleBlock.Heading -> {
            val headingScale = when (block.level) {
                1 -> 1.38f
                2 -> 1.24f
                3 -> 1.14f
                else -> 1.06f
            }
            Text(
                text = block.text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = NotoSerifSinhalaFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (fontSizeSp * headingScale).sp,
                    lineHeight = (fontSizeSp * headingScale * 1.38f).sp
                ),
                color = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 6.dp)
            )
        }

        is ArticleBlock.Paragraph -> {
            val annotated = remember(block.segments, accentColor) {
                buildInlineAnnotatedString(block.segments, accentColor, onOpenUrl)
            }
            Text(
                text = annotated,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = bodyFontFamily,
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp * lineHeightMultiplier).sp
                ),
                color = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
        }

        is ArticleBlock.Quote -> {
            Surface(
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, bottomStart = 4.dp),
                color = surfaceHighlightColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .background(accentColor)
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterVertically)
                    )
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = bodyFontFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * lineHeightMultiplier).sp
                        ),
                        color = textColor.copy(alpha = 0.92f),
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }
            }
        }

        is ArticleBlock.BulletItem -> {
            val annotated = remember(block.segments, accentColor) {
                buildInlineAnnotatedString(block.segments, accentColor, onOpenUrl)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = block.bulletLabel,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSizeSp.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = accentColor,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Text(
                    text = annotated,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = bodyFontFamily,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * lineHeightMultiplier).sp
                    ),
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        is ArticleBlock.ImageBlock -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = block.imageUrl,
                    contentDescription = block.caption ?: "ලිපියේ රූපය",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { zoomedImageUrl = block.imageUrl }
                )
                if (!block.caption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = block.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }
            }
        }

        is ArticleBlock.VideoEmbedBlock -> {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = surfaceHighlightColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(block.watchUrl)))
                        }.onFailure {
                            onOpenUrl(block.watchUrl)
                        }
                    }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!block.thumbnailUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            AsyncImage(
                                model = block.thumbnailUrl,
                                contentDescription = block.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x55000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = accentColor,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "වීඩියෝව වාදනය කරන්න",
                                            tint = Color.Black,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "නරඹන්න",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor
                        )
                    }
                }
            }
        }

        is ArticleBlock.AdSenseBlock -> {
            AdSenseBannerView(
                adClient = block.adClient ?: fallbackAdClient,
                adSlot = block.adSlot ?: fallbackAdSlot,
                rawInsHtml = block.rawInsHtml,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )
        }

        ArticleBlock.Divider -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = secondaryTextColor.copy(alpha = 0.25f)
            )
        }
    }

    if (zoomedImageUrl != null) {
        Dialog(
            onDismissRequest = { zoomedImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { zoomedImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = zoomedImageUrl,
                    contentDescription = "විශාල කරන ලද රූපය",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = { zoomedImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "වසන්න",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun buildInlineAnnotatedString(
    segments: List<InlineSegment>,
    accentColor: Color,
    onOpenUrl: (String) -> Unit
) = buildAnnotatedString {
    for (segment in segments) {
        val url = segment.linkUrl
        if (url != null) {
            withLink(
                LinkAnnotation.Url(
                    url = url,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = accentColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    ),
                    linkInteractionListener = { onOpenUrl(url) }
                )
            ) {
                append(segment.text)
            }
        } else {
            withStyle(
                SpanStyle(
                    fontWeight = if (segment.isBold) FontWeight.Bold else null,
                    fontStyle = if (segment.isItalic) FontStyle.Italic else null
                )
            ) {
                append(segment.text)
            }
        }
    }
}

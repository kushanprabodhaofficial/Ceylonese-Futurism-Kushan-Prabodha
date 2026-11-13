package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.BlogMetaEntity
import com.example.ui.navigation.AppRoutes
import com.example.ui.theme.NotoSerifSinhalaFamily
import com.example.ui.theme.SpaceGroteskFamily

object AuthorLinks {
    const val YOUTUBE_URL = "https://youtube.com/@kushanprabodha"
    const val FACEBOOK_URL = "https://www.facebook.com/Kushanprabodhaliyanapathiran"
    const val BLOG_URL = "https://kushanprabodha.blogspot.com"
}

@Composable
fun AuthorSideDrawerContent(
    blogMeta: BlogMetaEntity?,
    currentRoute: String?,
    onNavigateToTab: (String) -> Unit,
    onOpenInAppWeb: (String, String) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun openExternalOrWeb(url: String, title: String) {
        onCloseDrawer()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val handled = runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (!handled) {
            onOpenInAppWeb(url, title)
        }
    }

    ModalDrawerSheet(
        modifier = modifier
            .widthIn(max = 330.dp)
            .testTag("author_side_drawer"),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Cosmic Author Hero Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_universe_emblem),
                    contentDescription = "Ceylonese Futurism Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x44070B14),
                                    Color(0xCC070B14),
                                    Color(0xF8070B14)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!blogMeta?.authorAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = blogMeta?.authorAvatarUrl,
                                contentDescription = "Kushan Prabodha",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                error = painterResource(id = R.drawable.ic_launcher_art)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_art),
                                contentDescription = "Kushan Prabodha",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Kushan Prabodha",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = NotoSerifSinhalaFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "කුෂාන් ප්‍රබෝධ",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF22D3EE)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x33F59E0B)
                    ) {
                        Text(
                            text = "CEYLONESE FUTURISM • AUTHOR & MUSICIAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFFFDE68A),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Author Description Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "කතුවරයා ගැන (Author Bio)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Text(
                        text = "L.P. කුෂාන් ප්‍රබෝධ ලියනපතිරණ (Kushan Prabodha) ශ්‍රී ලාංකික විද්‍යා ප්‍රබන්ධ ලේඛකයෙකු, බහු-මාධ්‍ය කලාකරුවෙකු සහ සංගීතඥයෙකි. " +
                            "හෙළ උරුමය, පුරාණ චින්තනය හා සයිබර්පන්ක් අනාගතවාදය මුසු කළ \"Ceylonese Futurism\" සහ \"The Entropy Sutras\" විශ්වයේ නිර්මාතෘවරයා වන ඔහු, " +
                            "සිංහලෙන් සහ ඉංග්‍රීසියෙන් විද්‍යා ප්‍රබන්ධ, තාක්ෂණික චින්තන හා පර්යේෂණාත්මක සංගීත නිර්මාණ එළිදක්වයි.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Social & Channel Links Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "නාලිකා සහ පිටු (Connect)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                DrawerExternalLinkCard(
                    icon = Icons.Filled.PlayCircle,
                    iconTint = Color(0xFFEF4444),
                    badgeBg = Color(0x22EF4444),
                    title = "YouTube නාලිකාව",
                    subtitle = "youtube.com/@kushanprabodha",
                    testTag = "drawer_youtube_link",
                    onClick = {
                        openExternalOrWeb(AuthorLinks.YOUTUBE_URL, "Kushan Prabodha — YouTube")
                    }
                )

                DrawerExternalLinkCard(
                    icon = Icons.Filled.Groups,
                    iconTint = Color(0xFF3B82F6),
                    badgeBg = Color(0x223B82F6),
                    title = "Facebook පිටුව",
                    subtitle = "Kushanprabodhaliyanapathiran",
                    testTag = "drawer_facebook_link",
                    onClick = {
                        openExternalOrWeb(AuthorLinks.FACEBOOK_URL, "Kushan Prabodha — Facebook")
                    }
                )

                DrawerExternalLinkCard(
                    icon = Icons.Outlined.Language,
                    iconTint = MaterialTheme.colorScheme.primary,
                    badgeBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    title = "බ්ලොග් අඩවිය (Web)",
                    subtitle = "kushanprabodha.blogspot.com",
                    testTag = "drawer_blog_web_link",
                    onClick = {
                        onCloseDrawer()
                        onOpenInAppWeb(
                            blogMeta?.blogUrl ?: AuthorLinks.BLOG_URL,
                            blogMeta?.blogTitle ?: "Ceylonese Futurism"
                        )
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Quick App Navigation inside Drawer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "ඇප් අංශ (Sections)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )

                val items = listOf(
                    Triple(AppRoutes.HOME, "සියලුම ලිපි (Stories)", Icons.Filled.AutoStories),
                    Triple(AppRoutes.EXPLORE, "වර්ග සහ ලේඛනාගාරය", Icons.Filled.Explore),
                    Triple(AppRoutes.LIBRARY, "සුරැකි ලිපි (Library)", Icons.Filled.Bookmark),
                    Triple(AppRoutes.SCOREBOARD, "ලකුණු සහ Leaderboard", Icons.Filled.EmojiEvents),
                    Triple(AppRoutes.UNIVERSE, "බ්ලොග් පිටු සහ තොරතුරු", Icons.Filled.Public)
                )

                items.forEach { (route, label, icon) ->
                    NavigationDrawerItem(
                        selected = currentRoute == route,
                        onClick = {
                            onCloseDrawer()
                            onNavigateToTab(route)
                        },
                        icon = {
                            Icon(imageVector = icon, contentDescription = null)
                        },
                        label = { Text(label) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerExternalLinkCard(
    icon: ImageVector,
    iconTint: Color,
    badgeBg: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBg,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "විවෘත කරන්න",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

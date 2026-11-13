package com.example.data.remote

import android.text.Html
import com.example.data.local.BlogMetaEntity
import com.example.data.local.BlogPageEntity
import com.example.data.local.BlogPostEntity
import com.example.data.model.BlogComment
import com.example.data.model.ExternalBlogLink
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class ParsedPostsFeed(
    val totalResults: Int,
    val posts: List<BlogPostEntity>,
    val blogTitle: String,
    val blogSubtitle: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val blogUrl: String
)

object BloggerJsonParser {

    const val BLOG_BASE_URL = "https://kushanprabodha.blogspot.com"

    fun parsePostsFeed(jsonText: String): ParsedPostsFeed {
        val root = JSONObject(jsonText)
        val feed = root.optJSONObject("feed") ?: JSONObject()

        val blogTitle = feed.optJSONObject("title")?.optString("\$t").orEmpty()
            .ifBlank { "Ceylonese Futurism" }
        val blogSubtitle = feed.optJSONObject("subtitle")?.optString("\$t").orEmpty()
        val totalResults = feed.optJSONObject("openSearch\$totalResults")
            ?.optString("\$t")?.toIntOrNull() ?: 0

        val feedAuthors = feed.optJSONArray("author")
        val firstAuthor = feedAuthors?.optJSONObject(0)
        val defaultAuthorName = firstAuthor?.optJSONObject("name")?.optString("\$t").orEmpty()
            .ifBlank { "Kushan Prabodha" }
        val defaultAuthorImage = upgradeImageUrl(
            firstAuthor?.optJSONObject("gd\$image")?.optString("src")
        )

        val blogUrl = extractAlternateLink(feed.optJSONArray("link"))
            ?.let { normalizeHttps(it) }
            ?: BLOG_BASE_URL

        val entries = feed.optJSONArray("entry") ?: JSONArray()
        val posts = ArrayList<BlogPostEntity>(entries.length())

        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            val rawId = entry.optJSONObject("id")?.optString("\$t").orEmpty()
            if (rawId.isBlank()) continue

            val numericPostId = rawId.substringAfterLast(".post-", "")
                .ifBlank { rawId.substringAfterLast("-", rawId) }

            val title = decodeHtmlEntities(
                entry.optJSONObject("title")?.optString("\$t").orEmpty()
            ).ifBlank { "නමක් නැති ලිපිය" }

            val contentObj = entry.optJSONObject("content")
            val summaryObj = entry.optJSONObject("summary")
            val rawContent = contentObj?.optString("\$t").orEmpty()
            val rawSummary = summaryObj?.optString("\$t").orEmpty()
            val isSummaryOnly = rawContent.isBlank() && rawSummary.isNotBlank()
            val bodyHtml = rawContent.ifBlank { rawSummary }

            val publishedIso = entry.optJSONObject("published")?.optString("\$t").orEmpty()
            val updatedIso = entry.optJSONObject("updated")?.optString("\$t").orEmpty()
            val publishedMillis = parseIso8601ToMillis(publishedIso.ifBlank { updatedIso })

            val entryAuthorObj = entry.optJSONArray("author")?.optJSONObject(0)
            val authorName = entryAuthorObj?.optJSONObject("name")?.optString("\$t").orEmpty()
                .ifBlank { defaultAuthorName }
            val authorImage = upgradeImageUrl(
                entryAuthorObj?.optJSONObject("gd\$image")?.optString("src")
            ) ?: defaultAuthorImage

            val postUrl = extractAlternateLink(entry.optJSONArray("link"))
                ?.let { normalizeHttps(it) }
                ?: blogUrl

            val labels = extractLabels(entry.optJSONArray("category"))
            val mediaThumb = upgradeImageUrl(
                entry.optJSONObject("media\$thumbnail")?.optString("url")
            )
            val htmlImage = extractFirstImageFromHtml(bodyHtml)?.let { upgradeImageUrl(it) }
            val thumbnailUrl = htmlImage ?: mediaThumb

            val commentCount = entry.optJSONObject("thr\$total")
                ?.optString("\$t")?.toIntOrNull() ?: 0

            val plainText = stripHtmlToPlainText(bodyHtml)
            val excerpt = buildExcerpt(plainText)
            val readingMinutes = estimateReadingMinutes(plainText)

            posts.add(
                BlogPostEntity(
                    id = rawId,
                    numericPostId = numericPostId,
                    title = title,
                    contentHtml = bodyHtml,
                    isSummaryOnly = isSummaryOnly,
                    excerpt = excerpt,
                    publishedIso = publishedIso,
                    publishedMillis = publishedMillis,
                    updatedIso = updatedIso,
                    authorName = authorName,
                    authorImageUrl = authorImage,
                    thumbnailUrl = thumbnailUrl,
                    postUrl = postUrl,
                    labelsDelimited = labels.joinToString("|||"),
                    commentCount = commentCount,
                    readingTimeMinutes = readingMinutes
                )
            )
        }

        return ParsedPostsFeed(
            totalResults = totalResults,
            posts = posts,
            blogTitle = blogTitle,
            blogSubtitle = blogSubtitle,
            authorName = defaultAuthorName,
            authorAvatarUrl = defaultAuthorImage,
            blogUrl = blogUrl
        )
    }

    fun parsePagesFeed(jsonText: String): List<BlogPageEntity> {
        val root = JSONObject(jsonText)
        val feed = root.optJSONObject("feed") ?: return emptyList()
        val entries = feed.optJSONArray("entry") ?: return emptyList()
        val pages = ArrayList<BlogPageEntity>(entries.length())

        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            val rawId = entry.optJSONObject("id")?.optString("\$t").orEmpty()
            if (rawId.isBlank()) continue

            val title = decodeHtmlEntities(
                entry.optJSONObject("title")?.optString("\$t").orEmpty()
            ).ifBlank { "පිටුව" }

            val bodyHtml = entry.optJSONObject("content")?.optString("\$t").orEmpty()
                .ifBlank { entry.optJSONObject("summary")?.optString("\$t").orEmpty() }
            val updatedIso = entry.optJSONObject("updated")?.optString("\$t").orEmpty()
            val pageUrl = extractAlternateLink(entry.optJSONArray("link"))
                ?.let { normalizeHttps(it) }
                ?: BLOG_BASE_URL
            val excerpt = buildExcerpt(stripHtmlToPlainText(bodyHtml))

            pages.add(
                BlogPageEntity(
                    id = rawId,
                    title = title,
                    contentHtml = bodyHtml,
                    excerpt = excerpt,
                    updatedIso = updatedIso,
                    pageUrl = pageUrl,
                    orderIndex = i
                )
            )
        }
        return pages
    }

    fun parseCommentsFeed(jsonText: String, fallbackPostId: String = "", fallbackPostTitle: String = ""): List<BlogComment> {
        val root = JSONObject(jsonText)
        val feed = root.optJSONObject("feed") ?: return emptyList()
        val entries = feed.optJSONArray("entry") ?: return emptyList()
        val comments = ArrayList<BlogComment>(entries.length())

        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            val id = entry.optJSONObject("id")?.optString("\$t").orEmpty()
            if (id.isBlank()) continue

            val authorObj = entry.optJSONArray("author")?.optJSONObject(0)
            val authorName = authorObj?.optJSONObject("name")?.optString("\$t").orEmpty()
                .ifBlank { "පාඨකයෙක්" }
            val authorAvatar = upgradeImageUrl(
                authorObj?.optJSONObject("gd\$image")?.optString("src")
            )

            val publishedIso = entry.optJSONObject("published")?.optString("\$t").orEmpty()
            val publishedMillis = parseIso8601ToMillis(publishedIso)

            val rawHtml = entry.optJSONObject("content")?.optString("\$t").orEmpty()
                .ifBlank { entry.optJSONObject("summary")?.optString("\$t").orEmpty() }
            val contentText = stripHtmlToPlainText(rawHtml)

            val inReplyTo = entry.optJSONObject("thr\$in-reply-to")
            val postHref = inReplyTo?.optString("href").orEmpty()
            val refId = inReplyTo?.optString("ref").orEmpty()
            val resolvedPostId = refId.ifBlank { fallbackPostId }

            val relatedTitle = entry.optJSONArray("link")?.let { links ->
                var found = ""
                for (j in 0 until links.length()) {
                    val link = links.optJSONObject(j)
                    if (link?.optString("rel") == "related") {
                        found = link.optString("title")
                        break
                    }
                }
                found
            }.orEmpty().ifBlank { fallbackPostTitle }

            val commentUrl = extractAlternateLink(entry.optJSONArray("link"))
                ?.let { normalizeHttps(it) }
                ?: normalizeHttps(postHref).ifBlank { BLOG_BASE_URL }

            if (contentText.isNotBlank()) {
                comments.add(
                    BlogComment(
                        id = id,
                        postId = resolvedPostId,
                        postTitle = decodeHtmlEntities(relatedTitle),
                        authorName = authorName,
                        authorAvatarUrl = authorAvatar,
                        publishedIso = publishedIso,
                        publishedMillis = publishedMillis,
                        contentText = contentText,
                        commentUrl = commentUrl
                    )
                )
            }
        }
        return comments
    }

    fun extractNavLinksFromBlogHtml(html: String): List<ExternalBlogLink> {
        val links = LinkedHashMap<String, String>()
        // Extract Blogger PageList / navigation links or notable menu links
        val anchorRegex = Regex(
            """<a\s+[^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        // Focus on header/nav/PageList sections if present
        val pageListMatch = Regex(
            """(?:id=['"]PageList[^'"]*['"]|class=['"][^'"]*PageList[^'"]*['"]|<nav\b[^>]*>)(.*?)(?:</nav>|</div>\s*</div>)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html)

        val scanTarget = pageListMatch?.groupValues?.getOrNull(1) ?: return emptyList()
        for (match in anchorRegex.findAll(scanTarget)) {
            val href = normalizeHttps(match.groupValues[1].trim())
            val title = stripHtmlToPlainText(match.groupValues[2]).trim()
            if (href.startsWith("http") && title.isNotBlank() && title.length <= 60 && !href.contains("#")) {
                links[href] = title
            }
        }
        return links.entries.map { ExternalBlogLink(title = it.value, url = it.key) }
    }

    fun extractMetaDescriptionFromHtml(html: String): String {
        val metaRegex = Regex(
            """<meta\s+[^>]*name=["']description["'][^>]*content=["']([^"']*)["']""",
            RegexOption.IGNORE_CASE
        )
        val match = metaRegex.find(html) ?: Regex(
            """<meta\s+[^>]*content=["']([^"']*)["'][^>]*name=["']description["']""",
            RegexOption.IGNORE_CASE
        ).find(html)
        return decodeHtmlEntities(match?.groupValues?.getOrNull(1).orEmpty()).trim()
    }

    fun extractFullPostBodyFromWebHtml(html: String): String? {
        val startMarkers = listOf(
            """class='post-body""",
            """class="post-body""",
            """itemprop='articleBody'""",
            """itemprop="articleBody""""
        )
        var startIdx = -1
        for (marker in startMarkers) {
            val idx = html.indexOf(marker, ignoreCase = true)
            if (idx != -1) {
                val tagClose = html.indexOf('>', idx)
                if (tagClose != -1) {
                    startIdx = tagClose + 1
                    break
                }
            }
        }
        if (startIdx == -1) return null

        val endMarkers = listOf(
            """<div class='post-footer""",
            """<div class="post-footer""",
            """<div class='post-share""",
            """<div class="post-share""",
            """<div id='comments""",
            """<div id="comments""",
            """<footer"""
        )
        var endIdx = html.length
        for (marker in endMarkers) {
            val idx = html.indexOf(marker, startIndex = startIdx, ignoreCase = true)
            if (idx != -1 && idx < endIdx) {
                endIdx = idx
            }
        }
        if (endIdx <= startIdx) return null
        val snippet = html.substring(startIdx, endIdx).trim()
        return snippet.ifBlank { null }
    }

    fun encodeNavLinks(links: List<ExternalBlogLink>): String =
        links.joinToString("\n") { "${it.title.replace("|", " ")}|${it.url}" }

    fun decodeNavLinks(raw: String?): List<ExternalBlogLink> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val idx = line.lastIndexOf('|')
            if (idx <= 0 || idx >= line.length - 1) null
            else ExternalBlogLink(
                title = line.substring(0, idx).trim(),
                url = line.substring(idx + 1).trim()
            )
        }
    }

    private fun extractAlternateLink(links: JSONArray?): String? {
        if (links == null) return null
        for (i in 0 until links.length()) {
            val linkObj = links.optJSONObject(i) ?: continue
            if (linkObj.optString("rel") == "alternate") {
                val href = linkObj.optString("href")
                if (href.isNotBlank()) return href
            }
        }
        return null
    }

    private fun extractLabels(categories: JSONArray?): List<String> {
        if (categories == null) return emptyList()
        val list = ArrayList<String>(categories.length())
        for (i in 0 until categories.length()) {
            val term = categories.optJSONObject(i)?.optString("term")?.trim().orEmpty()
            if (term.isNotBlank()) {
                list.add(term)
            }
        }
        return list.distinct()
    }

    private fun extractFirstImageFromHtml(html: String): String? {
        val imgRegex = Regex(
            """<img\s+[^>]*src=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        )
        for (match in imgRegex.findAll(html)) {
            val src = match.groupValues[1].trim()
            if (src.startsWith("http") && !src.contains("icon_18") && !src.contains("tracking")) {
                return src
            }
        }
        return null
    }

    fun upgradeImageUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        var url = normalizeHttps(rawUrl.trim())
        if (url.startsWith("//")) url = "https:$url"
        if (!url.startsWith("http")) return null
        // Blogger tiny thumbnails like /s72-c/ or /s72-w400-h300-c/ or =s72-c -> /s800/
        url = url.replace(Regex("""/s\d+(-[a-zA-Z0-9_-]+)?/"""), "/s800/")
        url = url.replace(Regex("""=s\d+(-[a-zA-Z0-9_-]+)?$"""), "=s800")
        return url
    }

    fun normalizeHttps(url: String): String {
        return when {
            url.startsWith("http://") -> "https://" + url.removePrefix("http://")
            url.startsWith("//") -> "https:$url"
            else -> url
        }
    }

    fun stripHtmlToPlainText(html: String): String {
        if (html.isBlank()) return ""
        val withoutStylesAndScripts = html
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        val spanned = Html.fromHtml(withoutStylesAndScripts, Html.FROM_HTML_MODE_COMPACT)
        return spanned.toString()
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun decodeHtmlEntities(text: String): String {
        if (text.isBlank()) return ""
        return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }

    fun buildExcerpt(plainText: String, maxChars: Int = 180): String {
        if (plainText.length <= maxChars) return plainText
        val cut = plainText.take(maxChars)
        val lastSpace = cut.lastIndexOf(' ')
        return (if (lastSpace > 60) cut.take(lastSpace) else cut).trimEnd() + "…"
    }

    fun estimateReadingMinutes(plainText: String): Int {
        if (plainText.isBlank()) return 1
        val words = plainText.split(Regex("\\s+")).count { it.isNotBlank() }
        // Sinhala prose typically reads around 140–170 words/min comfortably
        return ((words + 149) / 150).coerceAtLeast(1)
    }

    fun parseIso8601ToMillis(iso: String): Long {
        if (iso.isBlank()) return 0L
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ"
        )
        for (pattern in patterns) {
            runCatching {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(iso)
                if (date != null) return date.time
            }
        }
        return 0L
    }

    fun formatSinhalaDate(millis: Long, fallbackIso: String = ""): String {
        if (millis <= 0L) return fallbackIso.take(10)
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return "$year ${sinhalaMonthName(month)} $day"
    }

    fun sinhalaMonthName(zeroBasedMonth: Int): String = when (zeroBasedMonth) {
        0 -> "ජනවාරි"
        1 -> "පෙබරවාරි"
        2 -> "මාර්තු"
        3 -> "අප්‍රේල්"
        4 -> "මැයි"
        5 -> "ජූනි"
        6 -> "ජූලි"
        7 -> "අගෝස්තු"
        8 -> "සැප්තැම්බර්"
        9 -> "ඔක්තෝබර්"
        10 -> "නොවැම්බර්"
        11 -> "දෙසැම්බර්"
        else -> ""
    }
}

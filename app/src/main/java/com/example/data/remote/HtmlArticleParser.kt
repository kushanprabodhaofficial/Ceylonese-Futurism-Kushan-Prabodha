package com.example.data.remote

import android.text.Html

sealed class ArticleBlock {
    data class Heading(val text: String, val level: Int) : ArticleBlock()
    data class Paragraph(val segments: List<InlineSegment>) : ArticleBlock()
    data class Quote(val text: String) : ArticleBlock()
    data class BulletItem(val bulletLabel: String, val segments: List<InlineSegment>) : ArticleBlock()
    data class ImageBlock(val imageUrl: String, val caption: String?) : ArticleBlock()
    data class VideoEmbedBlock(val title: String, val watchUrl: String, val thumbnailUrl: String?) : ArticleBlock()
    data object Divider : ArticleBlock()
}

data class InlineSegment(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val linkUrl: String? = null
)

object HtmlArticleParser {

    fun parse(rawHtml: String): List<ArticleBlock> {
        if (rawHtml.isBlank()) return emptyList()

        val cleaned = rawHtml
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            .replace("<br\\s*/?>".toRegex(RegexOption.IGNORE_CASE), "\n")
            .replace("</div>".toRegex(RegexOption.IGNORE_CASE), "\n")
            .replace("<div[^>]*>".toRegex(RegexOption.IGNORE_CASE), "\n")

        val blocks = mutableListOf<ArticleBlock>()

        // Tokenize major block tags while keeping inline text & media in order
        val blockTokenRegex = Regex(
            """(<h[1-6]\b[^>]*>.*?</h[1-6]>|<blockquote\b[^>]*>.*?</blockquote>|<li\b[^>]*>.*?</li>|<p\b[^>]*>.*?</p>|<hr\b[^>]*/?>|<iframe\b[^>]*>.*?</iframe>|<iframe\b[^>]*/?>|<img\b[^>]*>)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        var lastIndex = 0
        var bulletIndex = 0

        for (match in blockTokenRegex.findAll(cleaned)) {
            if (match.range.first > lastIndex) {
                val between = cleaned.substring(lastIndex, match.range.first)
                appendLooseTextAndImages(between, blocks)
            }

            val token = match.value
            val lower = token.lowercase()

            when {
                lower.startsWith("<hr") -> {
                    blocks.add(ArticleBlock.Divider)
                    bulletIndex = 0
                }
                lower.startsWith("<h") -> {
                    val level = token.getOrNull(2)?.digitToIntOrNull() ?: 2
                    val headingText = BloggerJsonParser.stripHtmlToPlainText(token)
                    if (headingText.isNotBlank()) {
                        blocks.add(ArticleBlock.Heading(headingText, level))
                    }
                    bulletIndex = 0
                }
                lower.startsWith("<blockquote") -> {
                    val quoteText = BloggerJsonParser.stripHtmlToPlainText(token)
                    if (quoteText.isNotBlank()) {
                        blocks.add(ArticleBlock.Quote(quoteText))
                    }
                    bulletIndex = 0
                }
                lower.startsWith("<li") -> {
                    bulletIndex++
                    val segments = parseInlineSegments(token)
                    if (segments.isNotEmpty()) {
                        blocks.add(ArticleBlock.BulletItem("•", segments))
                    }
                }
                lower.startsWith("<iframe") -> {
                    parseVideoIframe(token)?.let { blocks.add(it) }
                    bulletIndex = 0
                }
                lower.startsWith("<img") -> {
                    extractImageBlock(token)?.let { blocks.add(it) }
                }
                lower.startsWith("<p") -> {
                    bulletIndex = 0
                    // A <p> in Blogger frequently wraps an <img> or <iframe> plus text
                    extractNestedMediaAndParagraph(token, blocks)
                }
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < cleaned.length) {
            appendLooseTextAndImages(cleaned.substring(lastIndex), blocks)
        }

        return blocks
    }

    private fun appendLooseTextAndImages(fragment: String, out: MutableList<ArticleBlock>) {
        if (fragment.isBlank()) return
        val lines = fragment.split(Regex("\n{2,}"))
        for (chunk in lines) {
            if (chunk.isBlank()) continue
            extractNestedMediaAndParagraph(chunk, out)
        }
    }

    private fun extractNestedMediaAndParagraph(fragment: String, out: MutableList<ArticleBlock>) {
        val imgRegex = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
        val iframeRegex = Regex("""<iframe\b[^>]*>.*?</iframe>|<iframe\b[^>]*/?>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

        var working = fragment
        for (iframeMatch in iframeRegex.findAll(fragment)) {
            parseVideoIframe(iframeMatch.value)?.let { out.add(it) }
            working = working.replace(iframeMatch.value, "")
        }

        val imgMatches = imgRegex.findAll(working).toList()
        if (imgMatches.isNotEmpty()) {
            val altCaption = BloggerJsonParser.stripHtmlToPlainText(working)
            for (imgMatch in imgMatches) {
                extractImageBlock(imgMatch.value, fallbackCaption = altCaption.takeIf { it.length in 2..120 })?.let {
                    out.add(it)
                }
                working = working.replace(imgMatch.value, "")
            }
        }

        // Split remaining text by explicit line breaks that look like paragraphs
        val textParts = working.split(Regex("""\n+"""))
        for (part in textParts) {
            val segments = parseInlineSegments(part)
            val totalText = segments.joinToString("") { it.text }.trim()
            if (totalText.isNotBlank()) {
                out.add(ArticleBlock.Paragraph(segments))
            }
        }
    }

    private fun extractImageBlock(imgTag: String, fallbackCaption: String? = null): ArticleBlock.ImageBlock? {
        val srcMatch = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(imgTag)
        val rawSrc = srcMatch?.groupValues?.getOrNull(1)?.trim() ?: return null
        val upgraded = BloggerJsonParser.upgradeImageUrl(rawSrc) ?: return null
        if (upgraded.contains("icon_18") || upgraded.contains("blogger.com/img/blank")) return null

        val altMatch = Regex("""alt=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(imgTag)
        val altText = altMatch?.groupValues?.getOrNull(1)?.let { BloggerJsonParser.decodeHtmlEntities(it) }
            ?.takeIf { it.isNotBlank() && !it.startsWith("http") }

        return ArticleBlock.ImageBlock(
            imageUrl = upgraded,
            caption = altText ?: fallbackCaption
        )
    }

    private fun parseVideoIframe(iframeTag: String): ArticleBlock.VideoEmbedBlock? {
        val src = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(iframeTag)?.groupValues?.getOrNull(1)?.trim() ?: return null
        val normalized = BloggerJsonParser.normalizeHttps(src)
        val youtubeId = Regex("""(?:youtube\.com/embed/|youtu\.be/)([A-Za-z0-9_-]{6,})""")
            .find(normalized)?.groupValues?.getOrNull(1)
        return if (youtubeId != null) {
            ArticleBlock.VideoEmbedBlock(
                title = "වීඩියෝව නරඹන්න (YouTube)",
                watchUrl = "https://www.youtube.com/watch?v=$youtubeId",
                thumbnailUrl = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"
            )
        } else {
            ArticleBlock.VideoEmbedBlock(
                title = "මාධ්‍ය අන්තර්ගතය විවෘත කරන්න",
                watchUrl = normalized,
                thumbnailUrl = null
            )
        }
    }

    private fun parseInlineSegments(htmlFragment: String): List<InlineSegment> {
        val withoutTagsToKeep = htmlFragment
            .replace(Regex("</?(?:p|div|span|ul|ol|li|font|center)[^>]*>", RegexOption.IGNORE_CASE), "")

        val inlineTagRegex = Regex(
            """(<a\b[^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>|<strong\b[^>]*>(.*?)</strong>|<b\b[^>]*>(.*?)</b>|<em\b[^>]*>(.*?)</em>|<i\b[^>]*>(.*?)</i>)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        val segments = mutableListOf<InlineSegment>()
        var lastIdx = 0

        for (match in inlineTagRegex.findAll(withoutTagsToKeep)) {
            if (match.range.first > lastIdx) {
                val plain = decodeClean(withoutTagsToKeep.substring(lastIdx, match.range.first))
                if (plain.isNotEmpty()) segments.add(InlineSegment(text = plain))
            }

            val full = match.value.lowercase()
            when {
                full.startsWith("<a") -> {
                    val url = BloggerJsonParser.normalizeHttps(match.groupValues[2].trim())
                    val text = decodeClean(match.groupValues[3])
                    if (text.isNotEmpty()) {
                        segments.add(InlineSegment(text = text, linkUrl = url))
                    }
                }
                full.startsWith("<strong") || full.startsWith("<b") -> {
                    val inner = match.groupValues[4].ifEmpty { match.groupValues[5] }
                    val text = decodeClean(inner)
                    if (text.isNotEmpty()) {
                        segments.add(InlineSegment(text = text, isBold = true))
                    }
                }
                full.startsWith("<em") || full.startsWith("<i") -> {
                    val inner = match.groupValues[6].ifEmpty { match.groupValues[7] }
                    val text = decodeClean(inner)
                    if (text.isNotEmpty()) {
                        segments.add(InlineSegment(text = text, isItalic = true))
                    }
                }
            }
            lastIdx = match.range.last + 1
        }

        if (lastIdx < withoutTagsToKeep.length) {
            val tail = decodeClean(withoutTagsToKeep.substring(lastIdx))
            if (tail.isNotEmpty()) segments.add(InlineSegment(text = tail))
        }

        // Collapse redundant whitespace safely
        return segments.mapNotNull { seg ->
            val cleanedText = seg.text.replace(Regex("[ \\t\\r\\n]+"), " ")
            if (cleanedText.isEmpty()) null else seg.copy(text = cleanedText)
        }
    }

    private fun decodeClean(fragment: String): String {
        val stripped = fragment.replace(Regex("<[^>]+>"), "")
        return Html.fromHtml(stripped, Html.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace('\u00A0', ' ')
    }
}

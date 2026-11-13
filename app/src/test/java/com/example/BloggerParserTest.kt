package com.example

import com.example.data.remote.ArticleBlock
import com.example.data.remote.BloggerJsonParser
import com.example.data.remote.HtmlArticleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BloggerParserTest {

    @Test
    fun `parsePostsFeed parses Sinhala title labels and upgrades thumbnail`() {
        val sampleFeed = """
            {
              "feed": {
                "title": {"${'$'}t": "Ceylonese Futurism"},
                "subtitle": {"${'$'}t": "සිංහල විද්‍යා ප්‍රබන්ධ"},
                "openSearch${'$'}totalResults": {"${'$'}t": "1"},
                "author": [{"name": {"${'$'}t": "Kushan Prabodha"}}],
                "link": [{"rel": "alternate", "href": "https://kushanprabodha.blogspot.com/"}],
                "entry": [
                  {
                    "id": {"${'$'}t": "tag:blogger.com,1999:blog-111.post-999"},
                    "published": {"${'$'}t": "2026-05-10T09:15:00.000+05:30"},
                    "updated": {"${'$'}t": "2026-05-10T09:15:00.000+05:30"},
                    "title": {"${'$'}t": "අවකාශයේ අවසාන පණිවිඩය"},
                    "content": {"${'$'}t": "<p>මෙය <b>විද්‍යා ප්‍රබන්ධ</b> කතාවකි.</p>"},
                    "category": [{"term": "විද්‍යා ප්‍රබන්ධ"}, {"term": "Entropy Sutras"}],
                    "link": [{"rel": "alternate", "href": "http://kushanprabodha.blogspot.com/2026/05/story.html"}],
                    "media${'$'}thumbnail": {"url": "https://blogger.googleusercontent.com/img/b/R29vZ2xl/s72-c/cover.jpg"},
                    "thr${'$'}total": {"${'$'}t": "4"}
                  }
                ]
              }
            }
        """.trimIndent()

        val parsed = BloggerJsonParser.parsePostsFeed(sampleFeed)
        assertEquals(1, parsed.totalResults)
        assertEquals(1, parsed.posts.size)
        val post = parsed.posts.first()
        assertEquals("999", post.numericPostId)
        assertEquals("අවකාශයේ අවසාන පණිවිඩය", post.title)
        assertEquals(listOf("විද්‍යා ප්‍රබන්ධ", "Entropy Sutras"), post.labelsList())
        assertEquals(4, post.commentCount)
        assertTrue(post.postUrl.startsWith("https://"))
        assertTrue(post.thumbnailUrl?.contains("/s800/") == true)
    }

    @Test
    fun `htmlArticleParser parses headings paragraphs quotes and images`() {
        val html = """
            <h2>පළමු පරිච්ඡේදය</h2>
            <p>නගරය <strong>නියෝන්</strong> ආලෝකයෙන් බැබළුණි.</p>
            <blockquote>කාලය යනු මායාවකි.</blockquote>
            <img src="http://example.com/s72-c/art.jpg" alt="Ceylon" />
        """.trimIndent()

        val blocks = HtmlArticleParser.parse(html)
        assertTrue(blocks.any { it is ArticleBlock.Heading && it.text == "පළමු පරිච්ඡේදය" })
        assertTrue(blocks.any { it is ArticleBlock.Quote && it.text.contains("මායාවකි") })
        assertTrue(blocks.any { it is ArticleBlock.ImageBlock && it.imageUrl.startsWith("https://") })
        assertTrue(blocks.any { it is ArticleBlock.Paragraph })
    }

    @Test
    fun `storyMoodAnalyzer matches Sinhala sci-fi stories to fitting soundscapes`() {
        val spacePost = com.example.data.local.BlogPostEntity(
            id = "1",
            numericPostId = "1",
            title = "අභ්‍යවකාශ තාරකා යානය",
            contentHtml = "<p>විශ්වයේ ශුන්‍ය අවකාශය හරහා ගමන් කළේය.</p>",
            isSummaryOnly = false,
            excerpt = "",
            publishedIso = "",
            publishedMillis = 0L,
            updatedIso = "",
            authorName = "Kushan",
            authorImageUrl = null,
            thumbnailUrl = null,
            postUrl = "",
            labelsDelimited = "විද්‍යා ප්‍රබන්ධ",
            commentCount = 0,
            readingTimeMinutes = 3
        )
        assertEquals(
            com.example.audio.StorySoundscape.COSMIC_VOID,
            com.example.audio.StoryMoodAnalyzer.analyzePost(spacePost)
        )

        val cyberPost = spacePost.copy(
            id = "2",
            title = "නියෝන් සයිබර් නගරයේ රොබෝ",
            contentHtml = "<p>කෘත්‍රිම බුද්ධි ජාලය සහ පරිගණක චිප්</p>"
        )
        assertEquals(
            com.example.audio.StorySoundscape.CYBERPUNK_PULSE,
            com.example.audio.StoryMoodAnalyzer.analyzePost(cyberPost)
        )
    }
}

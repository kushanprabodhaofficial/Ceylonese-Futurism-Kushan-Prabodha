package com.example

import com.example.audio.StoryMoodAnalyzer
import com.example.audio.SynthArchitecture
import com.example.data.local.BlogPostEntity
import com.example.data.model.SciFiSubGenre
import com.example.data.model.SubGenreClassifier
import com.example.data.remote.ArticleBlock
import com.example.data.remote.BloggerJsonParser
import com.example.data.remote.HtmlArticleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun `htmlArticleParser parses headings paragraphs quotes images and adsense blocks`() {
        val html = """
            <h2>පළමු පරිච්ඡේදය</h2>
            <p>නගරය <strong>නියෝන්</strong> ආලෝකයෙන් බැබළුණි.</p>
            <ins class="adsbygoogle" data-ad-client="ca-pub-1234567890123456" data-ad-slot="9876543210"></ins>
            <blockquote>කාලය යනු මායාවකි.</blockquote>
            <img src="http://example.com/s72-c/art.jpg" alt="Ceylon" />
        """.trimIndent()

        val blocks = HtmlArticleParser.parse(html)
        assertTrue(blocks.any { it is ArticleBlock.Heading && it.text == "පළමු පරිච්ඡේදය" })
        assertTrue(blocks.any { it is ArticleBlock.Quote && it.text.contains("මායාවකි") })
        assertTrue(blocks.any { it is ArticleBlock.ImageBlock && it.imageUrl.startsWith("https://") })
        assertTrue(blocks.any { it is ArticleBlock.Paragraph })
        val adBlock = blocks.filterIsInstance<ArticleBlock.AdSenseBlock>().firstOrNull()
        assertEquals("ca-pub-1234567890123456", adBlock?.adClient)
        assertEquals("9876543210", adBlock?.adSlot)
    }

    @Test
    fun `adsense metadata extractor finds publisher and slots from blog html`() {
        val sampleHtml = """
            <html><head>
            <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-5544332211009988"></script>
            </head><body>
            <ins class="adsbygoogle" data-ad-slot="1122334455"></ins>
            <ins class="adsbygoogle" data-ad-slot="6677889900"></ins>
            </body></html>
        """.trimIndent()

        assertEquals("ca-pub-5544332211009988", BloggerJsonParser.extractAdSensePublisherId(sampleHtml))
        assertEquals(listOf("1122334455", "6677889900"), BloggerJsonParser.extractAdSenseSlotIds(sampleHtml))
    }

    @Test
    fun `subGenreClassifier categorizes time travel and hela heritage stories accurately`() {
        val timeTravelPost = BlogPostEntity(
            id = "t1",
            numericPostId = "t1",
            title = "කාලතරණ යාත්‍රිකයා",
            contentHtml = "<p>අතීතය සහ අනාගතය අතර කාලය හරහා සමාන්තර ගමන.</p>",
            isSummaryOnly = false,
            excerpt = "",
            publishedIso = "",
            publishedMillis = 0L,
            updatedIso = "",
            authorName = "Kushan",
            authorImageUrl = null,
            thumbnailUrl = null,
            postUrl = "",
            labelsDelimited = "Kushan prabodha",
            commentCount = 0,
            readingTimeMinutes = 4
        )
        assertTrue(SubGenreClassifier.classifyPost(timeTravelPost).contains(SciFiSubGenre.TIME_TRAVEL))

        val helaPost = timeTravelPost.copy(
            id = "h1",
            title = "හෙළ සූත්‍රය හා සුගලා දේවිය",
            contentHtml = "<p>පුරාණ ලංකාවේ බෞද්ධ දර්ශනය සහ රාවණ තාක්ෂණය</p>"
        )
        assertTrue(SubGenreClassifier.classifyPost(helaPost).contains(SciFiSubGenre.HELA_HERITAGE))
    }

    @Test
    fun `storyMoodAnalyzer creates unique theme-matched pair of two distinct tracks per story`() {
        val spacePostA = BlogPostEntity(
            id = "post-space-1",
            numericPostId = "101",
            title = "අභ්‍යවකාශ තාරකා යානය",
            contentHtml = "<p>විශ්වයේ ශුන්‍ය අවකාශය හරහා ගමන් කළේය.</p>",
            isSummaryOnly = false,
            excerpt = "",
            publishedIso = "2026-01-10T10:00:00Z",
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
        val spacePostB = spacePostA.copy(
            id = "post-space-2",
            numericPostId = "102",
            title = "නීහාරිකා ද්වාරය සහ තාරකා ගමන",
            publishedIso = "2026-02-14T10:00:00Z"
        )
        val cyberPost = spacePostA.copy(
            id = "post-cyber-1",
            numericPostId = "201",
            title = "නියෝන් සයිබර් නගරයේ රොබෝ",
            contentHtml = "<p>කෘත්‍රිම බුද්ධි ජාලය සහ පරිගණක චිප්</p>"
        )

        val pairA = StoryMoodAnalyzer.createMusicPairForPost(spacePostA)
        val pairB = StoryMoodAnalyzer.createMusicPairForPost(spacePostB)
        val pairCyber = StoryMoodAnalyzer.createMusicPairForPost(cyberPost)

        // Each story has 2 distinct tracks with different keys/architectures/seeds
        assertEquals(2, pairA.tracks.size)
        assertNotEquals(pairA.primaryTrack.trackId, pairA.secondaryTrack.trackId)
        assertNotEquals(pairA.primaryTrack.architecture, pairA.secondaryTrack.architecture)
        assertNotEquals(pairA.primaryTrack.rootMidi, pairA.secondaryTrack.rootMidi)
        assertNotEquals(pairA.primaryTrack.seed, pairA.secondaryTrack.seed)

        // Two different stories even in the same sub-genre receive different unique music pairs
        assertNotEquals(pairA.primaryTrack.trackId, pairB.primaryTrack.trackId)
        assertNotEquals(pairA.primaryTrack.seed, pairB.primaryTrack.seed)
        assertNotEquals(pairA.primaryTrack.titleSinhala, pairB.primaryTrack.titleSinhala)

        // Theme-matched synthesis architecture
        assertEquals(SynthArchitecture.CYBER_NEON_ARP, pairCyber.primaryTrack.architecture)
    }
}

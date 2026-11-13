package com.example.audio

import com.example.data.local.BlogPostEntity
import com.example.data.remote.BloggerJsonParser

enum class StorySoundscape(
    val displayNameSinhala: String,
    val subtitleSinhala: String
) {
    COSMIC_VOID(
        displayNameSinhala = "අභ්‍යවකාශ නිහැඬියාව",
        subtitleSinhala = "Cosmic Space Ambient"
    ),
    CYBERPUNK_PULSE(
        displayNameSinhala = "සයිබර්පන්ක් රිද්මය",
        subtitleSinhala = "Neon Synthwave Pulse"
    ),
    MYSTIC_CEYLON(
        displayNameSinhala = "හෙළ අභිරහස් නාදය",
        subtitleSinhala = "Mystic Ceylonese Drone"
    ),
    TIME_HORIZON(
        displayNameSinhala = "කාල තරණ ස්වරය",
        subtitleSinhala = "Temporal Dreamscape"
    ),
    DYSTOPIAN_TENSION(
        displayNameSinhala = "අඳුරු අනාගත නාදය",
        subtitleSinhala = "Dark Sci-Fi Suspense"
    ),
    SERENE_CONTEMPLATION(
        displayNameSinhala = "සෞම්‍ය චින්තන නාදය",
        subtitleSinhala = "Atmospheric Reading Pad"
    )
}

object StoryMoodAnalyzer {

    private val cosmicKeywords = listOf(
        "අභ්‍යවකාශ", "අවකාශ", "තාරකා", "තරු", "විශ්ව", "ග්‍රහ", "සඳ", "හිරු", "චක්‍රාවාට",
        "නිහාරිකා", "ශුන්‍ය", "space", "cosmic", "void", "entropy", "galaxy", "star", "orbit",
        "planet", "universe", "nebula", "astronaut", "moon", "mars", "rocket"
    )

    private val cyberKeywords = listOf(
        "සයිබර්", "රොබෝ", "පරිගණක", "කෘත්‍රිම", "බුද්ධි", "නියෝන්", "ජාල", "ඩිජිටල්", "යන්ත්‍ර",
        "චිප්", "හොලෝග්‍රෑම්", "cyber", "robot", "android", "ai", "neural", "neon", "matrix",
        "virtual", "digital", "machine", "circuit", "hack", "synth", "tech", "code"
    )

    private val mysticKeywords = listOf(
        "සූත්‍ර", "හෙළ", "ලංකා", "පුරාණ", "කර්ම", "භව", "යක්ෂ", "රාවණ", "වෙහෙර", "දේව",
        "මන්ත්‍ර", "සිංහල", "රජ", "වන", "මුහුද", "sutra", "ceylon", "lanka", "folklore",
        "myth", "ancient", "temple", "karma", "spirit", "monk", "ritual", "buddh"
    )

    private val timeKeywords = listOf(
        "කාල", "සිහින", "මතක", "අතීත", "අනාගත", "සමාන්තර", "මානය", "ක්වොන්ටම්", "ප්‍රතිබිම්බ",
        "time", "dream", "memory", "past", "future", "parallel", "dimension", "quantum",
        "mirror", "portal", "chron", "echo"
    )

    private val dystopianKeywords = listOf(
        "විනාශ", "අඳුරු", "මරණ", "යුද්ධ", "වෛරස", "බිය", "භීතිකා", "සෙවණැලි", "අවසාන",
        "රාත්‍රී", "ලේ", "dark", "war", "death", "apocalypse", "dystopia", "shadow", "virus",
        "extinction", "fear", "nightmare", "ruin", "blood", "doom"
    )

    fun analyzePost(post: BlogPostEntity): StorySoundscape {
        val sampleText = buildString {
            append(post.title).append(' ')
            append(post.labelsDelimited.replace("|||", " ")).append(' ')
            append(post.excerpt).append(' ')
            // Include first ~1500 chars of stripped HTML for deeper theme matching
            val bodySample = BloggerJsonParser.stripHtmlToPlainText(post.contentHtml.take(4000))
            append(bodySample.take(1500))
        }.lowercase()

        val scores = mapOf(
            StorySoundscape.COSMIC_VOID to countMatches(sampleText, cosmicKeywords),
            StorySoundscape.CYBERPUNK_PULSE to countMatches(sampleText, cyberKeywords),
            StorySoundscape.MYSTIC_CEYLON to countMatches(sampleText, mysticKeywords),
            StorySoundscape.TIME_HORIZON to countMatches(sampleText, timeKeywords),
            StorySoundscape.DYSTOPIAN_TENSION to countMatches(sampleText, dystopianKeywords)
        )

        val best = scores.maxByOrNull { it.value }
        return if (best != null && best.value > 0) {
            best.key
        } else {
            // Deterministic fallback based on post ID hash so every story feels distinct
            val fallbacks = listOf(
                StorySoundscape.SERENE_CONTEMPLATION,
                StorySoundscape.COSMIC_VOID,
                StorySoundscape.TIME_HORIZON,
                StorySoundscape.MYSTIC_CEYLON
            )
            val idx = (post.id.hashCode() and Int.MAX_VALUE) % fallbacks.size
            fallbacks[idx]
        }
    }

    private fun countMatches(text: String, keywords: List<String>): Int {
        var score = 0
        for (kw in keywords) {
            if (text.contains(kw)) {
                score += if (kw.length >= 5) 2 else 1
            }
        }
        return score
    }
}

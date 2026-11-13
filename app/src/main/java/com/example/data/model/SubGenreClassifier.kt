package com.example.data.model

import com.example.data.local.BlogPostEntity
import com.example.data.remote.BloggerJsonParser

enum class SciFiSubGenre(
    val titleSinhala: String,
    val subtitleEnglish: String,
    val descriptionSinhala: String
) {
    TIME_TRAVEL(
        titleSinhala = "කාලතරණ",
        subtitleEnglish = "Time Travel & Temporal",
        descriptionSinhala = "කාල යාත්‍රා, අතීත-අනාගත විරුද්ධාභාස සහ සමාන්තර කාල රේඛා"
    ),
    HELA_HERITAGE(
        titleSinhala = "හෙළ සංස්කෘතිකාංග මිශ්‍ර",
        subtitleEnglish = "Ceylonese Heritage & Myth",
        descriptionSinhala = "ශ්‍රී ලාංකික ඉතිහාසය, ජනශ්‍රැති, බෞද්ධ චින්තනය හා අනාගතවාදය"
    ),
    CYBERPUNK_AI(
        titleSinhala = "සයිබර්පන්ක් සහ AI",
        subtitleEnglish = "Cyberpunk, Robotics & AI",
        descriptionSinhala = "කෘත්‍රිම බුද්ධිය, රොබෝ තාක්ෂණය, නියෝන් නගර සහ සයිබර් ලෝක"
    ),
    SPACE_COSMOS(
        titleSinhala = "අභ්‍යවකාශ සහ විශ්වය",
        subtitleEnglish = "Space, Stars & Cosmos",
        descriptionSinhala = "තාරකා යාත්‍රා, ග්‍රහලෝක, එන්ට්‍රොපිය සහ විශ්වයේ ගැඹුර"
    ),
    CONSCIOUSNESS_PHILOSOPHY(
        titleSinhala = "විඥානය හා දර්ශනවාදය",
        subtitleEnglish = "Mind, Karma & Philosophy",
        descriptionSinhala = "මනස, මතක, සිහින, කර්මය සහ යථාර්ථය පිළිබඳ විද්‍යා ප්‍රබන්ධ"
    ),
    DARK_DYSTOPIA(
        titleSinhala = "අඳුරු අනාගතය",
        subtitleEnglish = "Dystopia & Post-Apocalypse",
        descriptionSinhala = "අනාගත විනාශයන්, යුද්ධ, වසංගත සහ පැවැත්මේ අරගල"
    )
}

data class SubGenreSummary(
    val subGenre: SciFiSubGenre,
    val count: Int,
    val latestPostTitle: String
)

object SubGenreClassifier {

    private val keywordMap: Map<SciFiSubGenre, List<Pair<String, Int>>> = mapOf(
        SciFiSubGenre.TIME_TRAVEL to listOf(
            "කාලතරණ" to 4, "කාල තරණ" to 4, "කාලය" to 2, "අතීත" to 2, "අනාගත" to 2,
            "සමාන්තර" to 3, "ප්‍රතිබිම්බ" to 2, "time" to 3, "temporal" to 3, "chron" to 3,
            "paradox" to 3, "timeline" to 3, "loop" to 2, "geometry" to 2, "past" to 2, "future" to 2
        ),
        SciFiSubGenre.HELA_HERITAGE to listOf(
            "හෙළ" to 4, "ලංකා" to 3, "සූත්‍ර" to 3, "බුදු" to 3, "බෞද්ධ" to 3, "කර්ම" to 3,
            "පුරාණ" to 3, "රාවණ" to 4, "යක්ෂ" to 3, "වෙහෙර" to 3, "සිංහල" to 2, "රජ" to 2,
            "සුගලා" to 4, "ceylon" to 3, "lanka" to 3, "sutra" to 3, "buddh" to 3, "karma" to 3,
            "folklore" to 3, "ancient" to 2, "temple" to 2, "monk" to 3, "sugala" to 4, "anatta" to 3
        ),
        SciFiSubGenre.CYBERPUNK_AI to listOf(
            "සයිබර්" to 4, "රොබෝ" to 4, "කෘත්‍රිම" to 3, "බුද්ධි" to 2, "පරිගණක" to 3,
            "නියෝන්" to 3, "ජාල" to 2, "ඩිජිටල්" to 3, "යන්ත්‍ර" to 3, "චිප්" to 3,
            "හොලෝග්‍රෑම්" to 3, "cyber" to 4, "robot" to 4, "android" to 4, "neural" to 3,
            "matrix" to 3, "digital" to 3, "virtual" to 3, "cloud" to 2, "ai" to 2, "code" to 2
        ),
        SciFiSubGenre.SPACE_COSMOS to listOf(
            "අභ්‍යවකාශ" to 4, "අවකාශ" to 3, "තාරකා" to 3, "තරු" to 2, "විශ්ව" to 3,
            "ග්‍රහ" to 3, "සඳ" to 2, "චක්‍රාවාට" to 4, "නිහාරිකා" to 4, "ශුන්‍ය" to 2,
            "space" to 4, "cosmic" to 4, "void" to 3, "entropy" to 3, "galaxy" to 3,
            "star" to 2, "orbit" to 3, "planet" to 3, "universe" to 3, "nebula" to 3, "mars" to 3
        ),
        SciFiSubGenre.CONSCIOUSNESS_PHILOSOPHY to listOf(
            "විඥාන" to 4, "මනස" to 3, "සිහින" to 3, "මතක" to 3, "ආත්ම" to 3,
            "අනාත්ම" to 4, "මායා" to 3, "දර්ශන" to 3, "සත්‍ය" to 2, "භව" to 3,
            "consciousness" to 4, "mind" to 3, "memory" to 3, "dream" to 3, "philosophy" to 3,
            "reality" to 3, "existence" to 3, "teleport" to 3, "soul" to 3, "quantum" to 2
        ),
        SciFiSubGenre.DARK_DYSTOPIA to listOf(
            "විනාශ" to 3, "අඳුරු" to 3, "මරණ" to 3, "යුද්ධ" to 3, "වෛරස" to 4,
            "අවසාන" to 2, "භීති" to 3, "සෙවණැලි" to 2, "රාත්‍රී" to 2, "අපාය" to 3,
            "dystopia" to 4, "apocalypse" to 4, "extinction" to 4, "war" to 3, "virus" to 3,
            "dark" to 2, "shadow" to 2, "doom" to 3, "death" to 2, "survival" to 3, "acid" to 2
        )
    )

    fun classifyPost(post: BlogPostEntity): List<SciFiSubGenre> {
        val titleAndLabels = "${post.title} ${post.labelsDelimited.replace("|||", " ")}".lowercase()
        val bodySample = BloggerJsonParser.stripHtmlToPlainText(post.contentHtml.take(5000)).lowercase()
        val fullSample = "$titleAndLabels ${post.excerpt.lowercase()} $bodySample"

        val scored = keywordMap.mapNotNull { (genre, weightedKeywords) ->
            var score = 0
            for ((kw, weight) in weightedKeywords) {
                if (titleAndLabels.contains(kw)) {
                    score += weight * 2
                } else if (fullSample.contains(kw)) {
                    score += weight
                }
            }
            if (score > 0) genre to score else null
        }.sortedByDescending { it.second }

        if (scored.isEmpty()) {
            return listOf(SciFiSubGenre.CONSCIOUSNESS_PHILOSOPHY)
        }

        val topScore = scored.first().second
        // Pick primary match plus up to 1 strong secondary sub-genre
        return scored
            .filter { it.second >= (topScore * 0.5f).toInt().coerceAtLeast(3) }
            .take(2)
            .map { it.first }
    }

    fun computeSummaries(posts: List<BlogPostEntity>): List<SubGenreSummary> {
        val grouped = linkedMapOf<SciFiSubGenre, MutableList<BlogPostEntity>>()
        for (genre in SciFiSubGenre.entries) {
            grouped[genre] = mutableListOf()
        }
        for (post in posts) {
            for (genre in classifyPost(post)) {
                grouped[genre]?.add(post)
            }
        }
        return grouped.entries
            .filter { it.value.isNotEmpty() }
            .map { (genre, list) ->
                SubGenreSummary(
                    subGenre = genre,
                    count = list.size,
                    latestPostTitle = list.firstOrNull()?.title.orEmpty()
                )
            }
            .sortedByDescending { it.count }
    }
}

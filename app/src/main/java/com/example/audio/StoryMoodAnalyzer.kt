package com.example.audio

import com.example.data.local.BlogPostEntity
import com.example.data.model.SciFiSubGenre
import com.example.data.model.SubGenreClassifier
import kotlin.math.abs
import kotlin.math.pow

enum class SynthArchitecture(val instrumentLabel: String) {
    MELODIC_PIANO_GROOVE("Rhodes & Vinyl"),
    ATMOS_PLUCK_DRIFT("Cosmic Pluck & Pad"),
    HELA_VEENA_PERCUSSION("Hela Veena & Drone"),
    TEMPORAL_CLOCKWORK("Temporal Chimes"),
    CYBER_NEON_ARP("Neon Synth Arp"),
    DARK_SUB_SUSPENSE("Sub Pulse & Bells")
}

data class StoryTrack(
    val trackId: String,
    val postId: String,
    val slotIndex: Int, // 0 or 1
    val architecture: SynthArchitecture,
    val titleSinhala: String,
    val shortButtonLabel: String,
    val subtitleSinhala: String,
    val seed: Long,
    val bpm: Double,
    val rootMidi: Int,
    val chordsHz: List<DoubleArray>,
    val melodyScaleHz: DoubleArray,
    val melodySteps: IntArray
)

data class StoryMusicPair(
    val postId: String,
    val subGenre: SciFiSubGenre,
    val primaryTrack: StoryTrack,
    val secondaryTrack: StoryTrack
) {
    val tracks: List<StoryTrack> get() = listOf(primaryTrack, secondaryTrack)
    fun trackAt(slotIndex: Int): StoryTrack = if (slotIndex == 1) secondaryTrack else primaryTrack
}

object StoryMoodAnalyzer {

    private val KEY_NAMES = arrayOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

    fun createMusicPairForPost(post: BlogPostEntity): StoryMusicPair {
        val subGenres = SubGenreClassifier.classifyPost(post)
        val primarySubGenre = subGenres.firstOrNull() ?: SciFiSubGenre.CONSCIOUSNESS_PHILOSOPHY
        val identityKey = "${post.numericPostId.ifBlank { post.id }}|${post.title.trim()}|${post.publishedIso}"
        val storyHash = stableHash64(identityKey)
        val shortTitle = buildShortTitle(post.title)

        val (arch1, arch2, labelPair) = when (primarySubGenre) {
            SciFiSubGenre.TIME_TRAVEL -> Triple(
                SynthArchitecture.TEMPORAL_CLOCKWORK,
                SynthArchitecture.MELODIC_PIANO_GROOVE,
                Pair("කාල රිද්මය" to "කාල චින්තන", "Temporal Chimes" to "Memory Rhodes")
            )
            SciFiSubGenre.HELA_HERITAGE -> Triple(
                SynthArchitecture.HELA_VEENA_PERCUSSION,
                SynthArchitecture.ATMOS_PLUCK_DRIFT,
                Pair("හෙළ වීණා" to "ගගන ස්වර", "Ceylonese Veena" to "Ancient Cosmos")
            )
            SciFiSubGenre.CYBERPUNK_AI -> Triple(
                SynthArchitecture.CYBER_NEON_ARP,
                SynthArchitecture.MELODIC_PIANO_GROOVE,
                Pair("නියෝන් කේත" to "ඩිජිටල් වැසි", "Cyber Neon Arp" to "Midnight Synth Lo-Fi")
            )
            SciFiSubGenre.SPACE_COSMOS -> Triple(
                SynthArchitecture.ATMOS_PLUCK_DRIFT,
                SynthArchitecture.TEMPORAL_CLOCKWORK,
                Pair("තාරකා අනුනාද" to "නීහාරිකා ස්වර", "Deep Space Pad" to "Orbital Pulse")
            )
            SciFiSubGenre.DARK_DYSTOPIA -> Triple(
                SynthArchitecture.DARK_SUB_SUSPENSE,
                SynthArchitecture.CYBER_NEON_ARP,
                Pair("අඳුරු ක්ෂිතිජ" to "ශේෂ අනාගත", "Dark Suspense" to "Dystopian Pulse")
            )
            SciFiSubGenre.CONSCIOUSNESS_PHILOSOPHY -> Triple(
                SynthArchitecture.MELODIC_PIANO_GROOVE,
                SynthArchitecture.ATMOS_PLUCK_DRIFT,
                Pair("විඥාන ප්‍රවාහ" to "සිහින මායා", "Contemplative Lo-Fi" to "Lucid Dreamscape")
            )
        }

        val track1 = buildUniqueTrack(
            postId = post.id,
            slotIndex = 0,
            storyHash = storyHash,
            subGenre = primarySubGenre,
            architecture = arch1,
            shortTitle = shortTitle,
            sinhalaStyleLabel = labelPair.first.first,
            englishStyleLabel = labelPair.second.first
        )

        val track2 = buildUniqueTrack(
            postId = post.id,
            slotIndex = 1,
            storyHash = storyHash xor 0x517CC1B727220A95L,
            subGenre = primarySubGenre,
            architecture = arch2,
            shortTitle = shortTitle,
            sinhalaStyleLabel = labelPair.first.second,
            englishStyleLabel = labelPair.second.second
        )

        return StoryMusicPair(
            postId = post.id,
            subGenre = primarySubGenre,
            primaryTrack = track1,
            secondaryTrack = track2
        )
    }

    private fun buildUniqueTrack(
        postId: String,
        slotIndex: Int,
        storyHash: Long,
        subGenre: SciFiSubGenre,
        architecture: SynthArchitecture,
        shortTitle: String,
        sinhalaStyleLabel: String,
        englishStyleLabel: String
    ): StoryTrack {
        val h1 = mixHash(storyHash, slotIndex.toLong() + 1L)
        val h2 = mixHash(h1, architecture.ordinal.toLong() + 31L)

        // Ensure Track 1 and Track 2 sit in distinct keys by offsetting slot 1 by 5 or 7 semitones
        val baseSemitone = positiveMod(storyHash, 12)
        val rootSemitone = if (slotIndex == 0) {
            baseSemitone
        } else {
            (baseSemitone + 5 + positiveMod(h1, 3)) % 12
        }
        val rootMidi = 36 + rootSemitone // Bass octave around C2..B2

        // Tempo unique to story & slot
        val (minBpm, maxBpm) = when (architecture) {
            SynthArchitecture.MELODIC_PIANO_GROOVE -> 66 to 78
            SynthArchitecture.ATMOS_PLUCK_DRIFT -> 62 to 76
            SynthArchitecture.HELA_VEENA_PERCUSSION -> 68 to 84
            SynthArchitecture.TEMPORAL_CLOCKWORK -> 82 to 104
            SynthArchitecture.CYBER_NEON_ARP -> 88 to 112
            SynthArchitecture.DARK_SUB_SUSPENSE -> 60 to 74
        }
        val bpm = (minBpm + positiveMod(h1, maxBpm - minBpm + 1)).toDouble()

        val scaleIntervals = when (subGenre) {
            SciFiSubGenre.HELA_HERITAGE ->
                if (slotIndex == 0) intArrayOf(0, 1, 4, 5, 7, 8, 11) // Bhairav / Raga-tinged Hela scale
                else intArrayOf(0, 2, 4, 7, 9, 11)
            SciFiSubGenre.TIME_TRAVEL ->
                if (slotIndex == 0) intArrayOf(0, 2, 3, 5, 7, 9, 10) // Dorian
                else intArrayOf(0, 2, 4, 6, 8, 10) // Whole-tone temporal shimmer
            SciFiSubGenre.CYBERPUNK_AI ->
                if (slotIndex == 0) intArrayOf(0, 2, 3, 7, 8, 11) // Cyberpunk harmonic minor
                else intArrayOf(0, 3, 5, 7, 10) // Minor pentatonic
            SciFiSubGenre.SPACE_COSMOS ->
                if (slotIndex == 0) intArrayOf(0, 2, 4, 6, 7, 9, 11) // Lydian cosmic
                else intArrayOf(0, 2, 5, 7, 9) // Suspended pentatonic
            SciFiSubGenre.DARK_DYSTOPIA ->
                if (slotIndex == 0) intArrayOf(0, 1, 3, 5, 7, 8, 10) // Phrygian dark
                else intArrayOf(0, 2, 3, 6, 7, 8, 11) // Double harmonic dark
            SciFiSubGenre.CONSCIOUSNESS_PHILOSOPHY ->
                if (slotIndex == 0) intArrayOf(0, 2, 4, 7, 9, 11) // Warm maj9 jazz
                else intArrayOf(0, 3, 5, 7, 9, 10) // Soft dorian dream
        }

        // Generate unique 4-chord progression (each chord has 1 bass voice + 4 upper voices)
        val progressionDegrees = when (positiveMod(h2, 4)) {
            0 -> intArrayOf(0, 3, 4, 2)
            1 -> intArrayOf(0, 5, 3, 4)
            2 -> intArrayOf(0, 4, 1, 5)
            else -> intArrayOf(0, 2, 5, 4)
        }

        val chordsHz = progressionDegrees.mapIndexed { chordIdx, deg ->
            val degOffset = scaleIntervals[deg % scaleIntervals.size]
            val thirdOffset = scaleIntervals[(deg + 2) % scaleIntervals.size] +
                if (deg + 2 >= scaleIntervals.size) 12 else 0
            val fifthOffset = scaleIntervals[(deg + 4) % scaleIntervals.size] +
                if (deg + 4 >= scaleIntervals.size) 12 else 0
            val seventhOffset = scaleIntervals[(deg + 5) % scaleIntervals.size] +
                if (deg + 5 >= scaleIntervals.size) 12 else 0
            val colorOffset = scaleIntervals[(deg + 1 + (chordIdx % 2)) % scaleIntervals.size] + 12

            doubleArrayOf(
                midiToHz(rootMidi + degOffset),
                midiToHz(rootMidi + 12 + degOffset),
                midiToHz(rootMidi + 12 + thirdOffset),
                midiToHz(rootMidi + 12 + fifthOffset),
                midiToHz(rootMidi + 12 + ((seventhOffset + colorOffset) / 2))
            )
        }

        val melodyScaleHz = DoubleArray(scaleIntervals.size + 2) { idx ->
            val octaveBoost = if (idx >= scaleIntervals.size) 12 else 0
            val interval = scaleIntervals[idx % scaleIntervals.size]
            midiToHz(rootMidi + 24 + interval + octaveBoost)
        }

        // Deterministic unique 16-step melody sequence (-1 means rest)
        val melodySteps = IntArray(16) { step ->
            val stepHash = mixHash(h2, step.toLong() * 17L + 3L)
            val density = if (architecture == SynthArchitecture.CYBER_NEON_ARP ||
                architecture == SynthArchitecture.TEMPORAL_CLOCKWORK
            ) 70 else 48
            if (positiveMod(stepHash, 100) < density) {
                positiveMod(stepHash ushr 8, melodyScaleHz.size)
            } else {
                -1
            }
        }

        val keyName = KEY_NAMES[rootSemitone]
        val slotNum = "0${slotIndex + 1}"
        val modeTag = subGenre.subtitleEnglish.substringBefore(" ")

        return StoryTrack(
            trackId = "${postId}_track_${slotIndex + 1}",
            postId = postId,
            slotIndex = slotIndex,
            architecture = architecture,
            titleSinhala = "$shortTitle • $sinhalaStyleLabel $slotNum",
            shortButtonLabel = "$slotNum • $sinhalaStyleLabel",
            subtitleSinhala = "$keyName $modeTag • ${bpm.toInt()} BPM • $englishStyleLabel",
            seed = h2,
            bpm = bpm,
            rootMidi = rootMidi,
            chordsHz = chordsHz,
            melodyScaleHz = melodyScaleHz,
            melodySteps = melodySteps
        )
    }

    private fun midiToHz(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    private fun buildShortTitle(rawTitle: String): String {
        val cleaned = rawTitle
            .substringBefore(" - ")
            .substringBefore(" | ")
            .substringBefore(" by ")
            .trim()
        if (cleaned.length <= 20) return cleaned.ifEmpty { "කතාව" }
        val words = cleaned.split(Regex("\\s+"))
        val sb = StringBuilder()
        for (word in words) {
            if (sb.isNotEmpty() && sb.length + word.length + 1 > 19) break
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(word)
        }
        return sb.toString().ifEmpty { cleaned.take(18) }
    }

    private fun stableHash64(input: String): Long {
        var h = -0x340d631b7bdddcdbL
        for (ch in input) {
            h = h xor ch.code.toLong()
            h *= 0x100000001b3L
        }
        return mixHash(h, input.length.toLong())
    }

    private fun mixHash(a: Long, b: Long): Long {
        var x = a xor (b * -0x61c8864680b583ebL)
        x = (x xor (x ushr 30)) * -0x40a7b892e31b1a47L
        x = (x xor (x ushr 27)) * -0x6b2fb644ecceee15L
        return x xor (x ushr 31)
    }

    private fun positiveMod(value: Long, mod: Int): Int {
        val r = abs(value % mod.toLong()).toInt()
        return if (r in 0 until mod) r else 0
    }
}

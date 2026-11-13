package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural audio engine synthesizing the exact unique StoryTrack composed for a story.
 * Every story has two bespoke tracks driven by its own harmonic key, BPM, chord progression,
 * melody step pattern, and theme-matched synthesis architecture.
 */
class StoryAmbientAudioEngine {

    companion object {
        private const val SAMPLE_RATE = 22050
        private const val CHUNK_SAMPLES = 2048
        private const val TWO_PI = 2.0 * PI
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playbackJob: Job? = null

    @Volatile
    private var targetVolume: Float = 0.55f

    @Volatile
    private var isStopRequested: Boolean = false

    @Volatile
    private var currentTrackId: String? = null

    fun setVolume(volume: Float) {
        targetVolume = volume.coerceIn(0f, 1f)
    }

    fun start(track: StoryTrack, volume: Float) {
        targetVolume = volume.coerceIn(0f, 1f)
        if (playbackJob?.isActive == true && currentTrackId == track.trackId && !isStopRequested) {
            return
        }
        stop()
        currentTrackId = track.trackId
        isStopRequested = false

        playbackJob = scope.launch {
            runSynthLoop(track)
        }
    }

    fun stop() {
        isStopRequested = true
        playbackJob?.cancel()
        playbackJob = null
        currentTrackId = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun runSynthLoop(track: StoryTrack) {
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(CHUNK_SAMPLES * 4)

        val audioTrack = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return

        val pcmBuffer = ShortArray(CHUNK_SAMPLES)
        val generator = TrackSynthInstance(track)

        try {
            audioTrack.play()
            var globalSampleIndex = 0L
            var masterEnvelope = 0.0

            while (scope.isActive && !isStopRequested && !Thread.currentThread().isInterrupted) {
                val vol = targetVolume.toDouble()
                for (i in 0 until CHUNK_SAMPLES) {
                    if (masterEnvelope < 1.0) {
                        masterEnvelope = (masterEnvelope + 1.0 / (SAMPLE_RATE * 1.6)).coerceAtMost(1.0)
                    }
                    val rawSample = generator.nextSample(globalSampleIndex++)
                    val scaled = (rawSample * masterEnvelope * vol * 0.30 * Short.MAX_VALUE)
                        .coerceIn(-Short.MAX_VALUE.toDouble(), Short.MAX_VALUE.toDouble())
                    pcmBuffer[i] = scaled.toInt().toShort()
                }
                val written = audioTrack.write(pcmBuffer, 0, CHUNK_SAMPLES)
                if (written < 0) break
            }
        } catch (_: Throwable) {
            // AudioTrack released cleanly on stop
        } finally {
            runCatching {
                if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.pause()
                    audioTrack.flush()
                }
                audioTrack.release()
            }
        }
    }

    private class TrackSynthInstance(private val track: StoryTrack) {
        private val random = Random(track.seed)
        private val voicePhases = DoubleArray(5)
        private val currentFreqs = track.chordsHz.first().copyOf()
        private var dronePhase = 0.0
        private var melodyPhase = 0.0
        private var melodyHarmPhase = 0.0
        private var melodyEnv = 0.0
        private var melodyFreq = track.melodyScaleHz.firstOrNull() ?: 440.0
        private var chordAttackEnv = 0.0
        private var lfoPhase = 0.0

        private var kickPhase = 0.0
        private var kickEnv = 0.0
        private var snareEnv = 0.0
        private var hatEnv = 0.0
        private var snareLowPass = 0.0
        private var hatHighPassPrev = 0.0
        private var pink0 = 0.0
        private var pink1 = 0.0
        private var pink2 = 0.0

        // 16th-note grid derived from this story track's unique BPM
        private val stepSamples = ((60.0 / track.bpm / 4.0) * SAMPLE_RATE).toLong().coerceAtLeast(900L)
        private val barSteps = 16
        private var lastStep = -1L

        // Unique subtle rhythmic variation per track seed
        private val kickSteps: Set<Int> = when (track.architecture) {
            SynthArchitecture.CYBER_NEON_ARP -> setOf(0, 4, 8, 12)
            SynthArchitecture.HELA_VEENA_PERCUSSION -> setOf(0, 6, 10, 14)
            SynthArchitecture.DARK_SUB_SUSPENSE -> setOf(0, 8)
            SynthArchitecture.MELODIC_PIANO_GROOVE -> if (abs(track.seed % 2L) == 0L) setOf(0, 7, 10) else setOf(0, 6, 11)
            else -> setOf(0, 10)
        }
        private val snareSteps: Set<Int> = when (track.architecture) {
            SynthArchitecture.CYBER_NEON_ARP -> setOf(4, 12)
            SynthArchitecture.MELODIC_PIANO_GROOVE -> setOf(4, 12)
            SynthArchitecture.HELA_VEENA_PERCUSSION -> setOf(4, 11, 14)
            else -> setOf(8)
        }

        fun nextSample(sampleIdx: Long): Double {
            val currentStepTotal = sampleIdx / stepSamples
            val stepInBar = (currentStepTotal % barSteps).toInt()
            val chordIdx = ((currentStepTotal / barSteps) % track.chordsHz.size).toInt()
            val targetChord = track.chordsHz[chordIdx]

            if (currentStepTotal != lastStep) {
                lastStep = currentStepTotal
                if (stepInBar == 0) {
                    chordAttackEnv = 1.0
                }
                if (stepInBar in kickSteps) {
                    kickEnv = when (track.architecture) {
                        SynthArchitecture.ATMOS_PLUCK_DRIFT -> 0.45
                        SynthArchitecture.TEMPORAL_CLOCKWORK -> 0.50
                        else -> 1.0
                    }
                    kickPhase = 0.0
                }
                if (stepInBar in snareSteps) {
                    snareEnv = when (track.architecture) {
                        SynthArchitecture.MELODIC_PIANO_GROOVE,
                        SynthArchitecture.CYBER_NEON_ARP,
                        SynthArchitecture.HELA_VEENA_PERCUSSION -> 1.0
                        else -> 0.32
                    }
                }
                if (stepInBar % 2 == 0) {
                    hatEnv = when (track.architecture) {
                        SynthArchitecture.TEMPORAL_CLOCKWORK -> 0.80
                        SynthArchitecture.CYBER_NEON_ARP -> 0.65
                        SynthArchitecture.MELODIC_PIANO_GROOVE -> if (stepInBar % 4 == 2) 0.68 else 0.35
                        else -> 0.22
                    }
                }
                val melIdx = track.melodySteps[stepInBar]
                if (melIdx >= 0) {
                    melodyFreq = track.melodyScaleHz[melIdx % track.melodyScaleHz.size]
                    melodyEnv = 1.0
                }
            }

            lfoPhase += TWO_PI * (if (track.architecture == SynthArchitecture.CYBER_NEON_ARP) 0.42 else 0.24) / SAMPLE_RATE
            if (lfoPhase > TWO_PI) lfoPhase -= TWO_PI
            val lfo = 0.5 + 0.5 * sin(lfoPhase)
            val tapeRatio = if (track.architecture == SynthArchitecture.MELODIC_PIANO_GROOVE) {
                1.0 + 0.0017 * sin(lfoPhase * 1.4)
            } else {
                1.0
            }

            // Smooth chord morph + envelope
            chordAttackEnv *= 0.99992
            val chordGain = when (track.architecture) {
                SynthArchitecture.MELODIC_PIANO_GROOVE -> 0.24 + 0.76 * chordAttackEnv
                SynthArchitecture.DARK_SUB_SUSPENSE -> 0.45 + 0.55 * lfo
                else -> 0.55 + 0.45 * chordAttackEnv
            }

            var padSum = 0.0
            for (v in 0 until 5) {
                val glide = if (track.architecture == SynthArchitecture.MELODIC_PIANO_GROOVE) 0.015 else 0.0005
                currentFreqs[v] += (targetChord[v] - currentFreqs[v]) * glide
                voicePhases[v] += TWO_PI * currentFreqs[v] * tapeRatio / SAMPLE_RATE
                if (voicePhases[v] > TWO_PI) voicePhases[v] -= TWO_PI

                val wave = when (track.architecture) {
                    SynthArchitecture.CYBER_NEON_ARP -> {
                        val s1 = sin(voicePhases[v])
                        val s2 = sin(voicePhases[v] * 2.0) * 0.25 + sin(voicePhases[v] * 3.0) * 0.12
                        s1 * 0.72 + s2
                    }
                    SynthArchitecture.HELA_VEENA_PERCUSSION -> {
                        sin(voicePhases[v]) * 0.78 + sin(voicePhases[v] * 2.0) * 0.22
                    }
                    else -> {
                        sin(voicePhases[v]) * 0.85 + sin(voicePhases[v] * 2.0) * 0.12
                    }
                }
                val weight = if (v == 0) 0.30 else 0.13
                padSum += wave * weight
            }
            padSum *= chordGain

            // Expressive Tanpura / Deep Sub drone for Hela Heritage & Dark Suspense
            var droneOut = 0.0
            if (track.architecture == SynthArchitecture.HELA_VEENA_PERCUSSION ||
                track.architecture == SynthArchitecture.DARK_SUB_SUSPENSE ||
                track.architecture == SynthArchitecture.ATMOS_PLUCK_DRIFT
            ) {
                val rootFreq = track.chordsHz[0][0]
                dronePhase += TWO_PI * rootFreq / SAMPLE_RATE
                if (dronePhase > TWO_PI) dronePhase -= TWO_PI
                droneOut = (sin(dronePhase) * 0.7 + sin(dronePhase * 1.5) * 0.25) * 0.15 * (0.65 + 0.35 * lfo)
            }

            // Story-unique Melody Voice
            var melOut = 0.0
            if (melodyEnv > 0.001) {
                val detune = if (track.architecture == SynthArchitecture.HELA_VEENA_PERCUSSION) 1.003 else 2.0
                melodyPhase += TWO_PI * melodyFreq * tapeRatio / SAMPLE_RATE
                melodyHarmPhase += TWO_PI * (melodyFreq * detune) * tapeRatio / SAMPLE_RATE
                if (melodyPhase > TWO_PI) melodyPhase -= TWO_PI
                if (melodyHarmPhase > TWO_PI) melodyHarmPhase -= TWO_PI

                val melDecay = when (track.architecture) {
                    SynthArchitecture.TEMPORAL_CLOCKWORK -> 0.99945
                    SynthArchitecture.CYBER_NEON_ARP -> 0.99950
                    SynthArchitecture.HELA_VEENA_PERCUSSION -> 0.99972
                    else -> 0.99976
                }
                melodyEnv *= melDecay
                val harmonicMix = when (track.architecture) {
                    SynthArchitecture.HELA_VEENA_PERCUSSION -> 0.35
                    SynthArchitecture.CYBER_NEON_ARP -> 0.30
                    SynthArchitecture.TEMPORAL_CLOCKWORK -> 0.28
                    else -> 0.18
                }
                val rawMel = sin(melodyPhase) * (1.0 - harmonicMix) + sin(melodyHarmPhase) * harmonicMix * melodyEnv
                melOut = rawMel * melodyEnv * 0.18
            }

            // Soft Percussion & Clockwork Ticks
            val white = random.nextDouble(-1.0, 1.0)
            var kickOut = 0.0
            if (kickEnv > 0.002) {
                val kickFreq = 44.0 + 72.0 * kickEnv * kickEnv
                kickPhase += TWO_PI * kickFreq / SAMPLE_RATE
                if (kickPhase > TWO_PI) kickPhase -= TWO_PI
                kickEnv *= 0.9985
                kickOut = sin(kickPhase) * kickEnv * 0.32
            }

            var snareOut = 0.0
            if (snareEnv > 0.002) {
                snareLowPass = snareLowPass * 0.78 + white * 0.22
                snareEnv *= 0.9978
                snareOut = snareLowPass * snareEnv * 0.20
            }

            var hatOut = 0.0
            if (hatEnv > 0.002) {
                val hp = white - hatHighPassPrev
                hatHighPassPrev = white
                hatEnv *= if (track.architecture == SynthArchitecture.TEMPORAL_CLOCKWORK) 0.992 else 0.994
                val hatGain = if (track.architecture == SynthArchitecture.TEMPORAL_CLOCKWORK) 0.055 else 0.038
                hatOut = hp * hatEnv * hatGain
            }

            // Atmospheric Air / Vinyl / Cosmic Rain texture
            pink0 = 0.99765 * pink0 + white * 0.0990460
            pink1 = 0.96300 * pink1 + white * 0.2965164
            pink2 = 0.57000 * pink2 + white * 1.0526913
            val textureAmt = when (track.architecture) {
                SynthArchitecture.MELODIC_PIANO_GROOVE -> 0.017
                SynthArchitecture.DARK_SUB_SUSPENSE -> 0.018 * (0.5 + 0.5 * lfo)
                else -> 0.009
            }
            val texture = (pink0 + pink1 + pink2) * textureAmt
            val vinylPop = if (track.architecture == SynthArchitecture.MELODIC_PIANO_GROOVE && random.nextInt(3400) == 0) {
                random.nextDouble(-0.06, 0.06)
            } else {
                0.0
            }

            return padSum + droneOut + melOut + kickOut + snareOut + hatOut + texture + vinylPop
        }
    }
}

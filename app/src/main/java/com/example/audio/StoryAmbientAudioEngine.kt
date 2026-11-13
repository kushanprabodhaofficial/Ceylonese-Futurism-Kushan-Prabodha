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
 * Real-time procedural ambient music synthesizer using Android AudioTrack.
 * Generates smooth, warm, click-free sci-fi and atmospheric soundscapes tailored to each story.
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
    private var currentSoundscape: StorySoundscape? = null

    fun setVolume(volume: Float) {
        targetVolume = volume.coerceIn(0f, 1f)
    }

    fun start(soundscape: StorySoundscape, volume: Float) {
        targetVolume = volume.coerceIn(0f, 1f)
        if (playbackJob?.isActive == true && currentSoundscape == soundscape && !isStopRequested) {
            return
        }
        stop()
        currentSoundscape = soundscape
        isStopRequested = false

        playbackJob = scope.launch {
            runSynthLoop(soundscape)
        }
    }

    fun stop() {
        isStopRequested = true
        playbackJob?.cancel()
        playbackJob = null
        currentSoundscape = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun runSynthLoop(soundscape: StorySoundscape) {
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
        val state = SynthState(soundscape)

        try {
            audioTrack.play()
            var globalSampleIndex = 0L
            var masterEnvelope = 0.0

            while (scope.isActive && !isStopRequested && !Thread.currentThread().isInterrupted) {
                val vol = targetVolume.toDouble()
                for (i in 0 until CHUNK_SAMPLES) {
                    // Smooth 2.5-second fade-in
                    if (masterEnvelope < 1.0) {
                        masterEnvelope = (masterEnvelope + 1.0 / (SAMPLE_RATE * 2.5)).coerceAtMost(1.0)
                    }
                    val rawSample = state.nextSample(globalSampleIndex++)
                    val scaled = (rawSample * masterEnvelope * vol * 0.28 * Short.MAX_VALUE)
                        .coerceIn(-Short.MAX_VALUE.toDouble(), Short.MAX_VALUE.toDouble())
                    pcmBuffer[i] = scaled.toInt().toShort()
                }
                val written = audioTrack.write(pcmBuffer, 0, CHUNK_SAMPLES)
                if (written < 0) break
            }
        } catch (_: Throwable) {
            // AudioTrack interrupted or stopped cleanly
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

    private class SynthState(private val soundscape: StorySoundscape) {
        private val phases = DoubleArray(6)
        private var lfoPhase = 0.0
        private var chimePhase1 = 0.0
        private var chimePhase2 = 0.0
        private var chimeEnv = 0.0
        private var chimeFreq = 440.0
        private var pinkState0 = 0.0
        private var pinkState1 = 0.0
        private var pinkState2 = 0.0
        private val random = Random(soundscape.ordinal * 7919 + 42)

        private val chords: Array<DoubleArray> = when (soundscape) {
            StorySoundscape.COSMIC_VOID -> arrayOf(
                doubleArrayOf(55.0, 110.0, 164.81, 220.0, 329.63),      // A5 / celestial open fifths
                doubleArrayOf(65.41, 130.81, 196.0, 246.94, 392.0),     // Cmaj7
                doubleArrayOf(49.0, 98.0, 146.83, 220.0, 293.66),       // Gsus2
                doubleArrayOf(43.65, 87.31, 130.81, 196.0, 261.63)      // Fmaj9
            )
            StorySoundscape.CYBERPUNK_PULSE -> arrayOf(
                doubleArrayOf(65.41, 130.81, 155.56, 196.0, 233.08),    // Cm7
                doubleArrayOf(51.91, 103.83, 155.56, 196.0, 261.63),    // Abmaj7
                doubleArrayOf(58.27, 116.54, 174.61, 207.65, 293.66),   // Bb6
                doubleArrayOf(49.0, 98.0, 146.83, 196.0, 233.08)        // Gm7
            )
            StorySoundscape.MYSTIC_CEYLON -> arrayOf(
                doubleArrayOf(73.42, 146.83, 220.0, 277.18, 349.23),    // D mystic raga drone
                doubleArrayOf(73.42, 146.83, 220.0, 293.66, 369.99),    // D major-minor shift
                doubleArrayOf(65.41, 146.83, 220.0, 261.63, 329.63),    // Suspended modal
                doubleArrayOf(73.42, 110.0, 146.83, 233.08, 293.66)     // Bhairav tint
            )
            StorySoundscape.TIME_HORIZON -> arrayOf(
                doubleArrayOf(69.30, 138.59, 207.65, 261.63, 329.63),   // C#m7
                doubleArrayOf(55.0, 110.0, 164.81, 220.0, 277.18),      // Amaj7
                doubleArrayOf(61.74, 123.47, 185.0, 246.94, 311.13),    // Badd9
                doubleArrayOf(82.41, 164.81, 246.94, 311.13, 369.99)    // Emaj9
            )
            StorySoundscape.DYSTOPIAN_TENSION -> arrayOf(
                doubleArrayOf(49.0, 98.0, 116.54, 146.83, 207.65),      // Gm(b9)
                doubleArrayOf(46.25, 92.50, 138.59, 174.61, 207.65),    // F#m7b5 mood
                doubleArrayOf(43.65, 87.31, 130.81, 155.56, 233.08),    // Fm7
                doubleArrayOf(49.0, 73.42, 110.0, 146.83, 185.0)        // Dark pedal
            )
            StorySoundscape.SERENE_CONTEMPLATION -> arrayOf(
                doubleArrayOf(65.41, 130.81, 196.0, 246.94, 329.63),    // Cmaj7
                doubleArrayOf(55.0, 110.0, 164.81, 220.0, 261.63),      // Am7
                doubleArrayOf(43.65, 87.31, 130.81, 196.0, 261.63),     // Fmaj9
                doubleArrayOf(49.0, 98.0, 146.83, 220.0, 293.66)        // G6sus4
            )
        }

        private val chimeScale: DoubleArray = when (soundscape) {
            StorySoundscape.COSMIC_VOID -> doubleArrayOf(440.0, 523.25, 659.25, 783.99, 987.77)
            StorySoundscape.CYBERPUNK_PULSE -> doubleArrayOf(261.63, 311.13, 392.0, 466.16, 523.25, 622.25)
            StorySoundscape.MYSTIC_CEYLON -> doubleArrayOf(293.66, 311.13, 369.99, 440.0, 466.16, 587.33)
            StorySoundscape.TIME_HORIZON -> doubleArrayOf(329.63, 415.30, 493.88, 659.25, 739.99)
            StorySoundscape.DYSTOPIAN_TENSION -> doubleArrayOf(196.0, 207.65, 293.66, 311.13)
            StorySoundscape.SERENE_CONTEMPLATION -> doubleArrayOf(392.0, 440.0, 523.25, 659.25, 783.99)
        }

        private val chordDurationSamples: Long = when (soundscape) {
            StorySoundscape.CYBERPUNK_PULSE -> SAMPLE_RATE * 5L
            StorySoundscape.DYSTOPIAN_TENSION -> SAMPLE_RATE * 8L
            else -> SAMPLE_RATE * 7L
        }

        private val chimeIntervalSamples: Long = when (soundscape) {
            StorySoundscape.CYBERPUNK_PULSE -> (SAMPLE_RATE * 0.45).toLong() // Gentle rhythmic arp
            StorySoundscape.MYSTIC_CEYLON -> (SAMPLE_RATE * 2.8).toLong()
            StorySoundscape.DYSTOPIAN_TENSION -> (SAMPLE_RATE * 4.5).toLong()
            else -> (SAMPLE_RATE * 3.2).toLong()
        }

        private val currentFreqs = chords[0].copyOf()

        fun nextSample(sampleIdx: Long): Double {
            val chordIdx = ((sampleIdx / chordDurationSamples) % chords.size).toInt()
            val targetChord = chords[chordIdx]

            // Slow glissando smoothing between chords so transitions never click
            for (v in 0 until 5) {
                currentFreqs[v] += (targetChord[v] - currentFreqs[v]) * 0.00025
                phases[v] += TWO_PI * currentFreqs[v] / SAMPLE_RATE
                if (phases[v] > TWO_PI) phases[v] -= TWO_PI
            }

            // Breathing LFO (0.12 Hz to 0.35 Hz)
            val lfoRate = when (soundscape) {
                StorySoundscape.CYBERPUNK_PULSE -> 0.45
                StorySoundscape.DYSTOPIAN_TENSION -> 0.14
                else -> 0.18
            }
            lfoPhase += TWO_PI * lfoRate / SAMPLE_RATE
            if (lfoPhase > TWO_PI) lfoPhase -= TWO_PI
            val lfo = 0.5 + 0.5 * sin(lfoPhase)

            // Pad voices with warm subtle 2nd-harmonic overtone
            val subBass = sin(phases[0]) * 0.34
            val voice1 = sin(phases[1]) * 0.24
            val voice2 = sin(phases[2]) * 0.20 * (0.65 + 0.35 * lfo)
            val voice3 = sin(phases[3]) * 0.16 * (1.0 - 0.3 * lfo)
            val voice4 = sin(phases[4]) * 0.11 * lfo

            var pad = subBass + voice1 + voice2 + voice3 + voice4

            // Add subtle pulse modulation for cyberpunk synthwave
            if (soundscape == StorySoundscape.CYBERPUNK_PULSE) {
                val pulsePhase = ((sampleIdx % (SAMPLE_RATE / 2)).toDouble() / (SAMPLE_RATE / 2))
                val pulseDuck = 0.72 + 0.28 * sin(pulsePhase * PI)
                pad *= pulseDuck
            }

            // Trigger melodic chime or soft arpeggio note periodically
            if (sampleIdx % chimeIntervalSamples == 0L) {
                val noteIdx = if (soundscape == StorySoundscape.CYBERPUNK_PULSE) {
                    ((sampleIdx / chimeIntervalSamples) % chimeScale.size).toInt()
                } else {
                    random.nextInt(chimeScale.size)
                }
                chimeFreq = chimeScale[noteIdx]
                chimeEnv = 1.0
            }

            var chimeOut = 0.0
            if (chimeEnv > 0.0005) {
                chimePhase1 += TWO_PI * chimeFreq / SAMPLE_RATE
                chimePhase2 += TWO_PI * (chimeFreq * 2.0) / SAMPLE_RATE
                if (chimePhase1 > TWO_PI) chimePhase1 -= TWO_PI
                if (chimePhase2 > TWO_PI) chimePhase2 -= TWO_PI

                val decay = when (soundscape) {
                    StorySoundscape.CYBERPUNK_PULSE -> 0.99945
                    StorySoundscape.MYSTIC_CEYLON -> 0.99982
                    else -> 0.99985
                }
                chimeEnv *= decay
                val tone = sin(chimePhase1) * 0.75 + sin(chimePhase2) * 0.25 * chimeEnv
                val chimeGain = if (soundscape == StorySoundscape.CYBERPUNK_PULSE) 0.14 else 0.16
                chimeOut = tone * chimeEnv * chimeGain
            }

            // Very soft filtered wind / space air noise
            val white = random.nextDouble(-1.0, 1.0)
            pinkState0 = 0.99765 * pinkState0 + white * 0.0990460
            pinkState1 = 0.96300 * pinkState1 + white * 0.2965164
            pinkState2 = 0.57000 * pinkState2 + white * 1.0526913
            val pink = (pinkState0 + pinkState1 + pinkState2) * 0.015
            val airLevel = when (soundscape) {
                StorySoundscape.COSMIC_VOID -> 0.45 * (0.4 + 0.6 * lfo)
                StorySoundscape.DYSTOPIAN_TENSION -> 0.50 * (0.5 + 0.5 * abs(sin(lfoPhase * 0.5)))
                else -> 0.22 * lfo
            }

            return (pad * 0.58) + chimeOut + (pink * airLevel)
        }
    }
}

package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

object LiquidSoundSynth {
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startPlaying(soundName: String) {
        stopPlaying()
        synthJob = scope.launch {
            try {
                when (soundName) {
                    "Dewdrop Serenade" -> playDewdropSerenade()
                    "Bubble Resonance" -> playBubbleResonance()
                    "Ocean Breeze" -> playOceanBreeze()
                    "Liquid Echo" -> playLiquidEcho()
                    "Rainfall Melody" -> playRainfallMelody()
                    else -> playDewdropSerenade() // fallback
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopPlaying() {
        synthJob?.cancel()
        synthJob = null
    }

    private suspend fun playToneSequence(tones: List<ToneDesc>, loopDelayMs: Long = 1000L) {
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            audioTrack.play()
            while (currentCoroutineContext().isActive) {
                for (tone in tones) {
                    if (!currentCoroutineContext().isActive) break
                    val numSamples = (sampleRate * tone.durationMs / 1000f).toInt()
                    val buffer = ShortArray(numSamples)
                    
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        
                        // Handle sliding frequency if start and end are different
                        val currentFreq = if (tone.endFreq != tone.startFreq) {
                            val ratio = i.toDouble() / numSamples
                            tone.startFreq + (tone.endFreq - tone.startFreq) * ratio
                        } else {
                            tone.startFreq
                        }
                        
                        var value = sin(2.0 * Math.PI * currentFreq * t)
                        
                        // Multi-harmonic rich liquid note
                        if (tone.useHarmonics) {
                            value = 0.6 * value + 
                                    0.25 * sin(2.0 * Math.PI * currentFreq * 2.0 * t) +
                                    0.15 * sin(2.0 * Math.PI * currentFreq * 3.0 * t)
                        }

                        // Apply envelope
                        val envelope = applyEnvelope(i, numSamples, tone.envelopeType)
                        val sampleValue = (value * envelope * Short.MAX_VALUE * tone.volume).toInt()
                        buffer[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    
                    audioTrack.write(buffer, 0, buffer.size)
                    delay(tone.postDelayMs)
                }
                delay(loopDelayMs)
            }
        } finally {
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun applyEnvelope(index: Int, totalSamples: Int, type: EnvelopeType): Double {
        val fraction = index.toDouble() / totalSamples
        return when (type) {
            EnvelopeType.DECAY -> {
                // Linear decay (bell/dewdrop)
                (1.0 - fraction).coerceIn(0.0, 1.0)
            }
            EnvelopeType.BUBBLE -> {
                // Quick rise, quick decay
                if (fraction < 0.15) {
                    fraction / 0.15
                } else {
                    ((1.0 - fraction) / 0.85).coerceIn(0.0, 1.0)
                }
            }
            EnvelopeType.SWELL -> {
                // Slower attack, slow decay
                sin(fraction * Math.PI)
            }
            EnvelopeType.RAIN -> {
                // Rapid noise-like crackling / short pulses
                val noise = 0.7 + 0.3 * Math.random()
                noise * (1.0 - fraction)
            }
        }
    }

    private suspend fun playDewdropSerenade() {
        // High-pitched pure resonant pentatonic notes with long decays
        val tones = listOf(
            ToneDesc(startFreq = 523.25, durationMs = 350, postDelayMs = 150, envelopeType = EnvelopeType.DECAY), // C5
            ToneDesc(startFreq = 587.33, durationMs = 250, postDelayMs = 100, envelopeType = EnvelopeType.DECAY), // D5
            ToneDesc(startFreq = 659.25, durationMs = 400, postDelayMs = 200, envelopeType = EnvelopeType.DECAY), // E5
            ToneDesc(startFreq = 783.99, durationMs = 500, postDelayMs = 300, envelopeType = EnvelopeType.DECAY)  // G5
        )
        playToneSequence(tones, loopDelayMs = 800)
    }

    private suspend fun playBubbleResonance() {
        // Upward sliding sweeps simulating bubble pops
        val tones = listOf(
            ToneDesc(startFreq = 220.0, endFreq = 440.0, durationMs = 120, postDelayMs = 80, envelopeType = EnvelopeType.BUBBLE, useHarmonics = true, volume = 0.7f),
            ToneDesc(startFreq = 293.66, endFreq = 587.33, durationMs = 100, postDelayMs = 60, envelopeType = EnvelopeType.BUBBLE, useHarmonics = true, volume = 0.6f),
            ToneDesc(startFreq = 329.63, endFreq = 659.25, durationMs = 140, postDelayMs = 120, envelopeType = EnvelopeType.BUBBLE, useHarmonics = true, volume = 0.8f)
        )
        playToneSequence(tones, loopDelayMs = 600)
    }

    private suspend fun playOceanBreeze() {
        // Gentle swell of low-to-mid warm frequencies with softer harmonics (soothing waves)
        val tones = listOf(
            ToneDesc(startFreq = 110.0, durationMs = 1200, postDelayMs = 100, envelopeType = EnvelopeType.SWELL, useHarmonics = true, volume = 0.5f),
            ToneDesc(startFreq = 164.81, durationMs = 1400, postDelayMs = 200, envelopeType = EnvelopeType.SWELL, useHarmonics = true, volume = 0.4f)
        )
        playToneSequence(tones, loopDelayMs = 400)
    }

    private suspend fun playLiquidEcho() {
        // Bell note followed by increasingly soft delays
        val tones = listOf(
            ToneDesc(startFreq = 440.0, durationMs = 400, postDelayMs = 250, envelopeType = EnvelopeType.DECAY), // Root
            ToneDesc(startFreq = 440.0, durationMs = 300, postDelayMs = 200, envelopeType = EnvelopeType.DECAY, volume = 0.5f), // Echo 1
            ToneDesc(startFreq = 440.0, durationMs = 250, postDelayMs = 150, envelopeType = EnvelopeType.DECAY, volume = 0.25f) // Echo 2
        )
        playToneSequence(tones, loopDelayMs = 1000)
    }

    private suspend fun playRainfallMelody() {
        // High rapid rain-patter ambient sequence
        val tones = listOf(
            ToneDesc(startFreq = 880.0, durationMs = 80, postDelayMs = 40, envelopeType = EnvelopeType.RAIN, volume = 0.3f),
            ToneDesc(startFreq = 987.77, durationMs = 60, postDelayMs = 30, envelopeType = EnvelopeType.RAIN, volume = 0.25f),
            ToneDesc(startFreq = 1046.50, durationMs = 90, postDelayMs = 50, envelopeType = EnvelopeType.RAIN, volume = 0.35f),
            ToneDesc(startFreq = 1174.66, durationMs = 70, postDelayMs = 40, envelopeType = EnvelopeType.RAIN, volume = 0.2f)
        )
        playToneSequence(tones, loopDelayMs = 500)
    }

    enum class EnvelopeType {
        DECAY, BUBBLE, SWELL, RAIN
    }

    data class ToneDesc(
        val startFreq: Double,
        val endFreq: Double = startFreq,
        val durationMs: Int,
        val postDelayMs: Long,
        val envelopeType: EnvelopeType,
        val useHarmonics: Boolean = false,
        val volume: Float = 0.8f
    )
}

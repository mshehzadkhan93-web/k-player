package com.captain.kplayer.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer

/**
 * Real 5-band equalizer built on Android's audio effects framework, attached
 * to ExoPlayer's audio session. The five target frequencies (60Hz, 230Hz,
 * 910Hz, 3.6kHz, 14kHz) are mapped to the nearest bands the device actually
 * offers. Everything is guarded: devices without audio effects report
 * [supported] = false instead of crashing.
 */
class EqualizerManager(audioSessionId: Int) {

    val supported: Boolean

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    /** Current audio boost multiplier (1.0 = 100%, 2.0 = 200%). */
    var audioBoost: Float = 1.0f
        private set

    /** Target frequencies in Hz shown in the UI. */
    val targetFrequencies = listOf(60, 230, 910, 3600, 14000)

    /** Device band index for each target frequency, or -1 when unavailable. */
    val bandIndices: List<Int>

    /** Band levels in dB, in the range -15..15. */
    var bandLevels: List<Float> = listOf(0f, 0f, 0f, 0f, 0f)
        private set

    /** Bass boost strength 0..100 (shown as a knob in the UI). */
    var bassBoostStrength: Int = 0
        private set

    var enabled: Boolean = false
        set(value) {
            field = value
            try {
                equalizer?.enabled = value
                bassBoost?.enabled = value && bassBoostStrength > 0
            } catch (_: Exception) {
                // Effects may be unavailable on some devices; ignore.
            }
        }

    init {
        var ok = false
        var indices: List<Int> = targetFrequencies.map { -1 }
        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            val bandCount = eq.numberOfBands.toInt()
            if (bandCount > 0) {
                indices = targetFrequencies.map { target ->
                    var best = 0
                    var bestDiff = Int.MAX_VALUE
                    for (band in 0 until bandCount) {
                        val centerHz = eq.getCenterFreq(band.toShort()) / 1000
                        val diff = kotlin.math.abs(centerHz - target)
                        if (diff < bestDiff) {
                            bestDiff = diff
                            best = band
                        }
                    }
                    best
                }
                ok = true
            }
            val bb = BassBoost(0, audioSessionId)
            bassBoost = bb
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            } catch (_: Exception) {
                loudnessEnhancer = null
            }
        } catch (_: Exception) {
            equalizer = null
            bassBoost = null
            loudnessEnhancer = null
            ok = false
        }
        bandIndices = indices
        supported = ok
    }

    /** Sets one band by target frequency, level in dB (-15..15). */
    fun setBandLevel(freqHz: Int, levelDb: Float) {
        val eq = equalizer ?: return
        val pos = targetFrequencies.indexOf(freqHz)
        if (pos < 0) return
        val band = bandIndices.getOrNull(pos) ?: return
        if (band < 0) return
        val clamped = levelDb.coerceIn(-15f, 15f)
        bandLevels = bandLevels.toMutableList().also { it[pos] = clamped }
        try {
            val range = eq.bandLevelRange
            val milliBel = (clamped * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt())
            eq.setBandLevel(band.toShort(), milliBel.toShort())
        } catch (_: Exception) {
        }
    }

    /** Applies a full preset: five dB levels for the five target frequencies. */
    fun applyPreset(levels: List<Float>) {
        levels.forEachIndexed { index, level ->
            setBandLevel(targetFrequencies[index], level)
        }
    }

    /** Bass boost strength 0..100. */
    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 100)
        bassBoostStrength = clamped
        try {
            bassBoost?.setStrength((clamped * 10).toShort())
            bassBoost?.enabled = enabled && clamped > 0
        } catch (_: Exception) {
        }
    }

    /**
     * Audio boost, 1.0 (100%) to 2.0 (200%), via LoudnessEnhancer target gain
     * (0..1000 millibels). Pass 1.0 to turn the boost off. While audio
     * passthrough is enabled the caller must not apply any boost, so the
     * raw bitstream reaches the receiver untouched.
     */
    fun setAudioBoost(boost: Float) {
        val clamped = boost.coerceIn(1.0f, 2.0f)
        audioBoost = clamped
        val gainMb = ((clamped - 1f) * 1000).toInt().coerceIn(0, 1000)
        try {
            loudnessEnhancer?.let {
                it.setTargetGain(gainMb)
                it.enabled = gainMb > 0
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        try {
            bassBoost?.release()
        } catch (_: Exception) {
        }
        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        bassBoost = null
        loudnessEnhancer = null
    }

    companion object {
        /** Preset name to five band levels in dB. */
        val PRESETS: Map<String, List<Float>> = mapOf(
            "Normal" to listOf(0f, 0f, 0f, 0f, 0f),
            "Bass Boost" to listOf(9f, 6f, 2f, 0f, -2f),
            "Vocal" to listOf(-3f, 0f, 4f, 7f, 3f),
            "Rock" to listOf(7f, 4f, -2f, 3f, 6f),
            "Pop" to listOf(3f, 5f, 6f, 3f, 1f),
            "Classical" to listOf(2f, 1f, 0f, 3f, 6f)
        )
    }
}

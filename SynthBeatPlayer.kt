package com.wakaunduvibez.studio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Small offline drum synthesizer. Generates a one-bar mono PCM loop (kick, snare, hats)
 * and loops it with AudioTrack. This is intentionally a first-step beat engine, not a full DAW.
 */
class SynthBeatPlayer {
    private var track: AudioTrack? = null

    @Synchronized
    fun play(bpm: Int) {
        stop()
        val sampleRate = 44100
        val safeBpm = bpm.coerceIn(70, 150)
        val barSeconds = 4.0 * 60.0 / safeBpm
        val frameCount = (sampleRate * barSeconds).toInt().coerceAtLeast(sampleRate)
        val mix = FloatArray(frameCount)
        val stepFrames = frameCount / 16

        for (step in 0 until 16) {
            val start = step * stepFrames
            // Punchy four-on-the-floor kick with an extra syncopated kick.
            if (step == 0 || step == 6 || step == 8 || step == 14) {
                addKick(mix, start, sampleRate, if (step == 0 || step == 8) 0.9f else 0.68f)
            }
            // Backbeat snare/clap-like noise.
            if (step == 4 || step == 12) addSnare(mix, start, sampleRate, 0.55f)
            // Eighth-note hats with a few 16th-note pickups.
            if (step % 2 == 0) addHat(mix, start, sampleRate, if (step % 4 == 0) 0.23f else 0.16f)
            if (step == 7 || step == 15) addHat(mix, start + stepFrames / 2, sampleRate, 0.12f)
        }

        val pcm = ShortArray(frameCount) { i ->
            val normalized = (mix[i] * 0.82f).coerceIn(-0.98f, 0.98f)
            (normalized * Short.MAX_VALUE).toInt().toShort()
        }
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuffer, pcm.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        val written = audioTrack.write(pcm, 0, pcm.size, AudioTrack.WRITE_BLOCKING)
        if (written <= 0) {
            audioTrack.release()
            throw IllegalStateException("Could not load drum loop audio")
        }
        val loopResult = audioTrack.setLoopPoints(0, pcm.size, -1)
        if (loopResult != AudioTrack.SUCCESS) {
            audioTrack.release()
            throw IllegalStateException("Could not loop drum audio")
        }
        track = audioTrack
        audioTrack.play()
    }

    @Synchronized
    fun stop() {
        val old = track
        track = null
        if (old != null) {
            try { if (old.playState == AudioTrack.PLAYSTATE_PLAYING) old.stop() } catch (_: Exception) {}
            try { old.flush() } catch (_: Exception) {}
            try { old.release() } catch (_: Exception) {}
        }
    }

    private fun addKick(out: FloatArray, start: Int, sr: Int, gain: Float) {
        val length = (sr * 0.22).toInt()
        for (i in 0 until length) {
            val p = start + i
            if (p >= out.size) break
            val t = i.toDouble() / sr
            val frequency = 42.0 + 105.0 * exp(-t * 24.0)
            val phase = 2.0 * PI * frequency * t
            val body = sin(phase) * exp(-t * 17.0)
            val click = sin(2.0 * PI * 1800.0 * t) * exp(-t * 95.0) * 0.10
            out[p] += ((body + click) * gain).toFloat()
        }
    }

    private fun addSnare(out: FloatArray, start: Int, sr: Int, gain: Float) {
        val length = (sr * 0.16).toInt()
        var previous = 0.0
        val random = Random(start + 41)
        for (i in 0 until length) {
            val p = start + i
            if (p >= out.size) break
            val t = i.toDouble() / sr
            val noise = random.nextDouble(-1.0, 1.0)
            val highPassed = noise - previous * 0.75
            previous = noise
            val tone = sin(2.0 * PI * 185.0 * t) * exp(-t * 28.0) * 0.35
            out[p] += ((highPassed * exp(-t * 22.0) * 0.48 + tone) * gain).toFloat()
        }
    }

    private fun addHat(out: FloatArray, start: Int, sr: Int, gain: Float) {
        val length = (sr * 0.045).toInt()
        val random = Random(start + 733)
        var previous = 0.0
        for (i in 0 until length) {
            val p = start + i
            if (p >= out.size) break
            val noise = random.nextDouble(-1.0, 1.0)
            val high = noise - previous
            previous = noise
            val t = i.toDouble() / sr
            out[p] += (high * exp(-t * 90.0) * gain).toFloat()
        }
    }
}

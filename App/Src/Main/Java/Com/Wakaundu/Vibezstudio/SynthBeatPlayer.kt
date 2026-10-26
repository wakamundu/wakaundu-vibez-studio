
package com.wakaunduvibez.studio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

class SynthBeatPlayer {
    @Volatile
    private var playing = false

    private var audioTrack: AudioTrack? = null
    private var worker: Thread? = null

    fun play(bpm: Int) {
        stop()
        playing = true

        worker = thread(start = true, name = "GengetoneBeat") {
            val sampleRate = 44100
            val beatDuration = 60000L / bpm.coerceIn(70, 150)
            val samplesPerBeat = (sampleRate * beatDuration / 1000).toInt()
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )

            audioTrack = track
            val buffer = ShortArray(samplesPerBeat)

            try {
                track.play()
                var beat = 0

                while (playing) {
                    for (i in buffer.indices) {
                        val t = i.toDouble() / sampleRate
                        val kick = if (beat % 4 == 0 || beat % 4 == 2) {
                            sin(2.0 * PI * (65.0 - 35.0 * t) * t) *
                                kotlin.math.exp(-t * 18.0)
                        } else 0.0

                        val snare = if (beat % 4 == 1 || beat % 4 == 3) {
                            val noise = kotlin.random.Random.nextDouble(-1.0, 1.0)
                            noise * kotlin.math.exp(-t * 32.0) * 0.35
                        } else 0.0

                        val hat = if (i % (sampleRate / 4) < 200) {
                            kotlin.random.Random.nextDouble(-1.0, 1.0) *
                                kotlin.math.exp(-(i % (sampleRate / 4)) / 45.0) * 0.12
                        } else 0.0

                        val value = ((kick + snare + hat) * 10000)
                            .coerceIn(-30000.0, 30000.0)
                        buffer[i] = value.toInt().toShort()
                    }

                    var offset = 0
                    while (offset < buffer.size && playing) {
                        offset += track.write(
                            buffer, offset, buffer.size - offset
                        ).coerceAtLeast(0)
                    }
                    beat++
                }
            } catch (_: Exception) {
                // Stop safely if audio playback is interrupted.
            } finally {
                try {
                    track.stop()
                } catch (_: Exception) {
                }
                track.release()
                audioTrack = null
            }
        }
    }

    fun stop() {
        playing = false
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (_: Exception) {
        }
    }
}

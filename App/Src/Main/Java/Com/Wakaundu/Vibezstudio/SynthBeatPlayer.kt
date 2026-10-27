
package com.wakaunduvibez.studio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SynthBeatPlayer {
    @Volatile
    private var playing = false

    @Volatile
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var worker: Thread? = null

    @Synchronized
    fun play(bpm: Int) {
        stop()

        playing = true
        val tempo = bpm.coerceIn(70, 150)

        worker = thread(start = true, name = "GengetoneBeat") {
            val sampleRate = 44100
            val samplesPerBeat = (sampleRate * 60.0 / tempo).toInt()
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (bufferSize <= 0) {
                playing = false
                return@thread
            }

            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(maxOf(bufferSize, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track

            try {
                val buffer = ShortArray(samplesPerBeat)
                track.play()
                var beat = 0

                while (playing && !Thread.currentThread().isInterrupted) {
                    for (i in buffer.indices) {
                        val t = i.toDouble() / sampleRate

                        val kick = if (beat % 4 == 0 || beat % 4 == 2) {
                            sin(2.0 * PI * (65.0 - 35.0 * t) * t) *
                                exp(-t * 18.0)
                        } else 0.0

                        val snare = if (beat % 4 == 1 || beat % 4 == 3) {
                            Random.nextDouble(-1.0, 1.0) *
                                exp(-t * 32.0) * 0.35
                        } else 0.0

                        val hatPosition = i % (sampleRate / 4)
                        val hat = if (hatPosition < 200) {
                            Random.nextDouble(-1.0, 1.0) *
                                exp(-hatPosition / 45.0) * 0.12
                        } else 0.0

                        val value = ((kick + snare + hat) * 10000)
                            .coerceIn(-30000.0, 30000.0)

                        buffer[i] = value.toInt().toShort()
                    }

                    var offset = 0
                    while (offset < buffer.size && playing) {
                        val written = track.write(
                            buffer, offset, buffer.size - offset
                        )
                        if (written <= 0) break
                        offset += written
                    }

                    beat++
                }
            } catch (_: Exception) {
                // Audio may be interrupted by the system.
            } finally {
                try {
                    track.pause()
                    track.flush()
                    track.stop()
                } catch (_: Exception) {
                }

                track.release()

                if (audioTrack === track) {
                    audioTrack = null
                }
                playing = false
            }
        }
    }

    @Synchronized
    fun stop() {
        playing = false

        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (_: Exception) {
        }
    }
}

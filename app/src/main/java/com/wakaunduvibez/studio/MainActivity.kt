
package com.wakaunduvibez.studio

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var recorder: MediaRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(18, 18, 30))
        }

        val title = TextView(this).apply {
            text = "WakaûndūVibez Studio"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Create your vibe. Build your sound."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        val recordButton = Button(this).apply {
            text = "Start Recording"
            setOnClickListener {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        100
                    )
                } else {
                    startRecording()
                    text = "Recording..."
                }
            }
        }

        val stopButton = Button(this).apply {
            text = "Stop Recording"
            setOnClickListener {
                stopRecording()
                recordButton.text = "Start Recording"
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(recordButton)
        layout.addView(stopButton)

        setContentView(layout)
    }

    private fun startRecording() {
        try {
            val file = java.io.File(
                getExternalFilesDir(null),
                "wakaundu-vocal.m4a"
            )

            recorder = if (android.os.Build.VERSION.SDK_INT >= 31) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            stopRecording()
        }
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        recorder?.release()
        recorder = null
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}

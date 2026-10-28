
package com.wakaunduvibez.studio

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private var startTime = 0L
    private var outputFile: File? = null

    private lateinit var timerText: TextView
    private lateinit var statusText: TextView
    private lateinit var recordButton: Button
    private lateinit var stopButton: Button

    private val handler = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val seconds = (System.currentTimeMillis() - startTime) / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60

                timerText.text = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    minutes,
                    remainingSeconds
                )

                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(18, 18, 30))
        }

        val title = TextView(this).apply {
            text = "WakaûndūVibez Studio"
            textSize = 27f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Create your vibe. Build your sound."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "READY TO RECORD"
            textSize = 18f
            setTextColor(Color.GREEN)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 8)
        }

        timerText = TextView(this).apply {
            text = "00:00"
            textSize = 44f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 24)
        }

        recordButton = Button(this).apply {
            text = "START RECORDING"
            setOnClickListener {
                checkPermissionAndRecord()
            }
        }

        stopButton = Button(this).apply {
            text = "STOP RECORDING"
            isEnabled = false
            setOnClickListener {
                stopRecording()
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(statusText)
        layout.addView(timerText)
        layout.addView(recordButton)
        layout.addView(stopButton)

        setContentView(layout)
    }

    private fun checkPermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        } else {
            startRecording()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        } else if (requestCode == 100) {
            statusText.text = "MICROPHONE PERMISSION NEEDED"
        }
    }

    private fun startRecording() {
        if (isRecording) return

        val file = File(
            getExternalFilesDir(null),
            "wakaundu-vocal.m4a"
        )

        var newRecorder: MediaRecorder? = null

        try {
            newRecorder = if (Build.VERSION.SDK_INT >= 31) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = newRecorder
            outputFile = file
            isRecording = true
            startTime = System.currentTimeMillis()

            statusText.text = "● RECORDING"
            statusText.setTextColor(Color.RED)
            recordButton.isEnabled = false
            stopButton.isEnabled = true

            handler.removeCallbacks(timerRunnable)
            handler.post(timerRunnable)

        } catch (e: Exception) {
            try {
                newRecorder?.release()
            } catch (_: Exception) {
            }

            recorder = null
            isRecording = false
            statusText.text = "RECORDING FAILED"
            statusText.setTextColor(Color.RED)
            recordButton.isEnabled = true
            stopButton.isEnabled = false
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        handler.removeCallbacks(timerRunnable)

        var saved = false

        try {
            recorder?.stop()
            saved = true
        } catch (_: Exception) {
            // Very short recordings may not be valid.
        }

        try {
            recorder?.release()
        } catch (_: Exception) {
        }

        recorder = null
        recordButton.isEnabled = true
        stopButton.isEnabled = false
        timerText.text = "00:00"

        statusText.text = if (saved) {
            "RECORDING SAVED"
        } else {
            "RECORDING TOO SHORT — TRY AGAIN"
        }

        statusText.setTextColor(
            if (saved) Color.GREEN else Color.RED
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(timerRunnable)

        if (isRecording) {
            isRecording = false
            try {
                recorder?.stop()
            } catch (_: Exception) {
            }
            try {
                recorder?.release()
            } catch (_: Exception) {
            }
            recorder = null
        }

        super.onDestroy()
    }
}

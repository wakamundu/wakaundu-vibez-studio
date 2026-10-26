package com.wakaunduvibez.studio

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF0B0B12)
private val Panel = Color(0xFF191927)
private val Violet = Color(0xFF9B5CFF)
private val Mint = Color(0xFF54F0C0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudioApp() }
    }
}

@Composable
fun StudioApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var beatPlaying by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready to make music") }
    var latestRecording by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var bpm by remember { mutableFloatStateOf(100f) }
    val beatPlayer = remember { SynthBeatPlayer() }
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        status = if (granted) "Microphone ready" else "Microphone permission is required to record"
    }

    DisposableEffect(Unit) {
        onDispose {
            try { recorder?.release() } catch (_: Exception) {}
            try { player?.release() } catch (_: Exception) {}
            beatPlayer.stop()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Violet,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("WAKAÛNDŪVIBEZ", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("STUDIO", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("Your phone. Your voice. Your sound.", color = Color.LightGray, fontSize = 13.sp)
                    }
                    Box(Modifier.size(54.dp).background(Violet, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Text("WV", fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("GENGETONE BEAT LAB", color = Mint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Built-in drum loop", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("A synthesized 4/4 loop with kick, snare and hi-hat. Adjust tempo, then play it while writing your vocals.", color = Color.LightGray, fontSize = 13.sp)
                        Text("${bpm.toInt()} BPM", color = Violet, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Slider(value = bpm, onValueChange = { bpm = it }, valueRange = 70f..150f, enabled = !beatPlaying)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    try {
                                        if (beatPlaying) {
                                            beatPlayer.stop()
                                            beatPlaying = false
                                            status = "Beat stopped"
                                        } else {
                                            beatPlayer.play(bpm.toInt())
                                            beatPlaying = true
                                            status = "Gengetone drum loop playing at ${bpm.toInt()} BPM"
                                        }
                                    } catch (e: Exception) {
                                        beatPlaying = false
                                        status = "Audio playback failed: ${e.localizedMessage ?: "try again"}"
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (beatPlaying) Color(0xFFFF4F73) else Violet)
                            ) { Text(if (beatPlaying) "■  Stop beat" else "▶  Play beat") }
                        }
                        Text("Tip: use headphones when recording vocals over the beat.", color = Mint, fontSize = 12.sp)
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("VOCAL BOOTH", color = Mint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(if (recording) "Recording your take…" else "Capture a fresh take", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(status, color = Color.LightGray, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    if (!permissionGranted) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else if (!recording) {
                                        try {
                                            val dir = File(context.filesDir, "recordings").apply { mkdirs() }
                                            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                            val output = File(dir, "vocal_$stamp.m4a")
                                            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
                                            r.setAudioSource(MediaRecorder.AudioSource.MIC)
                                            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                            r.setAudioEncodingBitRate(192000)
                                            r.setAudioSamplingRate(44100)
                                            r.setOutputFile(output.absolutePath)
                                            r.prepare()
                                            r.start()
                                            recorder = r
                                            latestRecording = output
                                            recording = true
                                            status = "Recording to app storage"
                                        } catch (e: Exception) {
                                            status = "Could not start recording: ${e.localizedMessage ?: "check microphone access"}"
                                            try { recorder?.release() } catch (_: Exception) {}
                                            recorder = null
                                        }
                                    } else {
                                        try {
                                            recorder?.stop()
                                            recorder?.release()
                                            recorder = null
                                            recording = false
                                            status = "Take saved • ${latestRecording?.name ?: "recording"}"
                                        } catch (e: Exception) {
                                            status = "Recording was too short. Try again."
                                            try { recorder?.release() } catch (_: Exception) {}
                                            recorder = null
                                            recording = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (recording) Color(0xFFFF4F73) else Violet),
                                modifier = Modifier.weight(1f)
                            ) { Text(if (recording) "■  Stop take" else "●  Record vocal") }
                            OutlinedButton(
                                enabled = latestRecording != null && !recording,
                                onClick = {
                                    try {
                                        player?.release()
                                        player = MediaPlayer().apply {
                                            setDataSource(latestRecording!!.absolutePath)
                                            prepare()
                                            start()
                                            setOnCompletionListener { it.release(); player = null; status = "Playback finished" }
                                        }
                                        status = "Playing latest vocal take"
                                    } catch (e: Exception) { status = "Could not play this take" }
                                }
                            ) { Text("▶ Play") }
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("MIX & MASTER", color = Mint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Polish your sound", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Next stage: synchronized multitrack playback, vocal EQ, compression, reverb, delay, limiter and WAV/M4A export.", color = Color.LightGray, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("EQ", "Reverb", "Compressor").forEach {
                                Surface(color = Color(0xFF29263A), shape = RoundedCornerShape(50)) {
                                    Text(it, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Text("VERSION 0.2 • SYNTH DRUM LOOP + VOCAL RECORDING", color = Color.Gray, fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }
    }
}

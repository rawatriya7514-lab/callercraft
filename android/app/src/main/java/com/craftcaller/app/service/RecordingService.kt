package com.craftcaller.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.craftcaller.app.MainActivity
import com.craftcaller.app.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Call recording via mic capture (foreground mic-type FGS).
 * NOTE: on many devices OS policy mutes the *other* party's audio in the mic
 * stream; this records the session mic reliably (like speakerphone capture).
 */
class RecordingService : Service() {

    companion object {
        const val ACTION_START = "com.craftcaller.app.action.RECORD_START"
        const val ACTION_STOP = "com.craftcaller.app.action.RECORD_STOP"

        const val CHANNEL_ID = "recording_channel"
        const val FOREGROUND_ID = 77

        @Volatile
        var isRecording: Boolean = false
            private set

        /** Last produced recording file (absolute path) for UI display. */
        @Volatile
        var lastRecordingPath: String? = null
            private set
    }

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRecording()
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground()
        startRecording()
        return START_STICKY
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Call recording",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, RecordingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val openApp = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Recording call")
            .setContentText("Tap to open CraftCaller")
            .setContentIntent(openApp)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(FOREGROUND_ID, notification)
        }
    }

    private fun startRecording() {
        if (isRecording) return
        try {
            val dir = File(
                getExternalFilesDir(Environment.DIRECTORY_RECORDINGS) ?: filesDir,
                "craftcaller_recordings"
            ).apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "call_$stamp.m4a")
            val mr: MediaRecorder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this)
                else @Suppress("DEPRECATION") MediaRecorder()

            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioSamplingRate(44100)
            mr.setAudioEncodingBitRate(128000)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            outputFile = file
            isRecording = true
        } catch (_: Exception) {
            isRecording = false
            stopSelf()
        }
    }

    private fun stopRecording() {
        runCatching {
            recorder?.stop()
            lastRecordingPath = outputFile?.absolutePath
        }
        runCatching { recorder?.release() }
        recorder = null
        isRecording = false
    }

    override fun onDestroy() {
        if (isRecording) stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

package com.craftcaller.app.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.craftcaller.app.util.Perms
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Call recording (microphone-capture based, the approach available to 3rd-party
 * apps since Android 10). Started/stopped from the in-call card or the home
 * screen; the file is written into app-private storage where the gallery can
 * open it through the FileProvider declared in the manifest.
 */
object CallRecorder {
    private val ref = AtomicReference<MediaRecorder?>(null)

    @Volatile var lastFile: File? = null
        private set

    fun canRecord(ctx: Context): Boolean = Perms.allGranted(ctx, listOf(Manifest.permission.RECORD_AUDIO))

    fun start(ctx: Context): String? {
        if (!canRecord(ctx)) return null
        if (ref.get() != null) return "already running"
        val dir = File(ctx.getExternalFilesDir(null), "recordings").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val out = File(dir, "craftcaller_$stamp.m4a")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioEncodingBitRate(128000)
            rec.setAudioSamplingRate(44100)
            rec.setOutputFile(out.absolutePath)
            rec.prepare()
            rec.start()
            ref.set(rec)
            lastFile = out
            return null
        } catch (ex: Throwable) {
            try { rec.stop() } catch (_: Throwable) {}
            try { rec.release() } catch (_: Throwable) {}
            ref.set(null)
            return ex.message ?: "start failed"
        }
    }

    fun stop(save: Boolean = true): String? {
        val rec = ref.get() ?: return null
        val f = lastFile
        try {
            rec.stop()
            rec.release()
        } catch (ex: Throwable) {
            try { rec.release() } catch (_: Throwable) {}
            ref.set(null)
            return ex.message
        }
        ref.set(null)
        return if (save) "saved: ${f?.name.orEmpty()}" else null
    }

    /** Service calls this at the end of every call. */
    fun abortIfRunning() {
        if (ref.get() != null) stop(false)
    }

    fun isRunning() = ref.get() != null

    @SuppressLint("SdCardPath")
    fun recordings(ctx: Context): List<File> {
        val dir = File(ctx.getExternalFilesDir(null), "recordings")
        return dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}

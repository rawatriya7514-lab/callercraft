package com.craftcaller.app.util

import android.content.Context
import android.os.Process
import android.util.Log

/**
 * On-device crash debugging aid: catches uncaught exceptions and persists the
 * flattened stack trace into `craftcaller_prefs` so the NEXT launch displays
 * it inside the app itself — no adb/logcat required (screenshot or Copy).
 */
object CrashReporter {

    private const val KEY_LAST_CRASH = "last_crash"

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { persist(context, thread.name, throwable) }
            // Let the framework log/remeasure first, then make sure we die.
            runCatching { previous?.uncaughtException(thread, throwable) }
            runCatching { Process.killProcess(Process.myPid()) }
        }
    }

    private fun persist(context: Context, threadName: String, throwable: Throwable) {
        val sb = StringBuilder()
        sb.append("THREAD: ").append(threadName)
            .append("   PID: ").append(Process.myPid()).append('\n').append('\n')
        sb.append(throwable.javaClass.name).append(": ")
            .append(throwable.message.orEmpty()).append('\n')
        throwable.stackTrace.take(14).forEach { f -> sb.append("    at ").append(f).append('\n') }
        var cause = throwable.cause
        while (cause != null && cause !== throwable) {
            sb.append("--- caused by ---\n")
            sb.append(cause.javaClass.name).append(": ").append(cause.message.orEmpty()).append('\n')
            cause.stackTrace.take(10).forEach { f -> sb.append("    at ").append(f).append('\n') }
            cause = cause.cause
        }
        val text = sb.toString()
        Log.e("CraftCrash", text)
        // commit() (not apply) so the trace survives instant process death.
        context.getSharedPreferences("craftcaller_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_CRASH, text)
            .commit()
    }
}

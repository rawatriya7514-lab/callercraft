package com.craftcaller.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Permission groups requested at different points (all runtime on Android 13+). */
object RequiredPermissions {

    /** Core caller-ID set: state + call log + contacts + reject-calls. */
    val CALLER_ID = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ANSWER_PHONE_CALLS
    )

    /** Smart SMS inbox. */
    val SMS = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS
    )

    /** Call recording (mic). */
    val MICROPHONE = arrayOf(Manifest.permission.RECORD_AUDIO)

    /** Android 13+ notification permission. */
    val NOTIFICATIONS: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else emptyArray()

    fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun allGranted(context: Context, permissions: Array<String>): Boolean =
        permissions.all { granted(context, it) }
}

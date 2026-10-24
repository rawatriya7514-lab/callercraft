package com.craftcaller.app.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telecom.TelecomManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.craftcaller.app.R
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.BlockedNumber
import com.craftcaller.app.data.local.entity.CallerInfo
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.util.Craft
import com.craftcaller.app.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Foreground service drawing the Truecaller-style incoming-call overlay
 * (name / number / spam verdict / Block-Report-Dismiss), even when the app
 * is not in the foreground. START_REDELIVER_INTENT keeps it alive briefly.
 */
class CallerIDService : Service() {

    companion object {
        const val ACTION_SHOW = "com.craftcaller.app.action.SHOW_OVERLAY"
        const val ACTION_HIDE = "com.craftcaller.app.action.HIDE_OVERLAY"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_NAME = "name"
        const val EXTRA_IS_SPAM = "is_spam"
        const val EXTRA_REPORTS = "reports"
        const val EXTRA_LOCATION = "location"

        private const val CHANNEL_ID = "caller_id_channel"
        private const val FOREGROUND_ID = 42
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private lateinit var prefs: Prefs

    private var overlayRoot: LinearLayout? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        windowManager = getSystemService(WindowManager::class.java)
        // Ignore incoming overlays while service is not yet ready.
        runCatching {
            registerReceiver(
                NoOpReceiver(),
                IntentFilter(ACTION_HIDE),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    Context.RECEIVER_NOT_EXPORTED else 0
            )
        }
    }

    /** Placeholder receiver reserved for future broadcast control of the overlay. */
    private class NoOpReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = Unit
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_SHOW) {
            removeOverlay()
            stopSelf()
            return START_NOT_STICKY
        }
        runCatching { startAsForeground() }.onFailure { startError ->
            // On API 31+ the system may reject background foreground-service
            // starts (ForegroundServiceStartNotAllowedException); on missing
            // manifest prerequisites it throws SecurityException. Never crash
            // the call flow for an overlay failure.
            android.util.Log.w("CallerIDService", "FGS start rejected: $startError")
            stopSelf()
            return START_NOT_STICKY
        }

        val number = intent.getStringExtra(EXTRA_NUMBER).orEmpty()
        val name: String? = intent.getStringExtra(EXTRA_NAME)
        val location: String? = intent.getStringExtra(EXTRA_LOCATION)
        val isSpam = intent.getBooleanExtra(EXTRA_IS_SPAM, false)
        val reports = intent.getIntExtra(EXTRA_REPORTS, 0)

        if (isSpam) {
            Toast.makeText(this, "CraftCaller: Spam call rejected", Toast.LENGTH_LONG).show()
            endActiveCall()
        }
        showOverlay(number, name, location, isSpam, reports)
        return START_REDELIVER_INTENT
    }

    // ----------------------------------------------------------------- FGS

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Caller ID",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Shows caller name for incoming calls" }
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("CraftCaller active")
            .setContentText("Caller ID is enabled")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(FOREGROUND_ID, notification)
        }
    }

    // -------------------------------------------------------------- Overlay

    @SuppressLint("InflateParams")
    private fun showOverlay(
        number: String,
        name: String?,
        location: String?,
        isSpam: Boolean,
        reports: Int
    ) {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Enable \"Display over other apps\" for CraftCaller",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        handler.post {
            removeOverlay()
            val root = buildOverlayView(number, name, location, isSpam, reports)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 90
            }
            runCatching { windowManager.addView(root, params) }
            overlayRoot = root
        }
    }

    private fun removeOverlay() {
        overlayRoot?.let { root ->
            runCatching { windowManager.removeView(root) }
            overlayRoot = null
        }
    }

    private fun buildOverlayView(
        number: String,
        name: String?,
        location: String?,
        isSpam: Boolean,
        reports: Int
    ): LinearLayout {
        val ctx = this
        val fgColor = if (isSpam) Color.WHITE else 0xFF16233B.toInt()
        val mutedColor = if (isSpam) 0xFFFFE0E0.toInt() else 0xFF6B7A90.toInt()

        val bg = GradientDrawable().apply {
            cornerRadius = 26f * resources.displayMetrics.density
            setColor(if (isSpam) 0xFFD32F2F.toInt() else 0xFFFFFFFF.toInt())
        }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22f), dp(18f), dp(22f), dp(18f))
            background = bg
        }

        addText(root, "CraftCaller", 12f, bold = true, color = if (isSpam) mutedColor else 0xFF0A6CFF.toInt(), top = 0)
        addText(
            root,
            when {
                isSpam -> "Spam — Likely Fraud"
                !name.isNullOrBlank() -> name!!
                reports > 0 -> "$reports spam report(s)"
                else -> "Unknown"
            },
            22f, bold = true, color = fgColor, top = 8
        )
        if (!name.isNullOrBlank() && reports > 0) {
            addText(root, "$reports spam report(s)", 14f, bold = false, color = mutedColor, top = 2)
        }
        addText(root, Craft.prettyNumber(number), 15f, bold = false, color = mutedColor, top = 2)
        (location ?: Craft.guessOperator(number) ?: "").takeIf { it.isNotEmpty() }?.let { info ->
            addText(root, info, 13f, bold = false, color = mutedColor, top = 2)
        }

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun chip(label: String, bg: Int, fg: Int, onClick: () -> Unit): TextView =
            TextView(ctx).apply {
                text = label
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(fg)
                val shape = GradientDrawable().apply {
                    cornerRadius = 40f * resources.displayMetrics.density
                    setColor(bg)
                }
                background = shape
                setPadding(dp(20f), dp(10f), dp(20f), dp(10f))
                gravity = Gravity.CENTER
                setOnClickListener { onClick() }
            }

        row.addView(
            chip("Block", if (isSpam) 0x33FFFFFF else 0x14E53935, if (isSpam) Color.WHITE else 0xFFE53935.toInt())
            { blockNumber(number) { dismissOverlay() } },
            rowParams()
        )
        row.addView(
            chip("Report spam", if (isSpam) 0x33FFFFFF else 0x14F59E0B, if (isSpam) Color.WHITE else 0xFFB26A00.toInt())
            { reportSpam(number); dismissOverlay() },
            rowParams()
        )
        row.addView(
            chip("Dismiss", if (isSpam) 0xFFFFFFFF.toInt() else 0xFF0A6CFF.toInt(), if (isSpam) 0xFFD32F2F.toInt() else Color.WHITE)
            { dismissOverlay() },
            rowParams()
        )

        val rowLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        rowLp.topMargin = dp(14f)
        root.addView(row, rowLp)
        return root
    }

    private fun addText(
        parent: LinearLayout,
        text: String,
        size: Float,
        bold: Boolean,
        color: Int,
        top: Int
    ) {
        if (text.isEmpty()) return
        val tv = TextView(this)
        tv.text = text
        tv.textSize = size
        tv.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        tv.setTextColor(color)
        tv.setPadding(0, dp(top.toFloat()), 0, 0)
        parent.addView(tv)
    }

    private fun rowParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = dp(10f) }

    // -------------------------------------------------------------- Actions

    private fun blockNumber(number: String, onDone: () -> Unit) {
        if (number.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                DatabaseProvider.get(applicationContext)
                    .blockedDao()
                    .insert(BlockedNumber(number = number, reason = "Overlay block"))
            }
            Toast.makeText(this, "Number blocked", Toast.LENGTH_SHORT).show()
            endActiveCall()
        }
        onDone()
    }

    private fun reportSpam(number: String) {
        if (number.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val db = DatabaseProvider.get(applicationContext)
            val known = db.callerDao().get(number)
            db.callerDao().upsert(
                CallerInfo(
                    number = number,
                    name = known?.name,
                    location = known?.location,
                    spamReports = (known?.spamReports ?: 0) + 1,
                    updatedAt = System.currentTimeMillis(),
                    source = known?.source ?: "user-report"
                )
            )
            runCatching { FirebaseRepository.reportSpam(number) }
        }
        Toast.makeText(this, "Spam report submitted", Toast.LENGTH_SHORT).show()
    }

    private fun endActiveCall() {
        runCatching {
            getSystemService(TelecomManager::class.java)?.endCall()
        }.onFailure {
            Toast.makeText(this, "Grant phone permission to reject calls", Toast.LENGTH_LONG).show()
        }
    }

    private fun dismissOverlay() {
        handler.post {
            removeOverlay()
            stopSelf()
        }
    }

    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

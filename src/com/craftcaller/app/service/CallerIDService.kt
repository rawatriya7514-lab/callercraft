package com.craftcaller.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.craftcaller.app.CallCardActivity
import com.craftcaller.app.MainActivity
import com.craftcaller.app.R
import com.craftcaller.app.data.CallerInfo
import com.craftcaller.app.data.Db
import com.craftcaller.app.data.Prefs
import com.craftcaller.app.data.Repo
import com.craftcaller.app.util.CallerHub
import com.craftcaller.app.util.Perms
import com.craftcaller.app.util.PhoneUtils
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * CraftCaller's always-on caller ID engine.
 *
 * Runs as a foreground service (type phoneCall) with:
 *  - TelephonyCallback (Android 12+) / PhoneStateListener (<=11) ring detection
 *  - instant overlay popup ([SYSTEM_ALERT_WINDOW]) with caller name & spam state
 *  - full-screen-intent notification as the tap-through path
 *  - auto-block of blocked + community spam numbers via TelecomManager
 *  - local call-record capture + after-call flow
 */
class CallerIDService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var wm: WindowManager? = null
    private var overlay: View? = null
    private var lastState = TelephonyManager.CALL_STATE_IDLE

    private val notifMgr by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager }

    // ------------------------------------------------------------ lifecycle

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Perms.granted(this, android.Manifest.permission.READ_PHONE_STATE)) {
            stopSelf(); return
        }
        running = true
        registerPhoneWatcher()
        registerReceiver(callStateReceiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
        startForegroundCompat(CallNotification.FG_ID, CallNotification.idleNotification(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Perms.granted(this, android.Manifest.permission.READ_PHONE_STATE)) {
            stopSelf(); return START_NOT_STICKY
        }
        val number = intent?.getStringExtra(EXTRA_NUMBER)
        if (!number.isNullOrEmpty()) captureIncoming(number)
        return START_STICKY
    }

    override fun onDestroy() {
        try { unregisterReceiver(callStateReceiver) } catch (_: Exception) {}
        unregisterPhoneWatcher()
        removeOverlay()
        running = false
        super.onDestroy()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    // ------------------------------------------------------------ wiring

    private var watcher31: Watcher31? = null
    private var watcher26: Watcher26? = null

    private fun registerPhoneWatcher() {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                watcher31 = Watcher31()
                tm.registerTelephonyCallback(mainExecutor, watcher31!!)
            } else {
                watcher26 = Watcher26()
                tm.listen(watcher26, PhoneStateListener.LISTEN_CALL_STATE)
            }
        } catch (_: Exception) {
        }
    }

    private fun unregisterPhoneWatcher() {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && watcher31 != null) {
                tm.unregisterTelephonyCallback(watcher31!!)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && watcher26 != null) {
                tm.listen(watcher26, PhoneStateListener.LISTEN_NONE)
            }
        } catch (_: Exception) {
        }
        watcher31 = null; watcher26 = null
    }

    private inner class Watcher31 : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) = stateChanged(state)
    }

    private inner class Watcher26 : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) =
            stateChanged(state, phoneNumber)
    }

    /** Manifest receiver also runs while the process is dead; it feeds us numbers. */
    private val callStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: return
            if (number.trim().isEmpty()) return
            val norm = PhoneUtils.normalize(number)
            if (norm.isNotEmpty() && norm != CallerHub.lastRingingNumber) {
                CallerHub.lastRingingNumber = norm
                captureIncoming(norm)
            }
        }
    }

    // ------------------------------------------------------------ state machine

    private fun stateChanged(state: Int, number: String? = null) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                val raw = if (!number.isNullOrEmpty()) PhoneUtils.normalize(number) else ""
                if (raw.isNotEmpty() && raw != CallerHub.lastRingingNumber) {
                    CallerHub.lastRingingNumber = raw
                    captureIncoming(raw)
                }
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> lastState = state

            TelephonyManager.CALL_STATE_IDLE -> {
                val wasRinging = lastState == TelephonyManager.CALL_STATE_RINGING
                val wasActive = lastState == TelephonyManager.CALL_STATE_OFFHOOK
                lastState = state
                if (wasRinging || wasActive) onCallEnded()
            }
        }
    }

    fun onCallEnded() {
        val number = CallerHub.lastRingingNumber
        removeOverlay()
        CallRecorder.abortIfRunning()
        CallNotification.cancelRing(this)
        CallerHub.callInProgress = false
        if (number.isNotEmpty()) {
            Thread {
                try {
                    val info = try { runBlocking { Repo.lookup(number) } } catch (_: Throwable) { Db.fastLookup(number) }
                    Db.putCall(number, info.name, System.currentTimeMillis(), 3, 0, info.spam)
                    Repo.refresh()
                    CallNotification.afterCall(this, info)
                } catch (_: Throwable) {
                }
            }.start()
        }
    }

    // ------------------------------------------------------------ incoming capture

    private fun captureIncoming(number: String) {
        val n = PhoneUtils.normalize(number)
        if (n.isEmpty()) return
        CallerHub.callInProgress = true
        val cached = Db.fastLookup(n)
        CallerHub.put(n, cached)
        // instant popup with cached data first (Truecaller-speed)
        updateOverlay(cached)
        CallNotification.incoming(this, cached)
        // then resolve identity in the community database
        Thread {
            val info = try { runBlocking { Repo.lookup(n) } } catch (_: Throwable) { cached }
            CallerHub.put(n, info)
            handler.post {
                updateOverlay(info)
                CallNotification.incoming(this, info)
            }
            maybeAutoBlock(n, info)
        }.start()
    }

    private fun maybeAutoBlock(number: String, info: CallerInfo) {
        if (!info.blocked && !(info.spam && Prefs.autoBlockSpam)) return
        val tm = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        try {
            val rejected = tm.endCall()
            if (rejected) {
                Db.putCall(number, info.displayName, System.currentTimeMillis(), 3, 0, true)
                removeOverlay()
                CallNotification.cancelRing(this)
                CallNotification.blocked(this, info)
            }
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }
    }

    // ------------------------------------------------------------ overlay popup

    fun updateOverlay(info: CallerInfo) {
        if (!Settings.canDrawOverlays(this) || info.number.isEmpty()) return
        handler.post { showOverlay(info) }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun showOverlay(info: CallerInfo) {
        if (!Settings.canDrawOverlays(this) || info.number.isEmpty()) return
        if (wm == null) wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (overlay != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).also { it.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }

        val accent = if (info.spam) 0xFFE53935.toInt() else 0xFF0087FF.toInt()
        val bg = if (info.spam) 0xFF200C12.toInt() else 0xFF14161C.toInt()

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(14))
            setBackgroundColor(bg)
        }
        val tag = TextView(this).apply {
            val reason = if (info.spam) ("🚩  " + info.spamReason) else "CraftCaller"
            text = reason
            setTextColor(accent)
            textSize = 12f
        }
        val title = TextView(this).apply {
            val loc = if (info.location.isEmpty()) "" else "  ·  ${info.location}"
            text = (if (info.name.isNotEmpty()) info.name else "Unknown") + loc
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 19f
            setPadding(0, dp(6), 0, 0)
        }
        val sub = TextView(this).apply {
            val tagExtra = when {
                info.spam -> "  ·  Spam flagged"
                info.isContact -> "  ·  In your contacts"
                else -> "  ·  Unknown"
            }
            text = info.number + tagExtra
            setTextColor(if (info.spam) 0xFFFF8A80.toInt() else 0xAAFFFFFF.toInt())
            textSize = 13f
            setPadding(0, dp(2), 0, 0)
        }
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }

        fun action(label: String, red: Boolean, click: () -> Unit): Button {
            val b = Button(this)
            b.text = label
            b.isAllCaps = false
            b.setBackgroundColor(if (red) 0xFFE53935.toInt() else 0xFF23262F.toInt())
            b.setTextColor(0xFFFFFFFF.toInt())
            b.setPadding(dp(10), dp(4), dp(10), dp(4))
            b.setOnClickListener { click() }
            return b
        }

        fun addBtn(b: Button) {
            actions.addView(b, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.setMargins(dp(3), 0, dp(3), 0)
            })
        }

        addBtn(action("Dismiss", false) { removeOverlay() })
        addBtn(action("Report spam", false) {
            Thread { try { runBlocking { Repo.reportSpam(info.number, "Likely fraud") } } catch (_: Throwable) {} }.start()
            removeOverlay()
        })
        val blockLabel = if (info.blocked) "Unblock" else "Block"
        addBtn(action(blockLabel, true) {
            Thread {
                try {
                    if (info.blocked) runBlocking { Repo.unblock(info.number) } else runBlocking { Repo.block(info.number) }
                } catch (_: Throwable) {
                }
                handler.post { removeOverlay() }
            }.start()
        })

        box.addView(tag)
        box.addView(title)
        box.addView(sub)
        box.addView(actions)

        try {
            overlay = box
            wm!!.addView(box, params)
        } catch (_: Exception) {
            overlay = null
        }
    }

    fun removeOverlay() {
        val v = overlay ?: return
        overlay = null
        try { wm?.removeView(v) } catch (_: Exception) {}
    }

    // ------------------------------------------------------------ internals

    private fun startForegroundCompat(id: Int, not: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(id, not, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
            } else {
                startForeground(id, not)
            }
        } catch (_: Throwable) {
        }
    }

    companion object {
        const val EXTRA_NUMBER = "number"

        @Volatile var running = false

        fun start(ctx: Context) {
            if (!Perms.granted(ctx, android.Manifest.permission.READ_PHONE_STATE)) return
            val i = Intent(ctx, CallerIDService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
                else ctx.startService(i)
            } catch (_: Throwable) {
            }
        }

        fun pushRing(ctx: Context, number: String) {
            if (number.isEmpty()) return
            val i = Intent(ctx, CallerIDService::class.java).putExtra(EXTRA_NUMBER, number)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
                else ctx.startService(i)
            } catch (_: Throwable) {
            }
        }

        fun stop(ctx: Context) {
            try { ctx.stopService(Intent(ctx, CallerIDService::class.java)) } catch (_: Throwable) {}
        }
    }
}

package com.craftcaller.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.craftcaller.app.ui.CraftCallerTheme
import com.craftcaller.app.ui.Root

/** Launch screen (single outer entry of the app). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CraftCallerTheme {
                Root()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // e.g. "open number detail" deep links from notifications
        setIntent(intent)
    }
}

/**
 * Lock-screen capable call card: shows for incoming (ringing) calls and as the
 * post-call sheet. Android >= 10: launched through the full-screen-intent
 * notification; when overlay permission is granted the service additionally
 * pins a passive banner above the dialer.
 */
class CallCardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        }
        enableEdgeToEdge()
        val number = intent.getStringExtra(EXTRA_NUMBER) ?: ""
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_RINGING
        val spam = intent.getBooleanExtra(EXTRA_SPAM, false)
        setContent {
            CraftCallerTheme {
                com.craftcaller.app.ui.callcard.CallCard(
                    number = number,
                    mode = mode,
                    hintSpam = spam,
                    onClose = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_NUMBER = "number"
        const val EXTRA_MODE = "mode"
        const val EXTRA_SPAM = "spam"

        const val MODE_RINGING = "ringing"
        const val MODE_AFTER = "after"

        fun ringIntent(ctx: Context, info: com.craftcaller.app.data.CallerInfo): Intent =
            Intent(ctx, CallCardActivity::class.java)
                .putExtra(EXTRA_NUMBER, info.number)
                .putExtra(EXTRA_MODE, MODE_RINGING)
                .putExtra(EXTRA_SPAM, info.spam)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )

        fun afterIntent(ctx: Context, info: com.craftcaller.app.data.CallerInfo): Intent =
            Intent(ctx, CallCardActivity::class.java)
                .putExtra(EXTRA_NUMBER, info.number)
                .putExtra(EXTRA_MODE, MODE_AFTER)
                .putExtra(EXTRA_SPAM, info.spam)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
    }
}

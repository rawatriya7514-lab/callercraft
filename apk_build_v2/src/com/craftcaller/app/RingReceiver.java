package com.craftcaller.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Caller-ID announcer during the RINGING state:
 * - Unknown number  -> banner with lookup result (warn tags in red)
 * - Blocked number  -> banner "Blocked by CraftCaller"
 * Runs both when the device holds the screening role and when it does not,
 * so caller-ID works even before the user grants the role.
 */
public class RingReceiver extends BroadcastReceiver {

    private static final String TAG = "CraftCaller.Ring";
    private static final String CHANNEL_CALLINFO = "craftcaller_callinfo";
    private static final int NOTIF_ID_CALLER = 4101;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null || !TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction()))
            return;
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) && number != null) {
            try {
                LookupTask.run(ctx, number);
            } catch (Exception ex) {
                Log.e(TAG, "ring lookup failed", ex);
            }
        } else if ((TelephonyManager.EXTRA_STATE_IDLE.equals(state)
                || TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state))) {
            // call finished / answered — clear the trailing banner
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(NOTIF_ID_CALLER);
        }
    }

    static void clearBanner(Context ctx) {
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(NOTIF_ID_CALLER);
        } catch (Exception ex) {
            Log.e(TAG, "cancel failed", ex);
        }
    }

    static void notifyCallerId(Context ctx, String number, String name, String tag, int score) {
        try {
            if (ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) return;
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            ensureChannel(nm);

            String title = name != null ? name : number;
            String text;
            if (tag != null && CallerDb.isWarnTag(tag)) {
                text = "⚠ " + tag + (score > 0 ? " · " + score + " report(s)" : "") + " · " + number;
            } else {
                text = "Caller ID · " + number;
            }

            PendingIntent pi = PendingIntent.getActivity(ctx, 0,
                    new Intent(ctx, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Notification n = new Notification.Builder(ctx, CHANNEL_CALLINFO)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setCategory(Notification.CATEGORY_CALL)
                    .setColor(0xFFEF4444)
                    .setVisibility(Notification.VISIBILITY_PRIVATE)
                    .setContentIntent(pi)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setAutoCancel(true)
                    .build();

            if (nm != null) nm.notify(NOTIF_ID_CALLER, n);
        } catch (Exception ex) {
            Log.e(TAG, "notify failed", ex);
        }
    }

    private static void ensureChannel(NotificationManager nm) {
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_CALLINFO,
                "Caller ID", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Live caller identification for incoming calls");
        nm.createNotificationChannel(ch);
    }
}

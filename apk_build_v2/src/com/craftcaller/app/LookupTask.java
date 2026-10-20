package com.craftcaller.app;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * Off-main-thread lookup for the ringing state: database classification
 * first, then device contacts fallback, then the heads-up banner.
 */
final class LookupTask {

    private LookupTask() {}

    static void run(Context ctx, String number) {
        final Context app = ctx.getApplicationContext();
        final String num = number;
        new Thread(() -> {
            try {
                CallerDb db = new CallerDb(app);
                db.seedIfNeeded();
                CallerDb.Entry e = db.lookup(num);
                String name = e != null ? e.name : null;
                if (name == null) {
                    final String contact = db.contactName(num);
                    if (contact != null) name = contact;
                }

                boolean ringingGate = true;
                if (ringingGate) {
                    String state = currentState(app);
                    if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) || state == null) {
                        RingReceiver.notifyCallerId(app, num, name,
                                e != null ? e.tag : null,
                                e != null ? e.score() : 0);
                    }
                }
            } catch (Exception ignored) {
            }
        }, "craftcaller-lookup").start();
    }

    private static String currentState(Context ctx) {
        try {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null && ctx.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return tm.getCallState() == TelephonyManager.CALL_STATE_RINGING
                        ? TelephonyManager.EXTRA_STATE_RINGING
                        : tm.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK
                        ? TelephonyManager.EXTRA_STATE_OFFHOOK
                        : TelephonyManager.EXTRA_STATE_IDLE;
            }
        } catch (Exception ignored) {}
        return null;
    }
}

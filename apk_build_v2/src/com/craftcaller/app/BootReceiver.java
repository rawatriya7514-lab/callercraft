package com.craftcaller.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Keeps the ringing-state listener functional after a reboot.
 * (The manifest receiver itself is static; this hook exists so a
 * database seed happens once, on boot.)
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            CallerDb db = new CallerDb(ctx);
            db.seedIfNeeded();
        } catch (Exception ex) {
            Log.e("CraftCaller.Boot", "boot seed failed", ex);
        }
    }
}

package com.craftcaller.app;

import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;

/**
 * CraftCaller call screening engine.
 *
 * Registered exactly like the reference architecture observed in the stock
 * caller-ID app (CallScreeningService + BIND_SCREENING_SERVICE +
 * android.telecom.CallScreeningService intent filter). The user grants the
 * "Caller ID & spam app" role (ROLE_CALL_SCREENING) from the app UI.
 *
 * Decision policy (offline, local database):
 *   1. Personal blocklist               -> DISALLOW + REJECT + skip log
 *   2. Auto-reject enabled AND spam>=5  -> REJECT
 *   3. Warn-tagged (reports 2..4)       -> ALLOW + Caller-ID warn banner
 *   4. Everything else                  -> ALLOW
 *
 * Caller-ID display itself happens through the ringing banner
 * (RingReceiver) — Telecom's CallResponse carries no display-name API.
 */
public class CraftCallScreeningService extends CallScreeningService {

    private static final String TAG = "CraftCaller.Screen";

    @Override
    public void onScreenCall(Call.Details details) {
        try {
            if (details == null
                    || details.getHandle() == null
                    || details.getCallDirection() != Call.Details.DIRECTION_INCOMING) {
                return;
            }
            String raw = details.getHandle().getSchemeSpecificPart();

            CallerDb db = new CallerDb(this);
            CallerDb.Entry e = db.lookup(raw);

            boolean autoReject = db.getBool("auto_reject_spam", true);
            boolean logDec = db.getBool("log_decisions", true);
            boolean banner = db.getBool("announce_banner", true);

            boolean reject = false;
            boolean disallow = false;
            String decision = "ALLOW";

            if (e != null && e.blocked) {
                reject = true;
                disallow = true;
                decision = "REJECT(blocked)";
            } else if (e != null && autoReject
                    && CallerDb.TAG_SPAM.equals(e.tag) && e.score() >= 5) {
                reject = true;
                decision = "REJECT(spam:" + e.score() + ")";
            } else if (e != null && CallerDb.isWarnTag(e.tag)) {
                decision = "ALLOW(warn:" + e.tag + ":" + e.score() + ")";
            }

            if (logDec) db.logScreen(raw, decision, null);

            CallScreeningService.CallResponse.Builder b =
                    new CallScreeningService.CallResponse.Builder();
            b.setRejectCall(reject);
            b.setDisallowCall(disallow);
            b.setSkipCallLog(disallow);
            b.setSkipNotification(reject);
            respondToCall(details, b.build());

            // Banner works with or without the screening role
            if (banner && !reject && e != null && CallerDb.isWarnTag(e.tag)) {
                RingReceiver.notifyCallerId(this, raw, e.name, e.tag, e.score());
            } else if (reject) {
                // clear a stale banner if a call was killed mid-ring
                RingReceiver.clearBanner(this);
            }
        } catch (Exception ex) {
            Log.e(TAG, "screening failed", ex);
        }
    }
}

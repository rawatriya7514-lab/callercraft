package com.craftcaller.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * CraftCaller — native caller-ID dashboard.
 * Tabs: Search / Recents / Shield / Community.
 */
public class MainActivity extends Activity {

    private static final int TAB_SEARCH = 0;
    private static final int TAB_RECENTS = 1;
    private static final int TAB_SHIELD = 2;
    private static final int TAB_COMMUNITY = 3;

    private static final int[] TAB_IDS = {
            R.id.tab_search, R.id.tab_recents, R.id.tab_shield, R.id.tab_community };

    private static final int REQ_PERMS = 71;
    private static final int REQ_ROLE = 72;

    private CallerDb db;
    private LinearLayout contentHost;
    private TextView statusChip;

    private int currentTab = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new CallerDb(this);
        db.seedIfNeeded();
        buildUi();

        if (getIntent() != null && "craftcaller".equals(getIntent().getScheme())) {
            Uri d = getIntent().getData();
            String num = d == null ? null : d.getQueryParameter("number");
            if (num != null) {
                selectTab(TAB_SEARCH);
                searchQuery = num;
            }
        }
        requestCorePermissionsIfNeeded();
    }

    String searchQuery = null;

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        selectTab(currentTab < 0 ? TAB_SEARCH : currentTab);
    }

    /* =============================== UI skeleton =============================== */

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color("#090D16"));

        /* ---- header ---- */
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(16), dp(14), dp(12));

        TextView logo = new TextView(this);
        logo.setText("🛡");
        logo.setTextSize(24);
        logo.setGravity(Gravity.CENTER);
        logo.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        header.addView(logo, logoLp);

        LinearLayout brandBox = new LinearLayout(this);
        brandBox.setOrientation(LinearLayout.VERTICAL);
        TextView brand = new TextView(this);
        brand.setText("CraftCaller");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(20);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        TextView brandSub = new TextView(this);
        brandSub.setText("Offline Caller-ID · Spam Shield");
        brandSub.setTextColor(color("#94A3B8"));
        brandSub.setTextSize(11);
        brandBox.addView(brand);
        brandBox.addView(brandSub);
        LinearLayout.LayoutParams brandLp =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        brandLp.leftMargin = dp(12);
        header.addView(brandBox, brandLp);

        statusChip = chip("SET UP", "#3B82F6");
        header.addView(statusChip);

        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        /* ---- content ---- */
        contentHost = new LinearLayout(this);
        contentHost.setOrientation(LinearLayout.VERTICAL);
        root.addView(contentHost, new LinearLayout.LayoutParams(-1, 0, 1f));

        /* ---- tabs ---- */
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(8), dp(6), dp(8), dp(10));
        tabs.setBackgroundColor(color("#0C111C"));

        String[][] tabsInfo = {
                { "🔎", "Search" }, { "📞", "Recents" }, { "🚫", "Shield" }, { "🌐", "Community" }
        };
        for (int i = 0; i < tabsInfo.length; i++) {
            TextView t = new TextView(this);
            t.setText(tabsInfo[i][0] + "\n" + tabsInfo[i][1]);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(11);
            t.setTextColor(color("#94A3B8"));
            t.setPadding(dp(4), dp(8), dp(4), dp(8));
            t.setId(TAB_IDS[i]);
            final int idx = i;
            t.setOnClickListener(v -> selectTab(idx));
            tabs.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        root.addView(tabs, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    private void selectTab(int idx) {
        if (idx == currentTab) { renderTab(idx); return; }
        currentTab = idx;
        for (int i = 0; i < TAB_IDS.length; i++) {
            TextView t = findViewById(TAB_IDS[i]);
            if (t != null) t.setTextColor(i == idx ? Color.WHITE : color("#64748B"));
        }
        renderTab(idx);
    }

    private void renderTab(int idx) {
        contentHost.removeAllViews();
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(10), dp(16), dp(28));
        sc.addView(page, new ScrollView.LayoutParams(-1, -2));

        if (idx == TAB_SEARCH) buildSearchPage(page);
        else if (idx == TAB_RECENTS) buildRecentsPage(page);
        else if (idx == TAB_SHIELD) buildShieldPage(page);
        else if (idx == TAB_COMMUNITY) buildCommunityPage(page);

        contentHost.addView(sc, new LinearLayout.LayoutParams(-1, -1));
    }

    /* =============================== SEARCH =============================== */

    private void buildSearchPage(final LinearLayout page) {
        page.addView(sectionTitle("Number Lookup"));

        final EditText q = new EditText(this);
        q.setHint("Number to inspect (e.g. +91 98765 43210)");
        q.setHintTextColor(color("#64748B"));
        q.setTextColor(Color.WHITE);
        q.setTextSize(14);
        q.setSingleLine(true);
        q.setPadding(dp(14), dp(12), dp(14), dp(12));
        q.setBackground(drawable("bg_search"));

        if (searchQuery != null) q.setText(searchQuery);

        LinearLayout row = rowLayout();
        row.addView(q, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView go = buttonPrimary("Lookup", v -> runLookup(q.getText().toString()));
        LinearLayout.LayoutParams goLp = new LinearLayout.LayoutParams(dp(92), dp(48));
        goLp.leftMargin = dp(8);
        row.addView(go, goLp);
        page.addView(row);

        LinearLayout resultHost = new LinearLayout(this);
        resultHost.setOrientation(LinearLayout.VERTICAL);
        resultHost.setId(R.id.lookup_result);
        page.addView(resultHost, pad(-1, -2, 0, dp(12), 0, 0));

        if (searchQuery != null) {
            runLookup(searchQuery);
        } else {
            resultHost.addView(body(
                    "Lookup uses offline structural rules plus community reports. "
                    + "Report spam numbers to grow the shield for everyone."));
        }

        page.addView(body("Reports stay on this device. Shared community sync ships in a later release."));
    }

    private void runLookup(String raw) {
        LinearLayout host = findViewById(R.id.lookup_result);
        if (host == null) return;
        host.removeAllViews();

        String key = CallerDb.normalize(raw);
        if (key.length() < 4) {
            toast("Enter a valid number");
            return;
        }
        searchQuery = key;

        CallerDb.Entry e = db.lookup(key);
        if (e == null) e = new CallerDb.Entry(key, null, null, 0, false);
        String name = e.name != null && e.name.length() > 0 ? e.name : db.contactName(key);

        LinearLayout card = card();

        TextView num = headerBig(key);
        card.addView(num);

        if (name != null) {
            card.addView(row("Name", name));
            if (e.name == null) db.saveName(key, name, null);
        }

        String status = e.blocked ? "🚫 BLOCKED on this device" : "Trust score: " + e.score();
        card.addView(row("Status", status));

        String tag = e.tag;
        TextView tagChip;
        if (tag == null) {
            tagChip = chip("✓ CLEAN — no reports", "#10B981");
        } else {
            String label = CallerDb.isWarnTag(tag) ? "⚠ " + tag : tag;
            if (e.reports > 0) label += " · " + e.reports + " report(s)";
            tagChip = chip(label, CallerDb.isWarnTag(tag) ? "#EF4444" : "#F59E0B");
        }
        card.addView(tagChip, pad(-2, -2, 0, dp(8), 0, dp(6)));

        LinearLayout actions = rowLayout();
        TextView dial = buttonGhost("📞 Dial", v ->
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + key))));
        actions.addView(dial, stretch);
        boolean blocked = db.isBlocked(key);
        TextView bl = buttonGhost(blocked ? "Unblock" : "🚫 Block", v -> {
            if (db.isBlocked(key)) db.unblock(key);
            else db.block(key, name);
            runLookup(key);
        });
        actions.addView(bl, stretch);

        card.addView(actions, pad(-1, -2, 0, dp(12), 0, 0));
        TextView report = buttonGhost("Report number…", v -> showReportDialog(key));
        card.addView(report, pad(-2, -2, 0, 0, 0, 0));

        host.addView(card, pad(-1, -2, 0, 0, 0, 0));
    }

    /* =============================== RECENTS =============================== */

    private void buildRecentsPage(LinearLayout page) {
        page.addView(sectionTitle("Recent Calls"));

        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            page.addView(body("Phone + call-log permission is needed to tag recent calls."));
            page.addView(buttonPrimary("Grant permissions", v -> requestCorePermissionsIfNeeded()));
            return;
        }

        int shown = 0;
        try {
            Cursor c = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    new String[]{ CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
                            CallLog.Calls.DATE, CallLog.Calls.TYPE },
                    null, null, CallLog.Calls.DATE + " DESC");
            if (c != null) {
                try {
                    while (c.moveToNext() && shown < 60) {
                        shown++;
                        addRecentRow(page,
                                c.getString(0), c.getString(1),
                                c.getLong(2), c.getInt(3));
                    }
                } finally {
                    c.close();
                }
            }
        } catch (SecurityException se) {
            page.addView(body("Call-log permission was revoked."));
        } catch (Exception ex) {
            page.addView(body("Could not read call log: " + ex.getMessage()));
        }
        if (shown == 0) page.addView(body("No calls in the log yet."));
    }

    private void addRecentRow(LinearLayout page, String rawNum, String cachedName,
                              long date, int type) {
        String key = CallerDb.normalize(rawNum);
        CallerDb.Entry e = key.length() >= 4 ? db.lookup(key) : null;
        String name = cachedName != null && cachedName.length() > 0
                ? cachedName
                : (e != null && e.name != null ? e.name : null);

        String dir = type == CallLog.Calls.MISSED_TYPE ? "Missed"
                : type == CallLog.Calls.OUTGOING_TYPE ? "Outgoing" : "Incoming";
        String time = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(new Date(date));

        String line1 = (name != null ? name : rawNum)
                + (e != null && e.blocked ? " · BLOCKED" : "");
        String line2 = dir + " · " + time
                + (e != null && e.tag != null ? " · " + e.tag : "");

        LinearLayout row = rowLayout();
        row.addView(avatar(name != null && name.length() > 0 ? name.substring(0, 1) : "#"));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        TextView l1 = new TextView(this);
        l1.setText(line1);
        l1.setTextColor(Color.WHITE);
        l1.setTextSize(14);
        l1.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView l2 = new TextView(this);
        l2.setText(line2);
        l2.setTextColor(color("#94A3B8"));
        l2.setTextSize(11);
        mid.addView(l1);
        mid.addView(l2);
        LinearLayout.LayoutParams midLp = new LinearLayout.LayoutParams(0, -2, 1f);
        midLp.leftMargin = dp(12);
        row.addView(mid, midLp);

        if (e != null && e.tag != null) {
            row.addView(chip(e.tag, CallerDb.isWarnTag(e.tag) ? "#EF4444" : "#F59E0B"));
        } else {
            row.addView(chip("✔", "#10B981"));
        }

        page.addView(rowCard(row), pad(-1, -2, 0, dp(6), 0, 0));
    }

    /* =============================== SHIELD =============================== */

    private void buildShieldPage(LinearLayout page) {
        page.addView(sectionTitle("Spam Shield"));

        LinearLayout roleCard = card();
        boolean active = screeningRoleHeld();
        roleCard.addView(row("Caller ID & spam role", active ? "✓ Active" : "Not active"));
        roleCard.addView(body(active
                ? "CraftCaller screens every incoming call and auto-rejects marked spam."
                : "Grant the system “Caller ID & spam” role to enable real-time screening, "
                + "auto-reject and name replacement while the phone rings."));
        roleCard.addView(buttonPrimary(
                active ? "Manage role" : "Activate spam shield",
                v -> requestScreeningRole()));
        page.addView(roleCard, pad(-1, -2, 0, 0, 0, dp(10)));

        LinearLayout toggles = card();
        toggles.addView(toggle("Auto-reject spam (reports ≥ 5)", db.getBool("auto_reject_spam", true),
                (btn, on) -> db.setBool("auto_reject_spam", on)));
        toggles.addView(toggle("Caller-ID banner while ringing", db.getBool("announce_banner", true),
                (btn, on) -> db.setBool("announce_banner", on)));
        toggles.addView(toggle("Save screening decisions", db.getBool("log_decisions", true),
                (btn, on) -> db.setBool("log_decisions", on)));
        page.addView(toggles, pad(-1, -2, 0, 0, 0, dp(10)));

        page.addView(sectionTitle("Blocklist"));
        boolean anyBlock = false;
        try {
            Cursor c = db.listBlocked();
            if (c != null) {
                try {
                    while (c.moveToNext()) {
                        anyBlock = true;
                        addBlockRow(page, c.getString(0), c.getString(1));
                    }
                } finally {
                    c.close();
                }
            }
        } catch (Exception ignored) {}
        if (!anyBlock) page.addView(body("Nothing blocked yet. Block numbers from Search."));

        page.addView(sectionTitle("Screening log"));
        int logged = 0;
        try {
            Cursor c = db.recentScreenLog(12);
            if (c != null) {
                try {
                    while (c.moveToNext()) {
                        logged++;
                        long ts = c.getLong(0);
                        String num = c.getString(1);
                        String dec = c.getString(2);
                        String time = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
                                .format(new Date(ts));
                        TextView t = new TextView(this);
                        t.setText(time + " · " + num + "\n" + dec);
                        t.setTextColor(color("#94A3B8"));
                        t.setTextSize(12);
                        t.setPadding(dp(10), dp(5), dp(6), dp(5));
                        page.addView(t);
                    }
                } finally {
                    c.close();
                }
            }
        } catch (Exception ignored) {}
        if (logged == 0) page.addView(body("No screening decisions yet."));
    }

    private void addBlockRow(LinearLayout page, final String num, String name) {
        LinearLayout row = rowLayout();
        TextView t = new TextView(this);
        t.setText((name != null && name.length() > 0 ? name + " · " : "") + num);
        t.setTextColor(Color.WHITE);
        t.setTextSize(14);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(t);
        row.addView(buttonGhost("Remove", v -> {
            db.unblock(num);
            renderTab(TAB_SHIELD);
            page.invalidate();
        }));
        page.addView(rowCard(row), pad(-1, -2, 0, dp(6), 0, 0));
    }

    /* =============================== COMMUNITY =============================== */

    private void buildCommunityPage(final LinearLayout page) {
        page.addView(sectionTitle("Community Shield"));

        LinearLayout reportCard = card();
        reportCard.addView(row("Report a number", "Helps your offline shield"));
        final EditText repNum = new EditText(this);
        repNum.setHint("Enter number…");
        repNum.setHintTextColor(color("#64748B"));
        repNum.setTextColor(Color.WHITE);
        repNum.setSingleLine(true);
        repNum.setPadding(dp(12), dp(10), dp(12), dp(10));
        repNum.setBackground(drawable("bg_search"));
        reportCard.addView(repNum, pad(-1, dp(46), 0, dp(10), 0, dp(8)));
        reportCard.addView(buttonPrimary("Open report dialog", v -> showReportDialog(repNum.getText().toString())));
        page.addView(reportCard, pad(-1, -2, 0, 0, 0, dp(10)));

        page.addView(sectionTitle("Top reported numbers"));
        int shown = 0;
        try {
            Cursor c = db.topSpammers(15);
            if (c != null) {
                try {
                    while (c.moveToNext()) {
                        shown++;
                        String num = c.getString(0);
                        String name = c.getString(1);
                        String tag = c.getString(2);
                        int reports = c.getInt(3);
                        addTopRow(page, num, name, tag, reports);
                    }
                } finally {
                    c.close();
                }
            }
        } catch (Exception ignored) {}
        if (shown == 0) {
            page.addView(body("No community reports yet — report the telemarketer from the Search tab."));
        }
    }

    private void addTopRow(LinearLayout page, final String num, String name, String tag, int reports) {
        LinearLayout row = rowLayout();
        row.addView(avatar(name != null && name.length() > 0 ? name.substring(0, 1) : "#"));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        TextView l1 = new TextView(this);
        l1.setText(name != null && name.length() > 0 ? name : num);
        l1.setTextColor(Color.WHITE);
        l1.setTextSize(14);
        l1.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView l2 = new TextView(this);
        l2.setText(num + " · " + (tag != null ? tag : "reported"));
        l2.setTextColor(color("#94A3B8"));
        l2.setTextSize(11);
        mid.addView(l1);
        mid.addView(l2);
        LinearLayout.LayoutParams midLp = new LinearLayout.LayoutParams(0, -2, 1f);
        midLp.leftMargin = dp(12);
        row.addView(mid, midLp);

        row.addView(chip(reports + "×", "#EF4444"));

        row.setOnClickListener(v -> showReportDialog(num));
        page.addView(rowCard(row), pad(-1, -2, 0, dp(6), 0, 0));
    }

    /* =============================== report dialog =============================== */

    private void showReportDialog(String raw) {
        final String key = CallerDb.normalize(raw);
        if (key.length() < 4) {
            toast("Enter a valid number");
            return;
        }
        final String[] tags = { CallerDb.TAG_SPAM, CallerDb.TAG_SCAM,
                CallerDb.TAG_TELEMARKETING, "Finance Fraud", "Known" };
        new AlertDialog.Builder(this)
                .setTitle("Report " + key)
                .setItems(tags, (d, which) -> {
                    db.report(key, tags[which], null);
                    toast("Reported as " + tags[which]);
                    renderTab(TAB_SEARCH);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* =============================== system =============================== */

    private boolean screeningRoleHeld() {
        if (Build.VERSION.SDK_INT < 29) return false;
        RoleManager rm = getSystemService(RoleManager.class);
        return rm != null && rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    private void requestScreeningRole() {
        if (Build.VERSION.SDK_INT < 29) {
            toast("Screening needs Android 10+");
            return;
        }
        RoleManager rm = getSystemService(RoleManager.class);
        if (rm == null || !rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            toast("Caller-ID role not available on this device");
            return;
        }
        startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), REQ_ROLE);
    }

    private void requestCorePermissionsIfNeeded() {
        String[] wanted;
        if (Build.VERSION.SDK_INT >= 33) {
            wanted = new String[]{ Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS,
                    Manifest.permission.POST_NOTIFICATIONS };
        } else {
            wanted = new String[]{ Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS };
        }
        boolean need = false;
        for (String p : wanted) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) need = true;
        }
        if (need) requestPermissions(wanted, REQ_PERMS);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_ROLE) {
            toast(res == RESULT_OK ? "Caller-ID role granted ✔" : "Role not granted");
            refreshStatus();
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        refreshStatus();
        if (req == REQ_PERMS) {
            boolean allOk = true;
            for (int r : results) if (r != PackageManager.PERMISSION_GRANTED) allOk = false;
            toast(allOk ? "Permissions granted — shield armed" : "Some permissions denied — shield degraded");
        }
    }

    private void refreshStatus() {
        if (statusChip == null) return;
        boolean active = screeningRoleHeld();
        statusChip.setText(active ? "SHIELD ACTIVE" : "SET UP");
        statusChip.setTextColor(Color.WHITE);
        statusChip.setBackground(drawable("bg_chip"));
        statusChip.getBackground().mutate().setColorFilter(
                active ? 0xFF10B981 : 0xFF3B82F6, android.graphics.PorterDuff.Mode.SRC_ATOP);
    }

    /* =============================== widgets =============================== */

    private TextView sectionTitle(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(18);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(10), 0, dp(10));
        return t;
    }

    private TextView body(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color("#94A3B8"));
        t.setTextSize(12);
        t.setLineSpacing(dp(2), 1f);
        t.setPadding(dp(4), dp(6), dp(4), dp(6));
        return t;
    }

    private TextView headerBig(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(24);
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        c.setBackground(drawable("bg_card"));
        return c;
    }

    private LinearLayout rowCard(View child) {
        LinearLayout c = card();
        c.addView(child, new LinearLayout.LayoutParams(-1, -2));
        return c;
    }

    private LinearLayout rowLayout() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private LinearLayout row(String k, String v) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(6), 0, dp(6));
        TextView kk = new TextView(this);
        kk.setText(k);
        kk.setTextColor(color("#94A3B8"));
        kk.setTextSize(12);
        r.addView(kk, new LinearLayout.LayoutParams(0, -2, 0.42f));
        TextView vv = new TextView(this);
        vv.setText(v);
        vv.setTextColor(Color.WHITE);
        vv.setTextSize(13);
        vv.setTypeface(Typeface.DEFAULT_BOLD);
        r.addView(vv, new LinearLayout.LayoutParams(0, -2, 0.58f));
        return r;
    }

    private TextView chip(String text, String color) {
        TextView c = new TextView(this);
        c.setText(text);
        c.setTextColor(Color.WHITE);
        c.setTextSize(10);
        c.setTypeface(Typeface.DEFAULT_BOLD);
        c.setPadding(dp(10), dp(5), dp(10), dp(5));
        c.setBackground(drawable("bg_chip"));
        return c;
    }

    private TextView avatar(String initial) {
        TextView a = new TextView(this);
        a.setText(initial == null ? "#" : initial.toUpperCase(Locale.getDefault()));
        a.setTextColor(Color.WHITE);
        a.setTextSize(15);
        a.setTypeface(Typeface.DEFAULT_BOLD);
        a.setGravity(Gravity.CENTER);
        a.setBackground(drawable("bg_btn_ghost"));
        return a;
    }

    private TextView buttonPrimary(String label, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), dp(12), dp(14), dp(12));
        b.setBackground(drawable("bg_btn_primary"));
        b.setOnClickListener(l);
        return b;
    }

    private TextView buttonGhost(String label, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(label);
        b.setTextColor(color("#DBEAFE"));
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        b.setBackground(drawable("bg_btn_ghost"));
        b.setOnClickListener(l);
        return b;
    }

    private interface BoolSetter { void set(View v, boolean on); }

    private Switch toggle(String label, boolean initial, final BoolSetter setter) {
        Switch s = new Switch(this);
        s.setText(label + "   ");
        s.setChecked(initial);
        s.setTextColor(Color.WHITE);
        s.setTextSize(13);
        s.setPadding(dp(10), dp(10), dp(10), dp(10));
        s.setOnClickListener(v -> setter.set(v, ((Switch) v).isChecked()));
        return s;
    }

    /* =============================== layout helpers =============================== */

    private LinearLayout.LayoutParams pad(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private LinearLayout.LayoutParams stretch = new LinearLayout.LayoutParams(0, -2, 1f);

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private int color(String hex) {
        return Color.parseColor(hex);
    }

    private Drawable drawable(String name) {
        int id = getResources().getIdentifier(name, "drawable", getPackageName());
        if (id == 0) return null;
        return getResources().getDrawable(id);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}

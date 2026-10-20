## 🚀 CraftCaller v2.0.0 — REAL Caller-ID Release (Native)

**v2 replaces the v1 web-mockup with a real native caller-ID and spam-shield app.**
Architecture is informed by static analysis (REA archive inventory + manifest decode)
of the installed reference app's shipped APK — the implementation is a clean-room
rebuild with own code and an offline, on-device intelligence model.

---

### 🌟 What actually works in v2

- **🛡️ Real call screening** — `CraftCallScreeningService` is registered exactly like
  the reference implementation: `android.telecom.CallScreeningService` action +
  `BIND_SCREENING_SERVICE` permission. After granting the system
  **"Caller ID & spam" role** (in-app, one tap) every incoming call is classified locally.
- **🚫 Auto-reject spam** — numbers carrying ≥ 5 community reports can be rejected
  automatically (toggle in Shield). Blocklist entries are rejected with call-log skip.
- **📱 Caller-ID banner during ringing** — a `PHONE_STATE` receiver + async lookup
  raises a heads-up notification naming the caller, the tag (Spam/Scam/Telemarketing)
  and the report count, even before the screening role is granted.
- **🔎 Number lookup** — normalized search matches reports DB, structural heuristics
  and your device contacts (READ_CONTACTS).
- **📞 Recents with tags** — call log rows stamped with spam tags / blocked state.
- **🌐 Community reports (offline)** — report numbers as Spam / Scam / Telemarketing /
  Finance Fraud; each report increments the counter shown everywhere in the app.
- **📝 Screening log** — every decision is stored locally and viewable in Shield.
- **Deep links** — `craftcaller://lookup?number=%2B919…` opens the lookup.

### 🧠 Offline intelligence model (own data — no third-party datasets)

- `rules` table: structural heuristics, e.g. the Indian `140` fixed-line
  telemarketing prefix series (added/extended by the user).
- `numbers` table: community report counters (this device). Score = rule-weight + reports.
- Trust policy: **≥ 5 → auto-reject candidate · 2–4 → warn banner · < 2 → clean**.

### ⚡ Architecture evidence base (from the reference APK)

Observed reference components that informed the design (own implementation):
- `TruecallerCallScreeningService` → our `CraftCallScreeningService`
- `CallerIdService` (phone-foreground announcer) → our `RingReceiver` + notifier
- `DialerNumberLookupService` → in-app lookup engine (`CallerDb`)

### 📦 Build

- `python3 build_v2_native.py` — AAPT2 + javac + d8 + apksigner (V1+V2+V3)
- Output: `CraftCaller.apk` (v2, ~270 KB) and a copy in `/storage/emulated/0/Download/`
- Keystore: `~/artraw-files/keystore/artraw-release.keystore` (alias `artraw`)
- v1 artifacts kept: `CraftCaller-v1.0.0-WebBundle.zip`, `CraftCaller-Standalone.html`,
  and `dashboard.html` remains bundled as an asset.

### 📲 First-run setup (on device)

1. Install `Download/CraftCaller.apk` (allow install from browser/files).
2. Open app → grant Phone / Calls / Contacts (+ Notifications on 13+).
3. Tab **Shield** → **Activate spam shield** → pick CraftCaller in the system dialog.
4. Calls are now screened; check decisions under Shield → Screening log.

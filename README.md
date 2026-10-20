# CraftCaller 📞

**A Truecaller-style caller-ID, spam-defence and smart-SMS app for Android — Kotlin · Jetpack Compose · MVVM · Firestore/Retrofit.**

Package: `com.craftcaller.app` · minSdk 26 (Android 8.0) · targetSdk 35 (Android 15)

---

## Features

| # | Feature | Implementation |
|---|---------|----------------|
| 1 | **Caller ID on incoming call** | `service/CallerIDService.kt` foreground service (`foregroundServiceType=phoneCall`) + `TelephonyCallback` (Android 12+) / `PhoneStateListener` (≤11) + manifest `PHONE_STATE` receiver; instant overlay popup (`TYPE_APPLICATION_OVERLAY`) and a full-screen-intent lock-screen call card (`CallCardActivity`) |
| 2 | **Community caller-ID search** | Search bar on the home tab → local SQLite cache → community DB (Firestore REST via **Retrofit**, collection `users`) with graceful offline fallback seeded data |
| 3 | **Spam detection & reporting** | Local reports + cloud reports (`reports` collection); ≥ **10 community reports** ⇒ “Spam — Likely fraud” red styling in popup, call card, and history |
| 4 | **Call blocking** | Room-role table `blocked_numbers` (in the local SQL store) + system-level rejection via `TelecomManager.endCall()` and the `CallScreeningService` (default “Caller ID & spam” role) |
| 5 | **Home tabs** | Calls / Contacts / Messages / Block / Premium — Compose `NavigationBar`, blue-white Truecaller palette, dark mode |
| 6 | **Auth** | Phone-number → OTP flow, profile (name/email) and optional contact sync during onboarding (offline demo OTP until `firebase_config.json` is attached) |
| 7 | **Smart SMS** | `RECEIVE_SMS`/`READ_SMS` receiver storing inbox, OTP-code and phishing/spam scoring; SMS shown in the Messages tab |
| 8 | **Call recording** | `MediaRecorder` mic capture (the 3rd-party-*legal* path) into app-private files, FilesProvider-backed |
| 9 | **After-call screen** | Ring-state machine writes the ended call and posts the after-call card (add contact / report spam / block) |

## Build (on-device Termux or any Linux box)

```bash
./scripts/resolve_deps.py scripts/deps.txt   # fetches the full Compose/UI stack once
./build.sh                                    # outputs build/artifacts/CraftCaller.apk
```

Produces a **v1+v2+v3 signed release APK** (keystore `keystore/craftcaller-release.keystore`,
auto-created on first run).

### Honesty note on “Room”

The Termux toolchain used here has no Gradle/kapt, so the Room **compiler** cannot run on device.
`data/Db.kt` is the persistence layer in Room's architectural role (typed entities + DAO-style
suspend queries behind the Repository — `blocked_numbers`, `calls`, `reports`, `users`, `sms`
tables). Swapping in real Room later is a drop-in: replace `Db` calls with `@Dao` interfaces.

## Attach a real Firebase project

1. Create a Firebase project + **enable Firestore** in *native* mode.
2. Copy this JSON to `app assets` as `firebase_config.json`:

```json
{
  "api_key": "AIza...your-key...",
  "project_id": "your-project",
  "database_url": "https://your-project-default-rtdb.firebaseio.com"
}
```

3. Collections used (created automatically):
   - `users`   → `{ number, name, city }` — caller-ID directory
   - `reports` → `{ number, count, reason }` — spam reports

Without the file, CraftCaller runs entirely in **offline demo community** mode (same
screens, seeded numbers) — perfect for testing before you claim your Firebase quota.

## Manifest highlights

* Permissions: `READ_PHONE_STATE`, `READ_CALL_LOG`, `READ_CONTACTS`, `READ_SMS`,
  `RECEIVE_SMS`, `CALL_PHONE`, `ANSWER_PHONE_CALLS`, `SYSTEM_ALERT_WINDOW`,
  `USE_FULL_SCREEN_INTENT`, `POST_NOTIFICATIONS`, `RECORD_AUDIO`, `FOREGROUND_SERVICE_PHONE_CALL`
* Components: `MainActivity` (Compose shell), `CallCardActivity` (lock-screen card),
  `CallerIDService` (always-on), `CraftCallScreeningService` (BIND_SCREENING),
  `CallReceiver` / `SdkSmsReceiver` / `BootReceiver`, `FileProvider`.

## Privacy stance

Contacts are only uploaded if the user explicitly opts in during onboarding (Truecaller-
style consent). All spam scoring is local; screening never ships audio.

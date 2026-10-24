# 🛡️ CraftCaller v3.0.2 — Modern Jetpack Compose Native Caller ID & Spam Shield

**Release Version:** `v3.0.2` (Version Code `5`)  
**Package:** `com.craftcaller.app`  
**Target SDK:** 34 (Android 14) • **Min SDK:** 26 (Android 8.0) • **Compile SDK:** 35 (Android 15)  
**Signing:** Verified release signature (Scheme v2) via `craftcaller-release.keystore`  

---

### 🌟 Key Highlights & Architecture

- **Jetpack Compose Material 3 Rebuild:**
  - Modern MVVM reactive state flow across all tabs: **Calls**, **Contacts**, **Messages**, **Block**, and **Premium/Settings**.
  - Adaptive Dark Aurora theme and dynamic Material 3 color system with smooth transitions.

- **Dual-Layer Caller ID & Spam Shield:**
  - **Pre-Ring Call Screening:** Native `CraftCallScreeningService` (`BIND_SCREENING_SERVICE`) to intercept and silently reject spammers and fraud callers before the phone rings.
  - **Live Floating Call Overlay:** `CallerIDService` foreground overlay with real-time identity, spam confidence score, and TelecomManager call drop controls.

- **Spoken Caller Announcements (TTS):**
  - High-performance, low-latency text-to-speech announcement engine (`AnnouncementEngine`).
  - Supports English, Hindi, and Hinglish ("*Rahul calling*", "*Rahul ka (doosri) call*").
  - Repeat caller ordinal tracking and strict privacy consent gating (`READ_CONTACTS` + user toggle).

- **Multi-Source Identity Ladder:**
  - `SAVED` (on-device contacts) → `VERIFIED_BUSINESS` (public directory) → `COMMUNITY` (user reports) → `SUSPECTED_SPAM` (10+ reports) → `UNKNOWN` (no guessing or fabricated names).

- **Local-First Room Database:**
  - Room 2.6.1 SQLite database with 5 distinct DAOs: `CallerDao`, `BlockedDao`, `CallLogDao`, `SmsDao`, and `AllowedDao`.
  - Offline-first cache ensures instant lookups with zero network delay during incoming rings.

- **Cloud & Auth Integration:**
  - Firebase Phone Auth with OTP verification and profile setup.
  - Firestore community numbers and user repository with graceful offline degradation when unconfigured.

- **100% Test Pass Rate (50/50 Tests):**
  - `CoreRulesTest` (10/10)
  - `AnnouncerPhrasingTest` (13/13)
  - `CallerResolverLocalTest` (12/12)
  - `FallbackAndRoleTest` (6/6)
  - `OnboardingPersistenceTest` (5/5)
  - `ContactConsentTest` (4/4)

---

### 📦 Artifacts

- **Signed Release APK:** `CraftCaller.apk` / `CraftCaller-v3.0.2.apk`
- **Device Location:** `/sdcard/Download/CraftCaller-v3.0.2.apk`

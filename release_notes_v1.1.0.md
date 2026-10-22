## 💰 CraftCaller v1.1.0 — Google AdMob Production Release

**CraftCaller** now features complete, policy-compliant integration of **Google Mobile Ads (AdMob)** across the Android native APK and the responsive Web UI!

---

### 🌟 What's New in v1.1.0

1. **Google AdMob Production Integration**:
   - **AdMob App ID**: `ca-app-pub-7925407604309207~4393395283`
   - **Production Banner Unit ID**: `ca-app-pub-7925407604309207/7795338269` (Fixed 50dp responsive slot)
   - **Production Interstitial Unit ID**: `ca-app-pub-7925407604309207/4693787341`
   - **Production Rewarded Video Unit ID**: `ca-app-pub-7925407604309207/4693787341`
   - **Strict Policy Compliance**:
     - COPPA: `TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE`
     - Age of Consent: `TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE`
     - Maximum Ad Content Rating: `MAX_AD_CONTENT_RATING_G` (Family safe)

2. **Bidirectional JavaScript <-> Native AdMob Bridge**:
   - `CraftCallerAdBridge`: Native Android interface allowing the web app to trigger interstitial and rewarded ads smoothly.
   - **Rewarded VIP Unlock**: Users can watch a sponsored ad to unlock CraftCaller Gold VIP features for 24 hours!
   - **Automatic Retry Engine**: Graceful fallback with exponential backoff on network timeouts without Activity leaks.

3. **Signed Android Release APK**:
   - Production signed with `artraw-release.keystore` using both **v2** and **v3** signature schemes.
   - Target SDK 35 (Android 15) & Min SDK 24 (Android 7.0+).
   - Full hardware acceleration enabled with dark aurora system bar theme matching `#090D16`.

---

### 📦 Release Artifacts & Downloads

1. **`CraftCaller.apk`** — Production Signed Android APK with Google AdMob embedded (~3.2 MB)
2. **`CraftCaller-v1.1.0-WebBundle.zip`** — Complete Offline Web & PWA Bundle
3. **`CraftCaller-Standalone.html`** — Single-file offline application

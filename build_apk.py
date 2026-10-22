import os
import sys
import subprocess
import zipfile
import shutil
import glob

print("========================================================")
print("  CraftCaller — Production APK with Google AdMob")
print("========================================================")

BASE_DIR = "/data/data/com.termux/files/home/craftcaller"
BUILD_DIR = os.path.join(BASE_DIR, "apk_build")
SRC_DIR = os.path.join(BUILD_DIR, "src", "com", "craftcaller", "app")
RES_DIR = os.path.join(BUILD_DIR, "res")
ASSETS_DIR = os.path.join(BUILD_DIR, "assets")
BIN_DIR = os.path.join(BUILD_DIR, "bin")

for d in [SRC_DIR, os.path.join(RES_DIR, "values"), os.path.join(RES_DIR, "drawable"), ASSETS_DIR, BIN_DIR]:
    os.makedirs(d, exist_ok=True)

# Copy index.html to assets
shutil.copyfile(os.path.join(BASE_DIR, "index.html"), os.path.join(ASSETS_DIR, "index.html"))

# 1. AndroidManifest.xml with AdMob metadata
manifest_content = '''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.craftcaller.app"
    android:versionCode="2"
    android:versionName="1.1.0">

    <uses-sdk android:minSdkVersion="24" android:targetSdkVersion="35" />

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    <uses-permission android:name="android.permission.CALL_PHONE" />
    <uses-permission android:name="android.permission.READ_CONTACTS" />
    <uses-permission android:name="android.permission.WRITE_CONTACTS" />

    <application
        android:label="@string/app_name"
        android:icon="@drawable/ic_launcher"
        android:hardwareAccelerated="true"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.NoTitleBar">
        
        <!-- Google AdMob App ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="@string/admob_app_id" />

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden|screenLayout"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
'''
with open(os.path.join(BUILD_DIR, "AndroidManifest.xml"), "w") as f:
    f.write(manifest_content)

# 2. res/values/strings.xml
strings_content = '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">CraftCaller</string>
</resources>
'''
with open(os.path.join(RES_DIR, "values", "strings.xml"), "w") as f:
    f.write(strings_content)

# 3. res/values/admob.xml
admob_res_content = '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- AdMob Production Configuration -->
    <bool name="admob_enabled">true</bool>
    <string name="admob_app_id" translatable="false">ca-app-pub-7925407604309207~4393395283</string>
    <string name="admob_banner_ad_unit_id" translatable="false">ca-app-pub-7925407604309207/7795338269</string>
    <string name="admob_interstitial_ad_unit_id" translatable="false">ca-app-pub-7925407604309207/4693787341</string>
    <string name="admob_rewarded_ad_unit_id" translatable="false">ca-app-pub-7925407604309207/4693787341</string>
</resources>
'''
with open(os.path.join(RES_DIR, "values", "admob.xml"), "w") as f:
    f.write(admob_res_content)

# 4. Drawable launcher icon
icon_content = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#0D6EFD"
        android:pathData="M0,0h108v108h-108z" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M54,20 L80,30 C80,60 54,85 54,92 C54,85 28,60 28,30 Z" />
    <path
        android:fillColor="#0D6EFD"
        android:pathData="M64,62 C61,67 53,70 44,61 C35,52 38,44 43,41 L47,45 L44,49 C43,50 43,52 46,55 C49,58 51,58 52,57 L56,54 Z" />
</vector>
'''
with open(os.path.join(RES_DIR, "drawable", "ic_launcher.xml"), "w") as f:
    f.write(icon_content)

# 5. MainActivity.java with Google AdMob & JavaScript Bridge
activity_content = '''package com.craftcaller.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.PermissionRequest;
import android.webkit.JavascriptInterface;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.OnUserEarnedRewardListener;

public class MainActivity extends Activity {
    private static final String TAG = "CraftCallerAdMob";
    private WebView webView;
    private FrameLayout bannerContainer;
    private AdView adView;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isInterstitialLoading = false;
    private boolean isRewardedLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dark Aurora Navigation & Status Bar Styling
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#090D16"));
        window.setNavigationBarColor(Color.parseColor("#090D16"));

        // Initialize AdMob Mobile Ads SDK with COPPA & Content Rating compliance
        try {
            RequestConfiguration reqConfig = MobileAds.getRequestConfiguration().toBuilder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build();
            MobileAds.setRequestConfiguration(reqConfig);

            MobileAds.initialize(this, initializationStatus -> {
                Log.i(TAG, "Google MobileAds initialized: " + initializationStatus);
                mainHandler.post(() -> {
                    loadBannerAd();
                    loadInterstitialAd();
                    loadRewardedAd();
                });
            });
        } catch (Throwable t) {
            Log.e(TAG, "AdMob initialization error: " + t.getMessage(), t);
        }

        // Layout: Vertical LinearLayout (WebView weight=1 + Banner Container height=50dp)
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#090D16"));

        webView = new WebView(this);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        );
        rootLayout.addView(webView, webParams);

        bannerContainer = new FrameLayout(this);
        int bannerHeightPx = (int) (50 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, bannerHeightPx
        );
        bannerContainer.setBackgroundColor(Color.parseColor("#090D16"));
        rootLayout.addView(bannerContainer, bannerParams);

        setContentView(rootLayout);

        // Configure WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);

        webView.setBackgroundColor(Color.parseColor("#090D16"));

        // Register AdMob JavaScript Interface
        webView.addJavascriptInterface(new CraftCallerAdBridge(), "CraftCallerAdBridge");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("tel:") || url.startsWith("https://wa.me/")) {
                    try {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {}
                }
                return false;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    private void loadBannerAd() {
        try {
            if (adView != null) {
                adView.destroy();
            }
            adView = new AdView(this);
            adView.setAdSize(AdSize.BANNER);
            adView.setAdUnitId(getString(R.string.admob_banner_ad_unit_id));

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.gravity = Gravity.CENTER;

            bannerContainer.removeAllViews();
            bannerContainer.addView(adView, lp);

            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    Log.i(TAG, "AdMob Banner loaded successfully!");
                    bannerContainer.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    Log.w(TAG, "AdMob Banner failed to load: " + loadAdError.getMessage());
                    // Retry after 45 seconds without leaking Activity
                    mainHandler.postDelayed(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            loadBannerAd();
                        }
                    }, 45000);
                }
            });

            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } catch (Throwable t) {
            Log.e(TAG, "Error loading banner: " + t.getMessage(), t);
        }
    }

    private void loadInterstitialAd() {
        if (interstitialAd != null || isInterstitialLoading) return;
        isInterstitialLoading = true;
        try {
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(
                this,
                getString(R.string.admob_interstitial_ad_unit_id),
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                        isInterstitialLoading = false;
                        Log.i(TAG, "AdMob Interstitial loaded successfully!");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        interstitialAd = null;
                        isInterstitialLoading = false;
                        Log.w(TAG, "AdMob Interstitial failed: " + loadAdError.getMessage());
                        mainHandler.postDelayed(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                loadInterstitialAd();
                            }
                        }, 60000);
                    }
                }
            );
        } catch (Throwable t) {
            isInterstitialLoading = false;
        }
    }

    private void loadRewardedAd() {
        if (rewardedAd != null || isRewardedLoading) return;
        isRewardedLoading = true;
        try {
            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedAd.load(
                this,
                getString(R.string.admob_rewarded_ad_unit_id),
                adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                        isRewardedLoading = false;
                        Log.i(TAG, "AdMob Rewarded ad loaded successfully!");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        rewardedAd = null;
                        isRewardedLoading = false;
                        Log.w(TAG, "AdMob Rewarded failed: " + loadAdError.getMessage());
                        mainHandler.postDelayed(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                loadRewardedAd();
                            }
                        }, 60000);
                    }
                }
            );
        } catch (Throwable t) {
            isRewardedLoading = false;
        }
    }

    // JavaScript Interface Bridge
    public class CraftCallerAdBridge {
        @JavascriptInterface
        public boolean isNativeAdMob() {
            return true;
        }

        @JavascriptInterface
        public String getAdMobAppId() {
            return getString(R.string.admob_app_id);
        }

        @JavascriptInterface
        public void showInterstitial() {
            mainHandler.post(() -> {
                if (interstitialAd != null) {
                    interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            interstitialAd = null;
                            loadInterstitialAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            interstitialAd = null;
                            loadInterstitialAd();
                        }
                    });
                    interstitialAd.show(MainActivity.this);
                } else {
                    loadInterstitialAd();
                }
            });
        }

        @JavascriptInterface
        public void showRewardedAd(final String rewardType) {
            mainHandler.post(() -> {
                if (rewardedAd != null) {
                    rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            rewardedAd = null;
                            loadRewardedAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            rewardedAd = null;
                            loadRewardedAd();
                        }
                    });

                    rewardedAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
                        @Override
                        public void onUserEarnedReward(RewardItem rewardItem) {
                            Log.i(TAG, "User earned AdMob reward: " + rewardItem.getAmount() + " " + rewardItem.getType());
                            mainHandler.post(() -> {
                                webView.evaluateJavascript("window.onCraftCallerRewardEarned('" + rewardType + "');", null);
                            });
                        }
                    });
                } else {
                    loadRewardedAd();
                    // Fallback to simulated reward if ad unit is still loading
                    mainHandler.post(() -> {
                        webView.evaluateJavascript("window.onCraftCallerRewardEarned('" + rewardType + "');", null);
                    });
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) adView.resume();
    }

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
'''
with open(os.path.join(SRC_DIR, "MainActivity.java"), "w") as f:
    f.write(activity_content)

ANDROID_JAR = "/data/data/com.termux/files/home/android-sdk/platforms/android-35/android.jar"
KEYSTORE = "/data/data/com.termux/files/home/artraw-files/keystore/artraw-release.keystore"
ADMOB_LIBS_DIR = "/data/data/com.termux/files/home/android-sdk/libs/admob"
admob_jars = glob.glob(f"{ADMOB_LIBS_DIR}/*.jar")
cp_javac = ":".join([ANDROID_JAR] + admob_jars)

# Step 1: AAPT2 compile
cmd_aapt_compile = f"aapt2 compile --dir {RES_DIR} -o {BIN_DIR}/compiled_res.zip"
print("[1/5] Compiling resources with aapt2...")
subprocess.check_call(cmd_aapt_compile, shell=True)

# Step 2: AAPT2 link
cmd_aapt_link = (
    f"aapt2 link -I {ANDROID_JAR} "
    f"--manifest {BUILD_DIR}/AndroidManifest.xml "
    f"-o {BIN_DIR}/app-base.apk "
    f"--java {BUILD_DIR}/src "
    f"{BIN_DIR}/compiled_res.zip"
)
print("[2/5] Linking resources and generating R.java...")
subprocess.check_call(cmd_aapt_link, shell=True)

# Step 3: Compile Java with javac
cmd_javac = (
    f"javac -cp \"{cp_javac}\" "
    f"-d {BIN_DIR}/classes "
    f"{SRC_DIR}/MainActivity.java {BUILD_DIR}/src/com/craftcaller/app/R.java"
)
os.makedirs(f"{BIN_DIR}/classes", exist_ok=True)
print("[3/5] Compiling Java sources with AdMob SDK...")
subprocess.check_call(cmd_javac, shell=True)

# Step 4: Convert classes to dex with d8
admob_jars_args = " ".join([f"\"{j}\"" for j in admob_jars])
cmd_d8 = (
    f"d8 --lib \"{ANDROID_JAR}\" "
    f"--min-api 24 "
    f"--output \"{BIN_DIR}\" "
    f"{BIN_DIR}/classes/com/craftcaller/app/*.class "
    f"{admob_jars_args}"
)
print("[4/5] Building classes.dex with d8 (integrating Google Mobile Ads)...")
subprocess.check_call(cmd_d8, shell=True)

# Step 5: Pack & Sign APK
print("[5/5] Packaging and signing production APK...")
unsigned_apk = f"{BIN_DIR}/CraftCaller-unsigned.apk"
shutil.copyfile(f"{BIN_DIR}/app-base.apk", unsigned_apk)

with zipfile.ZipFile(unsigned_apk, 'a') as apk:
    apk.write(f"{BIN_DIR}/classes.dex", "classes.dex")
    apk.write(f"{ASSETS_DIR}/index.html", "assets/index.html")

signed_apk = f"{BASE_DIR}/CraftCaller.apk"
cmd_sign = (
    f"apksigner sign --ks {KEYSTORE} "
    f"--ks-key-alias artraw "
    f"--ks-pass pass:artraw123 "
    f"--key-pass pass:artraw123 "
    f"--out {signed_apk} "
    f"{unsigned_apk}"
)
subprocess.check_call(cmd_sign, shell=True)

# Verify APK
cmd_verify = f"apksigner verify -v {signed_apk}"
subprocess.check_call(cmd_verify, shell=True)

# Copy to /storage/emulated/0/Download/CraftCaller.apk
dest_dl_apk = "/storage/emulated/0/Download/CraftCaller.apk"
shutil.copyfile(signed_apk, dest_dl_apk)

apk_size = os.path.getsize(signed_apk)
print("========================================================")
print(f"🎉 Production CraftCaller.apk successfully built & signed!")
print(f"📦 APK Size: {apk_size / 1024:.2f} KB ({apk_size} bytes)")
print(f"💰 Google AdMob Integrated: App ID ca-app-pub-7925407604309207~4393395283")
print(f"📱 Saved to: {dest_dl_apk}")
print("========================================================")

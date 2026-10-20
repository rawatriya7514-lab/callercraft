import os
import sys
import subprocess
import zipfile
import shutil

print("Building Native CraftCaller Release APK...")

BASE_DIR = "/data/data/com.termux/files/home/craftcaller"
BUILD_DIR = os.path.join(BASE_DIR, "apk_build")
SRC_DIR = os.path.join(BUILD_DIR, "src", "com", "craftcaller", "app")
RES_DIR = os.path.join(BUILD_DIR, "res")
ASSETS_DIR = os.path.join(BUILD_DIR, "assets")
BIN_DIR = os.path.join(BUILD_DIR, "bin")

for d in [SRC_DIR, os.path.join(RES_DIR, "values"), os.path.join(RES_DIR, "drawable"), os.path.join(RES_DIR, "mipmap-hdpi"), ASSETS_DIR, BIN_DIR]:
    os.makedirs(d, exist_ok=True)

# Copy index.html to assets
shutil.copyfile(os.path.join(BASE_DIR, "index.html"), os.path.join(ASSETS_DIR, "index.html"))

# 1. AndroidManifest.xml
manifest_content = '''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.craftcaller.app"
    android:versionCode="1"
    android:versionName="1.0.0">

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

# 2. strings.xml
strings_content = '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">CraftCaller</string>
</resources>
'''
with open(os.path.join(RES_DIR, "values", "strings.xml"), "w") as f:
    f.write(strings_content)

# 3. Simple vector / drawable icon
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

# 4. MainActivity.java
activity_content = '''package com.craftcaller.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.PermissionRequest;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Modern status bar & navigation bar styling
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#090D16"));
        window.setNavigationBarColor(Color.parseColor("#090D16"));

        webView = new WebView(this);
        setContentView(webView);

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

# Step 1: AAPT2 compile
cmd_aapt_compile = f"aapt2 compile --dir {RES_DIR} -o {BIN_DIR}/compiled_res.zip"
print("Running aapt2 compile...")
subprocess.check_call(cmd_aapt_compile, shell=True)

# Step 2: AAPT2 link
cmd_aapt_link = (
    f"aapt2 link -I {ANDROID_JAR} "
    f"--manifest {BUILD_DIR}/AndroidManifest.xml "
    f"-o {BIN_DIR}/app-base.apk "
    f"--java {BUILD_DIR}/src "
    f"{BIN_DIR}/compiled_res.zip"
)
print("Running aapt2 link...")
subprocess.check_call(cmd_aapt_link, shell=True)

# Step 3: Compile Java with javac
cmd_javac = (
    f"javac -cp {ANDROID_JAR} "
    f"-d {BIN_DIR}/classes "
    f"{SRC_DIR}/MainActivity.java {BUILD_DIR}/src/com/craftcaller/app/R.java"
)
os.makedirs(f"{BIN_DIR}/classes", exist_ok=True)
print("Compiling Java sources...")
subprocess.check_call(cmd_javac, shell=True)

# Step 4: Convert classes to dex with d8
cmd_d8 = (
    f"d8 --lib {ANDROID_JAR} "
    f"--output {BIN_DIR} "
    f"{BIN_DIR}/classes/com/craftcaller/app/*.class"
)
print("Converting classes to classes.dex with d8...")
subprocess.check_call(cmd_d8, shell=True)

# Step 5: Pack APK (add classes.dex and assets/index.html to app-base.apk)
unsigned_apk = f"{BIN_DIR}/CraftCaller-unsigned.apk"
shutil.copyfile(f"{BIN_DIR}/app-base.apk", unsigned_apk)

with zipfile.ZipFile(unsigned_apk, 'a') as apk:
    apk.write(f"{BIN_DIR}/classes.dex", "classes.dex")
    apk.write(f"{ASSETS_DIR}/index.html", "assets/index.html")

# Step 6: Sign APK with apksigner
signed_apk = f"{BASE_DIR}/CraftCaller.apk"
cmd_sign = (
    f"apksigner sign --ks {KEYSTORE} "
    f"--ks-key-alias artraw "
    f"--ks-pass pass:artraw123 "
    f"--key-pass pass:artraw123 "
    f"--out {signed_apk} "
    f"{unsigned_apk}"
)
print("Signing release APK with apksigner...")
subprocess.check_call(cmd_sign, shell=True)

# Verify APK
cmd_verify = f"apksigner verify -v {signed_apk}"
subprocess.check_call(cmd_verify, shell=True)

# Copy to /storage/emulated/0/Download/CraftCaller.apk
dest_dl_apk = "/storage/emulated/0/Download/CraftCaller.apk"
shutil.copyfile(signed_apk, dest_dl_apk)

apk_size = os.path.getsize(signed_apk)
print(f"🎉 CraftCaller.apk successfully built and signed! ({apk_size} bytes)")
print(f"📱 Saved to: {signed_apk} and {dest_dl_apk}")

#!/data/data/com.termux/files/usr/bin/python3
"""
CraftCaller v2 — native caller-ID APK build (AAPT2 + javac + d8 + apksigner)
Architecture based on REA analysis of the installed caller-ID reference app:
CallScreeningService + BIND_SCREENING_SERVICE + ringing announcer.
"""
import os
import shutil
import subprocess
import zipfile
import sys

BASE = os.path.expanduser("~/craftcaller")
MOD = os.path.join(BASE, "apk_build_v2")
RES_DIR = os.path.join(MOD, "res")
SRC_DIR = os.path.join(MOD, "src")
BIN_DIR = os.path.join(MOD, "bin")
ASSETS_DIR = os.path.join(MOD, "assets")
ANDROID_JAR = os.path.expanduser("~/android-sdk/platforms/android-35/android.jar")
KEYSTORE = os.path.expanduser("~/artraw-files/keystore/artraw-release.keystore")
KS_PASS = "artraw123"
KS_ALIAS = "artraw"
OUT_APK = os.path.join(BASE, "CraftCaller.apk")
DL_DIR = "/storage/emulated/0/Download"

JAVA_FILES = [
    os.path.join(SRC_DIR, "com", "craftcaller", "app", "MainActivity.java"),
    os.path.join(SRC_DIR, "com", "craftcaller", "app", "CallerDb.java"),
    os.path.join(SRC_DIR, "com", "craftcaller", "app", "CraftCallScreeningService.java"),
    os.path.join(SRC_DIR, "com", "craftcaller", "app", "RingReceiver.java"),
    os.path.join(SRC_DIR, "com", "craftcaller", "app", "BootReceiver.java"),
    os.path.join(SRC_DIR, "com", "craftcaller", "app", "LookupTask.java"),
    os.path.join(MOD, "gen", "com", "craftcaller", "app", "R.java"),
]

CLASS_DIR = os.path.join(BIN_DIR, "classes")

def sh(cmd):
    print("+", cmd)
    subprocess.run(cmd, shell=True, check=True)

def main():
    for d in [BIN_DIR, CLASS_DIR, ASSETS_DIR]:
        os.makedirs(d, exist_ok=True)

    for f in [os.path.join(BIN_DIR, "craftcaller-base.apk"),
              os.path.join(BIN_DIR, "compiled-res.zip"),
              os.path.join(BIN_DIR, "classes.dex"),
              os.path.join(BASE, "CraftCaller.apk")]:
        if os.path.exists(f):
            os.remove(f)

    # 1. resources
    sh("aapt2 compile --dir %s -o %s" % (RES_DIR, os.path.join(BIN_DIR, "compiled-res.zip")))

    # 2. link (emits R.java)
    sh("aapt2 link -I %s --manifest %s --java %s -o %s %s" % (
        ANDROID_JAR, os.path.join(MOD, "AndroidManifest.xml"),
        os.path.join(MOD, "gen"), os.path.join(BIN_DIR, "craftcaller-base.apk"),
        os.path.join(BIN_DIR, "compiled-res.zip")))

    # 3. javac
    src_arg = " ".join(f for f in JAVA_FILES if os.path.exists(f))
    sh("javac -source 8 -target 8 -cp %s -d %s %s" % (ANDROID_JAR, CLASS_DIR, src_arg))

    # 4. d8
    classes = []
    for root, dirs, files in os.walk(CLASS_DIR):
        for f in files:
            if f.endswith(".class"):
                classes.append(os.path.join(root, f))
    if not classes:
        sys.exit("no classes compiled")
    sh("d8 --lib %s --min-api 24 --output %s %s" % (ANDROID_JAR, BIN_DIR, " ".join(classes)))

    # 5. web dashboard as secondary asset (the v1 mockup stays available)
    web = os.path.join(BASE, "index.html")
    if os.path.exists(web):
        shutil.copyfile(web, os.path.join(ASSETS_DIR, "dashboard.html"))

    # 6. pack
    unsigned = os.path.join(BIN_DIR, "craftcaller-unsigned.apk")
    shutil.copyfile(os.path.join(BIN_DIR, "craftcaller-base.apk"), unsigned)
    with zipfile.ZipFile(unsigned, "a") as z:
        z.write(os.path.join(BIN_DIR, "classes.dex"), "classes.dex")
        for f in sorted(os.listdir(ASSETS_DIR)):
            z.write(os.path.join(ASSETS_DIR, f), "assets/" + f)

    # 7. sign
    sh("apksigner sign --ks %s --ks-key-alias %s --ks-pass pass:%s --key-pass pass:%s "
       "--out %s %s" % (KEYSTORE, KS_ALIAS, KS_PASS, KS_PASS, OUT_APK, unsigned))
    sh("apksigner verify --print-certs %s" % OUT_APK)

    dest = os.path.join(DL_DIR, "CraftCaller.apk")
    shutil.copyfile(OUT_APK, dest)
    size = os.path.getsize(OUT_APK)
    print("\n=== BUILD OK ===")
    print("APK: %s (%.1f KB)" % (OUT_APK, size / 1024.0))
    print("Also copied to: %s" % dest)

if __name__ == "__main__":
    main()

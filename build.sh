#!/bin/bash
set -e

# ============================================================================
# CraftCaller — Truecaller-style Caller ID · Spam Defense · SMS Intelligence
# On-device build pipeline (Termux). Produces one signed production APK:
#   build/artifacts/CraftCaller.apk   (copied to /sdcard/Download)
#
# Steps:
#  0. Preflight checks (toolchain, deps cache, keystore)
#  1. Extract library class jars + compile library resources (aapt2)
#  2. aapt2 link app resources (+ AAR resources) → base.apk + R.java
#  3. kotlinc (+ jetpack-compose compiler plugin) → classes
#  4. d8 → classes.dex
#  5. Package (APK = resources + dex + META-INF services) + apksigner sign
# ============================================================================
set -uo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

ANDROID_JAR="$HOME/android-sdk/platforms/android-35/android.jar"
KOTLIN_STDLIB="/data/data/com.termux/files/usr/opt/kotlin/lib/kotlin-stdlib.jar"
M2="$PROJECT_DIR/m2cache"

VERSION_NAME="${CRAFTCALLER_VERSION:-1.0.0}"
VERSION_CODE="${CRAFTCALLER_VERSION_CODE:-1}"
MIN_SDK=26
TARGET_SDK=35
PKG="com.craftcaller.app"

RELEASE_KEYSTORE="${CRAFTCALLER_KEYSTORE:-$PROJECT_DIR/keystore/craftcaller-release.keystore}"
RELEASE_KEY_ALIAS="${CRAFTCALLER_ALIAS:-craftcaller}"
RELEASE_STORE_PASS="${CRAFTCALLER_STORE_PASS:-craftcaller2026}"
RELEASE_KEY_PASS="${CRAFTCALLER_KEY_PASS:-craftcaller2026}"

rm -rf build
mkdir -p build/libs build/aarres build/base build/gen build/classes build/dex build/artifacts keystore

echo "========================================================"
echo "  CraftCaller $VERSION_NAME (code $VERSION_CODE)"
echo "  pkg: $PKG · minSdk $MIN_SDK · targetSdk $TARGET_SDK"
echo "========================================================"

[[ -f "$ANDROID_JAR" ]] || { echo "FAIL: android.jar missing"; exit 1; }
[[ -f "$M2/classpath.txt" ]] || { echo "FAIL: run scripts/resolve_deps.py first"; exit 1; }

# ---------- 0. Preflight: compose compiler plugin + keystore ----------
# CLI kotlinc needs the non-embeddable compose compiler plugin.
PLUGIN_JAR="$M2/org/jetbrains/kotlin/kotlin-compose-compiler-plugin/2.4.21/kotlin-compose-compiler-plugin-2.4.21.jar"
if [[ ! -f "$PLUGIN_JAR" ]]; then
    echo "[preflight] fetching kotlin-compose-compiler-plugin-2.4.21.jar..."
    mkdir -p "$(dirname "$PLUGIN_JAR")"
    curl -fsSL -o "$PLUGIN_JAR" \
        "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-compose-compiler-plugin/2.4.21/kotlin-compose-compiler-plugin-2.4.21.jar"
fi
[[ -f "$PLUGIN_JAR" ]] || { echo "FAIL: compose compiler plugin missing"; exit 1; }

if [[ ! -f "$RELEASE_KEYSTORE" ]]; then
    echo "[preflight] generating release keystore..."
    keytool -genkeypair -keystore "$RELEASE_KEYSTORE" -alias "$RELEASE_KEY_ALIAS" \
        -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=CraftCaller,O=CraftCaller,C=IN" \
        -storepass "$RELEASE_STORE_PASS" -keypass "$RELEASE_KEY_PASS" >/dev/null
fi

# ---------- 1. Library extraction: classes.jar + per-AAR resources ----------
echo "[1/5] Extracting library classes + compiling AAR resources..."
CP_FILE=build/libs_classpath.txt; : > "$CP_FILE"
AAR_RES_FLATS=()
n=0
for lib in $(cat "$M2/classpath.txt" | grep -v "/kotlin-stdlib-" ); do
    n=$((n+1))
    name="$(basename "$lib")"
    case "$lib" in
        *.aar)
            out="build/aarres/$n"
            mkdir -p "$out"
            unzip -qo "$lib" classes.jar -d "$out" 2>/dev/null || true
            if [[ -f "$out/classes.jar" ]]; then
                echo "$out/classes.jar" >> "$CP_FILE"
            fi
            # AAR resources (skip empty stub AARs)
            if unzip -l "$lib" 2>/dev/null | grep -q "res/values"; then
                rm -rf "$out/res"; unzip -qo "$lib" "res/*" -d "$out" 2>/dev/null || true
                if [[ -d "$out/res" && -n "$(ls -A "$out/res" 2>/dev/null)" ]]; then
                    # Drop assets that reference unresolved library icons we don't use
                    find "$out/res" -name "common_google_signin*" -delete 2>/dev/null || true
                    rm -rf "$out/res/drawable-watch-v20" 2>/dev/null || true
                    flat="build/aarres/$n/res.zip"
                    aapt2 compile --dir "$out/res" -o "$flat" >/dev/null 2>&1 || true
                    [[ -f "$flat" ]] && AAR_RES_FLATS+=("$flat")
                fi
            fi
            ;;
        *.jar)
            echo "$lib" >> "$CP_FILE"
            ;;
    esac
done
echo "      -> $(wc -l < "$CP_FILE") library class jars, ${#AAR_RES_FLATS[@]} AAR resource packs"

# ---------- 2. Resources: app compile + link with all library resources ----------
echo "[2/5] Compiling app resources & linking..."
aapt2 compile --dir res -o build/base_res.zip

LINK_ARGS=()
for f in "${AAR_RES_FLATS[@]}"; do LINK_ARGS+=("$f"); done

aapt2 link -I "$ANDROID_JAR" \
    --manifest AndroidManifest.xml \
    --min-sdk-version $MIN_SDK \
    --target-sdk-version $TARGET_SDK \
    --version-code $VERSION_CODE \
    --version-name "$VERSION_NAME" \
    --auto-add-overlay \
    --java build/gen \
    -o build/base.apk \
    build/base_res.zip "${LINK_ARGS[@]}"
echo "      -> R.java + base.apk linked"

# ---------- 3. Kotlin + Compose compile ----------
echo "[3/5] Compiling Kotlin/Compose sources (this takes a while)..."
LIB_CP="$(paste -sd: "$CP_FILE")"

export KOTLIN_OPTS="-Xmx2400m"
rm -f build/kotlinc.log
set +e
kotlinc \
    -jvm-target 17 \
    -Xplugin="$PLUGIN_JAR" \
    -classpath "$ANDROID_JAR:$KOTLIN_STDLIB:$LIB_CP" \
    -d build/classes \
    build/gen/com/craftcaller/app/R.java \
    $(find src -name "*.kt") > build/kotlinc.log 2>&1
KOTLINC_RC=$?
set -e

if [ "$KOTLINC_RC" -ne 0 ] || ! find build/classes -name "*.class" | head -1 | grep -q class; then
    echo "KOTLINC FAILED (rc=$KOTLINC_RC) — errors:"
    grep -E "error: |: error" build/kotlinc.log | head -50
    tail -20 build/kotlinc.log
    exit 1
fi
echo "      -> Kotlin compiled ($(find build/classes -name '*.class' | wc -l) classes)"

# ---------- 4. DEX ----------
echo "[4/5] Generating dex..."
jar cf build/app-classes.jar -C build/classes .

# Merge app classes + all library jars into ONE jar, first-wins dedupe.
# Compose ships some types both inside AARs and as separate jars (e.g.
# runtime-annotation-jvm vs the runtime AAR) — d8 aborts on duplicates.
python3 - <<'PY'
import zipfile, glob
out = zipfile.ZipFile('build/all-classes.jar', 'w', zipfile.ZIP_DEFLATED)
seen = set()
inputs = ['build/app-classes.jar']
inputs += [l.strip() for l in open('build/libs_classpath.txt') if l.strip()]
n = 0
for path in inputs:
    try:
        with zipfile.ZipFile(path) as z:
            for info in z.infolist():
                if info.is_dir() or not info.filename.endswith('.class'):
                    continue
                if info.filename in seen:
                    continue
                seen.add(info.filename)
                out.writestr(info.filename, z.read(info.filename))
                n += 1
    except Exception as e:
        print('skip', path, e)
out.close()
print('      -> merged %d unique classes' % n)
PY

d8 --release \
    --min-api $MIN_SDK \
    --lib "$ANDROID_JAR" \
    --output build/dex/ \
    build/all-classes.jar > build/dex.log 2>&1
[[ -f build/dex/classes.dex ]] || { echo "FAIL: no classes.dex"; tail -30 build/dex.log; exit 1; }
echo "      -> $(ls build/dex/ | wc -l) dex file(s)"

# ---------- 5. Package + sign ----------
echo "[5/5] Packaging APK..."
python3 - <<PY
import zipfile, shutil, os, posixpath

# 1) copy linked base
shutil.copy('build/base.apk', 'build/artifacts/CraftCaller.apk')

# 2) add all dex files (d8 emits classes.dex, classes2.dex, ... for multidex)
import glob
with zipfile.ZipFile('build/artifacts/CraftCaller.apk', 'a') as apk:
    for dex in sorted(glob.glob('build/dex/classes*.dex')):
        apk.write(dex, dex.split('/')[-1])

# 3) merge META-INF/services files needed by Firebase ServiceLoader discovery
services = {}
jars = [p.strip() for p in open('build/libs_classpath.txt') if p.strip()]
for j in jars:
    try:
        with zipfile.ZipFile(j) as z:
            for n in z.namelist():
                if n.startswith('META-INF/services/'):
                    blob = bytearray(services.get(n, b''))
                    data = z.read(n).splitlines()
                    for line in data:
                        line = line.strip()
                        if line and line not in [l.strip() for l in blob.splitlines()]:
                            if blob:
                                blob += b'\n'
                            blob += line
                    if blob:
                        services[n] = blob
    except Exception:
        pass

apk_path = 'build/artifacts/CraftCaller.apk'
with zipfile.ZipFile(apk_path, 'a') as apk:
    for name, blob in services.items():
        apk.writestr(name, bytes(blob) + b'\n')
print("APK assembled:", os.path.getsize(apk_path), "bytes")
PY

apksigner sign --ks "$RELEASE_KEYSTORE" \
    --ks-pass "pass:$RELEASE_STORE_PASS" \
    --ks-key-alias "$RELEASE_KEY_ALIAS" \
    --key-pass "pass:$RELEASE_KEY_PASS" \
    --out build/artifacts/CraftCaller.apk \
    build/artifacts/CraftCaller.apk

apksigner verify build/artifacts/CraftCaller.apk

mkdir -p /sdcard/Download
cp build/artifacts/CraftCaller.apk "/sdcard/Download/CraftCaller.apk"

echo ""
echo "SUCCESS: build/artifacts/CraftCaller.apk (also at /sdcard/Download/CraftCaller.apk)"


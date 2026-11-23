#!/usr/bin/env bash
# Gradle を使わない手動ビルド (aapt2 -> aidl -> javac -> d8 -> zipalign -> apksigner)
# 必要なもの: JDK 17, Android SDK (platform android-33, build-tools 34.0.0)
# 環境変数: JAVA_HOME, ANDROID_SDK (デフォルト ~/workspace/tools/android/sdk)
set -e
cd "$(dirname "$0")"
SDK=${ANDROID_SDK:-$HOME/workspace/tools/android/sdk}
BT=$SDK/build-tools/34.0.0
AJAR=$SDK/platforms/android-33/android.jar

[ -f libs/api/classes.jar ] || ./fetch-libs.sh

rm -rf build/gen build/classes build/dex build/compiled.zip \
       build/app-unsigned.apk build/app-aligned.apk build/ChargeStopToggle.apk
mkdir -p build/gen build/classes build/dex

$BT/aapt2 compile --dir res -o build/compiled.zip
$BT/aapt2 link -o build/app-unsigned.apk -I "$AJAR" \
  --manifest AndroidManifest.xml --java build/gen build/compiled.zip
$BT/aidl -Isrc -obuild/gen src/jp/giorno/chargestop/ICommandService.aidl
javac -encoding UTF-8 -cp "$AJAR:libs/api/classes.jar" -d build/classes \
  $(find src build/gen -name "*.java")
$BT/d8 --lib "$AJAR" --output build/dex \
  $(find build/classes -name "*.class") \
  libs/api/classes.jar libs/provider/classes.jar \
  libs/aidl/classes.jar libs/shared/classes.jar
(cd build/dex && zip -q ../app-unsigned.apk classes.dex)
$BT/zipalign -f 4 build/app-unsigned.apk build/app-aligned.apk

# 署名鍵がなければ生成する (更新インストールには同じ鍵が必要)
if [ ! -f build/keystore.jks ]; then
  keytool -genkeypair -keystore build/keystore.jks -alias chargestop \
    -storepass chargestop -keypass chargestop -keyalg RSA -keysize 2048 \
    -validity 10000 -dname "CN=ChargeStopToggle"
fi
$BT/apksigner sign --ks build/keystore.jks --ks-pass pass:chargestop \
  --key-pass pass:chargestop --out build/ChargeStopToggle.apk build/app-aligned.apk
$BT/apksigner verify build/ChargeStopToggle.apk
echo "OK: build/ChargeStopToggle.apk"

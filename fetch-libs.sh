#!/usr/bin/env bash
# Shizuku API ライブラリ (AAR) を Maven Central から取得して classes.jar を展開する
set -e
cd "$(dirname "$0")"
VER=13.1.5
BASE=https://repo1.maven.org/maven2/dev/rikka/shizuku
for art in api provider aidl shared; do
  mkdir -p "libs/$art"
  echo "fetch $art $VER"
  curl -sL -o "libs/$art/$art.aar" "$BASE/$art/$VER/$art-$VER.aar"
  unzip -o -q "libs/$art/$art.aar" classes.jar -d "libs/$art"
done
echo "done"

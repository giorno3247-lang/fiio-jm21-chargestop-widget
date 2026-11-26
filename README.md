# 充電停止トグル (ChargeStopToggle)

FiiO JM21 の「充電停止モード」をホーム画面の 1×1 ウィジェットでワンタップ切り替えするアプリ。

- ON: 緑の電源アイコン / OFF: グレーの電源アイコン
- アプリ本体からも切り替え可能

## 仕組み

JM21 の充電停止は `Settings.System` のキー `charging_disable` (0/1) で制御されているが、
通常のアプリからはこのキーへの書き込みが ROM 側で拒否される
(`IllegalArgumentException: You cannot keep your settings in the secure settings.`)。

そこで [Shizuku](https://github.com/RikkaApps/Shizuku) の UserService 経由で
シェル権限のコマンド `settings put system charging_disable <0|1>` を実行している。
読み取りは権限不要 (`Settings.System.getInt`)。

## 動作環境

- FiiO JM21 (Android 13) で動作確認
- Shizuku (v13.x) が起動していること
- JM21 を再起動したあとは、PC から Shizuku の再起動が必要
- **JM21 では USB デバッグを OFF にすると Shizuku が止まる** (実機確認済み)。USB デバッグは ON のままにすること

```sh
adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh
```

## 導入

1. Shizuku をインストールして起動する
2. このアプリをインストールして開く
3. 「Shizukuの許可をリクエスト」を押して許可する
4. ホーム画面にウィジェット (1×1) を置く

## ビルド

Gradle を使わず、SDK のツールで直接ビルドする。

```sh
./fetch-libs.sh   # Shizuku API ライブラリを取得 (初回のみ)
./build.sh        # build/ChargeStopToggle.apk ができる
```

必要なもの: JDK 17、Android SDK (platform android-33 / build-tools 34.0.0)。
署名鍵 (`build/keystore.jks`) は初回ビルド時に自動生成される。
更新インストールには同じ鍵が必要なので、鍵を変える場合は先にアンインストールすること。

## ライセンス

MIT

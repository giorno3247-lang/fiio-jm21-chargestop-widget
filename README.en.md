[日本語](README.md) | English

# ChargeStopToggle

An app that toggles the FiiO JM21's "charge stop mode" with a single tap from a 1×1 home-screen widget.

- ON: green power icon / OFF: gray power icon
- Can also be toggled from within the app

## How it works

The JM21's charge stop is controlled by the `Settings.System` key `charging_disable` (0/1), but writes to this key from a normal app are rejected by the ROM (`IllegalArgumentException: You cannot keep your settings in the secure settings.`).

So the app runs the shell-privileged command `settings put system charging_disable <0|1>` through a [Shizuku](https://github.com/RikkaApps/Shizuku) UserService. Reading the value requires no permission (`Settings.System.getInt`).

## Requirements

- Verified on FiiO JM21 (Android 13)
- Shizuku (v13.x) must be running
- After rebooting the JM21, Shizuku needs to be restarted from a PC
- **On the JM21, turning USB debugging OFF stops Shizuku** (verified on the actual device). Keep USB debugging ON.

```sh
adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh
```

## Setup

1. Install Shizuku and start it
2. Install this app and open it
3. Tap "Request Shizuku permission" and grant it
4. Place the widget (1×1) on your home screen

## Building

Built directly with the SDK tools, without Gradle.

```sh
./fetch-libs.sh   # Fetch the Shizuku API libraries (first time only)
./build.sh        # Produces build/ChargeStopToggle.apk
```

Requirements: JDK 17, Android SDK (platform android-33 / build-tools 34.0.0).
The signing key (`build/keystore.jks`) is generated automatically on the first build.
Update installs require the same key, so if you change the key, uninstall the app first.

## License

MIT

# OneLoop - Medication Tracker (Android)

OneLoop is a personal Android medication reminder and schedule app, rebuilt to match the iOS SwiftUI UIv2 redesign.

> **Disclaimer:** OneLoop is a personal organization tool. It is **not** a medical device and does not provide medical advice, diagnosis, or treatment.

This repository’s **main** branch is the UIv2 redesign. The previous Android app is kept on the **`legacy-android-v1`** branch.

## Features

- Today dashboard with dose adherence, due / missed / next cards
- Flexible schedules, including staged dose changes
- Local exact-alarm reminders with snooze and “Mark taken” actions
- History that persists after a medication is removed
- Home screen Glance widget
- Floating capsule navigation or Material navigation bar
- First-launch splash, photo onboarding, medical disclaimer, and Skip sign-in
- Optional Supabase account + medication upload/download

## Requirements

- Android Studio Otter 3 / compatible with AGP 9.3
- JDK 17+ (Android Studio bundled JBR works)
- Android 12 (API 31) or newer
- Validated form-factor targets:
  - Android 12–14 phones, including MyOS 12
  - Google Pixel 8 and newer
  - Samsung Galaxy S22 family through S26 family (running Android 15+)
  - Samsung Galaxy A series from the 2023 lineup and newer (running Android 15+)

## Open in Android Studio

1. Open the `Android` folder
2. Let Gradle sync
3. Select the **app** run configuration
4. Run on an Android 12+ emulator/device

Command line:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug
```

## Project structure

```
app/src/main/java/com/davidcarranco/oneloop/medtracker/
  data/           # models, JSON persistence, Supabase, preferences
  notifications/  # exact alarms, boot reschedule, notification actions
  ui/             # Compose screens (Today, Schedule, History, Settings, auth)
  widget/         # Glance home-screen widget
```

Local medication JSON is stored under app-private files in `OneLoop/`, matching the iOS file names (`medications.json`, `medicationInfoHistory.json`).

## Configuration

Support / privacy URLs live in `AppInfo.kt`.

Supabase (optional cloud sync) is configured in `SupabaseConfig.kt`:

- Project URL + publishable key
- Auth redirect: `oneloopuiv2://auth-callback`
- Also allow the hosted HTTPS bridge used by the iOS app
- Legacy `oneloop://auth-callback` remains registered for older redirects

Add `oneloopuiv2://auth-callback` to **Supabase → Authentication → URL Configuration → Redirect URLs**.

## Device notes

- **Pixel 8+:** gesture navigation, punch-hole cutout, predictive back
- **Samsung One UI:** exact alarms and notification permission are requested; boot + timezone receivers reschedule reminders after reboot
- **minSdk 31** supports Android 12 (MyOS 12). Runtime notification permission is requested only on Android 13+.

## License

Copyright (c) 2026 David Carranco. All rights reserved.

This project is **not** MIT-licensed. You may download and run it **only for personal testing and evaluation**. You may not use it commercially, modify it (except to build/run it yourself for testing), or distribute copies. See [LICENSE](LICENSE).

## Author

David Carranco — [DCSvisuals](https://github.com/DCSvisuals)

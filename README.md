# OneLoop — Medication Tracker (Android)

OneLoop is a personal Android medication reminder and schedule app, ported from the iOS SwiftUI app.

> **Disclaimer:** OneLoop is a personal organization tool. It is **not** a medical device and does not provide medical advice, diagnosis, or treatment.

## Requirements

- Android Studio Otter 3 / compatible with AGP 9.3
- JDK 17+ (Android Studio bundled JBR works)
- Android 15 (API 35) or newer
- Validated form-factor targets:
  - Google Pixel 8 and newer
  - Samsung Galaxy S22 family through S26 family (running Android 15+)
  - Samsung Galaxy A series from the 2023 lineup and newer (running Android 15+)

## Features

- Today dashboard with dose adherence, due / missed / next cards
- Flexible schedules, including staged dose changes
- Local exact-alarm reminders with snooze and “Mark taken” actions
- History that persists after a medication is removed
- Home screen Glance widget
- Floating capsule navigation or Material navigation bar
- First-launch medical disclaimer, privacy policy, and support links
- Optional Supabase account + medication upload/download

## Open in Android Studio

1. Open the `Android` folder
2. Let Gradle sync
3. Select the **app** run configuration
4. Run on a Pixel 8+ emulator/device, or a Samsung device on Android 15+

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

Local medication JSON is stored under app-private files in `OneLoop/`, matching the iOS file names (`medications.json`, `medicationHistory.json`).

## Configuration

Support / privacy URLs live in `AppInfo.kt`.

Supabase (optional cloud sync) is configured in `SupabaseConfig.kt`:

- Project URL + publishable key
- Auth redirect: `oneloop://auth-callback`
- Also allow the hosted HTTPS bridge used by the iOS app

Add `oneloop://auth-callback` to **Supabase → Authentication → URL Configuration → Redirect URLs**.

## Device notes

- **Pixel 8+:** gesture navigation, punch-hole cutout, predictive back
- **Samsung One UI:** exact alarms and notification permission are requested; boot + timezone receivers reschedule reminders after reboot
- **minSdk 35** keeps the app on Android 15 behavior (edge-to-edge, 16 KB page size ready, per-app language)

## License

MIT — see [LICENSE](LICENSE).

## Author

David Carranco — [DCSvisuals](https://github.com/DCSvisuals)

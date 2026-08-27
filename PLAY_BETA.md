# OneLoop UIv2 — Play Console closed / internal testing

Use this to ship a beta APK/AAB to testers. The app ID is
`com.davidcarranco.oneloop.medtracker` (debug builds add `.debug`).

## 1. Create a Play Console app (once)

1. Open [Google Play Console](https://play.google.com/console)
2. Create app → **OneLoop UIv2**
3. Category: **Health & Fitness** (not Medical)
4. Fill privacy policy URL:
   `https://dcsvisuals.github.io/OneLoop/PrivacyPolicy/`
5. Complete **App content**: privacy policy, medical/health disclaimer, target audience (not children), Data safety (on-device medication data; optional account email)

## 2. Create an upload keystore (once)

```bash
keytool -genkey -v -keystore oneloop-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias oneloop
```

Keep the `.jks` and passwords out of git. Store them in a password manager.

Create `keystore.properties` next to `settings.gradle.kts` (already gitignored via `local.properties` pattern — do **not** commit this file):

```
storeFile=/absolute/path/to/oneloop-upload.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=oneloop
keyPassword=YOUR_KEY_PASSWORD
```

## 3. Build a signed App Bundle

```bash
cd Android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:bundleRelease
```

The AAB is at:

`app/build/outputs/bundle/release/app-release.aab`

Or in Android Studio: **Build → Generate Signed App Bundle / APK → Android App Bundle**.

## 4. Internal testing (fastest for friends / yourself)

1. Play Console → **Testing → Internal testing**
2. Create a release, upload `app-release.aab`
3. Add testers by Google account email
4. Copy the **opt-in URL** and send it to testers
5. Testers open the link on the device, accept, then install from Play Store

Internal testing is usually available within minutes after processing. Testers must use the same Google account you added.

## 5. Closed testing (broader beta)

1. **Testing → Closed testing → Create track**
2. Upload the same AAB
3. Add a testers list (emails) or a Google Group
4. Submit for review (first closed-test release is reviewed)
5. Share the track opt-in URL after it is approved

## 6. Device QA before inviting testers

- First-launch splash → Welcome → Daily Loop → Notifications → Policy toggle → Skip
- Skip sign-in; add / edit / remove a medication
- Mark taken, snooze, History after removing a med
- Notifications allow / deny / Open Settings
- Light / dark + floating capsule vs Material navigation
- Home-screen widget
- Pixel 8+ and a Samsung One UI device on Android 15+

## 7. Review notes (paste into Play Console)

> Personal medication reminder utility — not a medical device and not for diagnosis or treatment. A medical disclaimer is required on first launch (toggle + Continue) and remains in Settings.
>
> No account required. Use Skip on the sign-in screen. Sample: Add medication → Mark taken → History. Enable notifications in Settings to test reminders.
>
> Cloud backup is optional. Google sign-in is available for testing; Sign in with Apple is shown disabled.

## Notes

- First Play upload of a new app (or a closed track) can take hours for review.
- Debug APKs (`assembleDebug`) install with applicationId `com.davidcarranco.oneloop.medtracker.debug` and will **not** share data with the Play build.
- Keep `legacy-android-v1` on GitHub if you need the old build; **main** is UIv2.

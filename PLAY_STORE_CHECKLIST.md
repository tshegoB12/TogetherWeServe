# Play Store preparation checklist (rubric: "prepared for publication", 5 marks)

## 1. Create the upload keystore (once, keep it OUT of git)
```bash
keytool -genkey -v -keystore tws-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias tws
```
Create `keystore.properties` in the project root (it is git-ignored):
```
storeFile=tws-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=tws
keyPassword=YOUR_KEY_PASSWORD
```
Back the keystore up somewhere safe. Losing it means you cannot update the app.

## 2. Build the signed release
```bash
./gradlew assembleRelease     # signed APK  -> app/build/outputs/apk/release/app-release.apk
./gradlew bundleRelease       # signed AAB  -> app/build/outputs/bundle/release/app-release.aab
```
Google Play requires an **AAB** for new apps; your lecturer's rubric asks for a **signed APK**. Submit both.
Verify: `apksigner verify --verbose app/build/outputs/apk/release/app-release.apk`

## 3. Make Google SSO work on the signed build
Firebase console → Project settings → your Android app → add the SHA-1 and SHA-256 of the release key (`keytool -list -v -keystore tws-release.jks -alias tws`). If you use Play App Signing, also add the SHA-1 shown in Play Console → App integrity. Download the new `google-services.json` and replace `app/google-services.json`.

## 4. Check the target API level
Play Console rejects uploads below the current minimum `targetSdk`. The project uses 34. Open Play Console → Create app → upload, and if it asks for a newer level, raise `compileSdk`/`targetSdk` (and AGP if prompted) and retest.

## 5. Evidence to submit
- [ ] Screenshot of `apksigner verify` output (or signed APK file)
- [ ] Screenshot of the AAB uploaded in Play Console (Internal testing track)
- [ ] 2–8 phone screenshots from a physical device (Home, Event detail, Schedule, Notifications, Profile, Settings, language change, offline banner)
- [ ] 512×512 app icon PNG and 1024×500 feature graphic
- [ ] Privacy policy URL (required because the app collects email and uses Firebase)
- [ ] Data-safety form completed (email, name, user ID, push token; encrypted in transit)
- [ ] Content rating questionnaire completed

## 6. Store listing text (ready to paste)
**App name:** TogetherWeServe
**Short description (80):** Find, claim and track local volunteering events with your team.
**Full description:**
TogetherWeServe connects community volunteers with local, cause-based volunteering opportunities and gives non-profits a simple way to publish events.
• Discover and search events, filter by cause
• Claim an event alone or sign up as a team with an invite code
• Works offline: browse cached events and register; it syncs when you reconnect
• Instant alerts when new events are published
• Track your volunteer impact and save favourite events
• Available in English, isiZulu and Sesotho
• Sign in with email or Google

# TogetherWeServe — Final PoE (v2.1-final)

![CI](https://github.com/MbalentleMatlhabane/TogetherWeServe/actions/workflows/android-ci.yml/badge.svg)

**OPSC6312 — Open Source Coding (Intermediate)**  
**Part 3: Final Asset Development, Additions, Tweaking and Final Submission**

TogetherWeServe is an Android volunteering platform that connects community volunteers with local, cause-based volunteering opportunities and gives non-profit organisations a simple way to publish events and manage volunteer sign-ups.

## Final-version improvements

This final version builds on the Part 2 prototype and addresses the areas identified in the prototype feedback: application stability, stronger REST API integration, clearer navigation, more polished UI, automated testing/CI, and a professional final submission structure.

### Core assessment features

| Requirement | Final implementation |
|---|---|
| Registration and secure login | Firebase Authentication with email/password; passwords are handled by Firebase rather than stored by the Android client. |
| Google SSO | Google Sign-In through Firebase Authentication. |
| Settings | Profile, notification preferences, language selection, offline/sync information, privacy/security and logout. |
| REST API | Firebase Cloud Functions exposes the custom REST API; Retrofit is used by the Android client. |
| NoSQL database | Cloud Firestore stores users, events, groups and registrations. |
| Offline mode + sync | RoomDB caches events and registrations and queues offline registrations. An app-wide network callback replays the queue automatically the moment connectivity returns, from any screen. |
| Real-time notifications | Firebase Cloud Messaging. A Cloud Function sends a data-only message to the `new_events` topic; the app shows a system notification (runtime permission requested on Android 13+), stores it, and lists it on the Notifications screen. The "Event reminders" switch in Settings subscribes/unsubscribes the topic. |
| South African language support | English, isiZulu and Sesotho. A unit test fails the build if any string is missing from a language. |
| User-defined feature 4 | Saved/Favourite events. |
| User-defined feature 5 | Volunteer Impact dashboard based on confirmed service registrations. |
| UI | Dedicated Home, Schedule, Notifications (live alert list), Profile and Settings destinations with consistent Material styling. |
| Organiser feature | Organisers can publish volunteering events through the REST API. |
| Group/team feature | Volunteers can create or join event groups using invite codes. |
| Search/filter | Volunteers can search events and filter by cause. |

## Architecture

```text
Android app (Kotlin)
        |
        | Retrofit + Firebase ID token
        v
Firebase Cloud Functions REST API
        |
        +---- Firebase Authentication
        |
        +---- Cloud Firestore (NoSQL)
        |
        +---- Firebase Cloud Messaging
        |
        +---- Topic: new_events

Android local layer
        |
        +---- RoomDB event cache
        +---- RoomDB registration cache
        +---- RoomDB pending offline actions
```

The REST API is the application's server-side business layer. Authentication tokens are verified before protected API operations are executed. Firestore access is locked down in `firestore.rules`, so the Android client does not directly write to Firestore.

## Offline and synchronisation design

When online, event and schedule data are refreshed from the REST API and cached in RoomDB. When offline, the user can still browse cached information and submit supported actions. An offline registration is stored in `PendingActionEntity` with a `pending_sync` status.

`TogetherWeServeApp` registers a `ConnectivityManager` network callback for the whole process. When the network returns it calls `syncPendingActions()` (guarded by a `Mutex` so an action can never be sent twice), then refreshes the schedule. The local placeholder row is replaced by the server copy, so no duplicate appears. Registrations the server permanently rejects (HTTP 4xx) are discarded instead of retrying forever.

## Notifications

The Android app subscribes to the Firebase Cloud Messaging `new_events` topic (only while "Event reminders" is enabled in Settings). A Firestore `onCreate` Cloud Function sends a **data-only** message whenever an organiser publishes an event, so `TwsFirebaseMessagingService.onMessageReceived` runs whether the app is open or closed. Each alert is saved locally (shown in the Notifications tab) and displayed as a system notification; tapping it opens the event. On Android 13+ the app asks for the `POST_NOTIFICATIONS` permission at launch.

> After changing `functions/src/index.ts` you must redeploy: `cd functions && npm install && firebase deploy --only functions`.

## Languages

The final app includes:

- English (`values/`)
- isiZulu (`values-zu/`)
- Sesotho (`values-st/`)

The language can be changed in Settings and (Android 13+) in the system per-app language settings (`locales_config.xml`).

The selected language is persisted locally and restored when the application starts.

## Final release changes

### v2.1-final (latest)

See `RELEASE_NOTES.md` for the full list: automatic offline sync, working push-notification permission and in-app alert list, honoured notification preferences, complete isiZulu/Sesotho translations, logging throughout, expanded unit tests and a fixed CI pipeline.

### v2.0-final

- Improved application navigation by giving Notifications and Profile their own destinations.
- Added saved/favourite events.
- Added Volunteer Impact dashboard.
- Added Sesotho language support alongside English and isiZulu.
- Added real FCM topic subscription for event alerts.
- Updated the event-created Cloud Function to publish notifications to the FCM topic.
- Improved Settings error/sync feedback.
- Added a final-version CI workflow for both `main` and `master` branches.
- CI runs unit tests and a release build and uploads the generated APK as an artifact.
- Updated app version to `2.0-final`.

## Automated testing

The project contains unit tests under `app/src/test`:

| Test class | What it protects |
|---|---|
| `EventFilterTest` | Search and cause filtering of events |
| `ImpactCalculatorTest` | Volunteer Impact maths |
| `NotificationCodecTest` | Notification history storage (round-trip, corrupt data, 50-item cap) |
| `SupportedLanguagesTest` | Language list and safe fallback to English |
| `StringsParityTest` | Every string exists in isiZulu and Sesotho with matching placeholders |

The GitHub Actions workflow (`.github/workflows/android-ci.yml`) runs on every push, pull request and the `final-poe` tag:

1. Checkout
2. JDK 17 setup (Gradle cache)
3. Verify `app/google-services.json` (committed client config, or supplied from the `GOOGLE_SERVICES_JSON` secret)
4. Unit tests with `./gradlew testDebugUnitTest` (HTML report uploaded as an artifact)
5. Debug build with `./gradlew assembleDebug`
6. Release build with `./gradlew assembleRelease`
7. APK artifact upload

Tag the final commit: `git tag final-poe && git push origin final-poe`.

## Publication preparation

The release build is signed automatically when a `keystore.properties` file exists in the project root (it is git-ignored). See `PLAY_STORE_CHECKLIST.md` for the exact keystore, signing, Firebase SHA-1 and Play Console steps, plus ready-to-paste store listing text. Do **not** commit the keystore or passwords to GitHub.

Evidence to include in the final submission:

- Signed release APK screenshot/file evidence.
- Google Play Console upload screenshot.
- Play Store listing screenshots if published.
- Final app screenshots from a physical Android phone.
- Final demonstration video link.

## Demonstration video — final script

The final video should be recorded on a **physical Android phone** and include a voice-over. Use this order:

1. Launch app on the phone and show the final UI.
2. Register a new account and explain that Firebase Authentication handles credentials.
3. Log in using email/password.
4. Log out and demonstrate Google SSO.
5. Open Home, search and filter volunteering events.
6. Open an event and save it as a favourite.
7. Register individually and show My Schedule.
8. Demonstrate Group/Team sign-up using an invite code.
9. Open Profile and show the Volunteer Impact and Saved Events information.
10. Open Settings and change notification preferences.
11. Change language to isiZulu and then Sesotho.
12. Turn connectivity off, open cached events, perform an offline registration and show `Pending sync`.
13. Restore connectivity and show the queued action synchronising.
14. Demonstrate a push notification generated from a newly published event.
15. Show Firebase Authentication data, Firestore data and the REST API response in the backend.
16. Show the GitHub repository, multiple commits, README and successful GitHub Actions run.
17. Show release APK / Play Console preparation evidence.

## Links

- GitHub: https://github.com/MbalentleMatlhabane/TogetherWeServe
- Prototype (Part 2) video: https://youtube.com/shorts/qC8cL0VMMjM?si=70Vv5KhDdKzinzDQ
- **Final PoE demonstration video:** `PASTE-YOUR-UNLISTED-YOUTUBE-LINK-HERE`

## Academic integrity / AI disclosure

See `AI_USAGE.md`. AI assistance must be described accurately and the final implementation must be reviewed and understood by the student before submission.

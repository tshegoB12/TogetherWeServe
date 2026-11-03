# Final PoE submission checklist (maps to the Part 3 rubric)

Things only YOU can do (marked ✋) are what separate "good" from 100.

| Rubric item (marks) | Status | What to do |
|---|---|---|
| Runs on a mobile device (5) | ✋ | Record everything on a physical Android phone, not an emulator. Test on Android 13+ if you can (notification permission). |
| SSO login (10) | ✋ | Add release SHA-1 in Firebase (see PLAY_STORE_CHECKLIST) and test Google sign-in on the phone. |
| Offline mode (10) | Fixed in code | Demo: airplane mode → claim event → "Pending sync" → turn data on → watch it become Confirmed *without* opening the Schedule tab first. |
| Real-time notification (10) | ✋ | Redeploy functions (`firebase deploy --only functions`), allow the permission, publish an event from an organiser account, show the alert + the Notifications tab. |
| Multi-language (10) | Fixed in code | Demo isiZulu then Sesotho on Home, Event detail, Schedule, Settings. Ask a first-language speaker to proof-read the strings. |
| User-defined 4 and 5 (20) | Present | Demo Saved events and Volunteer Impact clearly; name them as innovative features in the voice-over. |
| UI (10) | OK | Keep design consistent; avoid showing raw error text. |
| GitHub, README, testing (10) | ✋ | Commit in several small commits (not one upload), push, confirm the Actions run is green, tag `final-poe`. No zip uploads. |
| Play Store preparation (5) | ✋ | Follow PLAY_STORE_CHECKLIST.md; include signed APK, Play Console screenshot, listing screenshots. |
| AI write-up (5) | Done | Edit AI_USAGE.md so it matches exactly what you used. Be honest. |
| Demo video (5) | ✋ | Voice-over, follow the script in README, also show Firebase Auth users, Firestore data, an API response, GitHub Actions run. Paste link in README. |

Suggested commit sequence: `ci: track google-services and fix workflow` → `feat: notification permission + live notification list` → `feat: automatic offline sync` → `i18n: complete isiZulu and Sesotho` → `test: add unit tests` → `docs: README, release notes, play store`.

# AI Tool Usage Write-up — TogetherWeServe (OPSC6312 PoE)

**Tools used:** ChatGPT (OpenAI) for rubric review and planning during Part 3, and Claude (Anthropic) for a final code/CI audit and fixes in October 2026.

## What AI was used for
- **Rubric mapping:** reading the PoE brief and listing which marked features (SSO, offline sync, notifications, languages, user-defined features, GitHub/CI, Play Store preparation, video) needed work.
- **Code audit:** reviewing the Kotlin project and finding gaps. Examples found: the notification runtime permission was never requested; offline sync only ran while the Schedule screen was open; the CI build would fail because `google-services.json` was git-ignored; no logging existed; 19 isiZulu and 2 Sesotho strings were missing.
- **Code written with AI help (reviewed by the team):** `NotificationItem`/`NotificationStore`/`NotificationAdapter` and the Notifications screen; the app-wide network callback in `TogetherWeServeApp`; the `Mutex`-guarded `syncPendingActions()` in `EventRepository`; `NotificationTopics`; the `POST_NOTIFICATIONS` request in `MainActivity`; the data-only FCM change in `functions/src/index.ts`; the unit tests in `app/src/test`; the GitHub Actions workflow; the release-signing block in `app/build.gradle.kts`.
- **Translations:** first drafts of the isiZulu and Sesotho strings were AI-generated and must be checked by a first-language speaker before the Play Store release.
- **Documentation:** drafting the README structure, release notes, Play Store checklist and demo script.

## What AI did not do
The app idea, the Firebase project setup, the Planning & Design and Research documents (Part 1), recording and narrating the demo video, device testing and the GitHub history were done by the team.

## How it was checked
All AI-assisted changes were read line by line, built in Android Studio, and tested on a physical Android phone. The unit tests run on every push through GitHub Actions. Logs from the app can be filtered with `TWS-`.

## Citation
OpenAI (2026) *ChatGPT* [Large language model]. Available at: https://chat.openai.com (Accessed: October 2026).
Anthropic (2026) *Claude* [Large language model]. Available at: https://claude.ai (Accessed: 26 October 2026).

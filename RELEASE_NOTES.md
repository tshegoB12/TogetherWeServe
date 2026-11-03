# TogetherWeServe — Release Notes

## v2.1-final (versionCode 3) — final PoE submission

### Innovative features in this version
- **Volunteer Impact dashboard** (Profile): confirmed events and estimated service hours, calculated by a unit-tested `ImpactCalculator`.
- **Saved / favourite events**: save any event from Event Details; the count shows on Profile.
- **Group / team sign-up with invite codes**: create or join a group for an event.
- **Live notification centre**: every push alert is kept in an in-app list; tapping opens the event.
- **Three-language app**: English, isiZulu and Sesotho.

### Fixed / improved since v2.0-final
| Area | Change |
|---|---|
| Offline sync | Sync now runs automatically app-wide when connectivity returns (previously only while the Schedule tab was open). Duplicate-send protection (Mutex), no duplicate schedule rows after sync, permanently rejected actions are discarded. |
| Push notifications | Runtime `POST_NOTIFICATIONS` permission requested on Android 13+. Cloud Function now sends data-only messages so alerts work in the background and are stored. Notification tap opens the event. |
| Notifications screen | Replaced the static text with a live, scrollable alert list, empty state and "Clear all". |
| Settings | "Event reminders" now genuinely subscribes/unsubscribes the FCM topic and is honoured when a message arrives. |
| Multi-language | Added the 19 missing isiZulu and 2 missing Sesotho strings, replaced leftover English text, localised schedule status labels and "Hosted by", added per-app language support (Android 13+). |
| Logging | `android.util.Log` added across auth, repository/sync, FCM, settings, event detail and network changes (filter Logcat by `TWS-`). |
| Testing / CI | New unit tests (impact, notification storage, languages, translation parity). CI now commits-safe: `google-services.json` is tracked, debug + release builds and test reports are uploaded, tag `final-poe` triggers a run. Removed a misplaced workflow in `functions/`. |
| Publishing | Optional release signing via `keystore.properties`; Play Store checklist and listing text added. |
| Versioning | `versionCode` 3, `versionName` 2.1-final. |

## v2.0-final
- Dedicated Notifications and Profile destinations; saved events; Volunteer Impact; Sesotho resources; FCM topic subscription; improved Settings feedback; first CI workflow.

## Prototype (Part 2)
- Email/password registration and login, Google SSO, REST API on Cloud Functions + Firestore, RoomDB cache, event discovery, individual registration, basic settings.

# Nimbus Alltag / Nimbus Daily — v2

Personal Android home-screen dashboard. Default language German, switchable to English and Traditional Chinese (繁體中文).

## Included
- Live weather, feels-like temperature, daily high/low, rain probability, humidity, wind, UV index, local sunrise and sunset from Open-Meteo.
- Optional Android **device calendar** integration (read-only, after READ_CALENDAR permission). It displays the phone's synced calendar events today. Your Google account may need Calendar sync enabled on the phone.
- Optional VBB Berlin/Brandenburg departures from an editable stop name. Service provided by the independent v6.vbb.transport.rest proxy, NOT an official VBB app. Delays are real-time when the upstream service provides them.
- Rule-based packing suggestions combining weather, today's calendar event titles and your typed plans. The refresh button refreshes weather + departures + device calendar + suggestions.
- Editable on-device home and university addresses (route opens Google Maps); city, stop, today's plans, °C/°F.
- German / English / Traditional Chinese dashboard and widget.
- ChatGPT button sends today's context through Android's Share Intent, without sharing private addresses unless you opt in.
- ChatGPT-to-app *manual* data import: share `NIMBUS_UPDATE: {"plans":"Uni 10 Uhr","home":"example address"}` as text to Nimbus Alltag, inspect the confirmation prompt, then confirm. New imported weather cities require the app's Save button to geocode correctly.

## Important limitations
No autonomous ChatGPT agent runs within the app: no OpenAI API backend/OAuth bridge or remote conversation access is configured. Android's Update button performs deterministic, explainable rules, not ChatGPT reasoning. ChatGPT memory and the app's local data are **not automatically synchronized**. Do not put your home address, calendars, passwords, or tokens in this PUBLIC GitHub repository or GitHub Actions logs. The app's SharedPreferences stay on your device and Android backup is disabled. No notifications or route-planning engine are included. VBB only covers Berlin/Brandenburg; tapping the Maps button opens transit directions for any saved address.

Built as Android debug APK, using installed debug signature for personal testing. This build has not been tested on a physical device.

Build: GitHub Actions → Build Nimbus Weather APK → artifact `NimbusAlltag-v2-debug-APK`. APK has the same Android applicationId as the v1 APK; Android may refuse an in-place upgrade if the debug signing key differs. If that happens, back up any v1 settings and uninstall v1 before installing v2.

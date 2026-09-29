# Nimbus Daily 3.0 — Personal Android daily dashboard

This is an **on-device Android app** for the user's personal daily briefing, inspired by their supplied dashboard UI example. Default language: German; settings can switch to English or Traditional Chinese.

## Sections
- **Home:** five-day date selector, color-coded calendar timeline, next appointment, calculated leave-home time if a VBB route can be verified, local weather, sunrise/sunset and an interactive daily packing checklist.
- **Calendar:** events are read from Android's Calendar Provider, including Google/Samsung calendars when synced on the phone; creates events using the installed calendar app. Calendar access is optional.
- **Transport:** VBB real-time departures for the specified stop; journeys for the next appointment are calculated when there is a suitable stop/home origin and destination in Berlin/Brandenburg. Arrive 10 minutes before the event; when originating from a stop, a locally configurable walk-to-stop buffer is subtracted from the departure time. Real-time availability and coverage depend on VBB. Outside its service region, use Google Maps; the app does not fabricate departure times.
- **Settings:** German, English, Traditional Chinese; personal name, home and university destination, preferred departure stop, manual plans, walking buffer, °C/°F, and **lock weather to manually chosen city**. By default, weather uses the phone's foreground location; saved last coordinates are used for widget refresh until the app opens again.
- **ChatGPT handoff:** copies a factual snapshot of the agenda, weather, transit and suggested packing list to the clipboard and opens ChatGPT. Home address is excluded unless explicitly ticked. ChatGPT Plus is not an always-on app API; ChatGPT cannot automatically read or edit the app from this chat.
- **Import from ChatGPT:** paste data beginning with `NIMBUS_V1` and a JSON object containing optional keys `name`, `home`, `uni`, `station`, `plan`, `city`, `locked`, `lang`, and `walk_buffer`. A confirmation preview appears before any settings are applied. `nimbusdaily://import?data=...` deep links are also supported for trusted sources, with user confirmation. Do not include secrets in shared links.

## Three home-screen widgets
Nimbus day briefing (4×4), small weather (2×2) and VBB travel (2×2). Android updates widgets no more frequently than the launcher/platform allows (requested interval 30 min). Refreshing them does not grant continuous background location access.

## Permissions and privacy
On first launch, Nimbus requests **foreground fine/coarse location** and **read calendar**. Both are optional. No contacts, notification or background-location permission is requested. Weather coordinates go to Open-Meteo, station/address and route queries to v6.vbb.transport.rest, and Google Maps receives an origin/destination only when the user clicks directions. Personal addresses/plans are saved only in Android SharedPreferences (no encryption or automatic remote sync); the public source repository contains no user-specific values.

## Installation
GitHub Actions builds a debug APK. Its runner-generated debug signing key may differ between builds. If Android rejects updating an earlier APK due to different signing certificates, uninstall the old APK first; this erases app-local saved settings. Test on a physical Android device to validate layouts, permissions and provider connectivity.

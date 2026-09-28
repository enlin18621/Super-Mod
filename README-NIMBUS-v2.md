# Nimbus Alltag v2 — mobile daily dashboard

Source is on **nimbus-dashboard-v2**, isolated from the repository's unrelated main branch and the Nimbus Weather v1 build.

**Features:** Open-Meteo weather, daily max/min, sunrise/sunset, UV index, wind, rainfall risk and humidity; optional read-only phone calendar; VBB Berlin/Brandenburg stop departures; rule-based today packing suggestions from weather, calendar titles and user plans; locally editable city, home, university destination and stop; Google Maps directions; German, English and Traditional Chinese; ChatGPT handoff via Android share; opt-in addresses in share; confirmed NIMBUS_UPDATE JSON import from Android Share.

**Privacy:** Addresses and plans stay in on-device SharedPreferences, with Android backups disabled. No user addresses are committed to GitHub. The repository is public: never commit your personal schedule, secrets, addresses or OpenAI API credentials here. Android READ_CALENDAR only after runtime permission.

**Not yet implemented:** ChatGPT API backend, autonomous ChatGPT reasoning on app refresh, automatic push from ChatGPT conversation to app, or permanent ChatGPT memory synchronization. The app's Refresh button executes live API reads and rule-based recommendations. To ask ChatGPT for daily reasoning, use the separate ChatGPT button. To import ChatGPT-generated updates, ask for a line `NIMBUS_UPDATE: {"plans":"Uni ab 10 Uhr","home":"[your private address]"}`, copy the text and tap **Import ChatGPT update from clipboard** in Nimbus Alltag (or share the text to Nimbus via Android Share), review the confirmation dialog, then save if the weather city changed. Daily plans are date-scoped so yesterday's packing hints don't carry over. An always-on two-way integration requires an authenticated private backend.

**Installation:** Download Actions artifact `NimbusAlltag-v2-debug-APK`, unzip to `app-debug.apk`, install on Android. This v2 uses a separate package `com.nimbus.daily` so it can be tested alongside v1 without uninstalling or losing v1 data. Add a 4×3 home screen widget; open the app to edit settings.

API references: https://open-meteo.com/en/docs ; https://v6.vbb.transport.rest/api.html ; https://developer.android.com/reference/android/provider/CalendarContract.Instances

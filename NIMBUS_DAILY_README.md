# Nimbus Daily v2
An Android personal dashboard + home-screen widget.

## Live inputs
- Open-Meteo: temperature, precipitation risk, UV index, sunrise/sunset.
- Android Calendar provider: reads Google and Samsung calendars synced on-device, with optional READ_CALENDAR runtime permission. No calendar credentials are transmitted by Nimbus.
- VBB transport.rest: Berlin/Brandenburg realtime departures for manually specified station.
- Google Maps: transit directions for local home and university/destination addresses (only when tapped).
- ChatGPT handoff: copies weather, calendar, transport and packing context to clipboard and opens ChatGPT when tapped. Home address is excluded unless explicitly opted in.

## Privacy and limitations
Home and university addresses remain in local Android SharedPreferences; this repository contains no private personal data. Weather coordinates and chosen station are sent to their respective weather/transit services when refreshed. ChatGPT is NOT integrated as an always-on model, cannot directly alter settings from a chat, and does not automatically read this device. A secure authenticated backend would be required for seamless cross-device profile syncing and automatic remote AI refresh.

## Edit
Open app → change city, nearest Berlin/Brandenburg stop, home/university destination, plans and language → Save. German is default; English and Traditional Chinese are included. Refresh updates weather, calendar, VBB and the rule-based packing checklist. Calendar event changes made to your connected Google Calendar in ChatGPT may appear when Google/Samsung sync completes on your phone and calendar read permission is allowed.

## Install
This is a debug APK produced by GitHub Actions. If upgrading from Nimbus Weather v1 installed from another GitHub runner, Android signing certificates may differ: uninstall v1 before installing v2, after copying any locally saved settings. Recreating the app will reset v1's local preferences.
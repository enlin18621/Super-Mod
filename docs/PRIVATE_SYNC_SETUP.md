# Nimbus private two-way sync (beta)

This is a **privacy-conscious beta bridge**, not a direct connection to your ChatGPT account or ChatGPT Memory. The beta installs separately from Nimbus Alltag v2/v3 as `com.nimbus.daily.syncbeta`, so testing it will not overwrite existing local data.

## Why this needs a private repository

The app source lives in a public GitHub repo; **never save your addresses, calendar details, GPS coordinates or tokens there**. Create a new empty, private repository named e.g. `nimbus-personal-sync` under your own account. Your ChatGPT GitHub connector also needs permission to access that dedicated repo for in-chat updates.

1. On GitHub, create a new **Private** repository. Do not use `Super-Mod` or any public repository.
2. In the private repo, create `nimbus-state.json` with this initial non-personal template, replacing example values through *this chat* when desired:
   ```json
   {
     "version": 1,
     "city": "Berlin",
     "stop": "",
     "plans_day": "2026-09-28",
     "plans": "",
     "lang": "de",
     "fahrenheit": false
   }
   ```
   You can optionally set `home` and `uni` in this **private** file; prefer editing the address directly in the phone if you do not wish to put it in any GitHub repo. `plans_day` must match today's local date or those plans will be ignored.
3. **On GitHub (not here)** create a fine-grained Personal Access Token scoped **only** to that private repository; grant *Contents → Read and Write*, and Metadata Read (automatically included). Do not give it Actions, Administration or other unnecessary permissions. GitHub may require account verification.
4. On your phone: Nimbus Alltag → Settings → Private GitHub sync. Enter `username/nimbus-personal-sync`, paste the token **on your phone only**, tick Enable, then Save and Sync. The app encrypts the token locally with Android Keystore. The private repo check will reject public repos.
5. Allow Android background work (Samsung: Settings → Apps → Nimbus Alltag → Battery → Unrestricted, if you want the best chance of timely refresh) and, if desired, notifications and location.
6. Now ask ChatGPT, using the **connected GitHub app** (ensure it can access the new repository): “Update `nimbus-state.json` in my private Nimbus sync repo: my university destination is ...” ChatGPT can edit the private JSON file when authorized. On the next app refresh/scheduled check, Nimbus pulls and applies it. No new APK is necessary.

## What the device can send back

`nimbus-device.json` is **not created** unless you explicitly select “Report device status” in the app. Status sends only timestamps, weather city and permissions. **Today's event titles/times**, **private addresses/plans**, and **precise GPS** are separate opt-ins (GPS additionally needs the private-data opt-in). Status reports are throttled to at most once per half hour. With your GitHub connection, ChatGPT can read the private status file when you ask. This does not make the chat run all the time.

## Calendar and Maps

- Nimbus reads the **Android on-device Calendar Provider** after permission, including calendars that Samsung Calendar is syncing locally. To make ChatGPT edit those events, sync them to **Google Calendar** on your phone, and let ChatGPT use the independently connected Google Calendar integration with explicit write authorization for the event. Samsung-only, unsynced calendars cannot be edited remotely by this chat.
- Nimbus opens Google Maps transit navigation, taking your saved home address or last current GPS point and destination. Google Maps account data (saved places/history etc.) is **not** synchronized and the app does not silently edit those records.
- For routes and live transit alerts in Berlin/Brandenburg, Nimbus uses independent `v6.vbb.transport.rest` APIs, with details subject to third-party availability. It does not guarantee departure notifications.

## AI

The optional OpenAI API key in the **separate AI settings** is billed independently from your ChatGPT subscription. It is device-encrypted. With AI Auto turned on, limited background AI briefings can use weather, device calendar, today's plans and a VBB route; calendar locations/destination are included only when the separate AI location-sharing toggle is enabled. No remote API can reach your ChatGPT Memory, this live conversation, or connectors through an ordinary app key.

## Uninstall / revoke

Disable sync in Nimbus (deletes the local sync token); then revoke the fine-grained PAT in GitHub settings. Delete the private repo or file separately if you want it removed from GitHub. This is a beta: an app build passing CI is not equivalent to a physical-device test. Android may defer 30-minute background schedules.

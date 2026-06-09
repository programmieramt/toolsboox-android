# Overwatcher

A personal fork of [Tools for Boox](https://github.com/gaborauth/toolsboox-android), optimized for use on Boox Air 4C and Palma 2 Pro.

## What's different in this fork

- **Renamed to Overwatcher** — package `com.overwatcher.programmieramt`
- **WebDAV sync** — bidirectional calendar sync between devices (Palma 2 Pro ↔ Air 4C) via your own WebDAV server. Replaces Google Drive.
- **Calendar permissions** — WRITE_CALENDAR added for full permission support on Android 14 (Boox Air 4C)
- **New app icon** — calendar with "0" on blue background
- **No ads, no Firebase Analytics** — stripped completely
- **Signed release builds via GitHub Actions** — reproducible APKs using the debug keystore

## Installation

Download the latest APK from [Releases](../../releases) and sideload it to your Boox device.

### Grant calendar permissions via ADB (if toggle is grayed out on Air 4C)

```
adb shell pm grant com.overwatcher.programmieramt android.permission.READ_CALENDAR
adb shell pm grant com.overwatcher.programmieramt android.permission.WRITE_CALENDAR
```

## WebDAV Sync Setup

Overwatcher syncs calendar data between devices via your own WebDAV server (NAS, Nextcloud, etc.) instead of Google Drive.

1. Open the app → toolbar sync icon → **Settings**
2. Under **WebDAV Sync**: enable and enter URL, username, password
3. The sync folder on your server will be `{url}/calendar/`
4. Repeat on the second device with the same credentials

The sync runs automatically when you open or close a calendar page.

**Two separate WebDAV configs:**
- **WebDAV Sync** — calendar JSON data, syncs between devices
- **WebCal Backup** — PDF exports to Ultrabridge

## Build

```bash
./gradlew assembleProdDebug   # debug APK
./gradlew assembleProdRelease # signed release APK (requires keystore.jks)
```

## Credits

This fork builds on the work of two projects:

**[Tools for Boox](https://github.com/gaborauth/toolsboox-android)** by [Gábor Auth](https://github.com/gaborauth) — the original calendar/planner app for Boox e-ink devices. A remarkable piece of work that makes these devices genuinely useful as daily planners. Please support the original:
- [Google Play](https://play.google.com/store/apps/details?id=com.toolsboox)
- [Patreon](https://www.patreon.com/toolsboox)
- [PayPal](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=SVJ9HDCVKAAKS)

**[Ledger](https://github.com/mjhfunctionalashtanga/toolsboox-android)** by [Michael Joel Hall](https://github.com/mjhfunctionalashtanga) — an earlier fork that added full-screen mode, pinch-zoom, pen color picker, Ultrabridge integration, and many other enhancements. Parts of the infrastructure in this fork draw from that work.

## License

GPLv3 — same as the original. See [LICENSE](../LICENSE).

# Overwatcher

A personal fork of [Tools for Boox](https://github.com/gaborauth/toolsboox-android), optimized for use on Boox Air 4C and Palma 2 Pro.

## What's different in this fork

- **Renamed to Overwatcher** — package `com.overwatcher.programmieramt`
- **Google Drive sync** — bidirectional sync for calendar data
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

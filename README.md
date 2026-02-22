<p align="center">
  <h1 align="center">GameVault</h1>
  <p align="center">Your personal game launcher & tracker for Android</p>
</p>

<p align="center">
  <img alt="Min SDK" src="https://img.shields.io/badge/min%20sdk-26-blue">
  <img alt="Target SDK" src="https://img.shields.io/badge/target%20sdk-35-blue">
  <img alt="Version" src="https://img.shields.io/badge/version-1.0.0-green">
  <img alt="Kotlin" src="https://img.shields.io/badge/kotlin-2.1.0-purple">
  <img alt="License" src="https://img.shields.io/badge/license-All%20Rights%20Reserved-red">
</p>

---

GameVault automatically detects the games installed on your device, tracks your playtime across sessions, and gives you detailed analytics — all in one place. It also ships with gaming tools like a floating timer, automatic Do Not Disturb, and a local DNS ad blocker.

## Screenshots

<!-- Replace with actual screenshots -->
<p align="center">
  <img src="docs/screenshots/home.png" width="200" alt="Home">
  <img src="docs/screenshots/detail.png" width="200" alt="Game Detail">
  <img src="docs/screenshots/stats.png" width="200" alt="Stats">
  <img src="docs/screenshots/settings.png" width="200" alt="Settings">
</p>

## Features

**Game Library** — Automatically detects installed games and keeps your library up to date as you install, update, or remove apps. Browse in grid, list, or icon view. Search, sort, favorite, and organise games into custom collections. Add personal notes, ratings, and tags to any title.

**Playtime Tracking** — Records play sessions via the Android Usage Stats API. View stats for today, this week, this month, or all time. Track your play streak and see daily playtime visualised in interactive charts.

**Gaming Tools** — Set a countdown timer that runs as a foreground service and optionally shows a draggable floating overlay on top of your game. Enable automatic Do Not Disturb when a game launches. Access GameVault instantly from the Quick Settings tile.

**Ad Blocker** — A lightweight DNS-based ad blocker powered by Android's VPN API. Only DNS traffic is routed through the local VPN — all other traffic flows normally. Comes with 200+ blocked ad domains out of the box, and supports per-game whitelisting for games that use rewarded ads.

**Analytics & Reports** — Weekly gaming report delivered as a notification. Game update log tracks version and size changes over time. Export your full library and session history as JSON or CSV.

**Home Screen Widget** — A Glance-powered widget for quick access from your home screen.

## Tech Stack

| | |
|-|-|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM, Repository pattern |
| **DI** | Hilt |
| **Database** | Room |
| **Preferences** | DataStore |
| **Navigation** | Navigation Compose |
| **Background work** | WorkManager, foreground services |
| **Charts** | Vico |
| **Image loading** | Coil |
| **Widget** | Glance AppWidget |

## Requirements

- Android 8.0+ (API 26)
- Android Studio Ladybug or newer
- JDK 17 (bundled JBR recommended)

## Getting Started

### Clone

```bash
git clone https://github.com/<your-org>/GameVault.git
cd GameVault
```

### Build

```bash
./gradlew assembleDebug
```

The APK is output to `app/build/outputs/apk/debug/`.

### Run

Open the project in Android Studio and run on a device or emulator with API 26+.

### Release build

```bash
./gradlew assembleRelease
```

ProGuard/R8 minification and resource shrinking are enabled for the release build type.

## Architecture

```
com.gamevault.app/
├── data/          Room database, DAOs, entities, repository
├── di/            Hilt modules
├── domain/        Domain models and use cases
├── receiver/      BroadcastReceivers (package events, boot)
├── service/       VPN, timer, overlay, playtime tracker, export/import
├── ui/            Compose screens, ViewModels, theme, navigation
├── util/          Formatting helpers
└── widget/        Glance home screen widget
```

The app follows **MVVM** with a **Repository** layer. ViewModels expose `StateFlow`s consumed by Compose screens via `collectAsStateWithLifecycle()`. Room provides the single source of truth, with DataStore handling lightweight key-value preferences.

## Permissions

GameVault requests only the permissions it needs. Each one is explained in the in-app settings screen, and features degrade gracefully when a permission is denied.

| Permission | Purpose |
|-----------|---------|
| `QUERY_ALL_PACKAGES` | Detect all installed games |
| `PACKAGE_USAGE_STATS` | Read playtime via UsageStatsManager |
| `SYSTEM_ALERT_WINDOW` | Floating timer overlay |
| `ACCESS_NOTIFICATION_POLICY` | Toggle Do Not Disturb |
| `FOREGROUND_SERVICE` | Timer, overlay, and VPN services |
| `POST_NOTIFICATIONS` | Timer alerts and weekly reports |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule background workers after reboot |
| `READ_MEDIA_IMAGES` | Screenshot gallery |
| `INTERNET` | DNS forwarding for the ad blocker |

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes
4. Push to the branch and open a pull request

Please make sure the project builds cleanly (`./gradlew assembleDebug`) before submitting.

## License

All rights reserved. See [LICENSE](LICENSE) for details.

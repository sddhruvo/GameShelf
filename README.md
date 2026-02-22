<p align="center">
  <h1 align="center">GameVault</h1>
  <p align="center">Your personal game launcher & tracker for Android</p>
</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-green">
  <img alt="Version" src="https://img.shields.io/badge/version-1.0.0-blue">
  <img alt="License" src="https://img.shields.io/badge/license-All%20Rights%20Reserved-red">
</p>

---

GameVault brings all your mobile games into one place. It detects your installed games, tracks how long you play, and gives you detailed stats and tools to manage your gaming habits — all offline, with zero ads and zero data collection.

## Screenshots

<!-- Replace with actual screenshots -->
<p align="center">
  <img src="docs/screenshots/home.png" width="200" alt="Home">
  <img src="docs/screenshots/detail.png" width="200" alt="Game Detail">
  <img src="docs/screenshots/stats.png" width="200" alt="Stats">
  <img src="docs/screenshots/settings.png" width="200" alt="Settings">
</p>

## Features

### Game Library
- Automatically detects installed games, or manually pick which apps count as games
- Three view modes — grid, list, and icon
- Search, sort (by name, last played, most played, install date, or size), and favorite your games
- Organize games into custom collections with names and descriptions
- Add personal ratings, notes, and tags to any game
- Hide games you don't want to see
- Set custom cover images

### Playtime Tracking
- Automatic session recording — just play, GameVault handles the rest
- Stats at a glance: today, this week, this month, and all time
- Interactive weekly bar chart showing your daily playtime
- Play streak tracking — see how many consecutive days you've been gaming
- Per-game breakdown with ranked most-played list

### Gaming Tools
- **Floating Timer** — draggable overlay that shows elapsed or remaining time on top of any game
- **Do Not Disturb** — automatically silences notifications when you launch a game
- **Daily Screen Time Goal** — set a daily limit and get notified when you hit it
- **Quick Settings Tile** — launch GameVault from your notification shade

### Ad Blocker
- Lightweight, local DNS-based ad blocking — no traffic leaves your device
- Starts automatically when you launch a game (if enabled)
- Per-game whitelisting for games that use rewarded ads

### Updates & History
- Tracks game version changes with old/new version numbers
- Records app size changes with each update
- Add your own changelog notes to any update
- Weekly gaming report notification

### Data & Backup
- Export your full library, sessions, and settings as JSON
- Import from a previous backup to restore everything
- Home screen widget for quick access

### Automatic Updates
- Checks for new releases on app launch
- Shows changelog and a direct download link when an update is available

## Download

Grab the latest APK from [Releases](https://github.com/sddhruvo/GameVault/releases).

## Privacy & Data

**Everything stays on your device.** GameVault does not collect, transmit, or share any personal data. There are no analytics, no telemetry, no servers.

The only network requests the app makes are:
- **Ad blocker** — local DNS filtering (queries stay on-device via a local VPN)
- **Update checker** — a single call to the GitHub API on launch to check for new versions

### Permissions and why they're needed

| Permission | Purpose | Required? |
|---|---|---|
| **Installed apps** | Detect which apps are games | No — add games manually instead |
| **Usage stats** | Calculate playtime per game | No — playtime tracking won't work |
| **Do Not Disturb** | Silence notifications while gaming | No |
| **Draw over apps** | Show the floating timer overlay | No |
| **VPN service** (local only) | Filter DNS for ad blocking | No |
| **Notifications** | Timer alerts and weekly reports | No |
| **Storage** | Pick custom cover images | No |

### What the app does NOT do

- Does not require an account or sign-in
- Does not collect analytics or crash reports
- Does not contain ads
- Does not share data with third parties

## Feedback

Found a bug? Use **Settings > Report a Bug** in the app to send a pre-filled email, or open an issue on [GitHub](https://github.com/sddhruvo/GameVault/issues).

## Support

If you enjoy GameVault and want to support its development:

[Buy me a coffee via PayPal](https://paypal.me/sddhruvo)

## License

All rights reserved. See [LICENSE](LICENSE) for details.

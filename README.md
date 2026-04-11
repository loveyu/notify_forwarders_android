# Notify Forwarders Android

[![Build APK](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml/badge.svg)](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-13.0%2B-green.svg)](https://android-arsenal.com/api?level=33)

> **[中文文档](README_CN.md)** | **English Documentation**

A modern Android notification forwarding application that captures system notifications and forwards them to a configured server in real-time.

## Features

- **Real-time Notification Forwarding**: Capture and forward Android system notifications instantly
- **Mirror Destinations**: Simultaneously forward notifications to multiple servers via DSN configuration
- **Smart Filtering**: YAML-based ignore rules (app name + regex/text matching) and deduplication
- **Clipboard & Image Integration**: Send clipboard content and gallery images with Base64 encoding
- **Icon URL Conversion**: Convert Base64 icon data to remote URLs with cache (memory + SQLite)
- **Modern UI**: Built with Jetpack Compose and Material Design 3
- **Multi-language Support**: 7 languages with automatic detection and manual switching
- **Battery Optimized**: Smart battery usage optimization with foreground service

## Quick Start

1. Install the APK and launch the app
2. Grant notification permissions:
   - **Notification Listener**: Required for reading system notifications
   - **POST_NOTIFICATIONS**: Required for foreground service (Android 13+)
3. Configure server address in settings
4. Optional: Enable mirror destinations, icon forwarding, clipboard features

### System Requirements

- Android 13 (API 33) or higher
- Notification listener permission
- Internet connection

### Development Requirements

- JDK 17
- Android SDK: API 36 (Android 16)
- Kotlin 2.2.10, Gradle 9.3.1, AGP 9.1.0

## Build

```bash
# Debug
./gradlew assembleDebug

# Release (requires keystore.properties)
./gradlew assembleRelease
```

## API Endpoints

| Endpoint | Purpose |
|----------|---------|
| `POST /api/notify` | Forward notifications |
| `POST /api/notify/clipboard/text` | Send clipboard text |
| `POST /api/notify/clipboard/image` | Send clipboard images |
| `POST /api/notify/image/raw` | Send gallery images + metadata |
| `GET /api/version` | Check server version |

Default port: `19283`. Content-Type: `application/json`.

## Mirror Forwarding

Configure mirror destinations in YAML to forward to multiple servers simultaneously:

```yaml
mirror:
  enabled: true
  destinations:
    - "http://192.168.1.100:19283?connectTimeout=5000&retry=3"
    - "https://api.example.com?token=secret&verifySSL=false"
```

DSN supports: `connectTimeout`, `writeTimeout`, `retry`, `retryInterval`, `token`, `verifySSL`.

Mirror forwarding is fully independent — failures do not affect the main server or other mirrors.

## YAML Configuration

The app supports remote or local YAML configuration with the following modules:

- **ignore-filter**: Filter notifications by app name + regex/text patterns
- **api**: Custom endpoint paths and timeouts
- **dedup-filter**: Duplicate message filtering with configurable strategy and time window
- **icon-url**: Convert Base64 icons to remote URLs with caching
- **mirror**: Mirror destinations with DSN format

See [example_config.yaml](app/src/main/res/raw/example_config.yaml) for full configuration reference.

## Multi-language Support

| Language | Code |
|----------|------|
| Simplified Chinese (default) | `zh` |
| English | `en` |
| Traditional Chinese | `zh-TW` |
| Japanese | `ja` |
| Russian | `ru` |
| French | `fr` |
| German | `de` |

## Technical Specifications

| Property | Value |
|----------|-------|
| Language | Kotlin 2.2.10 |
| UI Framework | Jetpack Compose (BOM 2025.07.00) |
| Min SDK | API 33 (Android 13) |
| Target SDK | API 36 (Android 16) |
| Compile SDK | API 36 |
| Java Compatibility | JDK 17 |
| Build System | Gradle 9.3.1 (Kotlin DSL) |
| Networking | HttpURLConnection (no third-party HTTP library) |
| Config Parsing | SnakeYAML 2.2 |

## Documentation

- [API Documentation](docs/API_DOCUMENTATION.md)
- [Build Instructions](docs/BUILD_INSTRUCTIONS.md)
- [Quick Start Guide](docs/QUICK_START.md)
- [Release Notes](docs/RELEASE_NOTES_v1.6.0.md)

## Automated Builds

- **Dev Branch**: Automatic debug APK builds
- **Release Tags**: Automatic release APK builds with GitHub releases

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

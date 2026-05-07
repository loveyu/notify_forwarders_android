# NotiHub - Android Notification Forwarder

[![Build APK](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml/badge.svg)](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml)

Captures Android system notifications in real-time and forwards them to your server. Supports multi-destination mirror forwarding, smart filtering, clipboard and image sending.

> **[中文文档](README_CN.md)** | **English Documentation**

## Features

- **Notification Forwarding** — Capture system notifications, POST as JSON to your server
- **Mirror Forwarding** — Per-endpoint parallel forwarding to multiple servers, fully independent
- **Filter & Dedup** — YAML-based ignore rules (regex/text) + duplicate message filtering (strategy + time window)
- **Clipboard & Image** — Send clipboard text/images, latest gallery image from device
- **Icon Processing** — Rounded corner cropping, Base64 encoding, remote URL conversion (LRU + SQLite cache)
- **Service Keep-Alive** — Foreground service + JobScheduler periodic check + boot auto-start + battery optimization whitelist
- **Multi-language** — 7 languages with system auto-detect and manual switching
- **Remote YAML Config** — Download config from URL, in-app preview with syntax highlighting, external editor support

## Quick Start

1. Install APK, grant notification listener permission
2. Configure server address in settings (supports verification code flow)
3. Optional: mirror forwarding, icon forwarding, remote YAML config

**Requirements**: Android 13+ (API 33)

## Build

```bash
./gradlew assembleDebug          # Debug build
./gradlew assembleRelease        # Release build (requires keystore.properties)
```

**Dev Environment**: JDK 17 · Kotlin 2.2.10 · Gradle 9.3.1 · AGP 9.1.0

## API Endpoints

| Endpoint | Purpose |
|----------|---------|
| `POST /api/notify` | Forward notifications |
| `POST /api/notify/clipboard/text` | Send clipboard text |
| `POST /api/notify/clipboard/image` | Send clipboard images |
| `POST /api/notify/image/raw` | Send gallery images |
| `GET /api/version` | Server version check |

Default port: `19283`. Content-Type: `application/json`.

## YAML Configuration

Config can be loaded locally or from a remote URL. Modules:

- **ignore-filter** — Filter by app name + regex/text patterns
- **dedup-filter** — Duplicate message filtering (strategy, time window, package matching)
- **api** — Custom endpoint paths and timeouts
- **icon-url** — Base64 icon to remote URL conversion
- **mirror** — Per-endpoint mirror destinations

Full reference: [example_config.yaml](app/src/main/res/raw/example_config.yaml).

## Mirror Forwarding

Each endpoint can have independent mirror destinations. DSN contains the full request path with parameters:

```yaml
mirror:
  enabled: true
  endpoints:
    notify:
      - "http://192.168.1.100:19283/api/notify?connectTimeout=5000&retry=3"
    clipboardText:
      - "https://api.example.com/api/notify/clipboard/text?token=secret&verifySSL=false"
```

DSN parameters: `connectTimeout`, `writeTimeout`, `retry`, `retryInterval`, `token`, `verifySSL`

Mirror forwarding runs asynchronously in parallel — failures do not affect the primary server or other mirrors.

## Multi-language

Simplified Chinese (default) · English · Traditional Chinese · Japanese · Russian · French · German

## License

[MIT License](LICENSE)

# Notify Forwarders Android

[![Build APK](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml/badge.svg)](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://android-arsenal.com/api?level=26)

> **[中文文档](README_CN.md)** | **English Documentation**

A modern Android notification forwarding application that captures system notifications and forwards them to a configured server in real-time.

## ✨ Features

- 📱 **Real-time Notification Forwarding**: Capture and forward Android system notifications instantly
- 🎨 **Modern UI**: Built with Material Design 3 guidelines
- ⚡ **High Performance**: Native UI powered by Jetpack Compose
- 🔧 **Flexible Configuration**: Customizable server address and notification limits
- 🔋 **Battery Optimized**: Smart battery usage optimization
- 🚀 **Automated CI/CD**: Complete build and release automation

## 📥 Download & Installation

### Latest Release
Download the latest stable version from the [Releases page](https://github.com/loveyu/notify_forwarders_android/releases).

### Development Builds
Development APKs are available in [Actions artifacts](https://github.com/loveyu/notify_forwarders_android/actions) (30-day retention).

### System Requirements
- Android 8.0 (API 26) or higher
- Notification access permission required
- Battery optimization exemption recommended

## 🚀 Quick Start

### Initial Setup
1. Install the APK and launch the app
2. Grant notification access permission
3. Configure server address in settings
4. Optional: Adjust notification limit settings

### Required Permissions
- **Notification Access**: Required for reading system notifications
- **Battery Optimization Exemption**: Recommended for stable background service
- **Internet**: Required for forwarding notifications to server

For detailed setup instructions, see [QUICK_START.md](QUICK_START.md).

## 🛠️ Development

### Prerequisites
- Android Studio Arctic Fox or newer
- JDK 17
- Android SDK API 35

### Local Build
```bash
# Clone the repository
git clone https://github.com/loveyu/notify_forwarders_android.git
cd notify_forwarders_android

# Build debug version
./gradlew assembleDebug

# Build release version (requires signing configuration)
./gradlew assembleRelease
```

### Signing Configuration
For release builds:
1. Copy `keystore.properties.template` to `keystore.properties`
2. Fill in your keystore information
3. Run the build command

See [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) for detailed build instructions.

## 🏗️ Technical Specifications

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM
- **Build System**: Gradle (Kotlin DSL)
- **Min SDK**: API 26 (Android 8.0)
- **Target SDK**: API 35 (Android 15)
- **Java Version**: JDK 17

## 🔄 Automated Builds

This project features complete GitHub Actions automation:

- **Dev Branch**: Automatic debug APK builds
- **Release Tags**: Automatic release APK builds with GitHub releases

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

If you encounter issues or have suggestions:
- Create an [Issue](https://github.com/loveyu/notify_forwarders_android/issues)

## 📚 Documentation

- **[中文文档](README_CN.md)** - Chinese documentation
- [Build Instructions](BUILD_INSTRUCTIONS.md) - Detailed build guide
- [Quick Start](QUICK_START.md) - Getting started guide
- [Java 17 Migration](JAVA17_MIGRATION.md) - Migration guide
- [Release Setup](RELEASE_SETUP.md) - Release configuration guide
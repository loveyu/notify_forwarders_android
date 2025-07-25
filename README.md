# Notify Forwarders Android

[![Build APK](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml/badge.svg)](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://android-arsenal.com/api?level=26)

> **[中文文档](README_CN.md)** | **English Documentation**

A modern Android notification forwarding application that captures system notifications and forwards them to a configured server in real-time.

## ✨ Features

- 📱 **Real-time Notification Forwarding**: Capture and forward Android system notifications instantly
- 🎨 **Modern UI**: Built with Material Design 3 guidelines and edge-to-edge display
- ⚡ **High Performance**: Native UI powered by Jetpack Compose
- 🔧 **Flexible Configuration**: Customizable server address and notification limits
- 🌍 **Multi-language Support**: 7 languages with automatic detection and manual switching
- 🔋 **Battery Optimized**: Smart battery usage optimization with modern permission handling
- 🚀 **Automated CI/CD**: Complete build and release automation
- 🔐 **Android 13+ Ready**: Full support for modern notification permissions and APIs
- 📋 **Smart Notification Management**: Updated notifications automatically move to the top of the list
- 🎯 **App Icon Support**: Optional forwarding of application icons with notifications

## 📥 Download & Installation

### Latest Release
Download the latest stable version from the [Releases page](https://github.com/loveyu/notify_forwarders_android/releases).

### Development Builds
Development APKs are available in [Actions artifacts](https://github.com/loveyu/notify_forwarders_android/actions) (30-day retention).

### System Requirements
- Android 13 (API 33) or higher
- Notification access permission required
- POST_NOTIFICATIONS permission required (Android 13+)
- Battery optimization exemption recommended

## 🚀 Quick Start

> **Note**: This app requires Android 13 (API 33) or higher. For older Android versions, please use a previous release.

### Initial Setup
1. Install the APK and launch the app
2. Grant notification permissions:
   - **POST_NOTIFICATIONS**: Required for displaying foreground service notifications (Android 13+)
   - **Notification Listener**: Required for reading system notifications
3. Configure server address in settings
4. Optional: Choose your preferred language in settings
5. Optional: Adjust notification limit settings
6. Optional: Enable notification icon forwarding

### Required Permissions
- **Notification Access**: Required for reading system notifications
- **POST_NOTIFICATIONS**: Required for displaying foreground service notifications (Android 13+)
- **Battery Optimization Exemption**: Recommended for stable background service
- **Internet**: Required for forwarding notifications to server

For detailed setup instructions, see [QUICK_START.md](QUICK_START.md).

### Permission Setup Guide

#### 1. POST_NOTIFICATIONS Permission (Android 13+)
- Automatically requested when the app starts
- Required for displaying foreground service notifications
- Can be granted through the app's permission request dialog

#### 2. Notification Listener Permission
- Must be manually enabled in Android settings
- Go to: Settings → Apps → Special app access → Notification access
- Find "Notify forwarders" and enable it
- This permission allows the app to read system notifications

### Troubleshooting

#### App not receiving notifications?
1. Ensure both permissions are granted
2. Check if battery optimization is disabled for the app
3. Verify the notification listener service is enabled
4. Restart the app if permissions were recently granted

#### Notifications not forwarding?
1. Check server address configuration
2. Ensure the server is running and accessible
3. Verify network connectivity
4. Check if notification forwarding is enabled in settings

## 📋 Recent Updates

### Version 1.2.0 - Android 13+ Support
- **🔐 Modern Permission System**: Full support for Android 13+ POST_NOTIFICATIONS permission
- **📱 Minimum Android Version**: Updated to Android 13 (API 33) for enhanced security and performance
- **🎯 Smart Notification Management**: Updated notifications now move to the top of the list automatically
- **🔧 Code Modernization**: Removed deprecated APIs and improved compatibility
- **🌍 Enhanced Translations**: Updated all language translations for new permission features
- **⚡ Performance Improvements**: Optimized notification handling and UI responsiveness

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

## 🌍 Multi-language Support

The app supports 7 languages with intelligent language detection:

- **🇨🇳 简体中文** (Simplified Chinese) - Default
- **🇺🇸 English**
- **🇹🇼 繁體中文** (Traditional Chinese)
- **🇯🇵 日本語** (Japanese)
- **🇷🇺 Русский** (Russian)
- **🇫🇷 Français** (French)
- **🇩🇪 Deutsch** (German)

### Language Features
- **Automatic Detection**: Automatically selects the best language based on system locale
- **Regional Support**: Chinese variants are automatically selected based on region (Mainland China → Simplified, Taiwan/Hong Kong/Macau → Traditional)
- **Manual Override**: Users can manually select their preferred language in settings
- **Persistent Settings**: Language preference is saved and applied on app restart

## 🏗️ Technical Specifications

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM
- **Build System**: Gradle (Kotlin DSL)
- **Min SDK**: API 33 (Android 13)
- **Target SDK**: API 35 (Android 15)
- **Java Version**: JDK 17
- **Internationalization**: Android Resources with 7 language variants

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
# Notify Forwarders Android

[![Build APK](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml/badge.svg)](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-13.0%2B-green.svg)](https://android-arsenal.com/api?level=33)

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
- 🎯 **Smart Icon Forwarding**: Intelligent icon handling with notification icons prioritized over app icons in both forwarding and UI display, featuring configurable rounded corners (5%-50%) and optimized caching

## 📥 Download & Installation

### Latest Release
Download the latest stable version from the [Releases page](https://github.com/loveyu/notify_forwarders_android/releases).

### Development Builds
Development APKs are available in [Actions artifacts](https://github.com/loveyu/notify_forwarders_android/actions) (30-day retention).

### System Requirements

#### Minimum Requirements
- **Android Version**: Android 13 (API level 33) or higher
- **RAM**: 2GB or more recommended
- **Storage**: 50MB free space for installation
- **Network**: Internet connection required for notification forwarding

#### Development Requirements
- **Android Studio**: Arctic Fox (2020.3.1) or newer
- **JDK**: OpenJDK 17 or Oracle JDK 17
- **Android SDK**: API level 35 (Android 15)
- **Gradle**: 8.11.1 or newer
- **Kotlin**: 2.0.21 or newer

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
6. Optional: Enable smart notification icon forwarding

### Required Permissions

#### Core Permissions
- **🔔 Notification Listener Service**:
  - **Purpose**: Read and capture system notifications from all apps
  - **Setup**: Manual configuration required in Android Settings
  - **Critical**: App cannot function without this permission

- **📱 POST_NOTIFICATIONS** (Android 13+):
  - **Purpose**: Display foreground service notifications and status updates
  - **Setup**: Automatically requested on first launch
  - **Required**: Essential for service visibility and user feedback

#### System Permissions
- **🌐 INTERNET**:
  - **Purpose**: Forward captured notifications to configured server
  - **Setup**: Automatically granted (normal permission)
  - **Required**: Core functionality depends on network access

- **⚡ FOREGROUND_SERVICE**:
  - **Purpose**: Run notification capture service in background
  - **Setup**: Automatically granted (normal permission)
  - **Required**: Ensures continuous notification monitoring

- **🔄 FOREGROUND_SERVICE_DATA_SYNC** (Android 14+):
  - **Purpose**: Specify foreground service type for data synchronization
  - **Setup**: Automatically granted (normal permission)
  - **Required**: Compliance with Android 14+ foreground service restrictions

#### Optional but Recommended
- **🔋 Battery Optimization Exemption**:
  - **Purpose**: Prevent system from killing the service to save battery
  - **Setup**: Manual configuration in Battery settings
  - **Benefit**: Ensures reliable notification forwarding

- **🚀 RECEIVE_BOOT_COMPLETED**:
  - **Purpose**: Automatically start service after device reboot
  - **Setup**: Automatically granted (normal permission)
  - **Benefit**: Seamless operation without manual restart

For detailed setup instructions, see [QUICK_START.md](QUICK_START.md).

### Permission Setup Guide

#### 1. POST_NOTIFICATIONS Permission (Android 13+)
**Automatic Setup:**
- Automatically requested when the app starts for the first time
- Required for displaying foreground service notifications and status updates
- Can be granted through the app's permission request dialog

**Manual Setup (if denied):**
1. Open Android Settings
2. Go to Apps → Notify Forwarders → Permissions
3. Enable "Notifications" permission
4. Restart the app

#### 2. Notification Listener Permission
**Manual Setup Required:**
1. Open Android Settings
2. Navigate to: **Settings → Apps → Special app access → Notification access**
   - Alternative path: **Settings → Security & Privacy → Privacy → Notification access**
3. Find "Notify Forwarders" in the list
4. Toggle the switch to enable notification access
5. Confirm the permission in the dialog that appears
6. Return to the app - it should now show "Permission Granted"

**Verification:**
- The app's main screen will show "Permission Granted" status
- If still showing "Permission Required", restart the app

#### 3. Battery Optimization Exemption (Recommended)
**Setup Steps:**
1. Open Android Settings
2. Go to **Battery → Battery Optimization** (or **Apps → Special app access → Battery optimization**)
3. Find "Notify Forwarders" in the list
4. Select "Don't optimize" or "Allow"
5. Confirm the selection

**Why This Matters:**
- Prevents Android from killing the service to save battery
- Ensures continuous notification monitoring
- Critical for reliable operation on devices with aggressive battery management

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

## 🎯 Smart Icon Forwarding

### Overview
The app features an intelligent icon forwarding system that enhances notification visualization on the server side.

### Key Features
- **🔄 Priority-based Icon Selection**: Notification icons are prioritized over application icons for better context in both server forwarding and local UI display
- **🎨 Configurable Rounded Corners**: Icons display with customizable rounded corners (5%-50%, default 10%) for modern aesthetics
- **⚡ Optimized Caching**: Only application icons are cached (up to 1 day), notification icons are processed in real-time
- **📦 Integrated Forwarding**: Icon data is sent directly with notifications, eliminating separate API calls
- **📱 Consistent UI Display**: Notification list shows the same icons that are forwarded to the server, ensuring visual consistency
- **🌍 Multi-language Support**: Icon settings are fully translated across all supported languages

### How It Works
1. **Icon Detection**: When a notification arrives, the system first checks for notification-specific icons (largeIcon, smallIcon)
2. **Fallback Strategy**: If no notification icon exists, the system uses the cached application icon
3. **Real-time Processing**: Notification icons are converted to Base64 and MD5 hash in real-time (no caching)
4. **Efficient Delivery**: Icon data is included directly in the notification payload to the server
5. **Consistent UI Display**: The notification list UI displays the same icon that was captured and forwarded, ensuring visual consistency
6. **Smart Caching**: Application icons are cached for 24 hours to reduce processing overhead

### Configuration
1. Enable icon forwarding in **Settings → Notification Icon Settings → Send Notification Icons**
2. Customize icon corner radius (5%-50%) using the slider in the same settings section

## 📋 Recent Updates

### Version 1.3.2 - Optimized Icon Display Consistency
- **🎯 Fixed Notification Icon Display**: Notification list now correctly displays the same icons that are forwarded to the server
- **🔄 Improved Icon Priority Logic**: UI components now prioritize notification icons over application icons, matching the forwarding behavior
- **📱 Enhanced Visual Consistency**: Ensures that users see the exact same icons in the app that are sent to the server
- **🛠️ Code Optimization**: Streamlined icon loading logic for better performance and maintainability

### Version 1.3.1 - Enhanced Icon Customization
- **🎨 Configurable Corner Radius**: Adjustable icon corner radius from 5% to 50% (default 10%)
- **⚙️ Enhanced Settings UI**: Interactive slider for real-time corner radius adjustment
- **🌍 Complete Translations**: Corner radius settings fully translated across all 7 supported languages
- **🔧 Improved Icon Processing**: Better notification icon detection and processing
- **📱 Real-time Preview**: Instant feedback when adjusting corner radius settings

### Version 1.3.0 - Smart Icon Forwarding
- **🎯 Intelligent Icon Handling**: Prioritizes notification icons over app icons for better context
- **🎨 Modern Icon Design**: Rounded corners replace circular icons for contemporary look
- **⚡ Optimized Performance**: Notification icons processed in real-time, app icons cached efficiently
- **📦 Streamlined Forwarding**: Icon data sent directly with notifications, no separate API calls
- **🌍 Complete Translations**: Icon settings fully translated across all 7 supported languages
- **🔧 Enhanced UI**: Improved test notification button layout for better multi-language support

### Version 1.2.0 - Android 13+ Support
- **🔐 Modern Permission System**: Full support for Android 13+ POST_NOTIFICATIONS permission
- **📱 Minimum Android Version**: Updated to Android 13 (API 33) for enhanced security and performance
- **🎯 Smart Notification Management**: Updated notifications now move to the top of the list automatically
- **🔧 Code Modernization**: Removed deprecated APIs and improved compatibility
- **🌍 Enhanced Translations**: Updated all language translations for new permission features
- **⚡ Performance Improvements**: Optimized notification handling and UI responsiveness

## 🛠️ Development

### Prerequisites

#### Required Software
- **Android Studio**: Arctic Fox (2020.3.1) or newer (Recommended: Latest stable)
- **JDK**: OpenJDK 17 or Oracle JDK 17
- **Android SDK**:
  - Platform: API 35 (Android 15)
  - Build Tools: 35.0.0 or newer
  - Platform Tools: Latest
- **Git**: For version control and repository cloning

#### Development Dependencies
The project uses Gradle Version Catalog for dependency management. Key dependencies include:

**Core Android Libraries:**
- AndroidX Core KTX: 1.16.0
- AndroidX Lifecycle Runtime KTX: 2.8.7
- AndroidX Activity Compose: 1.10.1

**Jetpack Compose:**
- Compose BOM: 2024.09.00
- Material Design 3: Latest
- UI Components: Latest
- UI Tooling: Latest (debug builds)

**Testing Libraries:**
- JUnit: 4.13.2
- AndroidX Test JUnit: 1.2.1
- Espresso Core: 3.6.1
- Compose UI Test: Latest

**Build Tools:**
- Android Gradle Plugin: 8.11.1
- Kotlin: 2.0.21
- Kotlin Compose Compiler: 2.0.21

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

### Core Technologies
- **Programming Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (BOM 2024.09.00)
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Build System**: Gradle 8.11.1 with Kotlin DSL
- **Dependency Management**: Version Catalog (TOML)

### Android Platform
- **Minimum SDK**: API 33 (Android 13.0)
- **Target SDK**: API 35 (Android 15.0)
- **Compile SDK**: API 35 (Android 15.0)
- **Java Compatibility**: JDK 17 (source & target)
- **Kotlin JVM Target**: 17

### Key Dependencies
- **AndroidX Core**: 1.16.0
- **Lifecycle Runtime**: 2.8.7
- **Activity Compose**: 1.10.1
- **Material Design 3**: Latest (via Compose BOM)
- **Compose UI**: Latest (via Compose BOM)

### Build Configuration
- **Android Gradle Plugin**: 8.11.1
- **Kotlin Compiler**: 2.0.21
- **ProGuard**: Enabled for release builds
- **Resource Shrinking**: Enabled for release builds
- **Code Obfuscation**: Enabled for release builds

### Internationalization
- **Supported Languages**: 7 language variants
- **Resource System**: Android Resources with automatic locale detection
- **Fallback Language**: Simplified Chinese (default)

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
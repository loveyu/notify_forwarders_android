# Notify Forwarders Android v1.4.0 Release Notes

**Release Date:** July 26, 2025  
**Version:** 1.4.0  
**Build:** 4

## 🎉 Major Features & Enhancements

### 📱 Android 13+ Upgrade
- **Upgraded minimum SDK to Android 13 (API 33)** for enhanced security and performance
- **Modern permission system** with improved user experience
- **Enhanced notification access** with better compatibility across Android versions

### 🎨 Notification Icon Support
- **NEW: Notification icon display** - View app icons alongside notification content
- **Configurable corner radius** for notification icons with smooth visual effects
- **Icon caching system** for optimal performance and memory management
- **Random colored icon testing** for development and debugging
- **Consistent UI display** across different notification types

### 🌍 Comprehensive Multi-Language Support
- **6 new languages added:**
  - 🇩🇪 German (Deutsch)
  - 🇫🇷 French (Français) 
  - 🇯🇵 Japanese (日本語)
  - 🇷🇺 Russian (Русский)
  - 🇹🇼 Traditional Chinese (繁體中文)
  - 🇺🇸 English (Enhanced)
- **Complete string resource extraction** - All hardcoded strings moved to resources
- **Dynamic language switching** with proper locale handling
- **Comprehensive translation coverage** for all UI elements

### ⚡ Enhanced User Experience
- **Quick toggle switches** for notification receive and forward functions
- **Improved settings interface** with better organization and accessibility
- **Enhanced notification service** with better reliability and performance
- **Modern UI components** following Material Design guidelines

## 🔧 Technical Improvements

### 🏗️ Architecture & Code Quality
- **New utility classes:**
  - `IconCacheManager` - Efficient icon caching and management
  - `LocaleHelper` - Dynamic language switching support
  - `PermissionUtils` - Streamlined permission handling
  - `ServerPreferences` - Enhanced server configuration management
- **Application class** for global state management
- **Improved error handling** and logging throughout the application

### 🧪 Testing & Quality Assurance
- **New unit tests** for icon cache management
- **Multi-language testing scripts** for translation verification
- **Enhanced build scripts** for development and release processes
- **Comprehensive documentation** updates

### 📚 Documentation Updates
- **Updated README** with comprehensive system requirements and setup instructions
- **New feature documentation:**
  - `ANDROID_13_UPGRADE_SUMMARY.md` - Detailed upgrade information
  - `ICON_FEATURE_README.md` - Icon functionality guide
  - `MULTILANG_COMPLETION_REPORT.md` - Multi-language implementation details
- **Enhanced build and setup instructions**

## 🐛 Bug Fixes & Optimizations

- **Fixed notification icon display consistency** across different UI states
- **Optimized notification processing** for better performance
- **Improved memory management** in icon caching system
- **Enhanced error handling** for edge cases
- **Better resource cleanup** to prevent memory leaks

## 🔄 Migration Notes

### For Existing Users
- **Automatic migration** from previous versions
- **Preserved user settings** and configurations
- **Enhanced permission prompts** may require re-granting notification access

### For Developers
- **Minimum Android version** now requires Android 13+
- **Updated dependencies** and build configurations
- **New utility classes** available for extended functionality

## 📋 System Requirements

- **Android 13.0 (API 33)** or higher
- **Notification access permission** required
- **Network access** for forwarding functionality
- **Storage permission** for icon caching (automatically managed)

## 🚀 What's Next

- Enhanced notification filtering options
- Advanced server configuration features
- Performance optimizations for large notification volumes
- Additional language support based on user feedback

## 📞 Support & Feedback

For issues, feature requests, or feedback:
- **GitHub Issues:** [Report bugs or request features](https://github.com/loveyu/notify_forwarders_android/issues)
- **Documentation:** Check the updated README and feature guides

---

**Full Changelog:** [v1.3.0...v1.4.0](https://github.com/loveyu/notify_forwarders_android/compare/v1.3.0...v1.4.0)

**Download:** [Release v1.4.0](https://github.com/loveyu/notify_forwarders_android/releases/tag/v1.4.0)

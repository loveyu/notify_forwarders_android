# Release Notes - Version 1.5.0

**Release Date**: July 26, 2025  
**Version Code**: 5

## 🎉 What's New

### 📌 Persistent Notification System
- **NEW**: Added persistent notification with quick action buttons for clipboard and image sending
- **Enhanced**: Persistent notification state management with improved reliability
- **Optimized**: Notification state restoration after clipboard/image operations

### 📋 Clipboard Integration
- **NEW**: Send clipboard content (text and images) with Base64 encoding via notification actions
- **NEW**: Copy notification icons as PNG images to clipboard with transparency support
- **Enhanced**: Smart clipboard access with automatic permission handling
- **Improved**: Clipboard functionality with concurrency control and error handling

### 📸 Image Gallery Access
- **NEW**: Send latest images from gallery with EXIF metadata extraction
- **Enhanced**: Smart media permission handling with user-friendly error notifications
- **Optimized**: Image processing with Base64 encoding for transmission

### 🔔 Notification Management
- **Enhanced**: Optimized notification list functionality with interactive click features
- **Improved**: Notification layout optimization for better user experience
- **Fixed**: Removed foreground service notifications while keeping persistent notification buttons active
- **Optimized**: Icon retrieval with fallback to notification extras

### ⚡ Quick Actions & UI Improvements
- **NEW**: Enhanced quick send functionality with smart permission handling
- **NEW**: Compact main screen layout with dedicated send buttons
- **Enhanced**: Quick task notification logic with improved UX
- **Improved**: Immediate UI updates for notification settings
- **Added**: Multi-language support for quick actions

### 🎨 Icon & Visual Enhancements
- **Enhanced**: Icon corner radius settings optimization (5%-50% configurable)
- **NEW**: Notification list icon display toggle
- **Improved**: Icon caching and display performance
- **Optimized**: Visual feedback and UI responsiveness

### 🔐 Permission & Error Handling
- **Enhanced**: Smart permission handling for clipboard and media access
- **NEW**: Intelligent error notifications with 20-second auto-dismiss
- **Improved**: User-friendly error notifications with detailed failure reasons
- **Optimized**: Permission check automation

### 📚 Documentation & API
- **NEW**: Comprehensive API documentation with complete REST API specifications
- **NEW**: API constants refactoring for better maintainability
- **Enhanced**: Updated README with new features and capabilities
- **Added**: Detailed implementation guides

## 🐛 Bug Fixes

- Fixed app crashes when clicking send clipboard/image buttons
- Resolved persistent notification state management issues
- Fixed UI flicker during notification operations
- Improved clipboard access reliability
- Enhanced notification forwarding stability
- Fixed quick send logic edge cases

## 🔧 Technical Improvements

- Refactored API constants structure for better organization
- Enhanced error handling throughout the application
- Improved concurrency control for background operations
- Optimized notification processing performance
- Enhanced state management reliability
- Improved memory usage and caching

## 📱 Compatibility

- **Android Version**: Android 13 (API level 33) or higher
- **Target SDK**: 35
- **Minimum SDK**: 33

## 🚀 Performance

- Optimized notification processing speed
- Improved memory usage for icon caching
- Enhanced background task efficiency
- Reduced UI response time
- Better battery usage optimization

## 📖 Documentation Updates

- Complete API documentation available in `API_DOCUMENTATION.md`
- Updated README with new features and installation instructions
- Enhanced build and setup documentation
- Added comprehensive feature guides

---

**Full Changelog**: [v1.4.0...v1.5.0](https://github.com/loveyu/notify_forwarders_android/compare/v1.4.0...v1.5.0)

**Download**: [Latest Release](https://github.com/loveyu/notify_forwarders_android/releases/tag/v1.5.0)

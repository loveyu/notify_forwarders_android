# Notify Forwarders Android

[![Build APK](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml/badge.svg)](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml)

一个Android通知转发应用。

## 功能特性

- 通知转发功能
- 现代化的Material Design 3界面
- 基于Jetpack Compose构建

## 自动构建

本项目配置了GitHub Actions自动构建：

- **Dev分支**: 自动构建Debug版本APK
- **Release标签**: 自动构建Release版本APK并创建GitHub Release

详细的构建说明请参考 [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md)

## 下载

### 最新Release版本
前往 [Releases页面](https://github.com/loveyu/notify_forwarders_android/releases) 下载最新的正式版本。

### 开发版本
开发版本的APK可以在 [Actions页面](https://github.com/loveyu/notify_forwarders_android/actions) 的Artifacts中下载。

## 本地构建

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 17
- Android SDK API 35

### 构建步骤
```bash
# 克隆仓库
git clone https://github.com/loveyu/notify_forwarders_android.git
cd notify_forwarders_android

# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本（需要配置签名）
./gradlew assembleRelease
```

## 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构**: MVVM
- **构建工具**: Gradle (Kotlin DSL)
- **最低SDK**: API 26 (Android 8.0)
- **目标SDK**: API 35 (Android 15)

## 许可证

MIT
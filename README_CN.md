# Notify Forwarders Android

[![Build APK](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml/badge.svg)](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml)

一个现代化的Android通知转发应用，可以将设备上的通知转发到指定的服务器。

## 功能特性

- 📱 **通知转发**: 实时捕获并转发Android系统通知
- 🎨 **现代化界面**: 基于Material Design 3设计规范
- ⚡ **高性能**: 使用Jetpack Compose构建的原生UI
- 🔧 **灵活配置**: 支持自定义服务器地址和通知数量限制
- 🌍 **多语言支持**: 支持7种语言，智能检测和手动切换
- 🔋 **电池优化**: 智能的电池使用优化
- 🚀 **自动构建**: 完整的CI/CD流程支持

## 下载安装

### 最新Release版本
前往 [Releases页面](https://github.com/loveyu/notify_forwarders_android/releases) 下载最新的正式版本APK。

### 开发版本
开发版本的APK可以在 [Actions页面](https://github.com/loveyu/notify_forwarders_android/actions) 的Artifacts中下载。

### 系统要求
- Android 13 (API 33) 或更高版本
- 需要通知访问权限
- 需要POST_NOTIFICATIONS权限（Android 13+）
- 建议关闭电池优化以确保服务稳定运行

## 使用说明

### 初始设置
1. 安装APK并打开应用
2. 授予通知权限：
   - **POST_NOTIFICATIONS**：显示前台服务通知所需（Android 13+）
   - **通知监听权限**：读取系统通知所需
3. 在设置中配置服务器地址
4. 可选：在设置中选择您偏好的语言
5. 可选：调整通知数量限制

### 权限配置
6. 可选：启用通知图标转发功能
- **通知访问权限**: 必需，用于读取系统通知
- **电池优化豁免**: 推荐，确保后台服务稳定运行
- **网络权限**: 必需，用于转发通知到服务器

## 🌍 多语言支持

应用支持7种语言，具备智能语言检测功能：

- **🇨🇳 简体中文** - 默认语言
- **🇺🇸 English** (英语)
- **🇹🇼 繁體中文** (繁体中文)
- **🇯🇵 日本語** (日语)
- **🇷🇺 Русский** (俄语)
- **🇫🇷 Français** (法语)
- **🇩🇪 Deutsch** (德语)

### 语言功能特性
- **自动检测**: 根据系统语言自动选择最佳匹配语言
- **地区支持**: 中文根据地区自动选择简繁体（中国大陆→简体，台港澳→繁体）
- **手动切换**: 用户可在设置中手动选择偏好语言
- **持久化设置**: 语言偏好会被保存并在应用重启时应用

## 技术规格

- **开发语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构模式**: MVVM
- **构建工具**: Gradle (Kotlin DSL)
- **最低SDK**: API 26 (Android 8.0)
- **目标SDK**: API 35 (Android 15)
- **Java版本**: JDK 17
- **国际化**: Android资源文件，支持7种语言变体

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

### 签名配置
Release版本需要配置签名：
1. 复制 `keystore.properties.template` 为 `keystore.properties`
2. 填入您的keystore信息
3. 运行构建命令

详细构建说明请参考 [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md)

## 📋 最近更新

### 版本 1.6.0 - 增强通知系统与高级剪贴板功能
- **🔔 优化持久通知**: 改进持久通知内容，提供简洁的状态显示和即时更新
- **📋 高级剪贴板集成**: 显著增强剪贴板功能，包含全面的测试套件并修复访问问题
- **🔐 增强媒体权限处理**: 改进媒体权限处理和通知系统集成
- **⚡ 运行时性能优化**: 优化应用运行时模式，添加适当的退出功能以更好地管理资源
- **🛠️ 现代化API**: 修复已弃用的固定API，现代化整个通知系统以更好地兼容Android 13+
- **🧪 全面测试套件**: 为剪贴板功能添加广泛的测试覆盖，确保在不同场景下的可靠性
- **🔄 实时状态更新**: 增强通知内容以显示接收和转发状态的即时更新
- **🚀 性能改进**: 各种底层优化，提供更流畅的操作和更好的电池效率

### 版本 1.5.0 - 持久通知与增强快速操作
- **📌 持久通知系统**: 添加带有剪贴板和图像发送快速操作按钮的持久通知
- **📋 增强剪贴板集成**: 通过通知操作发送剪贴板内容（文本和图像），支持Base64编码
- **📸 图库访问**: 发送图库中的最新图像，包含文件元数据（文件名、创建时间、修改时间、文件路径）
- **🔔 智能错误通知**: 20秒自动消失的错误通知，提供详细的失败原因
- **🎯 智能图标转发**: 将通知图标作为PNG图像复制到剪贴板，支持透明度
- **🔐 智能权限处理**: 自动权限检查，为剪贴板和媒体访问提供用户友好的错误通知
- **⚡ 增强快速操作**: 紧凑的主屏幕布局，专用的剪贴板和图像内容发送按钮
- **🎨 图标与视觉增强**: 优化图标圆角半径设置和通知列表图标显示切换
- **📚 全面文档**: 完整的API文档，包含REST API规范和实现细节

## 自动构建

本项目配置了完整的GitHub Actions自动构建流程：

- **Dev分支**: 自动构建Debug版本APK
- **Release标签**: 自动构建Release版本APK并创建GitHub Release

## 快速开始

详细的快速开始指南请参考 [QUICK_START.md](QUICK_START.md)

## 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 许可证

本项目采用 MIT 许可证 - 详情请查看 [LICENSE](LICENSE) 文件

## 支持

如果您遇到问题或有建议，请：
- 创建 [Issue](https://github.com/loveyu/notify_forwarders_android/issues)

## 相关文档

- [English Documentation](README.md)
- [构建说明](BUILD_INSTRUCTIONS.md)
- [快速开始](QUICK_START.md)
- [Java 17迁移指南](JAVA17_MIGRATION.md)
- [发布设置](RELEASE_SETUP.md)

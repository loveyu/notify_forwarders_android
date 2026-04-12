# Notify Forwarders Android

[![Build APK](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml/badge.svg)](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml)

一个现代化的 Android 通知转发应用，实时捕获系统通知并转发到配置的服务器。

> **[English Documentation](README.md)** | **中文文档**

## 功能特性

- **实时通知转发**: 捕获并转发 Android 系统通知
- **镜像目的地**: 通过 DSN 配置同时转发到多个服务器
- **智能过滤**: 基于 YAML 的忽略规则（应用名 + 正则/文本匹配）和去重过滤
- **剪贴板与图片集成**: 发送剪贴板内容和图库图片（Base64 编码）
- **图标 URL 转换**: 将 Base64 图标数据转换为远程 URL，支持缓存（内存 + SQLite）
- **现代化界面**: 基于 Jetpack Compose 和 Material Design 3
- **多语言支持**: 7 种语言，智能检测和手动切换
- **电池优化**: 前台服务 + 智能电池优化

## 快速开始

1. 安装 APK 并打开应用
2. 授予通知权限：
   - **通知监听权限**: 读取系统通知
   - **POST_NOTIFICATIONS**: 前台服务通知（Android 13+）
3. 在设置中配置服务器地址
4. 可选：启用镜像转发、图标转发、剪贴板功能

### 系统要求

- Android 13 (API 33) 或更高版本
- 通知监听权限
- 网络连接

### 开发环境

- JDK 17
- Android SDK: API 36 (Android 16)
- Kotlin 2.2.10、Gradle 9.3.1、AGP 9.1.0

## 构建

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本（需要 keystore.properties）
./gradlew assembleRelease
```

## API 端点

| 端点 | 用途 |
|------|------|
| `POST /api/notify` | 转发通知 |
| `POST /api/notify/clipboard/text` | 发送剪贴板文本 |
| `POST /api/notify/clipboard/image` | 发送剪贴板图片 |
| `POST /api/notify/image/raw` | 发送图库图片及元数据 |
| `GET /api/version` | 检查服务器版本 |

默认端口: `19283`，Content-Type: `application/json`。

## 镜像转发

通过 YAML 按端点独立配置镜像目的地，同时转发到多个服务器：

```yaml
mirror:
  enabled: true
  endpoints:
    notify:
      - "http://192.168.1.100:19283/api/notify?connectTimeout=5000&retry=3"
    clipboardText:
      - "https://api.example.com/api/notify/clipboard/text?token=secret&verifySSL=false"
```

每个端点（notify、clipboardText、clipboardImage、imageRaw）可独立配置镜像列表。DSN 包含完整请求路径，POST 时直接提交到该 URL。

DSN 支持: `connectTimeout`、`writeTimeout`、`retry`、`retryInterval`、`token`、`verifySSL`。

镜像转发完全独立，失败不影响主服务器和其他镜像。

## YAML 配置

应用支持远程或本地 YAML 配置，包含以下模块：

- **ignore-filter**: 按应用名 + 正则/文本模式过滤通知
- **api**: 自定义端点路径和超时
- **dedup-filter**: 重复消息过滤（可配置策略和时间窗口）
- **icon-url**: Base64 图标转远程 URL，支持缓存
- **mirror**: 按端点独立配置镜像目的地（DSN 包含完整路径）

完整配置参考 [example_config.yaml](app/src/main/res/raw/example_config.yaml)。

## 多语言支持

| 语言 | 代码 |
|------|------|
| 简体中文（默认） | `zh` |
| English | `en` |
| 繁體中文 | `zh-TW` |
| 日本語 | `ja` |
| Русский | `ru` |
| Français | `fr` |
| Deutsch | `de` |

## 技术规格

| 属性 | 值 |
|------|-----|
| 开发语言 | Kotlin 2.2.10 |
| UI 框架 | Jetpack Compose (BOM 2025.07.00) |
| 最低 SDK | API 33 (Android 13) |
| 目标 SDK | API 36 (Android 16) |
| 编译 SDK | API 36 |
| Java 兼容 | JDK 17 |
| 构建工具 | Gradle 9.3.1 (Kotlin DSL) |
| 网络库 | HttpURLConnection（无第三方 HTTP 库） |
| 配置解析 | SnakeYAML 2.2 |

## 相关文档

- [API 文档](docs/API_DOCUMENTATION.md)
- [构建说明](docs/BUILD_INSTRUCTIONS.md)
- [快速开始指南](docs/QUICK_START.md)
- [发布说明](docs/RELEASE_NOTES_v1.6.0.md)

## 自动构建

- **Dev 分支**: 自动构建 Debug 版本 APK
- **Release 标签**: 自动构建 Release 版本并创建 GitHub Release

## 贡献

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详情请查看 [LICENSE](LICENSE) 文件

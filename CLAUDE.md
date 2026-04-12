# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Android 通知转发应用（NotifyForwarders），监听系统通知并通过 HTTP POST 转发到配置的服务器。支持通知过滤、去重、图标URL转换、镜像多目的地转发。

## 构建命令

```bash
./gradlew assembleDebug          # 构建 Debug APK
./gradlew assembleRelease        # 构建 Release APK（需 keystore.properties）
./gradlew test                   # 运行单元测试
./gradlew connectedAndroidTest   # 运行设备测试
```

构建环境使用国内镜像源（腾讯云 Gradle + 阿里云 Maven），无需代理即可构建。

## Commit 规则

- 使用 `feat`/`fix`/`refactor`/`docs`/`chore` 等常规前缀
- commit message 使用中文描述
- **不添加任何 AI 签名**（不添加 `Co-Authored-By` 等行）

## 技术栈

- Kotlin 2.2, Android SDK 33-36, JDK 17
- Jetpack Compose + Material 3
- Gradle 9.3.1, AGP 9.1.0
- SnakeYAML 2.2（配置解析）
- 网络：纯 `HttpURLConnection`，无第三方 HTTP 库
- 持久化：`SharedPreferences`

## 架构

```
NotificationListenerService (通知捕获)
  → AppConfigManager (YAML 配置/过滤/去重)
  → NotificationService.forwardNotificationToServer() (主转发)
  → MirrorForwarder (镜像转发，独立异步)
```

核心数据流：通知到达 → 过滤检查 → 去重检查 → 图标处理 → 主服务器转发 + 镜像并行转发。

### 关键类

| 类 | 职责 |
|---|---|
| `NotificationService` | 通知监听、捕获、转发入口 |
| `NotificationActionService` | 剪贴板/图片发送（通知按钮触发） |
| `AppConfigManager` | YAML 配置单例，解析、过滤、持久化 |
| `MirrorForwarder` | 镜像转发工具（并行、重试、认证、SSL） |
| `ApiConstants` | 所有端点路径、超时、字段名常量 |
| `ServerPreferences` | SharedPreferences 封装 |

### YAML 配置模块

配置文件 `ignore_filter_config.yaml`，由 `AppConfigManager` 解析为 `AppConfig`，包含：
- `ignore-filter`：按应用名+正则/文本过滤通知
- `api`：自定义端点路径和超时
- `dedup-filter`：按包名的重复消息过滤（策略+时间窗口）
- `icon-url`：图标 Base64→远程URL 转换（含缓存）
- `mirror`：按端点独立配置镜像目的地（DSN 包含完整路径）

### 转发端点

所有端点均已支持镜像转发：
- `/api/notify` — 通知转发（NotificationService）
- `/api/notify/clipboard/text` — 剪贴板文本（NotificationActionService + ClipboardFloatingActivity）
- `/api/notify/clipboard/image` — 剪贴板图片（同上）
- `/api/notify/image/raw` — 相册图片（NotificationActionService）

## 多语言

支持 7 种语言，默认中文。资源目录：`values/`（中文）、`values-en/`、`values-zh-rTW/`、`values-ja/`、`values-ru/`、`values-fr/`、`values-de/`。

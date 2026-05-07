# NotiHub - Android 通知转发

[![Build APK](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml/badge.svg)](https://github.com/loveyu/notify_forwarders_android/actions/workflows/build.yml)

实时捕获 Android 系统通知，转发到自建服务器。支持多目的地镜像转发、智能过滤去重、剪贴板与图片发送。

> **[English Documentation](README.md)** | **中文文档**

## 功能

- **通知转发** — 实时捕获系统通知，JSON 格式 POST 到配置的服务器
- **镜像转发** — 按端点独立配置，并行转发到多个服务器，互不影响
- **过滤与去重** — YAML 配置忽略规则（正则/文本）+ 重复消息过滤（策略+时间窗口）
- **剪贴板与图片** — 发���剪贴板文本/图片、设备图库最新图片
- **图标处理** — 通知图标圆角裁切、Base64 编码、远程 URL 转换（LRU + SQLite 双层缓存）
- **服务保活** — 前台服务 + JobScheduler 定时检查 + 开机自启 + 电池优化白名单
- **多语言** — 7 种语言，跟随系统或手动切换
- **YAML 远程配置** — 从 URL 下载配置，支持在线预览和外部编辑器打开

## 快速开始

1. 安装 APK，授予通知监听权限
2. 设置中配置服务器地址（支持验证码校验）
3. 可选：配置镜像转发、图标转发、远程 YAML

**系统要求**: Android 13+ (API 33)

## 构建

```bash
./gradlew assembleDebug          # Debug 版本
./gradlew assembleRelease        # Release 版本（需 keystore.properties）
```

**开发环境**: JDK 17 · Kotlin 2.2.10 · Gradle 9.3.1 · AGP 9.1.0

## API 端点

| 端点 | 用途 |
|------|------|
| `POST /api/notify` | 转发通知 |
| `POST /api/notify/clipboard/text` | 发送剪贴板文本 |
| `POST /api/notify/clipboard/image` | 发送剪贴板图片 |
| `POST /api/notify/image/raw` | 发送图库图片 |
| `GET /api/version` | 服务器版本检查 |

默认端口 `19283`，Content-Type: `application/json`。

## YAML 配置

配置文件支持本地或远程加载，模块包括：

- **ignore-filter** — 按应用名 + 正则/文本过滤通知
- **dedup-filter** — 重复消息过滤（策略、时间窗口、包名匹配）
- **api** — 自定义端点路径和超时
- **icon-url** — Base64 图标转远程 URL
- **mirror** — 按端点独立配置镜像目的地

完整配置参考 [example_config.yaml](app/src/main/res/raw/example_config.yaml)。

## 镜像转发

每个端点可独立配置多个镜像目的地，DSN 包含完整请求路径和参数：

```yaml
mirror:
  enabled: true
  endpoints:
    notify:
      - "http://192.168.1.100:19283/api/notify?connectTimeout=5000&retry=3"
    clipboardText:
      - "https://api.example.com/api/notify/clipboard/text?token=secret&verifySSL=false"
```

DSN 参数: `connectTimeout`、`writeTimeout`、`retry`、`retryInterval`、`token`、`verifySSL`

镜像转发异步并行，失败不影响主服务器和其他镜像。

## 多语言

简体中文（默认）· English · 繁體中文 · 日本語 · Русский · Français · Deutsch

## 许可证

[MIT License](LICENSE)

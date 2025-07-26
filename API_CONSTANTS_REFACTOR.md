# API常量重构总结

## 概述

本次重构将所有远程API相关的硬编码字符串、端点路径、超时配置等统一定义在一个常量文件中，提高了代码的可维护性和一致性。

## 新增文件

### `app/src/main/java/com/hestudio/notifyforwarders/constants/ApiConstants.kt`

这是新创建的API常量文件，包含以下内容：

#### 基础配置
- `DEFAULT_PORT = 19283` - 默认端口号
- `HTTP_PROTOCOL = "http://"` - HTTP协议前缀
- `HTTPS_PROTOCOL = "https://"` - HTTPS协议前缀
- `CONTENT_TYPE_JSON = "application/json"` - JSON内容类型
- `CHARSET_UTF8 = "UTF-8"` - UTF-8字符编码

#### API端点路径
- `ENDPOINT_NOTIFY = "/api/notify"` - 通知转发API端点
- `ENDPOINT_CLIPBOARD_TEXT = "/api/notify/clipboard/text"` - 剪贴板文本API端点
- `ENDPOINT_CLIPBOARD_IMAGE = "/api/notify/clipboard/image"` - 剪贴板图片API端点
- `ENDPOINT_IMAGE_RAW = "/api/notify/image/raw"` - 相册图片API端点
- `ENDPOINT_VERSION = "/api/version"` - 版本检查API端点

#### HTTP方法
- `METHOD_GET = "GET"` - HTTP GET方法
- `METHOD_POST = "POST"` - HTTP POST方法

#### 超时配置
- `TIMEOUT_NOTIFY_CONNECT/READ = 5000` - 通知转发超时时间
- `TIMEOUT_CLIPBOARD_CONNECT/READ = 10000` - 剪贴板操作超时时间
- `TIMEOUT_IMAGE_CONNECT/READ = 10000` - 图片操作超时时间
- `TIMEOUT_VERSION_CONNECT/READ = 5000` - 版本检查超时时间

#### JSON字段名
- `FIELD_DEVICE_NAME = "devicename"` - 设备名称字段
- `FIELD_APP_NAME = "appname"` - 应用名称字段
- `FIELD_CONTENT = "content"` - 内容字段
- `FIELD_TITLE = "title"` - 标题字段
- `FIELD_DESCRIPTION = "description"` - 描述字段
- `FIELD_TYPE = "type"` - 类型字段
- `FIELD_MIME_TYPE = "mimeType"` - MIME类型字段
- `FIELD_UNIQUE_ID = "uniqueId"` - 唯一ID字段
- `FIELD_ID = "id"` - ID字段
- `FIELD_ICON_MD5 = "iconMd5"` - 图标MD5字段
- `FIELD_ICON_BASE64 = "iconBase64"` - 图标Base64字段
- `FIELD_VERSION = "version"` - 版本字段

#### HTTP头部
- `HEADER_EXIF = "X-EXIF"` - EXIF数据头部字段

#### 内容类型值
- `CONTENT_TYPE_TEXT = "text"` - 文本内容类型
- `CONTENT_TYPE_IMAGE = "image"` - 图片内容类型

#### 工具方法
- `buildApiUrl(serverAddress, endpoint)` - 构建完整的API URL
- `formatServerAddress(address)` - 格式化服务器地址，确保包含端口号

## 修改的文件

### 1. `NotificationService.kt`
- 添加了 `ApiConstants` 导入
- 将硬编码的API路径 `"http://$serverAddress/api/notify"` 替换为 `ApiConstants.buildApiUrl(serverAddress, ApiConstants.ENDPOINT_NOTIFY)`
- 将硬编码的HTTP方法、内容类型、超时时间、字符编码等替换为对应的常量
- 将JSON字段名替换为常量引用

### 2. `NotificationActionService.kt`
- 添加了 `ApiConstants` 导入
- 更新了三个主要方法：
  - `sendClipboardText()` - 使用 `ENDPOINT_CLIPBOARD_TEXT`
  - `sendClipboardImage()` - 使用 `ENDPOINT_CLIPBOARD_IMAGE`
  - `sendImageRaw()` - 使用 `ENDPOINT_IMAGE_RAW`
- 将所有硬编码的配置替换为常量引用

### 3. `SettingsActivity.kt`
- 添加了 `ApiConstants` 导入
- 更新了两个主要方法：
  - `checkServerVersion()` - 使用 `ENDPOINT_VERSION`
  - `sendVerificationCode()` - 使用 `ENDPOINT_NOTIFY`
- 简化了URL构建逻辑，使用 `buildApiUrl()` 工具方法

### 4. `ServerPreferences.kt`
- 添加了 `ApiConstants` 导入
- 删除了重复的 `DEFAULT_PORT` 常量定义
- 将 `formatServerAddress()` 方法重构为调用 `ApiConstants.formatServerAddress()`

## 重构的好处

### 1. 统一管理
- 所有API相关的配置都集中在一个文件中
- 便于维护和修改API端点或配置

### 2. 减少错误
- 避免了硬编码字符串可能导致的拼写错误
- 统一的字段名确保了API调用的一致性

### 3. 提高可读性
- 代码中使用有意义的常量名而不是魔法字符串
- 每个常量都有详细的注释说明其用途

### 4. 便于扩展
- 新增API端点时只需在常量文件中添加
- 修改超时时间等配置只需修改一处

### 5. 版本控制友好
- API变更时只需修改常量文件
- 减少了多文件同时修改的复杂性

## 完整的API端点列表

应用现在使用以下5个远程API端点：

| 端点 | 方法 | 用途 | 超时时间 |
|------|------|------|----------|
| `/api/notify` | POST | 转发通知到服务器 | 5秒 |
| `/api/clipboard/text` | POST | 发送剪贴板文本内容 | 10秒 |
| `/api/clipboard/image` | POST | 发送剪贴板图片内容 | 10秒 |
| `/api/image/raw` | POST | 发送相册图片及EXIF数据 | 10秒 |
| `/api/version` | GET | 检查服务器版本兼容性 | 5秒 |

## 测试结果

- ✅ 代码编译成功
- ✅ 所有API端点都已使用常量定义
- ✅ 保持了原有功能的完整性
- ✅ 提高了代码的可维护性

## 后续建议

1. 考虑将版本号也定义为常量
2. 可以考虑将超时时间设置为可配置项
3. 未来如果需要支持HTTPS，可以通过修改常量轻松实现

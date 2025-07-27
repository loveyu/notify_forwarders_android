package com.hestudio.notifyforwarders.constants

/**
 * API常量定义
 * 统一管理所有远程API端点、配置参数和网络设置
 */
object ApiConstants {
    
    // ==================== 基础配置 ====================
    
    /**
     * 默认端口号
     */
    const val DEFAULT_PORT = 19283
    
    /**
     * HTTP协议前缀
     */
    const val HTTP_PROTOCOL = "http://"
    
    /**
     * HTTPS协议前缀
     */
    const val HTTPS_PROTOCOL = "https://"
    
    /**
     * 内容类型 - JSON
     */
    const val CONTENT_TYPE_JSON = "application/json"
    
    /**
     * 字符编码
     */
    const val CHARSET_UTF8 = "UTF-8"
    
    // ==================== API端点路径 ====================
    
    /**
     * 通知转发API端点
     * 用于转发捕获的通知到服务器
     */
    const val ENDPOINT_NOTIFY = "/api/notify"
    
    /**
     * 剪贴板文本API端点
     * 用于发送剪贴板文本内容
     */
    const val ENDPOINT_CLIPBOARD_TEXT = "/api/notify/clipboard/text"
    
    /**
     * 剪贴板图片API端点
     * 用于发送剪贴板图片内容
     */
    const val ENDPOINT_CLIPBOARD_IMAGE = "/api/notify/clipboard/image"
    
    /**
     * 相册图片API端点
     * 用于发送相册图片及EXIF数据
     */
    const val ENDPOINT_IMAGE_RAW = "/api/notify/image/raw"
    
    /**
     * 版本检查API端点
     * 用于检查服务器版本兼容性
     */
    const val ENDPOINT_VERSION = "/api/version"
    
    // ==================== HTTP方法 ====================
    
    /**
     * HTTP GET方法
     */
    const val METHOD_GET = "GET"
    
    /**
     * HTTP POST方法
     */
    const val METHOD_POST = "POST"
    
    // ==================== 超时配置 ====================
    
    /**
     * 通知转发请求超时时间（毫秒）
     */
    const val TIMEOUT_NOTIFY_CONNECT = 5000
    const val TIMEOUT_NOTIFY_READ = 5000
    
    /**
     * 剪贴板内容发送超时时间（毫秒）
     */
    const val TIMEOUT_CLIPBOARD_CONNECT = 10000
    const val TIMEOUT_CLIPBOARD_READ = 10000
    
    /**
     * 图片发送超时时间（毫秒）
     */
    const val TIMEOUT_IMAGE_CONNECT = 10000
    const val TIMEOUT_IMAGE_READ = 10000
    
    /**
     * 版本检查超时时间（毫秒）
     */
    const val TIMEOUT_VERSION_CONNECT = 5000
    const val TIMEOUT_VERSION_READ = 5000
    
    // ==================== 版本信息 ====================
    
    /**
     * 应用名称
     */
    const val APP_NAME = "NotifyForwarders"
    
    // ==================== HTTP头部 ====================
    
    // ==================== JSON字段名 ====================
    
    /**
     * 设备名称字段
     */
    const val FIELD_DEVICE_NAME = "devicename"
    
    /**
     * 应用名称字段
     */
    const val FIELD_APP_NAME = "appname"
    
    /**
     * 内容字段
     */
    const val FIELD_CONTENT = "content"
    
    /**
     * 标题字段
     */
    const val FIELD_TITLE = "title"
    
    /**
     * 描述字段
     */
    const val FIELD_DESCRIPTION = "description"
    
    /**
     * 类型字段
     */
    const val FIELD_TYPE = "type"
    
    /**
     * MIME类型字段
     */
    const val FIELD_MIME_TYPE = "mimeType"
    
    /**
     * 唯一ID字段
     */
    const val FIELD_UNIQUE_ID = "uniqueId"
    
    /**
     * ID字段
     */
    const val FIELD_ID = "id"
    
    /**
     * 图标MD5字段
     */
    const val FIELD_ICON_MD5 = "iconMd5"
    
    /**
     * 图标Base64字段
     */
    const val FIELD_ICON_BASE64 = "iconBase64"
    
    /**
     * 版本字段
     */
    const val FIELD_VERSION = "version"

    /**
     * 文件名字段
     */
    const val FIELD_FILE_NAME = "fileName"

    /**
     * 文件路径字段
     */
    const val FIELD_FILE_PATH = "filePath"

    /**
     * 创建时间字段
     */
    const val FIELD_DATE_ADDED = "dateAdded"

    /**
     * 修改时间字段
     */
    const val FIELD_DATE_MODIFIED = "dateModified"
    
    // ==================== 内容类型值 ====================
    
    /**
     * 文本内容类型
     */
    const val CONTENT_TYPE_TEXT = "text"
    
    /**
     * 图片内容类型
     */
    const val CONTENT_TYPE_IMAGE = "image"
    
    // ==================== 工具方法 ====================
    
    /**
     * 构建完整的API URL
     * @param serverAddress 服务器地址（包含端口）
     * @param endpoint API端点路径
     * @return 完整的API URL
     */
    fun buildApiUrl(serverAddress: String, endpoint: String): String {
        val formattedAddress = if (!serverAddress.startsWith(HTTP_PROTOCOL) && 
                                 !serverAddress.startsWith(HTTPS_PROTOCOL)) {
            "$HTTP_PROTOCOL$serverAddress"
        } else {
            serverAddress
        }
        return "$formattedAddress$endpoint"
    }
    
    /**
     * 格式化服务器地址，确保包含端口号
     * @param address 原始服务器地址
     * @return 格式化后的服务器地址
     */
    fun formatServerAddress(address: String): String {
        if (address.isBlank()) return ""
        
        // 如果地址已经包含端口号，则直接返回
        if (address.contains(":")) {
            return address.trim()
        }
        
        // 否则添加默认端口号
        return "${address.trim()}:$DEFAULT_PORT"
    }
}

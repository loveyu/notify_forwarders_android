package com.hestudio.notifyforwarders.util

/**
 * 图标URL转换缓存配置
 */
data class IconUrlCacheConfig(
    val memoryCache: Boolean = true,
    val memoryCacheSize: Int = 2000,
    val sqliteCache: Boolean = true
)

/**
 * 图标URL转换配置
 * 当发送通知图标开启时生效，将base64图片转为URL
 */
data class IconUrlConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val token: String = "",
    val tag: String = "phone-icon",
    val checkEndpoint: String = "/tools/resource/check",
    val uploadEndpoint: String = "/tools/resource/upload-raw",
    val headerAuthToken: String = "x-auth-token",
    val headerUploadTag: String = "x-upload-tag",
    val headerUploadFilename: String = "x-upload-filename",
    val headerUploadFilesize: String = "x-upload-filesize",
    val headerUploadSource: String = "x-upload-source",
    val headerUploadDescription: String = "x-upload-description",
    val filenameTemplate: String = "icon_{md5}.png",
    val descriptionTemplate: String = "{appName}-通知图标",
    val cache: IconUrlCacheConfig = IconUrlCacheConfig()
)

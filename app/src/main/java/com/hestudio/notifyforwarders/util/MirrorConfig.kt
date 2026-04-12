package com.hestudio.notifyforwarders.util

import android.net.Uri
import android.util.Log

/**
 * 镜像端点配置
 * 每个端点可独立配置一个或多个镜像目的地
 */
data class MirrorEndpointConfig(
    val notify: List<String> = emptyList(),
    val clipboardText: List<String> = emptyList(),
    val clipboardImage: List<String> = emptyList(),
    val imageRaw: List<String> = emptyList()
) {
    /**
     * 根据端点名称获取镜像 DSN 列表
     */
    fun getDestinations(endpointName: String): List<String> = when (endpointName) {
        "notify" -> notify
        "clipboardText" -> clipboardText
        "clipboardImage" -> clipboardImage
        "imageRaw" -> imageRaw
        else -> emptyList()
    }

    /**
     * 检查是否有任何端点配置了镜像
     */
    fun hasAnyDestinations(): Boolean =
        notify.isNotEmpty() || clipboardText.isNotEmpty() ||
            clipboardImage.isNotEmpty() || imageRaw.isNotEmpty()
}

/**
 * 镜像配置
 * 用于将通知同时推送到多个目的地，每个端点独立配置
 */
data class MirrorConfig(
    val enabled: Boolean = false,
    val endpoints: MirrorEndpointConfig = MirrorEndpointConfig()
)

/**
 * 解析后的镜像目的地
 * 由 DSN 字符串解析而来
 *
 * url 为完整的请求地址，POST 时直接使用
 */
data class MirrorDestination(
    val url: String,
    val connectTimeout: Int = 5000,
    val writeTimeout: Int = 5000,
    val retry: Int = 0,
    val retryInterval: Int = 1000,
    val token: String? = null,
    val verifySSL: Boolean = true
)

/**
 * 解析镜像 DSN 字符串
 *
 * DSN 格式: http[s]://[userinfo@]host[:port][/full/path][?params]
 *
 * DSN 中的路径为完整的请求路径，POST 时直接使用该 URL。
 * 例如: http://mirror.example.com/api/notify?retry=3
 *   → POST http://mirror.example.com/api/notify
 *
 * 支持的查询参数:
 * - connectTimeout: 连接超时(ms), 默认 5000
 * - writeTimeout:   写入/读取超时(ms), 默认 5000
 * - retry:          重试次数, 默认 0
 * - retryInterval:  重试间隔(ms), 默认 1000
 * - token:          认证令牌, 设置 Authorization: Bearer {token} 请求头
 * - verifySSL:      是否验证SSL证书, 默认 true
 */
fun parseMirrorDsn(dsn: String): Result<MirrorDestination> {
    return try {
        val uri = Uri.parse(dsn)

        val scheme = uri.scheme ?: "http"
        val host = uri.host
        val port = uri.port
        val path = uri.path

        if (host.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("DSN 缺少主机地址: $dsn"))
        }

        // 构建完整 URL: scheme://[userinfo@]host[:port][/full/path]
        val sb = StringBuilder()
        sb.append(scheme).append("://")
        uri.userInfo?.let { sb.append(it).append("@") }
        sb.append(host)
        if (port != -1) {
            sb.append(":").append(port)
        }
        if (!path.isNullOrBlank() && path != "/") {
            sb.append(path)
        }
        val url = sb.toString()

        // 解析查询参数
        val connectTimeout = uri.getQueryParameter("connectTimeout")?.toIntOrNull() ?: 5000
        val writeTimeout = uri.getQueryParameter("writeTimeout")?.toIntOrNull() ?: 5000
        val retry = uri.getQueryParameter("retry")?.toIntOrNull() ?: 0
        val retryInterval = uri.getQueryParameter("retryInterval")?.toIntOrNull() ?: 1000
        val token = uri.getQueryParameter("token")
        val verifySSL = uri.getQueryParameter("verifySSL")?.toBooleanStrictOrNull() ?: true

        Result.success(
            MirrorDestination(
                url = url,
                connectTimeout = connectTimeout,
                writeTimeout = writeTimeout,
                retry = retry,
                retryInterval = retryInterval,
                token = token,
                verifySSL = verifySSL
            )
        )
    } catch (e: Exception) {
        Log.e("MirrorConfig", "解析镜像DSN失败: $dsn", e)
        Result.failure(e)
    }
}

package com.hestudio.notifyforwarders.util

import android.net.Uri
import android.util.Log

/**
 * 镜像目的地配置
 * 用于将通知同时推送到多个目的地
 */
data class MirrorConfig(
    val enabled: Boolean = false,
    val destinations: List<String> = emptyList()
)

/**
 * 解析后的镜像目的地
 * 由 DSN 字符串解析而来
 *
 * baseUrl 为 scheme://host[:port][/path_prefix]，各端点路径追加到其后
 */
data class MirrorDestination(
    val baseUrl: String,
    val connectTimeout: Int = 5000,
    val writeTimeout: Int = 5000,
    val retry: Int = 0,
    val retryInterval: Int = 1000,
    val token: String? = null,
    val verifySSL: Boolean = true
) {
    /**
     * 构建完整请求 URL
     * @param endpoint API 端点路径，如 /api/notify
     */
    fun buildUrl(endpoint: String): String {
        val base = baseUrl.trimEnd('/')
        val path = endpoint.trimStart('/')
        return "$base/$path"
    }
}

/**
 * 解析镜像 DSN 字符串
 *
 * DSN 格式: http[s]://[userinfo@]host[:port][/path_prefix][?params]
 *
 * DSN 中的路径作为前缀，各端点路径追加到其后。
 * 例如: http://mirror.example.com/proxy?retry=3
 *   /api/notify → http://mirror.example.com/proxy/api/notify
 *   /api/notify/clipboard/text → http://mirror.example.com/proxy/api/notify/clipboard/text
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

        // 构建 baseUrl: scheme://[userinfo@]host[:port][/path_prefix]
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
        val baseUrl = sb.toString()

        // 解析查询参数
        val connectTimeout = uri.getQueryParameter("connectTimeout")?.toIntOrNull() ?: 5000
        val writeTimeout = uri.getQueryParameter("writeTimeout")?.toIntOrNull() ?: 5000
        val retry = uri.getQueryParameter("retry")?.toIntOrNull() ?: 0
        val retryInterval = uri.getQueryParameter("retryInterval")?.toIntOrNull() ?: 1000
        val token = uri.getQueryParameter("token")
        val verifySSL = uri.getQueryParameter("verifySSL")?.toBooleanStrictOrNull() ?: true

        Result.success(
            MirrorDestination(
                baseUrl = baseUrl,
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

package com.hestudio.notifyforwarders.util

/**
 * API端点配置
 */
data class ApiEndpointConfig(
    val notify: String? = null,
    val clipboardText: String? = null,
    val clipboardImage: String? = null,
    val imageRaw: String? = null,
    val version: String? = null
)

/**
 * 超时配置
 */
data class TimeoutConfig(
    val connect: Int? = null,
    val read: Int? = null
)

/**
 * 超时分组配置
 */
data class TimeoutGroupConfig(
    val notify: TimeoutConfig? = null,
    val clipboard: TimeoutConfig? = null,
    val image: TimeoutConfig? = null,
    val version: TimeoutConfig? = null
)

/**
 * API配置（包含端点和超时）
 */
data class ApiConfig(
    val endpoints: ApiEndpointConfig? = null,
    val timeouts: TimeoutGroupConfig? = null
)

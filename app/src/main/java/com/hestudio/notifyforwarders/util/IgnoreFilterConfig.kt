package com.hestudio.notifyforwarders.util

/**
 * 消息过滤配置
 * 用于屏蔽不需要推送的通知消息
 */
data class IgnoreFilterRule(
    val appName: String,
    val regex: List<String> = emptyList(),
    val text: List<String> = emptyList()
)

/**
 * 过滤配置数据类
 */
data class IgnoreFilterConfig(
    val rules: List<IgnoreFilterRule> = emptyList()
) {
    companion object {
        fun empty() = IgnoreFilterConfig(emptyList())
    }
}

/**
 * 验证过滤规则配置是否有效
 */
fun IgnoreFilterConfig.validate(): List<String> {
    val errors = mutableListOf<String>()

    rules.forEachIndexed { index, rule ->
        if (rule.appName.isBlank()) {
            errors.add("规则 ${index + 1}: appName 不能为空")
        }

        rule.regex.forEachIndexed { regexIndex, pattern ->
            try {
                Regex(pattern)
            } catch (e: Exception) {
                errors.add("规则 ${index + 1} regex[$regexIndex]: 正则表达式无效 - $pattern")
            }
        }
    }

    return errors
}

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

/**
 * 完整配置（包含过滤规则和API配置）
 */
data class AppConfig(
    val ignoreFilter: IgnoreFilterConfig = IgnoreFilterConfig.empty(),
    val api: ApiConfig = ApiConfig()
)

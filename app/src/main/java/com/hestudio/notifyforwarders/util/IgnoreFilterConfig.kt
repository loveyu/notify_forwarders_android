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
 * 重复消息过滤策略
 */
enum class DedupStrategy {
    /** 比较 title 和 content（默认，与PHP端行为一致） */
    TITLE_CONTENT,
    /** 仅比较 title */
    TITLE,
    /** 仅比较 content */
    CONTENT
}

/**
 * 按应用单独配置的重复过滤规则
 * packageName 支持精确匹配和前缀匹配（以 . 结尾表示前缀）
 */
data class DedupAppConfig(
    val packageName: String,
    val strategy: DedupStrategy? = null,
    val timeWindow: Int? = null
)

/**
 * 重复消息过滤配置
 */
data class DedupFilterConfig(
    val enabled: Boolean = false,
    val onlyApps: Boolean = false,
    val strategy: DedupStrategy = DedupStrategy.TITLE_CONTENT,
    val timeWindow: Int = 20,
    val apps: List<DedupAppConfig> = emptyList()
)

/**
 * 完整配置（包含过滤规则、API配置和重复过滤配置）
 */
data class AppConfig(
    val ignoreFilter: IgnoreFilterConfig = IgnoreFilterConfig.empty(),
    val api: ApiConfig = ApiConfig(),
    val dedupFilter: DedupFilterConfig = DedupFilterConfig()
)

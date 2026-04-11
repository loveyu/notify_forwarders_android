package com.hestudio.notifyforwarders.util

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

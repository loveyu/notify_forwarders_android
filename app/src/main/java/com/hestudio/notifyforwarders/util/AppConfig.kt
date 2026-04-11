package com.hestudio.notifyforwarders.util

/**
 * 完整配置（包含过滤规则、API配置、重复过滤配置和图标URL转换配置）
 */
data class AppConfig(
    val ignoreFilter: IgnoreFilterConfig = IgnoreFilterConfig.empty(),
    val api: ApiConfig = ApiConfig(),
    val dedupFilter: DedupFilterConfig = DedupFilterConfig(),
    val iconUrl: IconUrlConfig = IconUrlConfig()
)

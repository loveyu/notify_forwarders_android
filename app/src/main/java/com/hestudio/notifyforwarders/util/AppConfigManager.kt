package com.hestudio.notifyforwarders.util

import android.content.Context
import android.util.Log
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileWriter

/**
 * 应用配置管理器
 * 负责从YAML解析配置、执行过滤逻辑、以及配置的持久化
 */
object AppConfigManager {
    private const val TAG = "AppConfigManager"
    private const val CONFIG_FILE_NAME = "ignore_filter_config.yaml"
    private const val KEY_REMOTE_CONFIG_URL = "remote_config_url"

    @Volatile
    private var currentConfig: AppConfig = AppConfig()

    @Volatile
    private var isConfigLoaded = false

    /**
     * 从YAML字符串解析配置
     */
    fun parseFromYaml(yamlContent: String): Result<AppConfig> {
        return try {
            val yaml = Yaml()
            val data = yaml.load<Map<String, Any>>(yamlContent)

            // 解析 ignore-filter 部分
            val rules = mutableListOf<IgnoreFilterRule>()
            val ignoreFilter = data["ignore-filter"]
            if (ignoreFilter is List<*>) {
                ignoreFilter.forEach { item ->
                    if (item is Map<*, *>) {
                        val appName = item["appName"]?.toString() ?: ""
                        val regexStr = item["regex"]
                        val textStr = item["text"]

                        val regexList = when (regexStr) {
                            is String -> listOf(regexStr)
                            is List<*> -> regexStr.filterIsInstance<String>()
                            else -> emptyList()
                        }

                        val textList = when (textStr) {
                            is String -> listOf(textStr)
                            is List<*> -> textStr.filterIsInstance<String>()
                            else -> emptyList()
                        }

                        rules.add(IgnoreFilterRule(appName, regexList, textList))
                    }
                }
            }

            val ignoreFilterConfig = IgnoreFilterConfig(rules)

            // 解析 dedup-filter 部分
            val dedupFilterConfig = parseDedupFilterConfig(data["dedup-filter"])

            // 解析 api 部分
            val apiConfig = parseApiConfig(data["api"])

            // 解析 icon-url 部分
            val iconUrlConfig = parseIconUrlConfig(data["icon-url"])

            // 解析 mirror 部分
            val mirrorConfig = parseMirrorConfig(data["mirror"])

            Result.success(AppConfig(ignoreFilterConfig, apiConfig, dedupFilterConfig, iconUrlConfig, mirrorConfig))
        } catch (e: Exception) {
            Log.e(TAG, "解析YAML配置失败", e)
            Result.failure(e)
        }
    }

    /**
     * 解析重复消息过滤配置
     */
    private fun parseDedupFilterConfig(dedupData: Any?): DedupFilterConfig {
        if (dedupData !is Map<*, *>) {
            return DedupFilterConfig()
        }

        val enabled = (dedupData["enabled"] as? Boolean) ?: false
        val onlyApps = (dedupData["onlyApps"] as? Boolean) ?: false
        val strategy = parseDedupStrategy(dedupData["strategy"]?.toString())
        val timeWindow = (dedupData["timeWindow"] as? Number)?.toInt() ?: 20

        val apps = mutableListOf<DedupAppConfig>()
        val appsData = dedupData["apps"]
        if (appsData is List<*>) {
            appsData.forEach { item ->
                if (item is Map<*, *>) {
                    val packageName = item["packageName"]?.toString() ?: return@forEach
                    val appStrategy = parseDedupStrategy(item["strategy"]?.toString())
                    val appTimeWindow = (item["timeWindow"] as? Number)?.toInt()
                    apps.add(DedupAppConfig(packageName, appStrategy, appTimeWindow))
                }
            }
        }

        return DedupFilterConfig(enabled, onlyApps, strategy, timeWindow, apps)
    }

    /**
     * 解析去重策略字符串
     */
    private fun parseDedupStrategy(value: String?): DedupStrategy {
        return when (value) {
            "title" -> DedupStrategy.TITLE
            "content" -> DedupStrategy.CONTENT
            "title_content" -> DedupStrategy.TITLE_CONTENT
            else -> DedupStrategy.TITLE_CONTENT
        }
    }

    /**
     * 解析API配置
     */
    private fun parseApiConfig(apiData: Any?): ApiConfig {
        if (apiData !is Map<*, *>) {
            return ApiConfig()
        }

        val endpoints = parseEndpointConfig(apiData["endpoints"])
        val timeouts = parseTimeoutConfig(apiData["timeouts"])

        return ApiConfig(endpoints, timeouts)
    }

    /**
     * 解析图标URL转换配置
     */
    private fun parseIconUrlConfig(iconUrlData: Any?): IconUrlConfig {
        if (iconUrlData !is Map<*, *>) {
            return IconUrlConfig()
        }

        val enabled = (iconUrlData["enabled"] as? Boolean) ?: false
        val baseUrl = iconUrlData["baseUrl"]?.toString() ?: ""
        val token = iconUrlData["token"]?.toString() ?: ""
        val tag = iconUrlData["tag"]?.toString() ?: "phone-icon"
        val checkEndpoint = iconUrlData["checkEndpoint"]?.toString() ?: "/tools/resource/check"
        val uploadEndpoint = iconUrlData["uploadEndpoint"]?.toString() ?: "/tools/resource/upload-raw"

        val cacheData = iconUrlData["cache"]
        val cacheConfig = if (cacheData is Map<*, *>) {
            IconUrlCacheConfig(
                memoryCache = (cacheData["memoryCache"] as? Boolean) ?: true,
                memoryCacheSize = (cacheData["memoryCacheSize"] as? Number)?.toInt() ?: 2000,
                sqliteCache = (cacheData["sqliteCache"] as? Boolean) ?: true
            )
        } else {
            IconUrlCacheConfig()
        }

        val headerAuthToken = iconUrlData["headerAuthToken"]?.toString() ?: "x-auth-token"
        val headerUploadTag = iconUrlData["headerUploadTag"]?.toString() ?: "x-upload-tag"
        val headerUploadFilename = iconUrlData["headerUploadFilename"]?.toString() ?: "x-upload-filename"
        val headerUploadFilesize = iconUrlData["headerUploadFilesize"]?.toString() ?: "x-upload-filesize"
        val headerUploadSource = iconUrlData["headerUploadSource"]?.toString() ?: "x-upload-source"
        val headerUploadDescription = iconUrlData["headerUploadDescription"]?.toString() ?: "x-upload-description"
        val filenameTemplate = iconUrlData["filenameTemplate"]?.toString() ?: "icon_{md5}.png"
        val descriptionTemplate = iconUrlData["descriptionTemplate"]?.toString() ?: "{appName}-通知图标"

        return IconUrlConfig(
            enabled, baseUrl, token, tag, checkEndpoint, uploadEndpoint,
            headerAuthToken, headerUploadTag, headerUploadFilename,
            headerUploadFilesize, headerUploadSource, headerUploadDescription,
            filenameTemplate, descriptionTemplate,
            cacheConfig
        )
    }

    /**
     * 解析镜像目的地配置
     */
    private fun parseMirrorConfig(mirrorData: Any?): MirrorConfig {
        if (mirrorData !is Map<*, *>) {
            return MirrorConfig()
        }

        val enabled = (mirrorData["enabled"] as? Boolean) ?: false

        val destinations = when (val dest = mirrorData["destinations"]) {
            is String -> listOf(dest)
            is List<*> -> dest.filterIsInstance<String>()
            else -> emptyList()
        }

        return MirrorConfig(enabled, destinations)
    }

    /**
     * 获取解析后的镜像目的地列表
     * 仅返回 enabled=true 且解析成功的目的地
     */
    fun getMirrorDestinations(): List<MirrorDestination> {
        val config = currentConfig.mirror
        if (!config.enabled || config.destinations.isEmpty()) {
            return emptyList()
        }
        return config.destinations.mapNotNull { dsn ->
            parseMirrorDsn(dsn).getOrNull()
        }
    }

    /**
     * 解析端点配置
     */
    private fun parseEndpointConfig(endpointsData: Any?): ApiEndpointConfig? {
        if (endpointsData !is Map<*, *>) {
            return null
        }

        val notify = endpointsData["notify"]?.toString()
        val clipboardText = endpointsData["clipboardText"]?.toString()
        val clipboardImage = endpointsData["clipboardImage"]?.toString()
        val imageRaw = endpointsData["imageRaw"]?.toString()
        val version = endpointsData["version"]?.toString()

        // 如果所有值都为空，返回null
        if (notify == null && clipboardText == null && clipboardImage == null && imageRaw == null && version == null) {
            return null
        }

        return ApiEndpointConfig(notify, clipboardText, clipboardImage, imageRaw, version)
    }

    /**
     * 解析超时配置
     */
    private fun parseTimeoutConfig(timeoutsData: Any?): TimeoutGroupConfig? {
        if (timeoutsData !is Map<*, *>) {
            return null
        }

        val notify = parseTimeoutItem(timeoutsData["notify"])
        val clipboard = parseTimeoutItem(timeoutsData["clipboard"])
        val image = parseTimeoutItem(timeoutsData["image"])
        val version = parseTimeoutItem(timeoutsData["version"])

        // 如果所有值都为空，返回null
        if (notify == null && clipboard == null && image == null && version == null) {
            return null
        }

        return TimeoutGroupConfig(notify, clipboard, image, version)
    }

    /**
     * 解析单个超时配置项
     */
    private fun parseTimeoutItem(timeoutData: Any?): TimeoutConfig? {
        if (timeoutData !is Map<*, *>) {
            return null
        }

        val connect = (timeoutData["connect"] as? Number)?.toInt()
        val read = (timeoutData["read"] as? Number)?.toInt()

        if (connect == null && read == null) {
            return null
        }

        return TimeoutConfig(connect, read)
    }

    /**
     * 检查通知是否应该被忽略
     * 匹配逻辑: appName匹配 AND (regex任意匹配 OR text任意匹配)
     */
    fun shouldIgnore(appName: String, title: String, content: String): Boolean {
        val config = currentConfig

        for (rule in config.ignoreFilter.rules) {
            // 检查appName是否匹配
            if (rule.appName != appName) {
                continue
            }

            // 检查regex规则
            val regexMatched = rule.regex.any { pattern ->
                try {
                    val regex = Regex(pattern)
                    regex.containsMatchIn(title) || regex.containsMatchIn(content)
                } catch (e: Exception) {
                    Log.w(TAG, "正则表达式匹配失败: $pattern", e)
                    false
                }
            }

            // 检查text规则 (使用strpos, 忽略大小写)
            val textMatched = rule.text.any { text ->
                val lowerTitle = title.lowercase()
                val lowerContent = content.lowercase()
                val lowerText = text.lowercase()
                lowerTitle.contains(lowerText) || lowerContent.contains(lowerText)
            }

            // 如果regex或text任意匹配，则忽略
            if (regexMatched || textMatched) {
                Log.d(TAG, "消息被过滤: appName=$appName, title=$title, content=$content")
                return true
            }
        }

        return false
    }

    /**
     * 缓存的最近一条消息（按packageName分组）
     */
    private val lastMessageCache = mutableMapOf<String, CachedMessage>()

    private data class CachedMessage(
        val title: String,
        val content: String,
        val timestamp: Long
    )

    /**
     * 匹配包名：精确匹配或前缀匹配（packageName以 . 结尾时视为前缀）
     */
    private fun matchPackageName(pattern: String, packageName: String): Boolean {
        return if (pattern.endsWith(".")) {
            packageName.startsWith(pattern)
        } else {
            packageName == pattern
        }
    }

    /**
     * 检查消息是否为重复消息
     * 如果最近 timeWindow 秒内相同 packageName 的消息内容匹配，则视为重复
     *
     * @param packageName 应用包名
     * @param title       通知标题
     * @param content     通知内容
     * @return true 表示是重复消息，应忽略
     */
    fun shouldDedup(packageName: String, title: String, content: String): Boolean {
        val config = currentConfig.dedupFilter
        if (!config.enabled) {
            return false
        }

        // 查找该包名匹配的自定义配置
        val appConfig = config.apps.find { matchPackageName(it.packageName, packageName) }

        // onlyApps=true 时，仅对 apps 列表中匹配的应用生效
        if (config.onlyApps && appConfig == null) {
            return false
        }

        val strategy = appConfig?.strategy ?: config.strategy
        val timeWindow = (appConfig?.timeWindow ?: config.timeWindow) * 1000L

        val cached = lastMessageCache[packageName]
        if (cached == null) {
            // 首条消息，缓存并放行
            lastMessageCache[packageName] = CachedMessage(title, content, System.currentTimeMillis())
            return false
        }

        val now = System.currentTimeMillis()
        if (now - cached.timestamp > timeWindow) {
            // 超出时间窗口，更新缓存并放行
            lastMessageCache[packageName] = CachedMessage(title, content, now)
            return false
        }

        // 在时间窗口内，按策略比较内容
        val isDuplicate = when (strategy) {
            DedupStrategy.TITLE_CONTENT -> cached.title == title && cached.content == content
            DedupStrategy.TITLE -> cached.title == title
            DedupStrategy.CONTENT -> cached.content == content
        }

        if (isDuplicate) {
            Log.d(TAG, "重复消息被过滤: packageName=$packageName, title=$title, strategy=$strategy")
            return true
        }

        // 内容不同，更新缓存
        lastMessageCache[packageName] = CachedMessage(title, content, now)
        return false
    }

    /**
     * 加载配置到内存
     */
    fun loadConfig(config: IgnoreFilterConfig) {
        currentConfig = AppConfig(ignoreFilter = config)
        isConfigLoaded = true
        Log.d(TAG, "配置已加载，共 ${config.rules.size} 条规则")
    }

    /**
     * 加载完整配置到内存
     */
    fun loadConfig(config: AppConfig) {
        currentConfig = config
        isConfigLoaded = true
        Log.d(TAG, "配置已加载，过滤规则 ${config.ignoreFilter.rules.size} 条")
    }

    /**
     * 加载完整配置到内存并初始化图标URL缓存
     * @param context 上下文，用于初始化SQLite缓存
     */
    fun loadConfig(config: AppConfig, context: Context) {
        loadConfig(config)

        // 初始化图标URL转换缓存
        val iconUrlConfig = config.iconUrl
        IconHashCache.init(
            context = context,
            memoryEnabled = iconUrlConfig.cache.memoryCache,
            memoryMaxSize = iconUrlConfig.cache.memoryCacheSize,
            sqliteEnabled = iconUrlConfig.cache.sqliteCache
        )
        IconUrlManager.init(iconUrlConfig)
    }

    /**
     * 从文件加载配置
     */
    fun loadFromFile(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, CONFIG_FILE_NAME)
            if (file.exists()) {
                val yamlContent = file.readText()
                val result = parseFromYaml(yamlContent)
                if (result.isSuccess) {
                    loadConfig(result.getOrThrow(), context)
                    true
                } else {
                    Log.e(TAG, "从文件加载配置失败", result.exceptionOrNull())
                    false
                }
            } else {
                Log.d(TAG, "配置文件不存在")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "从文件加载配置异常", e)
            false
        }
    }

    /**
     * 保存配置到文件
     */
    fun saveToFile(context: Context, config: IgnoreFilterConfig): Boolean {
        return saveToFile(context, AppConfig(ignoreFilter = config))
    }

    /**
     * 保存完整配置到文件
     */
    fun saveToFile(context: Context, config: AppConfig): Boolean {
        return try {
            val file = File(context.filesDir, CONFIG_FILE_NAME)
            FileWriter(file).use { writer ->
                writer.write(convertToYaml(config))
            }
            Log.d(TAG, "配置已保存到文件")
            true
        } catch (e: Exception) {
            Log.e(TAG, "保存配置到文件失败", e)
            false
        }
    }

    /**
     * 将配置转换为YAML字符串
     */
    fun convertToYaml(config: IgnoreFilterConfig): String {
        return convertToYaml(AppConfig(ignoreFilter = config))
    }

    /**
     * 将完整配置转换为YAML字符串
     */
    fun convertToYaml(config: AppConfig): String {
        val sb = StringBuilder()
        sb.append("# ignore-filter 配置文件\n")
        sb.append("# 用途: 屏蔽不需要推送的通知消息\n\n")
        sb.append("ignore-filter:\n")

        config.ignoreFilter.rules.forEach { rule ->
            sb.append("  - appName: \"${rule.appName}\"\n")
            if (rule.regex.isNotEmpty()) {
                if (rule.regex.size == 1) {
                    sb.append("    regex: '${rule.regex[0]}'\n")
                } else {
                    sb.append("    regex:\n")
                    rule.regex.forEach { pattern ->
                        sb.append("      - '$pattern'\n")
                    }
                }
            }
            if (rule.text.isNotEmpty()) {
                if (rule.text.size == 1) {
                    sb.append("    text: '${rule.text[0]}'\n")
                } else {
                    sb.append("    text:\n")
                    rule.text.forEach { text ->
                        sb.append("      - '$text'\n")
                    }
                }
            }
        }

        // 添加 API 配置
        if (config.api.endpoints != null || config.api.timeouts != null) {
            sb.append("\n# API端点和超时配置\n")
            sb.append("api:\n")

            // 端点配置
            if (config.api.endpoints != null) {
                sb.append("  endpoints:\n")
                config.api.endpoints.notify?.let { sb.append("    notify: \"$it\"\n") }
                config.api.endpoints.clipboardText?.let { sb.append("    clipboardText: \"$it\"\n") }
                config.api.endpoints.clipboardImage?.let { sb.append("    clipboardImage: \"$it\"\n") }
                config.api.endpoints.imageRaw?.let { sb.append("    imageRaw: \"$it\"\n") }
                config.api.endpoints.version?.let { sb.append("    version: \"$it\"\n") }
            }

            // 超时配置
            if (config.api.timeouts != null) {
                sb.append("  timeouts:\n")
                config.api.timeouts.notify?.let { timeout ->
                    sb.append("    notify:\n")
                    timeout.connect?.let { sb.append("      connect: $it\n") }
                    timeout.read?.let { sb.append("      read: $it\n") }
                }
                config.api.timeouts.clipboard?.let { timeout ->
                    sb.append("    clipboard:\n")
                    timeout.connect?.let { sb.append("      connect: $it\n") }
                    timeout.read?.let { sb.append("      read: $it\n") }
                }
                config.api.timeouts.image?.let { timeout ->
                    sb.append("    image:\n")
                    timeout.connect?.let { sb.append("      connect: $it\n") }
                    timeout.read?.let { sb.append("      read: $it\n") }
                }
                config.api.timeouts.version?.let { timeout ->
                    sb.append("    version:\n")
                    timeout.connect?.let { sb.append("      connect: $it\n") }
                    timeout.read?.let { sb.append("      read: $it\n") }
                }
            }
        }

        // 添加 dedup-filter 配置
        val dedup = config.dedupFilter
        if (dedup.enabled || dedup.apps.isNotEmpty()) {
            sb.append("\n# 重复消息过滤配置\n")
            sb.append("dedup-filter:\n")
            sb.append("  enabled: ${dedup.enabled}\n")
            if (dedup.onlyApps) {
                sb.append("  onlyApps: true\n")
            }
            sb.append("  strategy: \"${dedup.strategy.name.lowercase()}\"\n")
            sb.append("  timeWindow: ${dedup.timeWindow}\n")
            if (dedup.apps.isNotEmpty()) {
                sb.append("  apps:\n")
                dedup.apps.forEach { app ->
                    sb.append("    - packageName: \"${app.packageName}\"\n")
                    app.strategy?.let { sb.append("      strategy: \"${it.name.lowercase()}\"\n") }
                    app.timeWindow?.let { sb.append("      timeWindow: $it\n") }
                }
            }
        }

        // 添加 icon-url 配置
        val iconUrl = config.iconUrl
        if (iconUrl.enabled) {
            sb.append("\n# 图标URL转换配置\n")
            sb.append("icon-url:\n")
            sb.append("  enabled: true\n")
            sb.append("  baseUrl: \"${iconUrl.baseUrl}\"\n")
            sb.append("  token: \"${iconUrl.token}\"\n")
            if (iconUrl.tag != "phone-icon") {
                sb.append("  tag: \"${iconUrl.tag}\"\n")
            }
            if (iconUrl.checkEndpoint != "/tools/resource/check") {
                sb.append("  checkEndpoint: \"${iconUrl.checkEndpoint}\"\n")
            }
            if (iconUrl.uploadEndpoint != "/tools/resource/upload-raw") {
                sb.append("  uploadEndpoint: \"${iconUrl.uploadEndpoint}\"\n")
            }
            if (iconUrl.headerAuthToken != "x-auth-token") {
                sb.append("  headerAuthToken: \"${iconUrl.headerAuthToken}\"\n")
            }
            if (iconUrl.headerUploadTag != "x-upload-tag") {
                sb.append("  headerUploadTag: \"${iconUrl.headerUploadTag}\"\n")
            }
            if (iconUrl.headerUploadFilename != "x-upload-filename") {
                sb.append("  headerUploadFilename: \"${iconUrl.headerUploadFilename}\"\n")
            }
            if (iconUrl.headerUploadFilesize != "x-upload-filesize") {
                sb.append("  headerUploadFilesize: \"${iconUrl.headerUploadFilesize}\"\n")
            }
            if (iconUrl.headerUploadSource != "x-upload-source") {
                sb.append("  headerUploadSource: \"${iconUrl.headerUploadSource}\"\n")
            }
            if (iconUrl.headerUploadDescription != "x-upload-description") {
                sb.append("  headerUploadDescription: \"${iconUrl.headerUploadDescription}\"\n")
            }
            if (iconUrl.filenameTemplate != "icon_{md5}.png") {
                sb.append("  filenameTemplate: \"${iconUrl.filenameTemplate}\"\n")
            }
            sb.append("  cache:\n")
            val cache = iconUrl.cache
            sb.append("    memoryCache: ${cache.memoryCache}\n")
            sb.append("    memoryCacheSize: ${cache.memoryCacheSize}\n")
            sb.append("    sqliteCache: ${cache.sqliteCache}\n")
        }

        // 添加 mirror 配置
        val mirror = config.mirror
        if (mirror.enabled || mirror.destinations.isNotEmpty()) {
            sb.append("\n# 镜像目的地配置\n")
            sb.append("mirror:\n")
            sb.append("  enabled: ${mirror.enabled}\n")
            if (mirror.destinations.isNotEmpty()) {
                sb.append("  destinations:\n")
                mirror.destinations.forEach { dest ->
                    sb.append("    - \"$dest\"\n")
                }
            }
        }

        return sb.toString()
    }

    /**
     * 获取外部存储路径下的配置文件路径（用于打开外部编辑器）
     */
    fun getExternalConfigFile(context: Context): File {
        val externalDir = context.getExternalFilesDir(null)
        return File(externalDir, CONFIG_FILE_NAME)
    }

    /**
     * 保存配置到外部目录（用于外部编辑器）
     */
    fun saveToExternalFile(context: Context, config: IgnoreFilterConfig): Boolean {
        return saveToExternalFile(context, AppConfig(ignoreFilter = config))
    }

    /**
     * 保存完整配置到外部目录（用于外部编辑器）
     */
    fun saveToExternalFile(context: Context, config: AppConfig): Boolean {
        return try {
            val file = getExternalConfigFile(context)
            FileWriter(file).use { writer ->
                writer.write(convertToYaml(config))
            }
            Log.d(TAG, "配置已保存到外部文件: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "保存配置到外部文件失败", e)
            false
        }
    }

    /**
     * 从外部文件加载配置
     */
    fun loadFromExternalFile(context: Context): AppConfig? {
        return try {
            val file = getExternalConfigFile(context)
            if (file.exists()) {
                val yamlContent = file.readText()
                parseFromYaml(yamlContent).getOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "从外部文件加载配置失败", e)
            null
        }
    }

    /**
     * 保存远程配置URL
     */
    fun saveRemoteConfigUrl(context: Context, url: String) {
        context.getSharedPreferences("remote_config_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REMOTE_CONFIG_URL, url)
            .apply()
    }

    /**
     * 获取保存的远程配置URL
     */
    fun getRemoteConfigUrl(context: Context): String {
        return context.getSharedPreferences("remote_config_prefs", Context.MODE_PRIVATE)
            .getString(KEY_REMOTE_CONFIG_URL, "") ?: ""
    }

    /**
     * 获取当前配置
     */
    fun getCurrentConfig(): AppConfig = currentConfig

    /**
     * 获取当前API配置
     */
    fun getApiConfig(): ApiConfig = currentConfig.api

    /**
     * 检查配置是否已加载
     */
    fun isConfigLoaded(): Boolean = isConfigLoaded

    /**
     * 清除当前配置
     */
    fun clearConfig() {
        currentConfig = AppConfig()
        isConfigLoaded = false
    }
}

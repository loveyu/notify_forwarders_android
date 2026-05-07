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
    private const val CONFIG_FILE_NAME = "full.yaml"
    private const val KEY_REMOTE_CONFIG_URL = "remote_config_url"

    @Volatile
    private var currentConfig: AppConfig = AppConfig()

    @Volatile
    private var isConfigLoaded = false

    /**
     * Application Context 引用，用于配置未加载时自动从文件恢复
     */
    @Volatile
    private var appContext: Context? = null

    /**
     * 初始化，保存 Application Context 供后续懒加载使用
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * 确保配置已加载，若未加载则从文件自动恢复
     * 在所有读取配置的公开方法入口处调用
     */
    private fun ensureConfigLoaded() {
        if (isConfigLoaded) return
        val ctx = appContext ?: return
        synchronized(this) {
            if (isConfigLoaded) return
            Log.w(TAG, "配置未加载，自动从文件恢复")
            loadFromFile(ctx)
        }
    }

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
                            is String -> listOf(normalizeRegexPattern(regexStr))
                            is List<*> -> regexStr.filterIsInstance<String>().map { normalizeRegexPattern(it) }
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

        val endpointsData = mirrorData["endpoints"]
        val endpoints = if (endpointsData is Map<*, *>) {
            MirrorEndpointConfig(
                notify = parseDsnList(endpointsData["notify"]),
                clipboardText = parseDsnList(endpointsData["clipboardText"]),
                clipboardImage = parseDsnList(endpointsData["clipboardImage"]),
                imageRaw = parseDsnList(endpointsData["imageRaw"])
            )
        } else {
            MirrorEndpointConfig()
        }

        return MirrorConfig(enabled, endpoints)
    }

    /**
     * 解析 DSN 列表（支持单字符串或数组）
     */
    private fun parseDsnList(value: Any?): List<String> = when (value) {
        is String -> listOf(value)
        is List<*> -> value.filterIsInstance<String>()
        else -> emptyList()
    }

    /**
     * 获取指定端点的解析后镜像目的地列表
     * 仅返回 enabled=true 且解析成功的目的地
     *
     * @param endpointName 端点名称，如 "notify"、"clipboardText"
     */
    fun getMirrorDestinations(endpointName: String): List<MirrorDestination> {
        ensureConfigLoaded()
        val config = currentConfig.mirror
        if (!config.enabled) {
            return emptyList()
        }
        val dsnList = config.endpoints.getDestinations(endpointName)
        if (dsnList.isEmpty()) {
            return emptyList()
        }
        return dsnList.mapNotNull { dsn ->
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
     * 规范化正则表达式模式，去除 preg_match 风格的 /分隔符/ 和修饰符
     * 例如: '/Running [\d]+ fibers/' → 'Running [\d]+ fibers'
     *       '/尚东大门状态变更/u' → '尚东大门状态变更'
     *       'Running [\d]+' → 'Running [\d]+' （无分隔符时原样返回）
     */
    private fun normalizeRegexPattern(pattern: String): String {
        val trimmed = pattern.trim()
        if (!trimmed.startsWith('/')) return trimmed
        val lastSlash = trimmed.lastIndexOf('/')
        if (lastSlash <= 0) return trimmed
        return trimmed.substring(1, lastSlash)
    }

    /**
     * 检查通知是否应该被忽略
     * 匹配逻辑: appName匹配 AND (regex任意匹配 OR text任意匹配)
     */
    fun shouldIgnore(appName: String, title: String, content: String): Boolean {
        ensureConfigLoaded()
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
        ensureConfigLoaded()
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
     * 将下载的原始 YAML 内容直接保存到 full.yaml，不做任何转换
     */
    fun saveRawYamlToFile(context: Context, rawYaml: String): Boolean {
        return try {
            val file = File(context.filesDir, CONFIG_FILE_NAME)
            file.writeText(rawYaml)
            Log.d(TAG, "原始配置内容已保存到文件: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "保存原始配置内容失败", e)
            false
        }
    }

    /**
     * 获取内部存储的原始配置文件
     */
    fun getRawYamlFile(context: Context): File {
        return File(context.filesDir, CONFIG_FILE_NAME)
    }

    /**
     * 获取外部存储路径下的配置文件路径（用于打开外部编辑器）
     */
    fun getExternalConfigFile(context: Context): File {
        val externalDir = context.getExternalFilesDir(null)
        return File(externalDir, CONFIG_FILE_NAME)
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
     * 检查配置是否已加载
     */
    fun isConfigLoaded(): Boolean = isConfigLoaded
}

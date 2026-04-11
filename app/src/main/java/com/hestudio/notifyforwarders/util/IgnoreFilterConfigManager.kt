package com.hestudio.notifyforwarders.util

import android.content.Context
import android.util.Log
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileWriter

/**
 * 消息过滤配置管理器
 * 负责从YAML解析配置、执行过滤逻辑、以及配置的持久化
 */
object IgnoreFilterConfigManager {
    private const val TAG = "IgnoreFilterConfig"
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

            // 解析 api 部分
            val apiConfig = parseApiConfig(data["api"])

            Result.success(AppConfig(ignoreFilterConfig, apiConfig))
        } catch (e: Exception) {
            Log.e(TAG, "解析YAML配置失败", e)
            Result.failure(e)
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
     * 从文件加载配置
     */
    fun loadFromFile(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, CONFIG_FILE_NAME)
            if (file.exists()) {
                val yamlContent = file.readText()
                val result = parseFromYaml(yamlContent)
                if (result.isSuccess) {
                    loadConfig(result.getOrThrow())
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

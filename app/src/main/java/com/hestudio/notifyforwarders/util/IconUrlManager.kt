package com.hestudio.notifyforwarders.util

import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * 图标URL转换管理器
 * 负责将base64图标数据转换为远程URL：
 * 1. 先查本地缓存（内存 + SQLite）
 * 2. 缓存未命中时，调用远程检查接口确认图标是否已存在
 * 3. 不存在则上传图标，获取URL
 * 4. 将结果写入缓存
 *
 * 使用 per-MD5 锁防止同一图标并发重复上传
 */
object IconUrlManager {
    private const val TAG = "IconUrlManager"
    private const val CONNECT_TIMEOUT = 10000
    private const val READ_TIMEOUT = 15000

    @Volatile
    private var config: IconUrlConfig = IconUrlConfig()

    @Volatile
    private var initialized = false

    // per-MD5锁，防止同一图标并发重复检查/上传
    private val md5Locks = ConcurrentHashMap<String, ReentrantLock>()

    /**
     * 使用配置初始化管理器
     */
    fun init(config: IconUrlConfig) {
        this.config = config
        this.initialized = config.enabled && config.baseUrl.isNotBlank()
        Log.d(TAG, "初始化: enabled=${config.enabled}, initialized=$initialized")
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(): Boolean = initialized

    /**
     * 将base64图标转换为URL
     * @param iconBase64 base64编码的图标数据
     * @param iconMd5 图标的MD5哈希
     * @param appName 应用名称（用于上传时的描述）
     * @return 转换后的URL，失败返回null
     */
    fun convertToUrl(iconBase64: String, iconMd5: String, appName: String): String? {
        if (!initialized) return null

        val lock = md5Locks.computeIfAbsent(iconMd5) { ReentrantLock() }
        lock.lock()
        try {
            // 1. 查本地缓存（加锁后再次检查，避免等待锁的线程重复操作）
            val cachedUrl = IconHashCache.get(iconMd5)
            if (cachedUrl != null) {
                Log.d(TAG, "本地缓存命中: $iconMd5 -> $cachedUrl")
                return cachedUrl
            }

            // 2. 远程检查图标是否存在
            val checkUrl = checkIconExists(iconMd5)
            if (checkUrl != null) {
                Log.d(TAG, "远程检查命中: $iconMd5 -> $checkUrl")
                IconHashCache.put(iconMd5, checkUrl)
                return checkUrl
            }

            // 3. 上传图标
            val iconBytes = Base64.decode(iconBase64, Base64.NO_WRAP)
            val uploadUrl = uploadIcon(iconBytes, iconMd5, appName)
            if (uploadUrl != null) {
                Log.d(TAG, "图标上传成功: $iconMd5 -> $uploadUrl")
                IconHashCache.put(iconMd5, uploadUrl)
                return uploadUrl
            }

            Log.w(TAG, "图标URL转换失败: $iconMd5")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "图标URL转换异常: $iconMd5", e)
            return null
        } finally {
            lock.unlock()
            // 清理不再需要的锁对象，避免内存泄漏
            md5Locks.remove(iconMd5, lock)
        }
    }

    /**
     * 检查远程图标是否已存在
     * POST {baseUrl}{checkEndpoint}
     * Body: {"resources": [{"tag": "{tag}", "md5": "{md5}"}]}
     */
    private fun checkIconExists(iconMd5: String): String? {
        val url = buildUrl(config.checkEndpoint)
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty(config.headerAuthToken, config.token)
            connection.doOutput = true
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT

            val body = JSONObject().apply {
                val resources = JSONArray().apply {
                    put(JSONObject().apply {
                        put("tag", config.tag)
                        put("md5", iconMd5)
                    })
                }
                put("resources", resources)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(body.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                val success = json.optBoolean("success", false)
                if (success) {
                    val data = json.optJSONObject("data")
                    val resources = data?.optJSONArray("resources")
                    if (resources != null && resources.length() > 0) {
                        val resource = resources.getJSONObject(0)
                        if (resource.optString("status") == "exists") {
                            val iconUrl = resource.optString("url", "")
                            if (iconUrl.isNotEmpty()) {
                                return iconUrl
                            }
                        }
                    }
                }
                Log.d(TAG, "远程检查: 图标不存在 $iconMd5")
            } else {
                Log.w(TAG, "远程检查失败: HTTP $responseCode")
            }

            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "远程检查异常: $iconMd5", e)
        }
        return null
    }

    /**
     * 上传图标到远程服务器
     * POST {baseUrl}{uploadEndpoint}
     * Body: raw binary image data
     */
    private fun uploadIcon(iconBytes: ByteArray, iconMd5: String, appName: String): String? {
        val url = buildUrl(config.uploadEndpoint)
        try {
            val mimeType = detectMimeType(iconBytes)

            val filename = config.filenameTemplate
                .replace("{md5}", iconMd5)
                .replace("{tag}", config.tag)
                .replace("{appName}", appName)

            val description = config.descriptionTemplate
                .replace("{appName}", appName)
                .replace("{md5}", iconMd5)
                .replace("{tag}", config.tag)

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", mimeType)
            connection.setRequestProperty(config.headerAuthToken, config.token)
            connection.setRequestProperty(config.headerUploadTag, config.tag)
            connection.setRequestProperty(config.headerUploadFilename, filename)
            connection.setRequestProperty(config.headerUploadFilesize, iconBytes.size.toString())
            connection.setRequestProperty(config.headerUploadSource, "phone-api")
            connection.setRequestProperty(config.headerUploadDescription, description)
            connection.setRequestProperty("x-upload-mime", mimeType)
            connection.doOutput = true
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT

            connection.outputStream.use { os ->
                os.write(iconBytes)
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                val code = json.optInt("code", -1)
                if (code == 0) {
                    val iconUrl = json.optJSONObject("data")
                        ?.optJSONObject("result")
                        ?.optJSONObject("resource")
                        ?.optString("url", "")
                    if (!iconUrl.isNullOrEmpty()) {
                        return iconUrl
                    }
                }
                Log.w(TAG, "上传响应异常: code=$code")
            } else {
                Log.w(TAG, "上传失败: HTTP $responseCode")
            }

            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "上传异常: $iconMd5", e)
        }
        return null
    }

    /**
     * 构建完整URL
     */
    private fun buildUrl(endpoint: String): String {
        val base = config.baseUrl.trimEnd('/')
        val path = endpoint.trimStart('/')
        return "$base/$path"
    }

    /**
     * 从二进制数据头部检测MIME类型
     */
    private fun detectMimeType(data: ByteArray): String {
        if (data.size < 12) return "image/png"
        return when {
            data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() && data[2] == 0xFF.toByte() -> "image/jpeg"
            data[0] == 0x89.toByte() && data[1] == 'P'.code.toByte() &&
                data[2] == 'N'.code.toByte() && data[3] == 'G'.code.toByte() -> "image/png"
            data[0] == 'G'.code.toByte() && data[1] == 'I'.code.toByte() &&
                data[2] == 'F'.code.toByte() && data[3] == '8'.code.toByte() -> "image/gif"
            data[0] == 'R'.code.toByte() && data[1] == 'I'.code.toByte() &&
                data[2] == 'F'.code.toByte() && data[3] == 'F'.code.toByte() &&
                data[8] == 'W'.code.toByte() && data[9] == 'E'.code.toByte() &&
                data[10] == 'B'.code.toByte() && data[11] == 'P'.code.toByte() -> "image/webp"
            else -> "image/png"
        }
    }
}

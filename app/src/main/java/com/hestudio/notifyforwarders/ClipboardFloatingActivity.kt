package com.hestudio.notifyforwarders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.hestudio.notifyforwarders.constants.ApiConstants
import com.hestudio.notifyforwarders.service.NotificationService
import com.hestudio.notifyforwarders.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 网络请求结果
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String) : NetworkResult<Nothing>()
}

/**
 * 剪贴板浮动Activity
 * 专门用于在通知栏点击时获取剪贴板内容并发送
 * 设计为透明浮动窗口，最小化用户界面干扰
 */
class ClipboardFloatingActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ClipboardFloatingActivity"
        private const val TASK_TIMEOUT_MS = 30000L // 30秒

        /**
         * 启动剪贴板浮动Activity
         */
        fun start(context: Context) {
            val intent = Intent(context, ClipboardFloatingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                       Intent.FLAG_ACTIVITY_CLEAR_TOP or
                       Intent.FLAG_ACTIVITY_SINGLE_TOP or
                       Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            context.startActivity(intent)
        }
    }

    // 防止重复处理剪贴板的标志位
    private var isClipboardProcessed = false

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { context ->
            val languageCode = ServerPreferences.getSelectedLanguage(context)
            LocaleHelper.setLocale(context, languageCode)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "ClipboardFloatingActivity onCreate")

        // 设置透明背景
        setContentView(android.R.layout.activity_list_item)
        findViewById<android.view.View>(android.R.id.content).setBackgroundColor(
            android.graphics.Color.TRANSPARENT
        )

        // 不在onCreate中处理剪贴板，等待onWindowFocusChanged
        Log.d(TAG, "等待窗口获得焦点后处理剪贴板")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !isClipboardProcessed) {
            Log.d(TAG, "窗口获得焦点，现在可以安全访问剪贴板")

            // 设置标志位防止重复处理
            isClipboardProcessed = true

            // 立即同步获取剪贴板内容（参考KDE Connect实现）
            val immediateClipboardContent = tryGetClipboardImmediately()

            // 开始处理剪贴板任务
            handleClipboardTask(immediateClipboardContent)
        } else if (hasFocus && isClipboardProcessed) {
            Log.d(TAG, "窗口再次获得焦点，但剪贴板已处理，跳过")
        }
    }

    /**
     * 立即同步获取剪贴板内容
     * 参考KDE Connect的实现，在Activity获得焦点时立即尝试读取剪贴板
     */
    private fun tryGetClipboardImmediately(): ClipboardImageUtils.ClipboardContent? {
        return try {
            Log.d(TAG, "立即同步获取剪贴板内容")

            // 使用专门的立即读取方法，不进行复杂的权限检查和重试
            ClipboardImageUtils.readClipboardContentImmediate(this)
        } catch (e: SecurityException) {
            Log.w(TAG, "立即获取剪贴板权限不足，将使用延迟获取策略", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "立即获取剪贴板失败，将使用延迟获取策略", e)
            null
        }
    }

    /**
     * 处理剪贴板任务
     * @param immediateContent 立即获取到的剪贴板内容，如果为null则使用延迟获取策略
     */
    private fun handleClipboardTask(immediateContent: ClipboardImageUtils.ClipboardContent?) {
        Log.d(TAG, "开始处理剪贴板任务")

        lifecycleScope.launch {
            val result = withTimeoutOrNull(TASK_TIMEOUT_MS) {
                try {
                    // 更新通知状态为发送中
                    if (ServerPreferences.isPersistentNotificationEnabled(this@ClipboardFloatingActivity)) {
                        NotificationService.updateNotificationState(
                            this@ClipboardFloatingActivity,
                            PersistentNotificationManager.SendingState.SENDING_CLIPBOARD
                        )
                    }

                    // 检查服务器地址配置
                    val serverAddress = ServerPreferences.getServerAddress(this@ClipboardFloatingActivity)
                    if (serverAddress.isEmpty()) {
                        Log.e(TAG, "服务器地址未配置")
                        ErrorNotificationUtils.showClipboardSendError(
                            this@ClipboardFloatingActivity,
                            getString(R.string.server_not_configured)
                        )
                        return@withTimeoutOrNull false
                    }

                    // 获取剪贴板内容：优先使用立即获取的内容，否则使用延迟获取策略
                    val clipboardContent = if (immediateContent != null && immediateContent.type != ClipboardImageUtils.ContentType.EMPTY) {
                        Log.d(TAG, "使用立即获取的剪贴板内容")
                        immediateContent
                    } else {
                        Log.d(TAG, "立即获取失败或内容为空，使用延迟获取策略")

                        // 减少等待时间，提高响应速度
                        delay(150)

                        // 使用重试机制读取剪贴板内容
                        try {
                            withContext(Dispatchers.Main) {
                                ClipboardImageUtils.readClipboardContentWithRetry(this@ClipboardFloatingActivity, 3)
                            }
                        } catch (e: SecurityException) {
                            Log.e(TAG, "剪贴板权限不足", e)
                            ErrorNotificationUtils.showClipboardPermissionError(this@ClipboardFloatingActivity)
                            return@withTimeoutOrNull false
                        } catch (e: Exception) {
                            Log.e(TAG, "读取剪贴板失败", e)
                            ErrorNotificationUtils.showClipboardSendError(
                                this@ClipboardFloatingActivity,
                                e.message ?: getString(R.string.unknown_error)
                            )
                            return@withTimeoutOrNull false
                        }
                    }

                    if (clipboardContent.type == ClipboardImageUtils.ContentType.EMPTY) {
                        Log.d(TAG, "剪贴板为空")
                        withContext(Dispatchers.Main) {
                            ToastManager.showToast(this@ClipboardFloatingActivity, getString(R.string.clipboard_empty))
                        }
                        return@withTimeoutOrNull false
                    }

                    Log.d(TAG, "成功获取剪贴板内容，类型: ${clipboardContent.type}")

                    // 记录内容长度（用于调试）
                    val contentLength = clipboardContent.content.length
                    Log.d(TAG, "剪贴板内容长度: $contentLength 字符")

                    // 发送剪贴板内容
                    val networkResult = when (clipboardContent.type) {
                        ClipboardImageUtils.ContentType.TEXT -> {
                            sendClipboardText(clipboardContent.content)
                        }
                        ClipboardImageUtils.ContentType.IMAGE -> {
                            sendClipboardImage(clipboardContent.content)
                        }
                        else -> NetworkResult.Error(getString(R.string.unknown_error))
                    }

                    when (networkResult) {
                        is NetworkResult.Success -> {
                            withContext(Dispatchers.Main) {
                                ToastManager.showToast(this@ClipboardFloatingActivity, getString(R.string.clipboard_sent_success))
                            }
                            Log.d(TAG, "剪贴板发送成功")
                            return@withTimeoutOrNull true
                        }
                        is NetworkResult.Error -> {
                            ErrorNotificationUtils.showClipboardSendError(
                                this@ClipboardFloatingActivity,
                                networkResult.message
                            )
                            return@withTimeoutOrNull false
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "处理剪贴板任务时发生未知错误", e)
                    ErrorNotificationUtils.showClipboardSendError(
                        this@ClipboardFloatingActivity,
                        e.message ?: getString(R.string.unknown_error)
                    )
                    return@withTimeoutOrNull false
                }
            }

            // 处理超时情况
            if (result == null) {
                Log.e(TAG, "剪贴板发送任务超时")
                ErrorNotificationUtils.showClipboardSendError(
                    this@ClipboardFloatingActivity,
                    getString(R.string.task_timeout)
                )
            }

            // 恢复通知状态
            if (ServerPreferences.isPersistentNotificationEnabled(this@ClipboardFloatingActivity)) {
                NotificationService.updateNotificationState(
                    this@ClipboardFloatingActivity,
                    PersistentNotificationManager.SendingState.IDLE
                )
            }

            // 关闭Activity
            finish()
        }
    }

    /**
     * 发送剪贴板文本
     */
    private suspend fun sendClipboardText(base64Content: String): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val serverAddress = ServerPreferences.getServerAddress(this@ClipboardFloatingActivity)
                val serverUrl = ApiConstants.buildApiUrl(serverAddress, ApiConstants.ENDPOINT_CLIPBOARD_TEXT)
                val url = URL(serverUrl)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = ApiConstants.METHOD_POST
                connection.setRequestProperty("Content-Type", ApiConstants.CONTENT_TYPE_JSON)
                connection.doOutput = true
                connection.connectTimeout = ApiConstants.TIMEOUT_CLIPBOARD_CONNECT
                connection.readTimeout = ApiConstants.TIMEOUT_CLIPBOARD_READ

                val jsonBody = JSONObject().apply {
                    put(ApiConstants.FIELD_CONTENT, base64Content)
                    put(ApiConstants.FIELD_DEVICE_NAME, getDeviceName())
                    put(ApiConstants.FIELD_TYPE, ApiConstants.CONTENT_TYPE_TEXT)
                }

                connection.outputStream.use { outputStream ->
                    outputStream.write(jsonBody.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "剪贴板文本发送响应码: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    NetworkResult.Success("发送成功")
                } else {
                    val errorMessage = try {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                    } catch (e: Exception) {
                        "HTTP $responseCode"
                    }
                    NetworkResult.Error("发送失败: $errorMessage")
                }

            } catch (e: Exception) {
                Log.e(TAG, "发送剪贴板文本失败", e)
                NetworkResult.Error(e.message ?: "网络错误")
            }
        }
    }

    /**
     * 发送剪贴板图片
     */
    private suspend fun sendClipboardImage(base64Content: String): NetworkResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val serverAddress = ServerPreferences.getServerAddress(this@ClipboardFloatingActivity)
                val serverUrl = ApiConstants.buildApiUrl(serverAddress, ApiConstants.ENDPOINT_CLIPBOARD_IMAGE)
                val url = URL(serverUrl)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = ApiConstants.METHOD_POST
                connection.setRequestProperty("Content-Type", ApiConstants.CONTENT_TYPE_JSON)
                connection.doOutput = true
                connection.connectTimeout = ApiConstants.TIMEOUT_CLIPBOARD_CONNECT
                connection.readTimeout = ApiConstants.TIMEOUT_CLIPBOARD_READ

                val jsonBody = JSONObject().apply {
                    put(ApiConstants.FIELD_CONTENT, base64Content)
                    put(ApiConstants.FIELD_DEVICE_NAME, getDeviceName())
                    put(ApiConstants.FIELD_TYPE, ApiConstants.CONTENT_TYPE_IMAGE)
                }

                connection.outputStream.use { outputStream ->
                    outputStream.write(jsonBody.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "剪贴板图片发送响应码: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    NetworkResult.Success("发送成功")
                } else {
                    val errorMessage = try {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                    } catch (e: Exception) {
                        "HTTP $responseCode"
                    }
                    NetworkResult.Error("发送失败: $errorMessage")
                }

            } catch (e: Exception) {
                Log.e(TAG, "发送剪贴板图片失败", e)
                NetworkResult.Error(e.message ?: "网络错误")
            }
        }
    }

    /**
     * 获取设备名称
     */
    private fun getDeviceName(): String {
        return try {
            android.os.Build.MODEL ?: "Unknown Device"
        } catch (e: Exception) {
            "Unknown Device"
        }
    }
}

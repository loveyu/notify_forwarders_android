package com.hestudio.notifyforwarders.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.hestudio.notifyforwarders.MainActivity
import com.hestudio.notifyforwarders.R
import com.hestudio.notifyforwarders.constants.ApiConstants
import com.hestudio.notifyforwarders.util.AppStateManager
import com.hestudio.notifyforwarders.util.ClipboardImageUtils
import com.hestudio.notifyforwarders.util.ErrorNotificationUtils
import com.hestudio.notifyforwarders.util.MediaPermissionUtils
import com.hestudio.notifyforwarders.util.PersistentNotificationManager
import com.hestudio.notifyforwarders.util.ServerPreferences
import com.hestudio.notifyforwarders.util.ToastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 网络请求结果
 */
sealed class NetworkResult {
    object Success : NetworkResult()
    data class Error(val message: String) : NetworkResult()
}

/**
 * 处理通知操作的服务
 */
class NotificationActionService : Service() {

    companion object {
        private const val TAG = "NotificationActionService"
        const val ACTION_SEND_CLIPBOARD = "com.hestudio.notifyforwarders.SEND_CLIPBOARD"
        const val ACTION_SEND_IMAGE = "com.hestudio.notifyforwarders.SEND_IMAGE"

        // 任务超时时间（毫秒）
        private const val TASK_TIMEOUT_MS = 30000L // 30秒

        /**
         * 发送剪贴板内容
         */
        fun sendClipboard(context: Context) {
            val intent = Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_SEND_CLIPBOARD
            }
            context.startService(intent)
        }

        /**
         * 发送最新图片
         */
        fun sendLatestImage(context: Context) {
            val intent = Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_SEND_IMAGE
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // 分别管理剪贴板和图片任务
    private var clipboardJob: Job? = null
    private var imageJob: Job? = null

    // 任务运行状态标志
    private val isClipboardTaskRunning = AtomicBoolean(false)
    private val isImageTaskRunning = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "NotificationActionService onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_SEND_CLIPBOARD -> {
                handleSendClipboard()
            }
            ACTION_SEND_IMAGE -> handleSendImage()
            else -> {
                Log.w(TAG, "未知的操作: ${intent?.action}")
                restoreNotificationStateToIdle()
                stopSelfSafely()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardJob?.cancel()
        imageJob?.cancel()
        isClipboardTaskRunning.set(false)
        isImageTaskRunning.set(false)

        // 确保恢复持久化通知状态
        restoreNotificationStateToIdle()
    }

    /**
     * 启动MainActivity让应用进入前台并获得焦点
     */
    private fun bringAppToForeground() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                       Intent.FLAG_ACTIVITY_CLEAR_TOP or
                       Intent.FLAG_ACTIVITY_SINGLE_TOP or
                       Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                // 添加这个标志来确保Activity获得焦点
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            startActivity(intent)
            Log.d(TAG, "已启动MainActivity，让应用进入前台并获得焦点")
        } catch (e: Exception) {
            Log.e(TAG, "启动MainActivity失败", e)
        }
    }

    /**
     * 仅在需要时启动MainActivity让应用进入前台
     * 减少不必要的页面闪烁
     */
    private fun bringAppToForegroundIfNeeded() {
        // 检查应用是否在后台，只有在后台时才启动MainActivity
        if (AppStateManager.isAppInBackground()) {
            Log.d(TAG, "应用在后台，启动MainActivity")
            bringAppToForeground()
        } else {
            Log.d(TAG, "应用已在前台，无需启动MainActivity")
        }
    }

    /**
     * 恢复持久化通知状态到空闲状态
     */
    private fun restoreNotificationStateToIdle() {
        try {
            // 只有在持久化通知开启时才更新持久化通知状态
            if (ServerPreferences.isPersistentNotificationEnabled(this)) {
                NotificationService.updateNotificationState(
                    this,
                    PersistentNotificationManager.SendingState.IDLE
                )
                Log.d(TAG, "持久化通知状态已恢复到空闲状态")
            }
        } catch (e: Exception) {
            Log.e(TAG, "恢复持久化通知状态失败", e)
        }
    }

    /**
     * 安全地停止服务
     */
    private fun stopSelfSafely() {
        try {
            // 检查是否还有任务在运行
            val hasRunningTasks = isClipboardTaskRunning.get() || isImageTaskRunning.get()

            if (!hasRunningTasks) {
                Log.d(TAG, "没有任务在运行，停止服务")
                // 停止服务
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "停止服务失败", e)
            // 即使出现异常，也要尝试停止服务
            try {
                stopSelf()
            } catch (e2: Exception) {
                Log.e(TAG, "强制停止服务失败", e2)
            }
        }
    }

    /**
     * 处理发送剪贴板内容
     * 直接在服务中处理，避免后台启动Activity的限制
     */
    private fun handleSendClipboard() {
        Log.d(TAG, "开始处理剪贴板发送，应用状态: ${AppStateManager.getStateDescription()}")

        // 检查是否已有剪贴板任务在运行
        if (isClipboardTaskRunning.get()) {
            Log.w(TAG, "剪贴板任务已在运行，忽略重复请求")
            ToastManager.showToast(this, getString(R.string.task_already_running_please_wait))
            restoreNotificationStateToIdle()
            stopSelfSafely()
            return
        }

        try {
            // 设置任务运行状态
            isClipboardTaskRunning.set(true)

            // 直接在服务中处理剪贴板，避免启动Activity
            Log.d(TAG, "在服务中直接处理剪贴板发送")

            clipboardJob = serviceScope.launch {
                handleClipboardInService()
            }

        } catch (e: Exception) {
            Log.e(TAG, "处理剪贴板发送失败", e)
            ErrorNotificationUtils.showClipboardSendError(
                this,
                e.message ?: getString(R.string.unknown_error)
            )
            isClipboardTaskRunning.set(false)
            restoreNotificationStateToIdle()
            stopSelfSafely()
        }
    }

    /**
     * 在服务中直接处理剪贴板发送
     */
    private suspend fun handleClipboardInService() {
        val result = withTimeoutOrNull(TASK_TIMEOUT_MS) {
            try {
                // 更新通知状态为发送中
                if (ServerPreferences.isPersistentNotificationEnabled(this@NotificationActionService)) {
                    NotificationService.updateNotificationState(
                        this@NotificationActionService,
                        PersistentNotificationManager.SendingState.SENDING_CLIPBOARD
                    )
                }

                // 检查服务器地址配置
                val serverAddress = ServerPreferences.getServerAddress(this@NotificationActionService)
                if (serverAddress.isEmpty()) {
                    Log.e(TAG, "服务器地址未配置")
                    ErrorNotificationUtils.showClipboardSendError(
                        this@NotificationActionService,
                        getString(R.string.server_not_configured)
                    )
                    return@withTimeoutOrNull false
                }

                // 等待一小段时间确保可以访问剪贴板
                delay(300)

                // 读取剪贴板内容
                val clipboardContent = try {
                    withContext(Dispatchers.Main) {
                        ClipboardImageUtils.readClipboardContentWithRetry(this@NotificationActionService, 3)
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "剪贴板权限不足", e)
                    ErrorNotificationUtils.showClipboardPermissionError(this@NotificationActionService)
                    return@withTimeoutOrNull false
                } catch (e: Exception) {
                    Log.e(TAG, "读取剪贴板失败", e)
                    ErrorNotificationUtils.showClipboardSendError(
                        this@NotificationActionService,
                        e.message ?: getString(R.string.unknown_error)
                    )
                    return@withTimeoutOrNull false
                }

                if (clipboardContent.type == ClipboardImageUtils.ContentType.EMPTY) {
                    Log.d(TAG, "剪贴板为空")
                    withContext(Dispatchers.Main) {
                        ToastManager.showToast(this@NotificationActionService, getString(R.string.clipboard_empty))
                    }
                    return@withTimeoutOrNull false
                }

                Log.d(TAG, "成功获取剪贴板内容，类型: ${clipboardContent.type}")

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
                            ToastManager.showToast(this@NotificationActionService, getString(R.string.clipboard_sent_success))
                        }
                        Log.d(TAG, "剪贴板发送成功")
                        return@withTimeoutOrNull true
                    }
                    is NetworkResult.Error -> {
                        ErrorNotificationUtils.showClipboardSendError(
                            this@NotificationActionService,
                            networkResult.message
                        )
                        return@withTimeoutOrNull false
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "处理剪贴板任务时发生未知错误", e)
                ErrorNotificationUtils.showClipboardSendError(
                    this@NotificationActionService,
                    e.message ?: getString(R.string.unknown_error)
                )
                return@withTimeoutOrNull false
            }
        }

        // 处理超时情况
        if (result == null) {
            Log.e(TAG, "剪贴板发送任务超时")
            ErrorNotificationUtils.showClipboardSendError(
                this@NotificationActionService,
                getString(R.string.task_timeout)
            )
        }

        // 恢复通知状态
        if (ServerPreferences.isPersistentNotificationEnabled(this@NotificationActionService)) {
            NotificationService.updateNotificationState(
                this@NotificationActionService,
                PersistentNotificationManager.SendingState.IDLE
            )
        }

        // 清理任务状态并停止服务
        isClipboardTaskRunning.set(false)
        stopSelfSafely()
    }

    /**
     * 处理发送最新图片
     */
    private fun handleSendImage() {
        Log.d(TAG, "开始处理图片发送，应用状态: ${AppStateManager.getStateDescription()}")

        // 检查是否已有图片任务在运行
        if (isImageTaskRunning.get()) {
            Log.w(TAG, "图片任务已在运行，忽略重复请求")
            ToastManager.showToast(this, getString(R.string.task_already_running_please_wait))
            restoreNotificationStateToIdle()
            stopSelfSafely()
            return
        }

        imageJob?.cancel()
        imageJob = serviceScope.launch {
            // 使用超时机制包装整个任务
            val result = withTimeoutOrNull(TASK_TIMEOUT_MS) {
                try {
                    // 设置任务运行状态
                    isImageTaskRunning.set(true)

                    // 只有在持久化通知开启时才更新持久化通知状态
                    if (ServerPreferences.isPersistentNotificationEnabled(this@NotificationActionService)) {
                        NotificationService.updateNotificationState(
                            this@NotificationActionService,
                            PersistentNotificationManager.SendingState.SENDING_IMAGE
                        )
                    }

                    // 检查服务器地址配置
                    val serverAddress = ServerPreferences.getServerAddress(this@NotificationActionService)
                    if (serverAddress.isEmpty()) {
                        Log.e(TAG, "服务器地址未配置")
                        ErrorNotificationUtils.showImageSendError(
                            this@NotificationActionService,
                            getString(R.string.server_not_configured)
                        )
                        return@withTimeoutOrNull false
                    }

                    // 检查媒体权限
                    if (!MediaPermissionUtils.hasMediaPermission(this@NotificationActionService)) {
                        Log.w(TAG, "媒体权限不足")
                        ErrorNotificationUtils.showMediaPermissionError(this@NotificationActionService)
                        return@withTimeoutOrNull false
                    }

                    val imageContent = ClipboardImageUtils.getLatestImage(this@NotificationActionService)

                    if (imageContent == null) {
                        return@withTimeoutOrNull false
                    }

                    val networkResult = sendImageRaw(imageContent)

                    when (networkResult) {
                        is NetworkResult.Success -> {
                            withContext(Dispatchers.Main) {
                                ToastManager.showToast(this@NotificationActionService, getString(R.string.image_sent_success))
                            }
                            Log.d(TAG, "图片发送成功，应用状态: ${AppStateManager.getStateDescription()}")
                            return@withTimeoutOrNull true
                        }
                        is NetworkResult.Error -> {
                            ErrorNotificationUtils.showImageSendError(
                                this@NotificationActionService,
                                networkResult.message
                            )
                            return@withTimeoutOrNull false
                        }
                    }

                } catch (e: SecurityException) {
                    Log.e(TAG, "媒体权限不足", e)
                    ErrorNotificationUtils.showMediaPermissionError(this@NotificationActionService)
                    return@withTimeoutOrNull false
                } catch (e: Exception) {
                    Log.e(TAG, "处理图片发送时发生未知错误", e)
                    ErrorNotificationUtils.showImageSendError(
                        this@NotificationActionService,
                        e.message ?: getString(R.string.unknown_error)
                    )
                    return@withTimeoutOrNull false
                }
            }

            // 处理超时情况
            if (result == null) {
                Log.e(TAG, "图片发送任务超时")
                ErrorNotificationUtils.showImageSendError(
                    this@NotificationActionService,
                    getString(R.string.task_timeout)
                )
            }

            // 清理任务状态
            isImageTaskRunning.set(false)

            // 恢复持久化通知状态
            restoreNotificationStateToIdle()

            stopSelfSafely()
        }
    }



    /**
     * 发送图片RAW内容
     */
    private suspend fun sendImageRaw(imageContent: ClipboardImageUtils.ImageContent): NetworkResult {
        return try {
            val serverAddress = ServerPreferences.getServerAddress(this)
            if (serverAddress.isEmpty()) {
                Log.e(TAG, "服务器地址未配置")
                return NetworkResult.Error(getString(R.string.server_not_configured))
            }

            val serverUrl = ApiConstants.buildApiUrl(serverAddress, ApiConstants.ENDPOINT_IMAGE_RAW)
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = ApiConstants.METHOD_POST
            connection.setRequestProperty("Content-Type", ApiConstants.CONTENT_TYPE_JSON)
            connection.doOutput = true
            connection.connectTimeout = ApiConstants.TIMEOUT_IMAGE_CONNECT
            connection.readTimeout = ApiConstants.TIMEOUT_IMAGE_READ

            val jsonBody = JSONObject().apply {
                put(ApiConstants.FIELD_CONTENT, imageContent.content)
                put(ApiConstants.FIELD_DEVICE_NAME, getDeviceName())
                put(ApiConstants.FIELD_MIME_TYPE, imageContent.mimeType)

                // 添加文件信息
                imageContent.fileName?.let { fileName ->
                    put(ApiConstants.FIELD_FILE_NAME, fileName)
                }
                imageContent.filePath?.let { filePath ->
                    put(ApiConstants.FIELD_FILE_PATH, filePath)
                }
                imageContent.dateAdded?.let { dateAdded ->
                    put(ApiConstants.FIELD_DATE_ADDED, dateAdded)
                }
                imageContent.dateModified?.let { dateModified ->
                    put(ApiConstants.FIELD_DATE_MODIFIED, dateModified)
                }
            }

            val outputStream = connection.outputStream
            val writer = OutputStreamWriter(outputStream, ApiConstants.CHARSET_UTF8)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            connection.disconnect()

            Log.d(TAG, "图片RAW发送响应: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                NetworkResult.Success
            } else {
                NetworkResult.Error(getString(R.string.network_error_with_code, responseCode))
            }

        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "连接服务器失败", e)
            NetworkResult.Error(getString(R.string.network_connection_failed))
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "网络请求超时", e)
            NetworkResult.Error(getString(R.string.network_timeout))
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "无法解析服务器地址", e)
            NetworkResult.Error(getString(R.string.network_host_unknown))
        } catch (e: Exception) {
            Log.e(TAG, "发送图片RAW失败", e)
            NetworkResult.Error(e.message ?: getString(R.string.unknown_error))
        }
    }



    /**
     * 发送剪贴板文本
     */
    private suspend fun sendClipboardText(base64Content: String): NetworkResult {
        return withContext(Dispatchers.IO) {
            try {
                val serverAddress = ServerPreferences.getServerAddress(this@NotificationActionService)
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
                    NetworkResult.Success
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
    private suspend fun sendClipboardImage(base64Content: String): NetworkResult {
        return withContext(Dispatchers.IO) {
            try {
                val serverAddress = ServerPreferences.getServerAddress(this@NotificationActionService)
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
                    NetworkResult.Success
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
        return Build.MODEL
    }

    /**
     * 显示Toast消息
     */
    private fun showToast(message: String) {
        ToastManager.showToast(this, message)
    }
}

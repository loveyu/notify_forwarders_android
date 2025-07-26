package com.hestudio.notifyforwarders.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.hestudio.notifyforwarders.R
import com.hestudio.notifyforwarders.constants.ApiConstants
import com.hestudio.notifyforwarders.util.ClipboardImageUtils
import com.hestudio.notifyforwarders.util.ServerPreferences
import com.hestudio.notifyforwarders.util.ErrorNotificationUtils
import com.hestudio.notifyforwarders.util.MediaPermissionUtils
import com.hestudio.notifyforwarders.util.AppStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

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
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "notification_action_service"
        const val ACTION_SEND_CLIPBOARD = "com.hestudio.notifyforwarders.SEND_CLIPBOARD"
        const val ACTION_SEND_IMAGE = "com.hestudio.notifyforwarders.SEND_IMAGE"

        /**
         * 发送剪贴板内容
         */
        fun sendClipboard(context: Context) {
            val intent = Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_SEND_CLIPBOARD
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * 发送最新图片
         */
        fun sendLatestImage(context: Context) {
            val intent = Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_SEND_IMAGE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var currentJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification("正在处理请求..."))

        when (intent?.action) {
            ACTION_SEND_CLIPBOARD -> handleSendClipboard()
            ACTION_SEND_IMAGE -> handleSendImage()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        currentJob?.cancel()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "快捷操作服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "处理快捷发送操作"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("快捷发送")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * 更新前台服务通知
     */
    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(content))
    }

    /**
     * 处理发送剪贴板内容
     */
    private fun handleSendClipboard() {
        Log.d(TAG, "开始处理剪贴板发送，应用状态: ${AppStateManager.getStateDescription()}")

        currentJob?.cancel()
        currentJob = serviceScope.launch {
            try {
                // 检查服务器地址配置
                val serverAddress = ServerPreferences.getServerAddress(this@NotificationActionService)
                if (serverAddress.isEmpty()) {
                    Log.e(TAG, "服务器地址未配置")
                    ErrorNotificationUtils.showClipboardSendError(
                        this@NotificationActionService,
                        getString(R.string.server_not_configured)
                    )
                    stopSelf()
                    return@launch
                }

                // 检查应用是否在前台，剪贴板访问需要前台权限
                if (AppStateManager.isAppInBackground()) {
                    Log.w(TAG, "应用在后台，无法访问剪贴板")
                    ErrorNotificationUtils.showClipboardPermissionError(this@NotificationActionService)
                    stopSelf()
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    showToast(getString(R.string.reading_clipboard))
                }

                val clipboardContent = try {
                    withContext(Dispatchers.Main) {
                        ClipboardImageUtils.readClipboardContent(this@NotificationActionService)
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "剪贴板权限不足", e)
                    ErrorNotificationUtils.showClipboardPermissionError(this@NotificationActionService)
                    stopSelf()
                    return@launch
                } catch (e: Exception) {
                    Log.e(TAG, "读取剪贴板失败", e)
                    ErrorNotificationUtils.showClipboardSendError(
                        this@NotificationActionService,
                        e.message ?: getString(R.string.unknown_error)
                    )
                    stopSelf()
                    return@launch
                }

                if (clipboardContent.type == ClipboardImageUtils.ContentType.EMPTY) {
                    withContext(Dispatchers.Main) {
                        showToast(getString(R.string.clipboard_empty))
                    }
                    stopSelf()
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    showToast(getString(R.string.sending_content))
                }

                val result = when (clipboardContent.type) {
                    ClipboardImageUtils.ContentType.TEXT -> {
                        sendClipboardText(clipboardContent.content)
                    }
                    ClipboardImageUtils.ContentType.IMAGE -> {
                        sendClipboardImage(clipboardContent.content)
                    }
                    else -> NetworkResult.Error(getString(R.string.unknown_error))
                }

                when (result) {
                    is NetworkResult.Success -> {
                        withContext(Dispatchers.Main) {
                            showToast(getString(R.string.clipboard_sent_success))
                        }
                        Log.d(TAG, "剪贴板发送成功，应用状态: ${AppStateManager.getStateDescription()}")
                    }
                    is NetworkResult.Error -> {
                        ErrorNotificationUtils.showClipboardSendError(
                            this@NotificationActionService,
                            result.message
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "处理剪贴板发送时发生未知错误", e)
                ErrorNotificationUtils.showClipboardSendError(
                    this@NotificationActionService,
                    e.message ?: getString(R.string.unknown_error)
                )
            } finally {
                stopSelf()
            }
        }
    }

    /**
     * 处理发送最新图片
     */
    private fun handleSendImage() {
        Log.d(TAG, "开始处理图片发送，应用状态: ${AppStateManager.getStateDescription()}")

        currentJob?.cancel()
        currentJob = serviceScope.launch {
            try {
                // 检查服务器地址配置
                val serverAddress = ServerPreferences.getServerAddress(this@NotificationActionService)
                if (serverAddress.isEmpty()) {
                    Log.e(TAG, "服务器地址未配置")
                    ErrorNotificationUtils.showImageSendError(
                        this@NotificationActionService,
                        getString(R.string.server_not_configured)
                    )
                    stopSelf()
                    return@launch
                }

                // 检查媒体权限
                if (!MediaPermissionUtils.hasMediaPermission(this@NotificationActionService)) {
                    Log.w(TAG, "媒体权限不足")
                    ErrorNotificationUtils.showMediaPermissionError(this@NotificationActionService)
                    stopSelf()
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    showToast(getString(R.string.reading_image))
                }

                val imageContent = ClipboardImageUtils.getLatestImage(this@NotificationActionService)

                if (imageContent == null) {
                    withContext(Dispatchers.Main) {
                        showToast(getString(R.string.no_images_found))
                    }
                    stopSelf()
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    showToast(getString(R.string.sending_content))
                }

                val result = sendImageRaw(imageContent)

                when (result) {
                    is NetworkResult.Success -> {
                        withContext(Dispatchers.Main) {
                            showToast(getString(R.string.image_sent_success))
                        }
                        Log.d(TAG, "图片发送成功，应用状态: ${AppStateManager.getStateDescription()}")
                    }
                    is NetworkResult.Error -> {
                        ErrorNotificationUtils.showImageSendError(
                            this@NotificationActionService,
                            result.message
                        )
                    }
                }

            } catch (e: SecurityException) {
                Log.e(TAG, "媒体权限不足", e)
                ErrorNotificationUtils.showMediaPermissionError(this@NotificationActionService)
            } catch (e: Exception) {
                Log.e(TAG, "处理图片发送时发生未知错误", e)
                ErrorNotificationUtils.showImageSendError(
                    this@NotificationActionService,
                    e.message ?: getString(R.string.unknown_error)
                )
            } finally {
                stopSelf()
            }
        }
    }

    /**
     * 发送剪贴板文本内容
     */
    private suspend fun sendClipboardText(base64Content: String): NetworkResult {
        return try {
            val serverAddress = ServerPreferences.getServerAddress(this)
            if (serverAddress.isEmpty()) {
                Log.e(TAG, "服务器地址未配置")
                return NetworkResult.Error(getString(R.string.server_not_configured))
            }

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

            val outputStream = connection.outputStream
            val writer = OutputStreamWriter(outputStream, ApiConstants.CHARSET_UTF8)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            connection.disconnect()

            Log.d(TAG, "剪贴板文本发送响应: $responseCode")

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
            Log.e(TAG, "发送剪贴板文本失败", e)
            NetworkResult.Error(e.message ?: getString(R.string.unknown_error))
        }
    }

    /**
     * 发送剪贴板图片内容
     */
    private suspend fun sendClipboardImage(base64Content: String): NetworkResult {
        return try {
            val serverAddress = ServerPreferences.getServerAddress(this)
            if (serverAddress.isEmpty()) {
                Log.e(TAG, "服务器地址未配置")
                return NetworkResult.Error(getString(R.string.server_not_configured))
            }

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

            val outputStream = connection.outputStream
            val writer = OutputStreamWriter(outputStream, ApiConstants.CHARSET_UTF8)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            connection.disconnect()

            Log.d(TAG, "剪贴板图片发送响应: $responseCode")

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
            Log.e(TAG, "发送剪贴板图片失败", e)
            NetworkResult.Error(e.message ?: getString(R.string.unknown_error))
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

            // 添加EXIF信息到header
            imageContent.exifData?.let { exifData ->
                connection.setRequestProperty(ApiConstants.HEADER_EXIF, exifData)
            }

            val jsonBody = JSONObject().apply {
                put(ApiConstants.FIELD_CONTENT, imageContent.content)
                put(ApiConstants.FIELD_DEVICE_NAME, getDeviceName())
                put(ApiConstants.FIELD_MIME_TYPE, imageContent.mimeType)
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
     * 获取设备名称
     */
    private fun getDeviceName(): String {
        return Build.MODEL
    }

    /**
     * 显示Toast消息
     */
    private fun showToast(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(this@NotificationActionService, message, Toast.LENGTH_SHORT).show()
        }
    }
}

package com.hestudio.notifyforwarders.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.hestudio.notifyforwarders.R
import com.hestudio.notifyforwarders.util.ClipboardImageUtils
import com.hestudio.notifyforwarders.util.ServerPreferences
import com.hestudio.notifyforwarders.util.PermissionUtils
import com.hestudio.notifyforwarders.util.ErrorNotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 处理通知操作的服务
 */
class NotificationActionService : IntentService("NotificationActionService") {

    companion object {
        private const val TAG = "NotificationActionService"
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

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_SEND_CLIPBOARD -> handleSendClipboard()
            ACTION_SEND_IMAGE -> handleSendImage()
        }
    }

    /**
     * 处理发送剪贴板内容
     */
    private fun handleSendClipboard() {
        Log.d(TAG, "开始处理剪贴板发送")
        
        showToast(getString(R.string.reading_clipboard))
        
        serviceScope.launch {
            try {
                val clipboardContent = ClipboardImageUtils.readClipboardContent(this@NotificationActionService)
                
                if (clipboardContent.type == ClipboardImageUtils.ContentType.EMPTY) {
                    showToast(getString(R.string.clipboard_empty))
                    return@launch
                }

                showToast(getString(R.string.sending_content))

                val success = when (clipboardContent.type) {
                    ClipboardImageUtils.ContentType.TEXT -> {
                        sendClipboardText(clipboardContent.content)
                    }
                    ClipboardImageUtils.ContentType.IMAGE -> {
                        sendClipboardImage(clipboardContent.content)
                    }
                    else -> false
                }

                if (success) {
                    showToast(getString(R.string.clipboard_sent_success))
                } else {
                    showToast(getString(R.string.clipboard_sent_failed))
                }

            } catch (e: Exception) {
                Log.e(TAG, "发送剪贴板内容失败", e)
                showToast(getString(R.string.clipboard_sent_failed))
            }
        }
    }

    /**
     * 处理发送最新图片
     */
    private fun handleSendImage() {
        Log.d(TAG, "开始处理图片发送")
        
        showToast(getString(R.string.reading_latest_image))
        
        serviceScope.launch {
            try {
                val imageContent = ClipboardImageUtils.getLatestImage(this@NotificationActionService)
                
                if (imageContent == null) {
                    showToast(getString(R.string.no_images_found))
                    return@launch
                }

                showToast(getString(R.string.sending_content))

                val success = sendImageRaw(imageContent)

                if (success) {
                    showToast(getString(R.string.image_sent_success))
                } else {
                    showToast(getString(R.string.image_sent_failed))
                }

            } catch (e: Exception) {
                Log.e(TAG, "发送图片失败", e)
                showToast(getString(R.string.image_sent_failed))
            }
        }
    }

    /**
     * 发送剪贴板文本内容
     */
    private suspend fun sendClipboardText(base64Content: String): Boolean {
        return try {
            val serverAddress = ServerPreferences.getServerAddress(this)
            if (serverAddress.isEmpty()) {
                Log.e(TAG, "服务器地址未配置")
                return false
            }

            val serverUrl = "http://$serverAddress/api/clipboard/text"
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val jsonBody = JSONObject().apply {
                put("content", base64Content)
                put("devicename", getDeviceName())
                put("type", "text")
            }

            val outputStream = connection.outputStream
            val writer = OutputStreamWriter(outputStream, "UTF-8")
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            connection.disconnect()
            
            Log.d(TAG, "剪贴板文本发送响应: $responseCode")
            responseCode == HttpURLConnection.HTTP_OK

        } catch (e: Exception) {
            Log.e(TAG, "发送剪贴板文本失败", e)
            false
        }
    }

    /**
     * 发送剪贴板图片内容
     */
    private suspend fun sendClipboardImage(base64Content: String): Boolean {
        return try {
            val serverAddress = ServerPreferences.getServerAddress(this)
            if (serverAddress.isEmpty()) {
                Log.e(TAG, "服务器地址未配置")
                return false
            }

            val serverUrl = "http://$serverAddress/api/clipboard/image"
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val jsonBody = JSONObject().apply {
                put("content", base64Content)
                put("devicename", getDeviceName())
                put("type", "image")
            }

            val outputStream = connection.outputStream
            val writer = OutputStreamWriter(outputStream, "UTF-8")
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            connection.disconnect()
            
            Log.d(TAG, "剪贴板图片发送响应: $responseCode")
            responseCode == HttpURLConnection.HTTP_OK

        } catch (e: Exception) {
            Log.e(TAG, "发送剪贴板图片失败", e)
            false
        }
    }

    /**
     * 发送图片RAW内容
     */
    private suspend fun sendImageRaw(imageContent: ClipboardImageUtils.ImageContent): Boolean {
        return try {
            val serverAddress = ServerPreferences.getServerAddress(this)
            if (serverAddress.isEmpty()) {
                Log.e(TAG, "服务器地址未配置")
                return false
            }

            val serverUrl = "http://$serverAddress/api/image/raw"
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            // 添加EXIF信息到header
            imageContent.exifData?.let { exifData ->
                connection.setRequestProperty("X-EXIF", exifData)
            }

            val jsonBody = JSONObject().apply {
                put("content", imageContent.content)
                put("devicename", getDeviceName())
                put("mimeType", imageContent.mimeType)
            }

            val outputStream = connection.outputStream
            val writer = OutputStreamWriter(outputStream, "UTF-8")
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            connection.disconnect()
            
            Log.d(TAG, "图片RAW发送响应: $responseCode")
            responseCode == HttpURLConnection.HTTP_OK

        } catch (e: Exception) {
            Log.e(TAG, "发送图片RAW失败", e)
            false
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

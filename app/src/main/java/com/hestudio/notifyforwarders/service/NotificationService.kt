package com.hestudio.notifyforwarders.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import com.hestudio.notifyforwarders.MainActivity
import com.hestudio.notifyforwarders.R
import com.hestudio.notifyforwarders.util.ServerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class NotificationService : NotificationListenerService() {
    
    companion object {
        const val TAG = "NotificationService"
        private const val FOREGROUND_SERVICE_ID = 1001
        private const val CHANNEL_ID = "notifyforwarders_service_channel"
        
        // 存储收到的通知，使用可观察的列表以便UI自动更新
        private val notifications = mutableStateListOf<NotificationData>()
        
        // 获取所有通知
        fun getNotifications(): List<NotificationData> {
            return notifications
        }
        
        // 清除通知列表
        fun clearNotifications() {
            notifications.clear()
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationService onCreate")
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "NotificationService onStartCommand")
        // 如果服务被系统杀死后重新创建，则返回START_STICKY以保持服务运行
        return START_STICKY
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras
        
        val packageName = sbn.packageName
        val appName = getApplicationName(packageName)
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val time = sbn.postTime
        
        Log.d(TAG, "通知已接收: $packageName, $appName, $title, $text")
        
        // 过滤掉没有内容的通知
        if (text.isBlank() && title.isBlank()) {
            Log.d(TAG, "过滤空内容通知: $packageName")
            return
        }
        
        val notificationData = NotificationData(
            id = sbn.id,
            packageName = packageName,
            appName = appName,
            title = title,
            content = text,
            time = time
        )
        
        // 添加到通知列表
        notifications.add(0, notificationData) // 添加到列表头部
        
        // 转发通知到配置的服务器
        forwardNotificationToServer(notificationData)
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 可以在这里处理通知被移除的情况
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NotificationService onDestroy")
        // 尝试重启服务
        val intent = Intent(this, NotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    // 获取应用名称
    private fun getApplicationName(packageName: String): String {
        try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            return packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "无法获取应用名称: $packageName", e)
            return packageName // 如果获取失败，返回包名作为备选
        }
    }
    
    private fun getDeviceName(): String {
        return Build.MODEL // 获取设备型号作为设备名称
    }
    
    private fun forwardNotificationToServer(notification: NotificationData) {
        val serverAddress = ServerPreferences.getServerAddress(this)
        
        if (serverAddress.isEmpty()) {
            Log.d(TAG, "服务器地址未配置，不转发通知")
            return
        }
        
        serviceScope.launch {
            try {
                val serverUrl = "http://$serverAddress/api/notify"
                Log.d(TAG, "正在转发通知到 $serverUrl")
                
                val url = URL(serverUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                // 创建JSON数据，使用指定的参数名
                val jsonBody = JSONObject().apply {
                    put("appname", notification.appName) // 改用appName而不是packageName
                    put("title", notification.title)
                    put("description", notification.content)
                    put("devicename", getDeviceName()) // 添加设备名称参数
                }
                
                // 发送JSON数据
                val outputStream = connection.outputStream
                val writer = OutputStreamWriter(outputStream, "UTF-8")
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()
                
                // 获取响应
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "通知转发成功")
                } else {
                    Log.e(TAG, "通知转发失败: HTTP $responseCode")
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "通知转发失败", e)
            }
        }
    }

    // 创建前台服务通知
    private fun startForeground() {
        // 创建通知渠道（Android 8.0+需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "通知转发服务",
                NotificationManager.IMPORTANCE_LOW // 低优先级，不会打扰用户
            ).apply {
                description = "保持通知转发服务运行"
                setShowBadge(false) // 不在启动器图标上显示通知角标
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 创建启动应用的PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("通知转发服务正在运行")
            .setContentText("点击管理通知转发设置")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 设置为不可清除
            .build()

        // 启动前台服务
        startForeground(FOREGROUND_SERVICE_ID, notification)
    }
}

// 通知数据类
data class NotificationData(
    val id: Int,
    val packageName: String,
    val appName: String, // 添加应用名称字段
    val title: String,
    val content: String,
    val time: Long
)

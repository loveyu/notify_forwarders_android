package com.hestudio.notifyforwarders.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.hestudio.notifyforwarders.R

/**
 * 错误通知工具类
 * 用于显示自动消失的错误通知
 */
object ErrorNotificationUtils {
    
    private const val ERROR_CHANNEL_ID = "error_notifications"
    private const val ERROR_NOTIFICATION_ID_BASE = 2000
    private var notificationIdCounter = ERROR_NOTIFICATION_ID_BASE
    
    /**
     * 初始化错误通知渠道
     */
    fun initializeErrorChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channel = NotificationChannel(
            ERROR_CHANNEL_ID,
            context.getString(R.string.error_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.error_notification_channel_desc)
            setShowBadge(false)
        }
        
        notificationManager.createNotificationChannel(channel)
    }
    
    /**
     * 显示错误通知，20秒后自动消失
     */
    fun showErrorNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = notificationIdCounter++
        
        // 确保渠道已创建
        initializeErrorChannel(context)
        
        // 创建通知
        val notification = NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        
        // 显示通知
        notificationManager.notify(notificationId, notification)
        
        // 20秒后自动取消通知
        Handler(Looper.getMainLooper()).postDelayed({
            notificationManager.cancel(notificationId)
        }, 20000) // 20秒
    }
    
    /**
     * 显示剪贴板权限错误通知
     */
    fun showClipboardPermissionError(context: Context) {
        showErrorNotification(
            context,
            context.getString(R.string.clipboard_permission_error_title),
            context.getString(R.string.clipboard_permission_error_message)
        )
    }
    
    /**
     * 显示媒体权限错误通知
     */
    fun showMediaPermissionError(context: Context) {
        showErrorNotification(
            context,
            context.getString(R.string.media_permission_error_title),
            context.getString(R.string.media_permission_error_message)
        )
    }
    
    /**
     * 显示剪贴板发送失败通知
     */
    fun showClipboardSendError(context: Context, reason: String) {
        showErrorNotification(
            context,
            context.getString(R.string.clipboard_send_error_title),
            context.getString(R.string.clipboard_send_error_message, reason)
        )
    }
    
    /**
     * 显示图片发送失败通知
     */
    fun showImageSendError(context: Context, reason: String) {
        showErrorNotification(
            context,
            context.getString(R.string.image_send_error_title),
            context.getString(R.string.image_send_error_message, reason)
        )
    }
    
    /**
     * 显示服务器连接错误通知
     */
    fun showServerConnectionError(context: Context) {
        showErrorNotification(
            context,
            context.getString(R.string.server_connection_error_title),
            context.getString(R.string.server_connection_error_message)
        )
    }
}

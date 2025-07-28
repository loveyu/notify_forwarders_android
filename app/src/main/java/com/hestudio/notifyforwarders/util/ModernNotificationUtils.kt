package com.hestudio.notifyforwarders.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 现代化通知工具类
 * 替代已弃用的通知相关API，提供Android Q+兼容的通知功能
 */
object ModernNotificationUtils {
    
    private const val TAG = "ModernNotificationUtils"
    
    /**
     * 创建通知渠道（替代已弃用的通知优先级设置）
     * @param context 上下文
     * @param channelId 渠道ID
     * @param channelName 渠道名称
     * @param importance 重要性级别
     * @param description 渠道描述
     */
    fun createNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        description: String? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description?.let { this.description = it }
                // 现代化设置，替代已弃用的方法
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Created notification channel: $channelId")
        }
    }
    
    /**
     * 安全地显示通知（处理权限检查）
     * @param context 上下文
     * @param notificationId 通知ID
     * @param notification 通知对象
     */
    fun showNotificationSafely(
        context: Context,
        notificationId: Int,
        notification: android.app.Notification
    ) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            
            // 检查通知权限
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, notification)
                Log.d(TAG, "Notification shown successfully: $notificationId")
            } else {
                Log.w(TAG, "Notifications are disabled, cannot show notification: $notificationId")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when showing notification: $notificationId", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification: $notificationId", e)
        }
    }
    
    /**
     * 取消通知
     * @param context 上下文
     * @param notificationId 通知ID
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(notificationId)
            Log.d(TAG, "Notification cancelled: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel notification: $notificationId", e)
        }
    }
    
    /**
     * 检查通知权限状态
     * @param context 上下文
     * @return 是否有通知权限
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check notification permission", e)
            false
        }
    }
    
    /**
     * 创建基础通知构建器（使用现代API）
     * @param context 上下文
     * @param channelId 渠道ID
     * @return 通知构建器
     */
    fun createNotificationBuilder(
        context: Context,
        channelId: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId).apply {
            // 使用现代化的通知设置
            setAutoCancel(true)
            setPriority(NotificationCompat.PRIORITY_DEFAULT)
            setDefaults(0) // 不使用默认设置，避免已弃用的行为
        }
    }
    
    /**
     * 清理已弃用的通知渠道
     * @param context 上下文
     * @param deprecatedChannelIds 已弃用的渠道ID列表
     */
    fun cleanupDeprecatedChannels(context: Context, deprecatedChannelIds: List<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            deprecatedChannelIds.forEach { channelId ->
                try {
                    notificationManager.deleteNotificationChannel(channelId)
                    Log.d(TAG, "Deleted deprecated notification channel: $channelId")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete deprecated channel: $channelId", e)
                }
            }
        }
    }
    
    /**
     * 获取通知管理器实例
     * @param context 上下文
     * @return NotificationManagerCompat实例
     */
    fun getNotificationManager(context: Context): NotificationManagerCompat {
        return NotificationManagerCompat.from(context)
    }
}

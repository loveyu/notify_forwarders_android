package com.hestudio.notifyforwarders.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hestudio.notifyforwarders.MainActivity
import com.hestudio.notifyforwarders.R
import com.hestudio.notifyforwarders.service.NotificationActionService

/**
 * 持久化通知状态管理器
 */
object PersistentNotificationManager {
    private const val TAG = "PersistentNotificationManager"

    /**
     * 发送状态枚举
     */
    enum class SendingState {
        IDLE,           // 空闲状态
        SENDING_CLIPBOARD,  // 正在发送剪贴板
        SENDING_IMAGE   // 正在发送图片
    }

    private var currentState = SendingState.IDLE

    /**
     * 更新持久化通知状态（已废弃，使用NotificationService.updateNotificationState）
     */
    @Deprecated("使用NotificationService.updateNotificationState替代")
    fun updateNotificationState(context: Context, state: SendingState) {
        currentState = state
        // 委托给NotificationService处理
        com.hestudio.notifyforwarders.service.NotificationService.updateNotificationState(context, state)
    }

    /**
     * 清除持久化通知（已废弃，现在由NotificationService统一管理）
     */
    @Deprecated("现在由NotificationService统一管理通知")
    fun clearPersistentNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(1000) // 清除持久化通知
            Log.d(TAG, "持久化通知已清除")
        } catch (e: Exception) {
            Log.e(TAG, "清除持久化通知失败", e)
        }
    }

    /**
     * 获取当前发送状态
     */
    fun getCurrentState(): SendingState = currentState


}

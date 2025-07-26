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
import com.hestudio.notifyforwarders.service.NotificationService

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
     * 更新持久化通知状态
     */
    fun updateNotificationState(context: Context, state: SendingState) {
        if (!ServerPreferences.isPersistentNotificationEnabled(context)) {
            return
        }

        currentState = state
        updateForegroundNotification(context)
    }

    /**
     * 获取当前发送状态
     */
    fun getCurrentState(): SendingState = currentState

    /**
     * 更新前台通知
     */
    private fun updateForegroundNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 创建点击通知的Intent
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val notificationBuilder = NotificationCompat.Builder(context, "notification_service_channel")
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(getNotificationText(context))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)

            // 添加操作按钮
            addActionButtons(context, notificationBuilder)

            // 更新通知
            notificationManager.notify(1000, notificationBuilder.build())
            Log.d(TAG, "持久化通知状态已更新: $currentState")

        } catch (e: Exception) {
            Log.e(TAG, "更新持久化通知失败", e)
        }
    }

    /**
     * 获取通知文本
     */
    private fun getNotificationText(context: Context): String {
        return when (currentState) {
            SendingState.SENDING_CLIPBOARD -> context.getString(R.string.sending_clipboard)
            SendingState.SENDING_IMAGE -> context.getString(R.string.sending_image)
            SendingState.IDLE -> context.getString(R.string.persistent_notification_text)
        }
    }

    /**
     * 添加操作按钮
     */
    private fun addActionButtons(context: Context, notificationBuilder: NotificationCompat.Builder) {
        // 剪贴板按钮
        val clipboardIntent = Intent(context, NotificationActionService::class.java).apply {
            action = NotificationActionService.ACTION_SEND_CLIPBOARD
        }
        val clipboardPendingIntent = PendingIntent.getService(
            context,
            1,
            clipboardIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 图片按钮
        val imageIntent = Intent(context, NotificationActionService::class.java).apply {
            action = NotificationActionService.ACTION_SEND_IMAGE
        }
        val imagePendingIntent = PendingIntent.getService(
            context,
            2,
            imageIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 根据当前状态设置按钮文本
        val clipboardButtonText = when (currentState) {
            SendingState.SENDING_CLIPBOARD -> context.getString(R.string.sending_clipboard_short)
            else -> context.getString(R.string.action_send_clipboard)
        }

        val imageButtonText = when (currentState) {
            SendingState.SENDING_IMAGE -> context.getString(R.string.sending_image_short)
            else -> context.getString(R.string.action_send_image)
        }

        notificationBuilder
            .addAction(
                R.drawable.ic_launcher_foreground,
                clipboardButtonText,
                clipboardPendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                imageButtonText,
                imagePendingIntent
            )
    }
}

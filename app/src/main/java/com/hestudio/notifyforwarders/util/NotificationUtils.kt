package com.hestudio.notifyforwarders.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.hestudio.notifyforwarders.service.NotificationService

object NotificationUtils {

    private const val TAG = "NotificationUtils"

    // 检查通知监听权限是否已开启
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    // 打开通知监听设置
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notification listener settings", e)
        }
    }

    // 重启通知监听服务
    fun toggleNotificationListenerService(context: Context) {
        val packageName = context.packageName
        val componentName = ComponentName(packageName, NotificationService::class.java.name)

        try {
            // 先禁用再启用服务以刷新连接
            context.packageManager.setComponentEnabledSetting(
                componentName,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )

            context.packageManager.setComponentEnabledSetting(
                componentName,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )

            Log.d(TAG, "Successfully toggled notification listener service")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle notification listener service", e)
        }
    }

    /**
     * 检查是否支持通知渠道（Android 8.0+）
     */
    fun supportsNotificationChannels(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    /**
     * 检查是否需要POST_NOTIFICATIONS权限（Android 13+）
     */
    fun requiresPostNotificationsPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}

package com.hestudio.notifyforwarders.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.hestudio.notifyforwarders.service.NotificationService

object NotificationUtils {
    
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
            e.printStackTrace()
        }
    }
    
    // 重启通知监听服务
    fun toggleNotificationListenerService(context: Context) {
        val packageName = context.packageName
        val componentName = ComponentName(packageName, NotificationService::class.java.name)
        
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
    }
}

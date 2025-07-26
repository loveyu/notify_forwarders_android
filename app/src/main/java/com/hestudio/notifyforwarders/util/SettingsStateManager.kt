package com.hestudio.notifyforwarders.util

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * 全局设置状态管理器
 * 用于在设置变化时立即更新UI
 */
object SettingsStateManager {
    
    // 通知列表图标显示状态
    private var _notificationListIconEnabled = mutableStateOf(true)
    val notificationListIconEnabled: Boolean by _notificationListIconEnabled
    
    /**
     * 初始化设置状态
     */
    fun initialize(context: Context) {
        _notificationListIconEnabled.value = ServerPreferences.isNotificationListIconEnabled(context)
    }
    
    /**
     * 更新通知列表图标显示状态
     */
    fun updateNotificationListIconEnabled(context: Context, enabled: Boolean) {
        ServerPreferences.saveNotificationListIconEnabled(context, enabled)
        _notificationListIconEnabled.value = enabled
    }
    
    /**
     * 获取通知列表图标显示状态的可观察状态
     */
    fun getNotificationListIconEnabledState() = _notificationListIconEnabled
}

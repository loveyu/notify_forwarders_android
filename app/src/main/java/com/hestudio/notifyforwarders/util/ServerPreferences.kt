package com.hestudio.notifyforwarders.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object ServerPreferences {
    private const val PREF_NAME = "server_preferences"
    private const val KEY_SERVER_ADDRESS = "server_address"
    private const val KEY_NOTIFICATION_LIMIT = "notification_limit"
    private const val KEY_NOTIFICATION_RECEIVE_ENABLED = "notification_receive_enabled"
    private const val KEY_NOTIFICATION_FORWARD_ENABLED = "notification_forward_enabled"
    private const val KEY_SELECTED_LANGUAGE = "selected_language"
    private const val KEY_NOTIFICATION_ICON_ENABLED = "notification_icon_enabled"
    private const val KEY_ICON_CORNER_RADIUS = "icon_corner_radius"
    private const val DEFAULT_PORT = 19283
    private const val MIN_NOTIFICATION_LIMIT = 1
    private const val DEFAULT_NOTIFICATION_LIMIT = 200
    private const val MAX_NOTIFICATION_LIMIT = 10000
    private const val MIN_ICON_CORNER_RADIUS = 5
    private const val DEFAULT_ICON_CORNER_RADIUS = 10
    private const val MAX_ICON_CORNER_RADIUS = 50
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveServerAddress(context: Context, address: String) {
        getPreferences(context).edit() {
            putString(KEY_SERVER_ADDRESS, formatServerAddress(address))
        }
    }
    
    fun getServerAddress(context: Context): String {
        return getPreferences(context).getString(KEY_SERVER_ADDRESS, "") ?: ""
    }
    
    // 确保服务器地址格式正确，如果没有指定端口则添加默认端口
    fun formatServerAddress(address: String): String {
        if (address.isBlank()) return ""
        
        // 如果地址已经包含端口号（例如 192.168.1.1:8080），则直接返回
        if (address.contains(":")) {
            return address.trim()
        }
        
        // 否则添加默认端口号
        return "${address.trim()}:$DEFAULT_PORT"
    }
    
    // 判断是否配置了有效的服务器地址
    fun hasValidServerConfig(context: Context): Boolean {
        val address = getServerAddress(context)
        return address.isNotBlank()
    }

    // 保存通知数量限制
    fun saveNotificationLimit(context: Context, limit: Int) {
        val validLimit = limit.coerceIn(MIN_NOTIFICATION_LIMIT, MAX_NOTIFICATION_LIMIT)
        getPreferences(context).edit() {
            putInt(KEY_NOTIFICATION_LIMIT, validLimit)
        }
    }

    // 获取通知数量限制
    fun getNotificationLimit(context: Context): Int {
        return getPreferences(context).getInt(KEY_NOTIFICATION_LIMIT, DEFAULT_NOTIFICATION_LIMIT)
    }

    // 获取最小通知数量限制
    fun getMinNotificationLimit(): Int {
        return MIN_NOTIFICATION_LIMIT
    }

    // 获取默认通知数量限制
    fun getDefaultNotificationLimit(): Int {
        return DEFAULT_NOTIFICATION_LIMIT
    }

    // 获取最大通知数量限制
    fun getMaxNotificationLimit(): Int {
        return MAX_NOTIFICATION_LIMIT
    }

    // 保存通知接收开关状态
    fun saveNotificationReceiveEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit() {
            putBoolean(KEY_NOTIFICATION_RECEIVE_ENABLED, enabled)
        }
    }

    // 获取通知接收开关状态，默认为true
    fun isNotificationReceiveEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_NOTIFICATION_RECEIVE_ENABLED, true)
    }

    // 保存通知转发开关状态
    fun saveNotificationForwardEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit() {
            putBoolean(KEY_NOTIFICATION_FORWARD_ENABLED, enabled)
        }
    }

    // 获取通知转发开关状态，如果配置了服务器地址默认为true，否则为false
    fun isNotificationForwardEnabled(context: Context): Boolean {
        val hasServerConfig = hasValidServerConfig(context)
        return getPreferences(context).getBoolean(KEY_NOTIFICATION_FORWARD_ENABLED, hasServerConfig)
    }

    // 检查是否可以开启通知转发（需要配置服务器地址）
    fun canEnableNotificationForward(context: Context): Boolean {
        return hasValidServerConfig(context)
    }

    // 保存选择的语言
    fun saveSelectedLanguage(context: Context, languageCode: String) {
        getPreferences(context).edit() {
            putString(KEY_SELECTED_LANGUAGE, languageCode)
        }
    }

    // 获取选择的语言，默认为系统语言
    fun getSelectedLanguage(context: Context): String {
        return getPreferences(context).getString(KEY_SELECTED_LANGUAGE, "system") ?: "system"
    }

    // 保存通知图标开关状态
    fun saveNotificationIconEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit() {
            putBoolean(KEY_NOTIFICATION_ICON_ENABLED, enabled)
        }
    }

    // 获取通知图标开关状态，默认为false
    fun isNotificationIconEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_NOTIFICATION_ICON_ENABLED, false)
    }

    // 保存图标圆角半径
    fun saveIconCornerRadius(context: Context, radius: Int) {
        val validRadius = radius.coerceIn(MIN_ICON_CORNER_RADIUS, MAX_ICON_CORNER_RADIUS)
        getPreferences(context).edit() {
            putInt(KEY_ICON_CORNER_RADIUS, validRadius)
        }
    }

    // 获取图标圆角半径，默认为10%
    fun getIconCornerRadius(context: Context): Int {
        return getPreferences(context).getInt(KEY_ICON_CORNER_RADIUS, DEFAULT_ICON_CORNER_RADIUS)
    }

    // 获取最小图标圆角半径
    fun getMinIconCornerRadius(): Int {
        return MIN_ICON_CORNER_RADIUS
    }

    // 获取默认图标圆角半径
    fun getDefaultIconCornerRadius(): Int {
        return DEFAULT_ICON_CORNER_RADIUS
    }

    // 获取最大图标圆角半径
    fun getMaxIconCornerRadius(): Int {
        return MAX_ICON_CORNER_RADIUS
    }
}
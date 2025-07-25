package com.hestudio.notifyforwarders.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object ServerPreferences {
    private const val PREF_NAME = "server_preferences"
    private const val KEY_SERVER_ADDRESS = "server_address"
    private const val KEY_NOTIFICATION_LIMIT = "notification_limit"
    private const val DEFAULT_PORT = 19283
    private const val MIN_NOTIFICATION_LIMIT = 1
    private const val DEFAULT_NOTIFICATION_LIMIT = 200
    private const val MAX_NOTIFICATION_LIMIT = 10000
    
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
}
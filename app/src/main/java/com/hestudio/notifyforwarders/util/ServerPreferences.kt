package com.hestudio.notifyforwarders.util

import android.content.Context
import android.content.SharedPreferences

object ServerPreferences {
    private const val PREF_NAME = "server_preferences"
    private const val KEY_SERVER_ADDRESS = "server_address"
    private const val DEFAULT_PORT = 19283
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveServerAddress(context: Context, address: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_SERVER_ADDRESS, formatServerAddress(address))
        editor.apply()
    }
    
    fun getServerAddress(context: Context): String {
        return getPreferences(context).getString(KEY_SERVER_ADDRESS, "") ?: ""
    }
    
    // 确保服务器地址格式正确，如果没有指定端口则添加默认端口
    private fun formatServerAddress(address: String): String {
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
}
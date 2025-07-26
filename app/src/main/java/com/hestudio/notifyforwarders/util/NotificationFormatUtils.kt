package com.hestudio.notifyforwarders.util

import com.hestudio.notifyforwarders.service.NotificationData
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * 通知格式化工具类
 * 提供通知数据的格式化功能
 */
object NotificationFormatUtils {

    /**
     * 移除字符串的前后空白符
     * @param text 原始文本
     * @return 移除空白符后的文本
     */
    fun trimWhitespace(text: String?): String {
        return text?.trim() ?: ""
    }

    /**
     * 检查文本是否为空（包括只有空白符的情况）
     * @param text 要检查的文本
     * @return 是否为空
     */
    fun isTextEmpty(text: String?): Boolean {
        return text.isNullOrBlank()
    }

    /**
     * 将通知数据转换为JSON字符串
     * @param notification 通知数据
     * @return JSON字符串
     */
    fun toJsonString(notification: NotificationData): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timeString = dateFormat.format(Date(notification.time))
        
        return JSONObject().apply {
            put("id", notification.id)
            put("packageName", notification.packageName)
            put("appName", notification.appName)
            put("title", notification.title)
            put("content", notification.content)
            put("time", notification.time)
            put("timeFormatted", timeString)
            put("uniqueId", notification.uniqueId)
            
            // 只有在图标数据存在时才添加
            notification.iconMd5?.let { iconMd5 ->
                put("iconMd5", iconMd5)
            }
            notification.iconBase64?.let { iconBase64 ->
                put("iconBase64", iconBase64)
            }
        }.toString(2) // 使用缩进格式化JSON
    }

    /**
     * 将通知数据转换为纯文本格式
     * @param notification 通知数据
     * @return 格式化的纯文本
     */
    fun toPlainText(notification: NotificationData): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timeString = dateFormat.format(Date(notification.time))
        
        val builder = StringBuilder()
        
        // 应用名称
        builder.append("应用: ${notification.appName}\n")
        
        // 时间
        builder.append("时间: $timeString\n")
        
        // 标题（如果不为空）
        val trimmedTitle = trimWhitespace(notification.title)
        if (trimmedTitle.isNotEmpty()) {
            builder.append("标题: $trimmedTitle\n")
        }
        
        // 内容（如果不为空）
        val trimmedContent = trimWhitespace(notification.content)
        if (trimmedContent.isNotEmpty()) {
            builder.append("内容: $trimmedContent\n")
        }
        
        // 包名
        builder.append("包名: ${notification.packageName}")
        
        return builder.toString()
    }

    /**
     * 获取通知的简短描述（用于Toast等场景）
     * @param notification 通知数据
     * @return 简短描述
     */
    fun getShortDescription(notification: NotificationData): String {
        val trimmedTitle = trimWhitespace(notification.title)
        val trimmedContent = trimWhitespace(notification.content)
        
        return when {
            trimmedTitle.isNotEmpty() && trimmedContent.isNotEmpty() -> {
                "${notification.appName}: $trimmedTitle"
            }
            trimmedTitle.isNotEmpty() -> {
                "${notification.appName}: $trimmedTitle"
            }
            trimmedContent.isNotEmpty() -> {
                "${notification.appName}: $trimmedContent"
            }
            else -> {
                notification.appName
            }
        }
    }

    /**
     * 格式化时间字符串
     * @param timestamp 时间戳
     * @return 格式化的时间字符串
     */
    fun formatTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

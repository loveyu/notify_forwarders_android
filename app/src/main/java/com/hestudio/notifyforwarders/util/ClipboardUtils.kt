package com.hestudio.notifyforwarders.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.hestudio.notifyforwarders.R

/**
 * 剪贴板工具类
 * 提供复制文本到剪贴板的功能
 */
object ClipboardUtils {
    private const val TAG = "ClipboardUtils"

    /**
     * 复制文本到剪贴板
     * @param context 上下文
     * @param text 要复制的文本
     * @param label 剪贴板标签
     * @param showToast 是否显示Toast提示
     */
    fun copyToClipboard(
        context: Context,
        text: String,
        label: String = "NotifyForwarders",
        showToast: Boolean = true
    ) {
        try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(label, text)
            clipboardManager.setPrimaryClip(clipData)
            
            if (showToast) {
                Toast.makeText(
                    context,
                    context.getString(R.string.copied_to_clipboard),
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            Log.d(TAG, "文本已复制到剪贴板: $label")
        } catch (e: Exception) {
            Log.e(TAG, "复制到剪贴板失败", e)
            if (showToast) {
                Toast.makeText(
                    context,
                    context.getString(R.string.copy_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 复制通知图标到剪贴板
     * @param context 上下文
     * @param iconBase64 图标的Base64数据
     */
    fun copyNotificationIcon(context: Context, iconBase64: String?) {
        if (iconBase64.isNullOrBlank()) {
            Toast.makeText(
                context,
                context.getString(R.string.no_icon_to_copy),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        copyToClipboard(
            context = context,
            text = iconBase64,
            label = "Notification Icon",
            showToast = true
        )
    }

    /**
     * 复制通知JSON到剪贴板
     * @param context 上下文
     * @param jsonString 通知的JSON字符串
     */
    fun copyNotificationJson(context: Context, jsonString: String) {
        copyToClipboard(
            context = context,
            text = jsonString,
            label = "Notification JSON",
            showToast = true
        )
    }

    /**
     * 复制通知纯文本到剪贴板
     * @param context 上下文
     * @param plainText 通知的纯文本
     */
    fun copyNotificationText(context: Context, plainText: String) {
        copyToClipboard(
            context = context,
            text = plainText,
            label = "Notification Text",
            showToast = true
        )
    }
}

package com.hestudio.notifyforwarders.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Toast管理器，确保同时只显示一个Toast
 */
object ToastManager {
    private var currentToast: Toast? = null

    /**
     * 显示Toast消息，会取消之前的Toast
     */
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        CoroutineScope(Dispatchers.Main).launch {
            // 取消之前的Toast
            currentToast?.cancel()
            
            // 创建新的Toast
            currentToast = Toast.makeText(context, message, duration)
            currentToast?.show()
        }
    }

    /**
     * 取消当前显示的Toast
     */
    fun cancelCurrentToast() {
        currentToast?.cancel()
        currentToast = null
    }
}

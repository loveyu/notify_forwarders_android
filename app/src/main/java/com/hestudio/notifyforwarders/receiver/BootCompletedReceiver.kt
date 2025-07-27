package com.hestudio.notifyforwarders.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hestudio.notifyforwarders.service.JobSchedulerService
import com.hestudio.notifyforwarders.service.NotificationService

/**
 * 开机自启动广播接收器
 */
class BootCompletedReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootCompletedReceiver"
        // 定义华为和小米等设备可能使用的常量
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_HTC_QUICKBOOT = "com.htc.intent.action.QUICKBOOT_POWERON"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == ACTION_QUICKBOOT_POWERON ||
            intent.action == ACTION_HTC_QUICKBOOT ||
            intent.action == "android.intent.action.REBOOT") {
            
            Log.d(TAG, "收到开机广播，启动服务")
            
            // 启动通知监听服务
            try {
                val notificationServiceIntent = Intent(context, NotificationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(notificationServiceIntent)
                } else {
                    context.startService(notificationServiceIntent)
                }
                
                // 启动JobScheduler保活服务
                JobSchedulerService.scheduleJob(context)
                
                Log.d(TAG, "服务启动成功")
            } catch (e: Exception) {
                Log.e(TAG, "启动服务失败", e)
            }
        }
    }
}
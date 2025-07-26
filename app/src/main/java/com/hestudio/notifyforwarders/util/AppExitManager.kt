package com.hestudio.notifyforwarders.util

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hestudio.notifyforwarders.service.JobSchedulerService
import com.hestudio.notifyforwarders.service.NotificationActionService
import com.hestudio.notifyforwarders.service.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

/**
 * 应用退出管理器
 * 负责完全退出应用并停止所有后台服务
 */
object AppExitManager {
    private const val TAG = "AppExitManager"

    /**
     * 完全退出应用
     * 停止所有服务、清除通知、结束进程
     */
    fun exitApp(context: Context) {
        Log.d(TAG, "开始退出应用")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1. 停止所有服务
                stopAllServices(context)
                
                // 2. 取消所有通知
                clearAllNotifications(context)
                
                // 3. 取消JobScheduler任务
                cancelJobScheduler(context)
                
                // 4. 清理缓存
                clearAppCache(context)
                
                // 5. 等待一小段时间确保所有操作完成
                delay(1000)
                
                // 6. 结束应用进程
                finishApp(context)
                
            } catch (e: Exception) {
                Log.e(TAG, "退出应用时发生错误", e)
                // 即使出现错误也要尝试结束进程
                finishApp(context)
            }
        }
    }

    /**
     * 重启应用
     */
    fun restartApp(context: Context) {
        Log.d(TAG, "重启应用")
        
        try {
            // 获取应用的启动Intent
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
            
            // 延迟一下再退出当前进程
            CoroutineScope(Dispatchers.Main).launch {
                delay(500)
                exitProcess(0)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "重启应用失败", e)
            // 如果重启失败，直接退出
            exitProcess(0)
        }
    }

    /**
     * 停止所有服务
     */
    private fun stopAllServices(context: Context) {
        Log.d(TAG, "停止所有服务")
        
        try {
            // 停止通知监听服务
            val notificationServiceIntent = Intent(context, NotificationService::class.java)
            context.stopService(notificationServiceIntent)
            
            // 停止通知操作服务
            val actionServiceIntent = Intent(context, NotificationActionService::class.java)
            context.stopService(actionServiceIntent)
            
            Log.d(TAG, "所有服务已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止服务时发生错误", e)
        }
    }

    /**
     * 清除所有通知
     */
    private fun clearAllNotifications(context: Context) {
        Log.d(TAG, "清除所有通知")
        
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
            
            Log.d(TAG, "所有通知已清除")
        } catch (e: Exception) {
            Log.e(TAG, "清除通知时发生错误", e)
        }
    }

    /**
     * 取消JobScheduler任务
     */
    private fun cancelJobScheduler(context: Context) {
        Log.d(TAG, "取消JobScheduler任务")
        
        try {
            JobSchedulerService.cancelJob(context)
            Log.d(TAG, "JobScheduler任务已取消")
        } catch (e: Exception) {
            Log.e(TAG, "取消JobScheduler任务时发生错误", e)
        }
    }

    /**
     * 清理应用缓存
     */
    private fun clearAppCache(context: Context) {
        Log.d(TAG, "清理应用缓存")
        
        try {
            // 清理图标缓存
            IconCacheManager.clearAllCache(context)
            
            // 清理通知列表
            NotificationService.clearNotificationsAndCache(context)
            
            Log.d(TAG, "应用缓存已清理")
        } catch (e: Exception) {
            Log.e(TAG, "清理缓存时发生错误", e)
        }
    }

    /**
     * 结束应用
     */
    private fun finishApp(context: Context) {
        Log.d(TAG, "结束应用进程")
        
        try {
            // 尝试使用ActivityManager结束应用
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val appTasks = activityManager.appTasks
                for (task in appTasks) {
                    task.finishAndRemoveTask()
                }
            }
            
            // 最后结束进程
            exitProcess(0)
            
        } catch (e: Exception) {
            Log.e(TAG, "结束应用时发生错误", e)
            // 强制退出
            exitProcess(0)
        }
    }
}

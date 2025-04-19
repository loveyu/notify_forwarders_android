package com.hestudio.notifyforwarders.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hestudio.notifyforwarders.util.NotificationUtils

/**
 * JobScheduler保活服务，定期检查通知服务是否存活，如果不存在则重启
 */
class JobSchedulerService : JobService() {

    companion object {
        private const val TAG = "JobSchedulerService"
        private const val JOB_ID = 10086
        
        // 设置JobScheduler，间隔15分钟自动执行一次
        fun scheduleJob(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            
            // 构建JobInfo
            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, JobSchedulerService::class.java))
                .setPeriodic(15 * 60 * 1000)  // 15分钟执行一次
                .setPersisted(true)           // 设备重启后仍然有效
                .setRequiresCharging(false)   // 不需要充电状态
                .setRequiresBatteryNotLow(false)  // 不需要考虑电池电量
                .build()
            
            try {
                // 注册JobScheduler
                val result = jobScheduler.schedule(jobInfo)
                if (result == JobScheduler.RESULT_SUCCESS) {
                    Log.d(TAG, "Job scheduled successfully!")
                } else {
                    Log.e(TAG, "Job scheduling failed!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Job scheduling error: ${e.message}")
            }
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "JobScheduler服务执行，检查通知监听服务状态")

        // 检查通知服务是否正常运行
        if (!NotificationUtils.isNotificationListenerEnabled(this)) {
            // 如果通知服务未运行，尝试重新启用
            NotificationUtils.toggleNotificationListenerService(this)
            Log.d(TAG, "通知服务不可用，尝试重新启用")
        }

        // 无论如何都尝试启动服务
        try {
            val serviceIntent = Intent(this, NotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d(TAG, "通知服务启动成功")
        } catch (e: Exception) {
            Log.e(TAG, "启动服务失败: ${e.message}")
        }

        // 任务完成，返回false表示系统可以回收资源
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "JobScheduler服务被系统停止")
        // 返回true表示如果服务被系统杀死，需要重新调度
        return true
    }
}
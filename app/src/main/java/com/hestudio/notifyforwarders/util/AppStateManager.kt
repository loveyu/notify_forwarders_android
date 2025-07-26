package com.hestudio.notifyforwarders.util

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * 应用状态管理器
 * 用于跟踪应用的前后台状态
 */
object AppStateManager : DefaultLifecycleObserver {
    
    private const val TAG = "AppStateManager"
    
    @Volatile
    private var isAppInForeground = false
    
    @Volatile
    private var isInitialized = false
    
    /**
     * 初始化应用状态管理器
     * 应该在Application或MainActivity中调用
     */
    fun initialize() {
        if (!isInitialized) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            isInitialized = true
            Log.d(TAG, "AppStateManager initialized")
        }
    }
    
    /**
     * 检查应用是否在前台
     */
    fun isAppInForeground(): Boolean {
        return isAppInForeground
    }
    
    /**
     * 检查应用是否在后台
     */
    fun isAppInBackground(): Boolean {
        return !isAppInForeground
    }
    
    /**
     * 使用ActivityManager检查应用是否在前台
     * 作为备用方法，当lifecycle方法不可用时使用
     */
    fun isAppInForegroundByActivityManager(context: Context): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningAppProcesses = activityManager.runningAppProcesses
            
            if (runningAppProcesses != null) {
                for (processInfo in runningAppProcesses) {
                    if (processInfo.processName == context.packageName) {
                        return processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    }
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking app foreground state", e)
            false
        }
    }
    
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isAppInForeground = true
        Log.d(TAG, "App moved to foreground")
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isAppInForeground = false
        Log.d(TAG, "App moved to background")
    }
    
    /**
     * 获取应用状态描述（用于调试）
     */
    fun getStateDescription(): String {
        return if (isAppInForeground) "Foreground" else "Background"
    }
}

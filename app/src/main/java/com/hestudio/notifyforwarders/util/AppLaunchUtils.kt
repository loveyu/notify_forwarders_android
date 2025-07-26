package com.hestudio.notifyforwarders.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import com.hestudio.notifyforwarders.R

/**
 * 应用启动工具类
 * 提供启动其他应用的功能
 */
object AppLaunchUtils {
    private const val TAG = "AppLaunchUtils"

    /**
     * 启动指定包名的应用
     * @param context 上下文
     * @param packageName 要启动的应用包名
     * @param appName 应用名称（用于显示错误信息）
     */
    fun launchApp(context: Context, packageName: String, appName: String) {
        try {
            val packageManager = context.packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            
            if (launchIntent != null) {
                // 添加标志以确保应用正确启动
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                
                context.startActivity(launchIntent)
                Log.d(TAG, "成功启动应用: $packageName ($appName)")
                
                Toast.makeText(
                    context,
                    context.getString(R.string.launching_app, appName),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Log.w(TAG, "无法获取启动Intent: $packageName ($appName)")
                Toast.makeText(
                    context,
                    context.getString(R.string.app_not_found, appName),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "权限不足，无法启动应用: $packageName ($appName)", e)
            Toast.makeText(
                context,
                context.getString(R.string.no_permission_to_launch, appName),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "启动应用失败: $packageName ($appName)", e)
            Toast.makeText(
                context,
                context.getString(R.string.launch_app_failed, appName),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 检查应用是否已安装
     * @param context 上下文
     * @param packageName 包名
     * @return 是否已安装
     */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 获取应用的启动Intent
     * @param context 上下文
     * @param packageName 包名
     * @return 启动Intent，如果应用不存在则返回null
     */
    fun getLaunchIntent(context: Context, packageName: String): Intent? {
        return try {
            context.packageManager.getLaunchIntentForPackage(packageName)
        } catch (e: Exception) {
            Log.e(TAG, "获取启动Intent失败: $packageName", e)
            null
        }
    }
}

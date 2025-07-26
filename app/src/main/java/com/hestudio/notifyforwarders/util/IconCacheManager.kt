package com.hestudio.notifyforwarders.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * 图标缓存管理器
 * 负责应用图标的获取、缓存、MD5计算和清理
 */
object IconCacheManager {
    private const val TAG = "IconCacheManager"
    private const val CACHE_DIR_NAME = "icon_cache"
    private const val ICON_PUSH_CACHE_DIR_NAME = "icon_push_cache"
    private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 1天
    private const val CLEANUP_INTERVAL_MS = 2 * 60 * 60 * 1000L // 2小时
    private const val ICON_PUSH_INTERVAL_MS = 10 * 60 * 1000L // 10分钟
    private const val MAX_ICON_SIZE = 120 // 最大图标尺寸120px
    
    // 内存缓存：包名 -> IconCacheData
    private val memoryCache = ConcurrentHashMap<String, IconCacheData>()
    
    // 图标推送记录缓存：MD5 -> 推送时间戳
    private val iconPushCache = ConcurrentHashMap<String, Long>()
    
    private var lastCleanupTime = 0L
    private val cleanupScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * 图标缓存数据
     */
    data class IconCacheData(
        val packageName: String,
        val appName: String,
        val iconMd5: String,
        val iconBase64: String,
        val cacheTime: Long
    )
    
    /**
     * 获取应用图标信息
     * @param context 上下文
     * @param packageName 包名
     * @param appName 应用名称
     * @return 图标缓存数据，如果获取失败返回null
     */
    fun getIconData(context: Context, packageName: String, appName: String): IconCacheData? {
        try {
            // 先检查内存缓存
            val cached = memoryCache[packageName]
            if (cached != null && !isCacheExpired(cached.cacheTime)) {
                Log.d(TAG, "从内存缓存获取图标: $packageName")
                return cached
            }
            
            // 尝试从磁盘缓存加载
            val diskCached = loadFromDiskCache(context, packageName)
            if (diskCached != null && !isCacheExpired(diskCached.cacheTime)) {
                Log.d(TAG, "从磁盘缓存获取图标: $packageName")
                memoryCache[packageName] = diskCached
                return diskCached
            }
            
            // 获取新的图标
            val iconData = generateIconData(context, packageName, appName)
            if (iconData != null) {
                // 保存到缓存
                memoryCache[packageName] = iconData
                saveToDiskCache(context, iconData)
                Log.d(TAG, "生成新图标缓存: $packageName")
            }
            
            return iconData
        } catch (e: Exception) {
            Log.e(TAG, "获取图标数据失败: $packageName", e)
            return null
        }
    }
    
    /**
     * 检查是否可以推送图标（10分钟限制）
     */
    fun canPushIcon(iconMd5: String): Boolean {
        val lastPushTime = iconPushCache[iconMd5] ?: 0L
        val currentTime = System.currentTimeMillis()
        return currentTime - lastPushTime > ICON_PUSH_INTERVAL_MS
    }
    
    /**
     * 记录图标推送时间
     */
    fun recordIconPush(iconMd5: String) {
        iconPushCache[iconMd5] = System.currentTimeMillis()
    }
    
    /**
     * 清理过期缓存
     */
    fun cleanupExpiredCache(context: Context) {
        val currentTime = System.currentTimeMillis()
        
        // 检查是否需要清理（每2小时清理一次）
        if (currentTime - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return
        }
        
        cleanupScope.launch {
            try {
                Log.d(TAG, "开始清理过期缓存")
                
                // 清理内存缓存
                val expiredKeys = memoryCache.entries.filter { 
                    isCacheExpired(it.value.cacheTime) 
                }.map { it.key }
                
                expiredKeys.forEach { key ->
                    memoryCache.remove(key)
                }
                
                // 清理磁盘缓存
                val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
                if (cacheDir.exists()) {
                    cacheDir.listFiles()?.forEach { file ->
                        if (currentTime - file.lastModified() > CACHE_DURATION_MS) {
                            file.delete()
                            Log.d(TAG, "删除过期缓存文件: ${file.name}")
                        }
                    }
                }
                
                // 清理图标推送记录缓存
                val expiredPushKeys = iconPushCache.entries.filter {
                    currentTime - it.value > ICON_PUSH_INTERVAL_MS
                }.map { it.key }
                
                expiredPushKeys.forEach { key ->
                    iconPushCache.remove(key)
                }
                
                lastCleanupTime = currentTime
                Log.d(TAG, "缓存清理完成，清理了 ${expiredKeys.size} 个内存缓存，${expiredPushKeys.size} 个推送记录")
                
            } catch (e: Exception) {
                Log.e(TAG, "清理缓存失败", e)
            }
        }
    }
    
    /**
     * 清空所有缓存
     */
    fun clearAllCache(context: Context) {
        cleanupScope.launch {
            try {
                Log.d(TAG, "清空所有缓存")
                
                // 清空内存缓存
                memoryCache.clear()
                iconPushCache.clear()
                
                // 清空磁盘缓存
                val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
                
                val pushCacheDir = File(context.cacheDir, ICON_PUSH_CACHE_DIR_NAME)
                if (pushCacheDir.exists()) {
                    pushCacheDir.deleteRecursively()
                }
                
                Log.d(TAG, "所有缓存已清空")
            } catch (e: Exception) {
                Log.e(TAG, "清空缓存失败", e)
            }
        }
    }
    
    /**
     * 生成图标数据
     */
    private fun generateIconData(context: Context, packageName: String, appName: String): IconCacheData? {
        try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val drawable = packageManager.getApplicationIcon(applicationInfo)
            
            // 转换为Bitmap并调整尺寸
            val bitmap = drawableToBitmap(drawable, MAX_ICON_SIZE)

            // 应用圆角效果
            val cornerRadius = ServerPreferences.getIconCornerRadius(context)
            val roundedBitmap = applyRoundedCorners(bitmap, cornerRadius)

            // 转换为Base64
            val iconBase64 = bitmapToBase64(roundedBitmap)
            
            // 计算MD5
            val iconMd5 = calculateMd5(iconBase64)
            
            return IconCacheData(
                packageName = packageName,
                appName = appName,
                iconMd5 = iconMd5,
                iconBase64 = iconBase64,
                cacheTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "生成图标数据失败: $packageName", e)
            return null
        }
    }
    
    /**
     * Drawable转Bitmap
     */
    private fun drawableToBitmap(drawable: Drawable, maxSize: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val originalBitmap = drawable.bitmap
            return resizeBitmap(originalBitmap, maxSize)
        }
        
        val intrinsicWidth = drawable.intrinsicWidth
        val intrinsicHeight = drawable.intrinsicHeight
        
        val width = if (intrinsicWidth > 0) minOf(intrinsicWidth, maxSize) else maxSize
        val height = if (intrinsicHeight > 0) minOf(intrinsicHeight, maxSize) else maxSize
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        
        return bitmap
    }
    
    /**
     * 调整Bitmap尺寸
     */
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 应用圆角效果
     * @param bitmap 原始图片
     * @param cornerRadiusPercent 圆角百分比 (0-50)，0为方形图标
     * @return 应用圆角后的图片
     */
    private fun applyRoundedCorners(bitmap: Bitmap, cornerRadiusPercent: Int): Bitmap {
        // 如果圆角为0，直接返回原图
        if (cornerRadiusPercent == 0) {
            return bitmap
        }

        val width = bitmap.width
        val height = bitmap.height
        val size = minOf(width, height)

        // 计算圆角半径，基于图片最小边的百分比
        val cornerRadius = (size * cornerRadiusPercent / 100f)

        // 创建输出bitmap
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 创建画笔
        val paint = Paint().apply {
            isAntiAlias = true
            color = 0xff424242.toInt()
        }

        // 创建圆角矩形路径
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        // 设置混合模式为SRC_IN，只保留重叠部分
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        // 绘制原始图片
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return output
    }
    
    /**
     * Bitmap转Base64
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
    
    /**
     * 计算MD5
     */
    fun calculateMd5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 检查缓存是否过期
     */
    private fun isCacheExpired(cacheTime: Long): Boolean {
        return System.currentTimeMillis() - cacheTime > CACHE_DURATION_MS
    }
    
    /**
     * 从磁盘缓存加载
     */
    private fun loadFromDiskCache(context: Context, packageName: String): IconCacheData? {
        // 简化实现，实际项目中可以使用更复杂的序列化方案
        return null
    }
    
    /**
     * 保存到磁盘缓存
     */
    private fun saveToDiskCache(context: Context, iconData: IconCacheData) {
        // 简化实现，实际项目中可以使用更复杂的序列化方案
    }
}

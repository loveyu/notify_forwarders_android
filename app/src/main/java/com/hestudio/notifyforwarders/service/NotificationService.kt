package com.hestudio.notifyforwarders.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import com.hestudio.notifyforwarders.ClipboardFloatingActivity
import com.hestudio.notifyforwarders.MainActivity
import com.hestudio.notifyforwarders.R
import com.hestudio.notifyforwarders.constants.ApiConstants
import com.hestudio.notifyforwarders.util.IconCacheManager
import com.hestudio.notifyforwarders.util.ModernNotificationUtils
import com.hestudio.notifyforwarders.util.PersistentNotificationManager
import com.hestudio.notifyforwarders.util.ServerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class NotificationService : NotificationListenerService() {
    
    companion object {
        const val TAG = "NotificationService"
        private const val FOREGROUND_SERVICE_ID = 1000  // 使用统一的通知ID
        private const val CHANNEL_ID = "notifyforwarders_service_channel"

        // 存储收到的通知，使用可观察的列表以便UI自动更新
        private val notifications = mutableStateListOf<NotificationData>()

        // 存储服务实例引用，用于外部调用
        private var serviceInstance: NotificationService? = null

        // 获取所有通知
        fun getNotifications(): List<NotificationData> {
            return notifications
        }

        // 清除通知列表
        fun clearNotifications() {
            notifications.clear()
        }

        // 清除通知列表和图标缓存
        fun clearNotificationsAndCache(context: Context) {
            notifications.clear()
            IconCacheManager.clearAllCache(context)
        }

        // 获取当前通知数量
        fun getNotificationCount(): Int {
            return notifications.size
        }

        // 刷新前台通知（用于持久化通知设置变更时）
        fun refreshForegroundNotification(context: Context) {
            serviceInstance?.updateForegroundNotification()
        }

        // 更新通知状态（替代PersistentNotificationManager的功能）
        fun updateNotificationState(context: Context, state: PersistentNotificationManager.SendingState) {
            serviceInstance?.updateNotificationState(state)
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // 当前通知状态
    private var currentNotificationState = PersistentNotificationManager.SendingState.IDLE

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationService onCreate")

        // 保存服务实例引用
        serviceInstance = this

        // 启动时清空图标缓存
        IconCacheManager.clearAllCache(this)

        // 立即启动前台服务通知，避免ANR
        startUnifiedForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "NotificationService onStartCommand")

        // 确保前台服务通知正在运行
        startUnifiedForegroundNotification()

        // 如果服务被系统杀死后重新创建，则返回START_STICKY以保持服务运行
        return START_STICKY
    }

    /**
     * 更新通知状态
     */
    fun updateNotificationState(state: PersistentNotificationManager.SendingState) {
        currentNotificationState = state
        updateUnifiedForegroundNotification()
    }

    /**
     * 启动统一的前台服务通知
     * 合并了原来的前台服务通知和持久化通知功能
     */
    private fun startUnifiedForegroundNotification() {
        try {
            createNotificationChannel()

            val notification = buildUnifiedNotification()
            startForeground(FOREGROUND_SERVICE_ID, notification)

            Log.d(TAG, "统一前台服务通知已启动")
        } catch (e: Exception) {
            Log.e(TAG, "启动统一前台服务通知失败", e)
        }
    }

    /**
     * 更新统一的前台服务通知
     */
    private fun updateUnifiedForegroundNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = buildUnifiedNotification()
            notificationManager.notify(FOREGROUND_SERVICE_ID, notification)

            Log.d(TAG, "统一前台服务通知已更新，状态: $currentNotificationState")
        } catch (e: Exception) {
            Log.e(TAG, "更新统一前台服务通知失败", e)
        }
    }

    /**
     * 构建统一的通知
     * 根据持久化通知设置和当前状态动态构建通知内容
     */
    private fun buildUnifiedNotification(): Notification {
        // 创建启动应用的PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val isPersistentEnabled = ServerPreferences.isPersistentNotificationEnabled(this)

        // 根据持久化通知设置决定通知内容
        val (title, text) = if (isPersistentEnabled) {
            // 持久化通知开启时，显示状态相关内容
            getString(R.string.app_name) to getNotificationText()
        } else {
            // 持久化通知关闭时，显示基本前台服务内容
            getString(R.string.foreground_service_title) to getString(R.string.foreground_service_text)
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 设置为不可清除

        // 如果持久化通知开启，添加操作按钮
        if (isPersistentEnabled) {
            addActionButtons(notificationBuilder)
        }

        return notificationBuilder.build()
    }

    /**
     * 获取通知文本
     */
    private fun getNotificationText(): String {
        return when (currentNotificationState) {
            PersistentNotificationManager.SendingState.SENDING_CLIPBOARD -> getString(R.string.sending_clipboard)
            PersistentNotificationManager.SendingState.SENDING_IMAGE -> getString(R.string.sending_image)
            PersistentNotificationManager.SendingState.IDLE -> getStatusNotificationText()
        }
    }

    /**
     * 获取状态通知文本，包含接收和转发状态
     */
    private fun getStatusNotificationText(): String {
        // 检查接收状态 - 基于通知监听权限和接收开关
        val hasNotificationPermission = com.hestudio.notifyforwarders.util.NotificationUtils.isNotificationListenerEnabled(this)
        val isReceiveEnabled = ServerPreferences.isNotificationReceiveEnabled(this)
        val receivingStatus = if (hasNotificationPermission && isReceiveEnabled) {
            getString(R.string.notification_status_receiving_enabled)
        } else {
            getString(R.string.notification_status_receiving_disabled)
        }

        // 检查转发状态 - 基于服务器配置和转发开关
        val hasServerConfig = ServerPreferences.hasValidServerConfig(this)
        val isForwardEnabled = ServerPreferences.isNotificationForwardEnabled(this)
        val forwardingStatus = when {
            !hasServerConfig -> getString(R.string.notification_status_forwarding_not_configured)
            isForwardEnabled -> getString(R.string.notification_status_forwarding_enabled)
            else -> getString(R.string.notification_status_forwarding_disabled)
        }

        // 组合状态文本
        return getString(R.string.persistent_notification_status_format, receivingStatus, forwardingStatus)
    }

    /**
     * 添加操作按钮
     */
    private fun addActionButtons(notificationBuilder: NotificationCompat.Builder) {
        // 创建剪贴板操作的PendingIntent - 启动ClipboardFloatingActivity
        val clipboardIntent = Intent(this, ClipboardFloatingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                   Intent.FLAG_ACTIVITY_CLEAR_TOP or
                   Intent.FLAG_ACTIVITY_SINGLE_TOP or
                   Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        val clipboardPendingIntent = PendingIntent.getActivity(
            this,
            1,
            clipboardIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 创建图片操作的PendingIntent
        val imageIntent = Intent(this, NotificationActionService::class.java).apply {
            action = NotificationActionService.ACTION_SEND_IMAGE
        }
        val imagePendingIntent = PendingIntent.getService(
            this,
            2,
            imageIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 按钮文本保持不变，不显示发送状态
        val clipboardButtonText = getString(R.string.action_send_clipboard)
        val imageButtonText = getString(R.string.action_send_image)

        notificationBuilder
            .addAction(
                R.drawable.ic_launcher_foreground,
                clipboardButtonText,
                clipboardPendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                imageButtonText,
                imagePendingIntent
            )
    }

    /**
     * 创建通知渠道（使用现代化API）
     */
    private fun createNotificationChannel() {
        // 使用现代化通知工具创建渠道
        ModernNotificationUtils.createNotificationChannel(
            context = this,
            channelId = CHANNEL_ID,
            channelName = getString(R.string.foreground_service_channel),
            importance = NotificationManager.IMPORTANCE_LOW,
            description = getString(R.string.foreground_service_channel_desc)
        )

        // 清理不再使用的历史通知渠道
        cleanupLegacyNotificationChannels()
    }

    /**
     * 清理不再使用的历史通知渠道（使用现代化API）
     */
    private fun cleanupLegacyNotificationChannels() {
        // 定义已弃用的通知渠道列表
        val deprecatedChannels = listOf(
            "clipboard_access_channel",
            "pinned_notification_channel", // 清理可能存在的已弃用的固定通知渠道
            "legacy_notification_channel"  // 清理其他可能的遗留渠道
        )

        // 使用现代化工具清理已弃用的渠道
        ModernNotificationUtils.cleanupDeprecatedChannels(this, deprecatedChannels)
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 检查通知接收开关
        if (!ServerPreferences.isNotificationReceiveEnabled(this)) {
            Log.d(TAG, "通知接收已关闭，忽略通知")
            return
        }

        val notification = sbn.notification
        val extras = notification.extras

        val packageName = sbn.packageName
        val appName = getApplicationName(packageName)
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val time = sbn.postTime

        Log.d(TAG, "通知已接收: $packageName, $appName, $title, $text")

        // 过滤掉没有内容的通知
        if (text.isBlank() && title.isBlank()) {
            Log.d(TAG, "过滤空内容通知: $packageName")
            return
        }
        
        // 生成唯一标识符
        // 使用包名+通知ID作为唯一标识
        val uniqueId = "$packageName:${sbn.id}"

        // 获取图标信息（如果开启了图标转发或列表显示功能）
        var iconMd5: String? = null
        var iconBase64: String? = null
        if (ServerPreferences.shouldProcessIcons(this)) {
            // 优先使用通知中的图标
            val notificationIcon = getNotificationIcon(notification)
            if (notificationIcon != null) {
                // 先计算原始图标的MD5值
                val originalIconBase64 = IconCacheManager.bitmapToBase64(notificationIcon)
                iconMd5 = IconCacheManager.calculateMd5(originalIconBase64)

                // 然后应用圆角效果
                val cornerRadius = ServerPreferences.getIconCornerRadius(this)
                val roundedIcon = applyRoundedCorners(notificationIcon, cornerRadius)
                iconBase64 = IconCacheManager.bitmapToBase64(roundedIcon)
                Log.d(TAG, "使用通知图标: $packageName")
            } else {
                // 使用应用图标（缓存）
                val iconData = IconCacheManager.getIconData(this, packageName, appName)
                if (iconData != null) {
                    iconMd5 = iconData.iconMd5
                    iconBase64 = iconData.iconBase64
                    Log.d(TAG, "使用应用图标: $packageName")
                }
            }

            // 定期清理过期缓存
            IconCacheManager.cleanupExpiredCache(this)
        }

        val notificationData = NotificationData(
            id = sbn.id,
            packageName = packageName,
            appName = appName,
            title = title,
            content = text,
            time = time,
            uniqueId = uniqueId,
            iconMd5 = iconMd5,
            iconBase64 = iconBase64
        )

        // 检查是否是现有通知的更新
        val existingIndex = notifications.indexOfFirst { it.uniqueId == uniqueId }
        if (existingIndex != -1) {
            // 是更新，先移除原有通知，然后将新通知添加到列表头部
            Log.d(TAG, "更新现有通知并移动到顶部: $uniqueId")
            notifications.removeAt(existingIndex)
            notifications.add(0, notificationData)
        } else {
            // 是新通知，添加到列表头部
            Log.d(TAG, "添加新通知: $uniqueId")
            notifications.add(0, notificationData)
        }

        // 检查通知数量限制（无论是新通知还是更新通知都需要检查）
        val notificationLimit = ServerPreferences.getNotificationLimit(this@NotificationService)
        if (notifications.size > notificationLimit) {
            // 移除超出限制的旧通知
            val removeCount = notifications.size - notificationLimit
            repeat(removeCount) {
                notifications.removeLastOrNull()
            }
            Log.d(TAG, "移除了 $removeCount 条旧通知，当前通知数量: ${notifications.size}")
        }
        
        // 转发通知到配置的服务器
        forwardNotificationToServer(notificationData)
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 可以在这里处理通知被移除的情况
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NotificationService onDestroy")

        // 清除服务实例引用
        serviceInstance = null

        // 尝试重启服务
        val intent = Intent(this, NotificationService::class.java)
        startForegroundService(intent)
    }
    
    // 获取应用名称
    private fun getApplicationName(packageName: String): String {
        try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            return packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "无法获取应用名称: $packageName", e)
            return packageName // 如果获取失败，返回包名作为备选
        }
    }
    
    private fun getDeviceName(): String {
        return Build.MODEL // 获取设备型号作为设备名称
    }

    /**
     * 从通知中获取图标
     * 优先使用 largeIcon，如果没有则尝试从extras中获取大图标
     */
    private fun getNotificationIcon(notification: Notification): Bitmap? {
        try {
            // 第一优先级：使用 largeIcon
            notification.getLargeIcon()?.let { icon ->
                val drawable = icon.loadDrawable(this)
                drawable?.let { d ->
                    val bitmap = when (d) {
                        is BitmapDrawable -> d.bitmap
                        else -> {
                            // 转换其他类型的 Drawable 为 Bitmap
                            val bitmap = createBitmap(
                                d.intrinsicWidth.coerceAtLeast(1),
                                d.intrinsicHeight.coerceAtLeast(1)
                            )
                            val canvas = Canvas(bitmap)
                            d.setBounds(0, 0, canvas.width, canvas.height)
                            d.draw(canvas)
                            bitmap
                        }
                    }
                    Log.d(TAG, "成功获取通知largeIcon")
                    return bitmap
                }
            }

//            // 第二优先级：尝试从extras中获取EXTRA_LARGE_ICON
//            val extras = notification.extras
//            extras?.let { bundle ->
//                // 尝试获取 EXTRA_LARGE_ICON
//                try {
//                    val largeIconBitmap = bundle.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON)
//                    if (largeIconBitmap != null && !largeIconBitmap.isRecycled) {
//                        Log.d(TAG, "成功从extras获取EXTRA_LARGE_ICON")
//                        return largeIconBitmap
//                    }
//                } catch (e: Exception) {
//                    Log.w(TAG, "从extras获取EXTRA_LARGE_ICON失败: ${e.message}")
//                }
//
//                // 尝试获取 EXTRA_LARGE_ICON_BIG
//                try {
//                    val largeIconBigBitmap = bundle.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON + ".big")
//                    if (largeIconBigBitmap != null && !largeIconBigBitmap.isRecycled) {
//                        Log.d(TAG, "成功从extras获取EXTRA_LARGE_ICON_BIG")
//                        return largeIconBigBitmap
//                    }
//                } catch (e: Exception) {
//                    Log.w(TAG, "从extras获取EXTRA_LARGE_ICON_BIG失败: ${e.message}")
//                }
//
//                // 第三优先级：尝试获取 EXTRA_PICTURE（但要谨慎，这通常是BigPictureStyle的内容图片）
//                try {
//                    val pictureBitmap = bundle.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
//                    if (pictureBitmap != null && !pictureBitmap.isRecycled) {
//                        // 检查图片尺寸，如果太大可能不适合作为图标
//                        val maxIconSize = 512 // 最大图标尺寸
//                        if (pictureBitmap.width <= maxIconSize && pictureBitmap.height <= maxIconSize) {
//                            Log.d(TAG, "成功从extras获取EXTRA_PICTURE作为图标")
//                            return pictureBitmap
//                        } else {
//                            Log.d(TAG, "EXTRA_PICTURE尺寸过大(${pictureBitmap.width}x${pictureBitmap.height})，不适合作为图标")
//                        }
//                    }
//                } catch (e: Exception) {
//                    Log.w(TAG, "从extras获取EXTRA_PICTURE失败: ${e.message}")
//                }
//            }
//
//            Log.d(TAG, "未能从通知中获取任何图标")
        } catch (e: Exception) {
            Log.w(TAG, "获取通知图标失败: ${e.message}")
        }
        return null
    }

    /**
     * 应用圆角效果到通知图标
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
        val output = createBitmap(width, height)
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
    
    private fun forwardNotificationToServer(notification: NotificationData) {
        // 检查通知转发开关
        if (!ServerPreferences.isNotificationForwardEnabled(this)) {
            Log.d(TAG, "通知转发已关闭，不转发通知")
            return
        }

        val serverAddress = ServerPreferences.getServerAddress(this)

        if (serverAddress.isEmpty()) {
            Log.d(TAG, "服务器地址未配置，不转发通知")
            return
        }
        
        serviceScope.launch {
            try {
                val serverUrl = ApiConstants.buildApiUrl(serverAddress, ApiConstants.ENDPOINT_NOTIFY)
                Log.d(TAG, "正在转发通知到 $serverUrl")

                val url = URL(serverUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = ApiConstants.METHOD_POST
                connection.setRequestProperty("Content-Type", ApiConstants.CONTENT_TYPE_JSON)
                connection.doOutput = true
                connection.connectTimeout = ApiConstants.TIMEOUT_NOTIFY_CONNECT
                connection.readTimeout = ApiConstants.TIMEOUT_NOTIFY_READ

                // 创建JSON数据，使用指定的参数名
                val jsonBody = JSONObject().apply {
                    put(ApiConstants.FIELD_APP_NAME, notification.appName) // 改用appName而不是packageName
                    put(ApiConstants.FIELD_TITLE, notification.title)
                    put(ApiConstants.FIELD_DESCRIPTION, notification.content)
                    put(ApiConstants.FIELD_DEVICE_NAME, getDeviceName()) // 添加设备名称参数
                    put(ApiConstants.FIELD_UNIQUE_ID, notification.uniqueId) // 添加唯一标识字段
                    put(ApiConstants.FIELD_ID, notification.id) // 这个包的记录ID

                    // 如果有图标信息，直接添加到请求中
                    notification.iconMd5?.let { iconMd5 ->
                        put(ApiConstants.FIELD_ICON_MD5, iconMd5)
                        notification.iconBase64?.let { iconBase64 ->
                            put(ApiConstants.FIELD_ICON_BASE64, iconBase64)
                        }
                    }
                }

                // 发送JSON数据
                val outputStream = connection.outputStream
                val writer = OutputStreamWriter(outputStream, ApiConstants.CHARSET_UTF8)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()
                
                // 获取响应
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "通知转发成功")
                    // 图标已经随通知一起发送，不需要额外的异步推送
                } else {
                    Log.e(TAG, "通知转发失败: HTTP $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "通知转发失败", e)
            }
        }
    }



    /**
     * 更新前台通知（用于持久化通知设置变更时）
     */
    fun updateForegroundNotification() {
        try {
            // 无论持久化通知是否开启，都保持前台服务运行
            // 只是根据设置改变通知的内容和功能
            updateUnifiedForegroundNotification()
            Log.d(TAG, "前台通知已更新")
        } catch (e: Exception) {
            Log.e(TAG, "更新前台通知失败", e)
        }
    }
}

// 通知数据类
data class NotificationData(
    val id: Int,
    val packageName: String,
    val appName: String, // 添加应用名称字段
    val title: String,
    val content: String,
    val time: Long,
    val uniqueId: String, // 添加唯一标识符，用于识别同一通知的更新
    val iconMd5: String? = null, // 图标MD5值
    val iconBase64: String? = null // 图标Base64数据
)

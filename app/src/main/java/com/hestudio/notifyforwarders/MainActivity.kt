package com.hestudio.notifyforwarders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.hestudio.notifyforwarders.service.JobSchedulerService
import com.hestudio.notifyforwarders.service.NotificationData
import com.hestudio.notifyforwarders.service.NotificationService
import com.hestudio.notifyforwarders.ui.theme.NotifyForwardersTheme
import com.hestudio.notifyforwarders.util.NotificationUtils
import com.hestudio.notifyforwarders.util.ServerPreferences
import com.hestudio.notifyforwarders.util.LocaleHelper
import com.hestudio.notifyforwarders.util.IconCacheManager
import com.hestudio.notifyforwarders.util.PermissionUtils
import com.hestudio.notifyforwarders.util.SettingsStateManager
import com.hestudio.notifyforwarders.service.NotificationActionService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // 添加权限状态跟踪变量
    private val hasNotificationPermission = mutableStateOf(false)
    private val hasPostNotificationsPermission = mutableStateOf(false)

    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPostNotificationsPermission.value = isGranted
        // 重新设置界面内容以反映权限状态变化
        updateUI()
    }

    // 电池优化豁免请求启动器
    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, getString(R.string.battery_optimization_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.battery_optimization_warning), Toast.LENGTH_SHORT).show()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { context ->
            val languageCode = ServerPreferences.getSelectedLanguage(context)
            LocaleHelper.setLocale(context, languageCode)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化权限状态
        hasNotificationPermission.value = NotificationUtils.isNotificationListenerEnabled(this)
        hasPostNotificationsPermission.value = PermissionUtils.hasPostNotificationsPermission(this)

        // 初始化设置状态管理器
        SettingsStateManager.initialize(this)

        enableEdgeToEdge()
        updateUI()

        // 启动通知监听服务
        startNotificationService()

        // 请求忽略电池优化
        requestBatteryOptimizationExemption()

        // 设置JobScheduler定时任务，定期检查服务是否活跃
        JobSchedulerService.scheduleJob(this)
    }

    private fun updateUI() {
        setContent {
            NotifyForwardersTheme {
                NotificationScreen(
                    hasPermission = hasNotificationPermission.value,
                    hasPostNotificationsPermission = hasPostNotificationsPermission.value,
                    requestPermission = { NotificationUtils.openNotificationListenerSettings(this) },
                    requestPostNotificationsPermission = {
                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    },
                    navigateToSettings = {
                        startActivity(
                            Intent(
                                this,
                                SettingsActivity::class.java
                            )
                        )
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 重启通知监听服务以确保连接正常
        NotificationUtils.toggleNotificationListenerService(this)

        // 更新权限状态
        val currentPermissionState = NotificationUtils.isNotificationListenerEnabled(this)
        val currentPostNotificationsState = PermissionUtils.hasPostNotificationsPermission(this)

        if (hasNotificationPermission.value != currentPermissionState ||
            hasPostNotificationsPermission.value != currentPostNotificationsState) {
            hasNotificationPermission.value = currentPermissionState
            hasPostNotificationsPermission.value = currentPostNotificationsState

            // 如果权限状态改变，重新设置界面内容
            updateUI()
        }
    }



    // 启动通知监听服务
    private fun startNotificationService() {
        try {
            val serviceIntent = Intent(this, NotificationService::class.java)
            startForegroundService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.service_start_failed), Toast.LENGTH_SHORT).show()
        }
    }

    // 请求电池优化豁免
    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val packageName = packageName
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:$packageName")
                }
                batteryOptimizationLauncher.launch(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.battery_optimization_request_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    hasPermission: Boolean,
    hasPostNotificationsPermission: Boolean,
    requestPermission: () -> Unit,
    requestPostNotificationsPermission: () -> Unit,
    navigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    // 确认对话框状态
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    // 添加设置图标按钮
                    IconButton(onClick = navigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        },
        // 添加FAB用于清除历史记录
        floatingActionButton = {
            if (hasPermission && NotificationService.getNotifications().isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showClearConfirmDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.clear_notification_history)
                    )
                }
            }
        }
    ) { innerPadding ->
        if (hasPermission && hasPostNotificationsPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 快捷开关区域
                QuickToggleSection()

                // 通知列表
                NotificationList(
                    notifications = NotificationService.getNotifications(),
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 显示确认对话框
            if (showClearConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showClearConfirmDialog = false },
                    title = { Text(stringResource(R.string.confirm_clear_title)) },
                    text = { Text(stringResource(R.string.confirm_clear_message)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                NotificationService.clearNotificationsAndCache(context)
                                showClearConfirmDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.confirm_clear_button))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirmDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        } else {
            PermissionRequest(
                hasNotificationListenerPermission = hasPermission,
                hasPostNotificationsPermission = hasPostNotificationsPermission,
                requestNotificationListenerPermission = requestPermission,
                requestPostNotificationsPermission = requestPostNotificationsPermission,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun NotificationList(
    notifications: List<NotificationData>,
    modifier: Modifier = Modifier
) {
    // 使用全局状态管理器获取图标显示状态
    val showIcon by SettingsStateManager.getNotificationListIconEnabledState()

    if (notifications.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.no_notifications))
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notifications) { notification ->
                NotificationItem(notification = notification, showIcon = showIcon)
            }
        }
    }
}

@Composable
fun AppIcon(
    packageName: String,
    appName: String,
    iconBase64: String? = null,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var iconBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // 尝试获取图标，优先使用通知图标
    androidx.compose.runtime.LaunchedEffect(packageName, iconBase64) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val bitmap = if (!iconBase64.isNullOrEmpty()) {
                    // 优先使用通知图标数据
                    try {
                        val iconBytes = Base64.decode(iconBase64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.size)
                    } catch (e: Exception) {
                        Log.w("AppIcon", "Failed to decode notification icon, falling back to app icon", e)
                        null
                    }
                } else {
                    null
                }

                val finalBitmap = bitmap ?: run {
                    // 回退到应用图标，首先尝试从缓存获取
                    val iconData = IconCacheManager.getIconData(context, packageName, appName)
                    if (iconData != null) {
                        // 从Base64解码图标
                        val iconBytes = Base64.decode(iconData.iconBase64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.size)
                    } else {
                        // 如果缓存中没有，直接从PackageManager获取
                        val packageManager = context.packageManager
                        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                        val drawable = packageManager.getApplicationIcon(applicationInfo)

                        // 转换为Bitmap
                        val bitmap = android.graphics.Bitmap.createBitmap(
                            drawable.intrinsicWidth.coerceAtLeast(1),
                            drawable.intrinsicHeight.coerceAtLeast(1),
                            android.graphics.Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        bitmap
                    }
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    iconBitmap = finalBitmap
                }
            } catch (e: Exception) {
                Log.e("AppIcon", "Failed to load icon for $packageName", e)
                // 如果获取失败，iconBitmap保持为null，会显示默认图标
            }
        }
    }

    val cornerRadius = size * 0.05f // 5% 圆角

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        iconBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = appName,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius)),
                contentScale = ContentScale.Crop
            )
        } ?: run {
            // 显示默认图标
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = appName,
                modifier = Modifier.size(size * 0.6f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NotificationItem(notification: NotificationData, showIcon: Boolean = true) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val timeString = dateFormat.format(Date(notification.time))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 应用图标，根据设置决定是否显示
            if (showIcon) {
                AppIcon(
                    packageName = notification.packageName,
                    appName = notification.appName,
                    iconBase64 = notification.iconBase64,
                    size = 48.dp
                )
            }

            // 通知内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notification.appName,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PermissionRequest(
    hasNotificationListenerPermission: Boolean,
    hasPostNotificationsPermission: Boolean,
    requestNotificationListenerPermission: () -> Unit,
    requestPostNotificationsPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.notification_permission_title),
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.notification_permission_desc),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // POST_NOTIFICATIONS权限请求
        if (!hasPostNotificationsPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.post_notifications_permission_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.post_notifications_permission_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = requestPostNotificationsPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.grant_post_notifications_permission))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 通知监听权限请求
        if (!hasNotificationListenerPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notification_listener_permission_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.notification_listener_permission_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = requestNotificationListenerPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.grant_notification_listener_permission))
                    }
                }
            }
        }
    }
}

@Composable
fun QuickToggleSection() {
    val context = LocalContext.current

    // 状态变量
    var notificationReceiveEnabled by remember {
        mutableStateOf(ServerPreferences.isNotificationReceiveEnabled(context))
    }
    var notificationForwardEnabled by remember {
        mutableStateOf(ServerPreferences.isNotificationForwardEnabled(context))
    }
    var showServerAddressDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 通知接收开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.notification_receive_switch),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = notificationReceiveEnabled,
                    onCheckedChange = { enabled ->
                        notificationReceiveEnabled = enabled
                        ServerPreferences.saveNotificationReceiveEnabled(context, enabled)

                        // 显示状态提示
                        val message = if (enabled) {
                            context.getString(R.string.notification_receive_enabled)
                        } else {
                            context.getString(R.string.notification_receive_disabled)
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 通知转发开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.notification_forward_switch),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = notificationForwardEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !ServerPreferences.canEnableNotificationForward(context)) {
                            // 如果没有配置服务器地址，显示提示
                            Toast.makeText(
                                context,
                                context.getString(R.string.notification_forward_no_server),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            notificationForwardEnabled = enabled
                            ServerPreferences.saveNotificationForwardEnabled(context, enabled)

                            if (enabled) {
                                // 开启时显示当前服务器地址
                                showServerAddressDialog = true
                            } else {
                                // 关闭时显示状态提示
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.notification_forward_disabled),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }

            // 快捷发送标题
            Text(
                text = stringResource(R.string.quick_send_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // 快捷操作按钮区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 发送剪贴板按钮
                Button(
                    onClick = {
                        val serverAddress = ServerPreferences.getServerAddress(context)
                        if (serverAddress.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.server_not_configured), Toast.LENGTH_SHORT).show()
                        } else {
                            NotificationActionService.sendClipboard(context)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = stringResource(R.string.send_clipboard_button),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 发送图片按钮
                Button(
                    onClick = {
                        val serverAddress = ServerPreferences.getServerAddress(context)
                        if (serverAddress.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.server_not_configured), Toast.LENGTH_SHORT).show()
                        } else {
                            NotificationActionService.sendLatestImage(context)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = stringResource(R.string.send_image_button),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // 服务器地址提示对话框
    if (showServerAddressDialog) {
        val serverAddress = ServerPreferences.getServerAddress(context)
        AlertDialog(
            onDismissRequest = { showServerAddressDialog = false },
            title = { Text(stringResource(R.string.notification_forward_enabled)) },
            text = {
                Text(stringResource(R.string.current_server_address, serverAddress))
            },
            confirmButton = {
                TextButton(onClick = { showServerAddressDialog = false }) {
                    Text(stringResource(R.string.confirm))
                }
            }
        )
    }
}

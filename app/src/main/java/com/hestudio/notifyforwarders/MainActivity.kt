package com.hestudio.notifyforwarders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.hestudio.notifyforwarders.service.JobSchedulerService
import com.hestudio.notifyforwarders.service.NotificationData
import com.hestudio.notifyforwarders.service.NotificationService
import com.hestudio.notifyforwarders.ui.theme.NotifyForwardersTheme
import com.hestudio.notifyforwarders.util.NotificationUtils
import com.hestudio.notifyforwarders.util.ServerPreferences
import com.hestudio.notifyforwarders.util.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    // 添加权限状态跟踪变量
    private val hasNotificationPermission = mutableStateOf(false)
    private val REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 1001

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

        enableEdgeToEdge()
        setContent {
            NotifyForwardersTheme {
                // 使用状态变量而不是直接调用函数
                NotificationScreen(
                    hasPermission = hasNotificationPermission.value,
                    requestPermission = { NotificationUtils.openNotificationListenerSettings(this) },
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

        // 启动通知监听服务
        startNotificationService()

        // 请求忽略电池优化
        requestBatteryOptimizationExemption()

        // 设置JobScheduler定时任务，定期检查服务是否活跃
        JobSchedulerService.scheduleJob(this)
    }

    override fun onResume() {
        super.onResume()
        // 重启通知监听服务以确保连接正常
        NotificationUtils.toggleNotificationListenerService(this)

        // 更新权限状态
        val currentPermissionState = NotificationUtils.isNotificationListenerEnabled(this)
        if (hasNotificationPermission.value != currentPermissionState) {
            hasNotificationPermission.value = currentPermissionState

            // 如果权限状态改变，重新设置界面内容
            setContent {
                NotifyForwardersTheme {
                    NotificationScreen(
                        hasPermission = hasNotificationPermission.value,
                        requestPermission = {
                            NotificationUtils.openNotificationListenerSettings(
                                this
                            )
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
    }

    // 启动通知监听服务
    private fun startNotificationService() {
        try {
            val serviceIntent = Intent(this, NotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "启动服务失败", Toast.LENGTH_SHORT).show()
        }
    }

    // 请求电池优化豁免
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val packageName = packageName
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:$packageName")
                    }
                    startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "无法请求电池优化豁免", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, "已获取电池优化豁免权限", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "应用可能在后台被系统杀死", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    hasPermission: Boolean,
    requestPermission: () -> Unit,
    navigateToSettings: () -> Unit
) {
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
                        contentDescription = "清除通知历史"
                    )
                }
            }
        }
    ) { innerPadding ->
        if (hasPermission) {
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
                    title = { Text("确认清除") },
                    text = { Text("确定要清除所有通知历史记录吗？") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                NotificationService.clearNotifications()
                                showClearConfirmDialog = false
                            }
                        ) {
                            Text("确认清除")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirmDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        } else {
            PermissionRequest(
                requestPermission = requestPermission,
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
                NotificationItem(notification = notification)
            }
        }
    }
}

@Composable
fun NotificationItem(notification: NotificationData) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val timeString = dateFormat.format(Date(notification.time))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notification.appName,  // 修改为显示应用名称而非包名
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
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PermissionRequest(
    requestPermission: () -> Unit,
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

        Button(onClick = requestPermission) {
            Text(stringResource(R.string.grant_permission))
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 通知接收开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.notification_receive_switch),
                    style = MaterialTheme.typography.bodyLarge
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
                    style = MaterialTheme.typography.bodyLarge
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
                    Text("确定")
                }
            }
        )
    }
}

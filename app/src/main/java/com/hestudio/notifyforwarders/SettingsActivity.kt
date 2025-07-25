package com.hestudio.notifyforwarders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.hestudio.notifyforwarders.service.NotificationService
import com.hestudio.notifyforwarders.ui.theme.NotifyForwardersTheme
import com.hestudio.notifyforwarders.util.NotificationUtils
import com.hestudio.notifyforwarders.util.ServerPreferences
import com.hestudio.notifyforwarders.util.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class SettingsActivity : ComponentActivity() {
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val testChannelId = "test_notifications_channel"
    private val progressChannelId = "progress_notifications_channel"
    private val progressNotificationId = 1002

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { context ->
            val languageCode = ServerPreferences.getSelectedLanguage(context)
            LocaleHelper.setLocale(context, languageCode)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannels()
        enableEdgeToEdge()

        setContent {
            NotifyForwardersTheme {
                SettingsScreen(
                    onBackPressed = { finish() },
                    onOpenNotificationSettings = {
                        NotificationUtils.openNotificationListenerSettings(
                            this
                        )
                    },
                    onOpenBatteryOptimizationSettings = { openBatteryOptimizationSettings() },
                    onSendRandomNotification = { sendRandomNotification() },
                    onSendProgressNotification = { sendProgressNotification() }
                )
            }
        }
    }

    private fun openBatteryOptimizationSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "无法打开电池优化设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 创建测试通知渠道
            val testChannel = NotificationChannel(
                testChannelId,
                "测试通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "用于发送测试通知"
            }

            // 创建进度通知渠道
            val progressChannel = NotificationChannel(
                progressChannelId,
                "进度通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于显示进度更新的通知"
            }

            // 注册通知渠道
            notificationManager.createNotificationChannel(testChannel)
            notificationManager.createNotificationChannel(progressChannel)
        }
    }

    private fun sendRandomNotification() {
        val random = Random.Default
        val titles = listOf(
            "今日资讯", "系统提醒", "重要通知", "新消息", "活动提醒",
            "安全提醒", "更新通知", "账户提醒", "日程提醒", "天气预报"
        )
        val contents = listOf(
            "您有一条新消息需要查看", "系统更新已完成", "您的账户有新的活动",
            "今日天气：晴天，温度25°C", "您的订单已发货", "电池电量低于20%",
            "新版本已推送，请及时更新", "明天有一个重要会议",
            "您有一个新的朋友请求", "周末活动邀请：户外烧烤"
        )

        val title = "测试通知 - " + titles[random.nextInt(titles.size)]
        val content = contents[random.nextInt(contents.size)] + " - " +
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val notificationId = random.nextInt(10000)

        val builder = NotificationCompat.Builder(this, testChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
        Toast.makeText(this, "已发送测试通知", Toast.LENGTH_SHORT).show()
    }

    private fun sendProgressNotification() {
        // 创建一个通知构建器
        val builder = NotificationCompat.Builder(this, progressChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("进度通知测试")
            .setContentText("正在更新进度...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // 确保每次使用相同的通知ID，以便系统识别为同一通知的更新

        // 立即显示一个0%的通知
        builder.setProgress(100, 0, false)
        notificationManager.notify(progressNotificationId, builder.build())

        // 启动一个线程来更新进度
        Thread {
            for (progress in 0..10) {
                val currentProgress = progress * 10
                // 更新通知进度
                builder.setProgress(100, currentProgress, false)
                    .setContentText("当前进度: $currentProgress%")
                notificationManager.notify(progressNotificationId, builder.build())

                try {
                    // 每次更新间隔300毫秒
                    Thread.sleep(300)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }

            // 进度完成后更新通知
            builder.setContentText("进度已完成")
                .setProgress(0, 0, false)
            notificationManager.notify(progressNotificationId, builder.build())

        }.start()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onSendRandomNotification: () -> Unit = {},
    onSendProgressNotification: () -> Unit = {}
) {
    val context = LocalContext.current
    var serverAddress by remember { mutableStateOf(ServerPreferences.getServerAddress(context)) }
    var verificationCode by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationMessage by remember { mutableStateOf("") }
    var isVerified by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 创建一个滚动状态
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState), // 添加垂直滚动功能
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部间距
            Spacer(modifier = Modifier.height(8.dp))

            // 通知权限设置
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notification_access),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.notification_access_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onOpenNotificationSettings,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.open_settings))
                    }
                }
            }

            // 后台运行设置
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "后台运行设置",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "设置应用的电池优化豁免，使通知服务能够在后台持续工作，不被系统休眠",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onOpenBatteryOptimizationSettings,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("电池优化设置")
                    }
                }
            }

            // 服务器设置
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.server_settings),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.server_settings_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = serverAddress,
                        onValueChange = { serverAddress = it },
                        label = { Text(stringResource(R.string.server_address)) },
                        placeholder = { Text(stringResource(R.string.server_address_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 添加验证码相关UI和逻辑
                    if (isVerifying && generatedCode.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "请在服务器端输入以下验证码",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = {
                                // 限制只能输入6位数字
                                if (it.length <= 6 && (it.isEmpty() || it.all { char -> char.isDigit() })) {
                                    verificationCode = it
                                }
                            },
                            label = { Text("验证码") },
                            placeholder = { Text("输入6位验证码") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (verificationMessage.isNotEmpty()) {
                            Text(
                                text = verificationMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                isVerifying = false
                                generatedCode = ""
                                verificationCode = ""
                                verificationMessage = ""
                                isVerified = false
                            }) {
                                Text("取消")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (verificationCode == generatedCode) {
                                        // 验证成功，保存服务器地址
                                        ServerPreferences.saveServerAddress(context, serverAddress)
                                        verificationMessage = "验证成功，已保存服务器地址"
                                        isVerified = true

                                        // 延迟关闭验证界面
                                        scope.launch {
                                            kotlinx.coroutines.delay(1500)
                                            isVerifying = false
                                            verificationCode = ""
                                            generatedCode = ""
                                            verificationMessage = ""
                                        }
                                    } else {
                                        // 验证失败
                                        verificationMessage = "验证码不正确，请重试"
                                        isVerified = false
                                    }
                                },
                                enabled = verificationCode.length == 6
                            ) {
                                Text("验证")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (serverAddress.isNotBlank()) {
                                    scope.launch {
                                        try {
                                            serverAddress =
                                                ServerPreferences.formatServerAddress(serverAddress)
                                            // 首先检查服务器版本
                                            val isVersionValid =
                                                checkServerVersion(serverAddress, context)

                                            if (isVersionValid) {
                                                // 版本匹配，生成6位随机验证码
                                                generatedCode = generateRandomCode()

                                                // 发送验证码到服务器
                                                val result = sendVerificationCode(
                                                    serverAddress,
                                                    generatedCode
                                                )
                                                if (result) {
                                                    isVerifying = true
                                                    verificationMessage = ""
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "无法连接到服务器，请检查地址是否正确",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } else {
                                                // 版本不匹配
                                                // 获取格式化的版本不匹配错误提示，将所需版本号作为参数传入
                                                val requiredVersion =
                                                    context.getString(R.string.server_version_required)
                                                val versionMismatchMsg = context.getString(
                                                    R.string.server_version_mismatch,
                                                    requiredVersion
                                                )
                                                Toast.makeText(
                                                    context,
                                                    versionMismatchMsg,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(
                                                context,
                                                "连接服务器失败: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "请先输入服务器地址",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("连接并验证")
                        }
                    }
                }
            }

            // 通知数量限制设置卡片
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notification_limit_settings),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.notification_limit_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 显示当前通知数量
                    Text(
                        text = stringResource(R.string.current_notification_count, NotificationService.getNotificationCount()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var notificationLimitText by remember {
                        mutableStateOf(ServerPreferences.getNotificationLimit(context).toString())
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = notificationLimitText,
                            onValueChange = { notificationLimitText = it },
                            label = { Text(stringResource(R.string.notification_limit_settings)) },
                            placeholder = { Text(stringResource(R.string.notification_limit_hint)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val limit = notificationLimitText.toIntOrNull()
                                if (limit != null && limit >= ServerPreferences.getMinNotificationLimit() && limit <= ServerPreferences.getMaxNotificationLimit()) {
                                    ServerPreferences.saveNotificationLimit(context, limit)
                                    Toast.makeText(context, context.getString(R.string.notification_limit_saved), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.notification_limit_invalid), Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }

            // 测试通知卡片
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "测试通知功能",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "用于测试通知发送和接收功能，可以发送随机内容通知或进度通知",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = onSendRandomNotification
                        ) {
                            Text("发送随机通知")
                        }

                        Button(
                            onClick = onSendProgressNotification
                        ) {
                            Text("发送进度通知")
                        }
                    }
                }
            }

            // 语言设置卡片
            LanguageSettingsCard()

            // 底部间距，确保最后一个元素下方有足够空间
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LanguageSettingsCard() {
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLanguage = LocaleHelper.getCurrentLanguage(context)

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.language_settings),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.language_settings_desc),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "当前语言: ${LocaleHelper.getLanguageDisplayName(context, currentLanguage)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showLanguageDialog = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.select_language))
            }
        }
    }

    // 语言选择对话框
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { selectedLanguage ->
                if (LocaleHelper.needsRestart(context, selectedLanguage)) {
                    LocaleHelper.saveLanguage(context, selectedLanguage)
                    Toast.makeText(context, context.getString(R.string.language_changed), Toast.LENGTH_SHORT).show()

                    // 重启应用
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    if (context is ComponentActivity) {
                        context.finish()
                    }
                }
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: LocaleHelper.SupportedLanguage,
    onLanguageSelected: (LocaleHelper.SupportedLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val languages = LocaleHelper.SupportedLanguage.getAllLanguages()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_language)) },
        text = {
            Column {
                languages.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == currentLanguage,
                            onClick = { onLanguageSelected(language) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LocaleHelper.getLanguageDisplayName(context, language),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 生成6位随机验证码
fun generateRandomCode(): String {
    val random = Random.Default
    val code = StringBuilder()
    repeat(6) {
        code.append(random.nextInt(10))
    }
    return code.toString()
}

// 检查服务器版本
suspend fun checkServerVersion(serverAddress: String, context: Context): Boolean =
    withContext(Dispatchers.IO) {
        try {
            // 格式化服务器地址，确保包含http://前缀
            val formattedAddress =
                if (!serverAddress.startsWith("http://") && !serverAddress.startsWith("https://")) {
                    "http://$serverAddress"
                } else {
                    serverAddress
                }

            val versionUrl = "$formattedAddress/api/version"
            val url = URL(versionUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            // 获取响应
            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取响应内容
                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()

                // 解析JSON响应
                try {
                    val jsonObject = JSONObject(response)
                    val version = jsonObject.optString("version", "")

                    // 从资源文件中获取所需的版本号
                    val requiredVersion = context.getString(R.string.server_version_required)

                    // 检查版本是否匹配
                    val isVersionMatch = version == requiredVersion
                    connection.disconnect()
                    return@withContext isVersionMatch
                } catch (e: Exception) {
                    e.printStackTrace()
                    connection.disconnect()
                    return@withContext false
                }
            } else {
                connection.disconnect()
                return@withContext false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

// 发送验证码到服务器
suspend fun sendVerificationCode(serverAddress: String, code: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            // 格式化服务器地址，确保包含http://前缀
            val formattedAddress =
                if (!serverAddress.startsWith("http://") && !serverAddress.startsWith("https://")) {
                    "http://$serverAddress"
                } else {
                    serverAddress
                }

            val serverUrl = "$formattedAddress/api/notify"
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            // 创建JSON数据
            val deviceName = Build.MODEL
            val appName = "NotifyForwarders"
            val jsonBody = JSONObject().apply {
                put("devicename", deviceName)
                put("appname", appName)
                put("title", "请求与本机建立连接")
                put("description", "请在手机输入以下验证码：$code")
            }

            // 发送JSON数据
            val outputStream = connection.outputStream
            val writer = OutputStreamWriter(outputStream, "UTF-8")
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            // 获取响应
            val responseCode = connection.responseCode
            connection.disconnect()

            return@withContext responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

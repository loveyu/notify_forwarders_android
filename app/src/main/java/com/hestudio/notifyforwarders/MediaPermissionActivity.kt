package com.hestudio.notifyforwarders

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hestudio.notifyforwarders.ui.theme.NotifyForwardersTheme
import com.hestudio.notifyforwarders.util.LocaleHelper
import com.hestudio.notifyforwarders.util.MediaPermissionUtils
import com.hestudio.notifyforwarders.util.ServerPreferences
import com.hestudio.notifyforwarders.util.ToastManager

/**
 * 媒体权限引导Activity
 * 提供友好的权限请求界面和引导
 */
class MediaPermissionActivity : ComponentActivity() {

    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            ToastManager.showToast(this, getString(R.string.media_permission_granted))
            finish()
        } else {
            // 权限被拒绝，更新UI状态
            updatePermissionState()
        }
    }

    // 设置页面启动器
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 从设置页面返回后检查权限状态
        if (MediaPermissionUtils.hasMediaPermission(this)) {
            ToastManager.showToast(this, getString(R.string.media_permission_granted))
            finish()
        } else {
            updatePermissionState()
        }
    }

    private var permissionState = mutableStateOf(PermissionState.UNKNOWN)

    enum class PermissionState {
        UNKNOWN,
        GRANTED,
        DENIED,
        PERMANENTLY_DENIED
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { context ->
            val languageCode = ServerPreferences.getSelectedLanguage(context)
            LocaleHelper.setLocale(context, languageCode)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        updatePermissionState()
        
        setContent {
            NotifyForwardersTheme {
                MediaPermissionScreen(
                    permissionState = permissionState.value,
                    onRequestPermission = { requestMediaPermission() },
                    onOpenSettings = { openAppSettings() },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionState()
    }

    private fun updatePermissionState() {
        permissionState.value = when {
            MediaPermissionUtils.hasMediaPermission(this) -> PermissionState.GRANTED
            shouldShowRequestPermissionRationale(getRequiredPermission()) -> PermissionState.DENIED
            else -> PermissionState.PERMANENTLY_DENIED
        }
        
        // 如果权限已授予，自动关闭Activity
        if (permissionState.value == PermissionState.GRANTED) {
            ToastManager.showToast(this, getString(R.string.media_permission_granted))
            finish()
        }
    }

    private fun requestMediaPermission() {
        val permission = getRequiredPermission()
        requestPermissionLauncher.launch(permission)
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        settingsLauncher.launch(intent)
    }

    private fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MediaPermissionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPermissionScreen(
    permissionState: MediaPermissionActivity.PermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.media_permission_title)) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 图标
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 标题
            Text(
                text = stringResource(R.string.media_permission_request_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 说明文字
            Text(
                text = stringResource(R.string.media_permission_request_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 权限状态卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (permissionState) {
                        MediaPermissionActivity.PermissionState.UNKNOWN,
                        MediaPermissionActivity.PermissionState.DENIED -> {
                            Text(
                                text = stringResource(R.string.media_permission_needed),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = stringResource(R.string.media_permission_explanation),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Button(
                                onClick = onRequestPermission,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.grant_media_permission))
                            }
                        }
                        
                        MediaPermissionActivity.PermissionState.PERMANENTLY_DENIED -> {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = stringResource(R.string.media_permission_permanently_denied_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = stringResource(R.string.media_permission_permanently_denied_description),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Button(
                                onClick = onOpenSettings,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(stringResource(R.string.open_app_settings))
                            }
                        }
                        
                        MediaPermissionActivity.PermissionState.GRANTED -> {
                            // 这种情况下Activity应该已经关闭了
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 取消按钮
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

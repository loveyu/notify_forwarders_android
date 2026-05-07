package com.hestudio.notifyforwarders

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.hestudio.notifyforwarders.util.AppConfigManager
import com.hestudio.notifyforwarders.util.validate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun RemoteConfigSettingsCard(
    onOpenExampleConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var configUrl by remember {
        mutableStateOf(AppConfigManager.getRemoteConfigUrl(context))
    }
    var isDownloading by remember { mutableStateOf(false) }
    var applyDetail by remember { mutableStateOf<String?>(null) }
    var showConfigNotExistDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.remote_config_settings),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.remote_config_settings_desc),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 配置地址输入框
            OutlinedTextField(
                value = configUrl,
                onValueChange = {
                    configUrl = it
                    AppConfigManager.saveRemoteConfigUrl(context, it)
                },
                label = { Text(stringResource(R.string.config_url)) },
                placeholder = { Text(stringResource(R.string.config_url_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 按钮区域
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                // 下载并应用按钮
                Button(
                    onClick = {
                        if (configUrl.isBlank()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.config_url_required),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        scope.launch {
                            isDownloading = true
                            val detail = downloadAndApplyConfig(context, configUrl)
                            if (detail != null) {
                                applyDetail = detail
                            }
                            isDownloading = false
                        }
                    },
                    enabled = !isDownloading
                ) {
                    Text(
                        if (isDownloading) stringResource(R.string.downloading)
                        else stringResource(R.string.download_and_apply)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 示例配置按钮
                OutlinedButton(
                    onClick = onOpenExampleConfig
                ) {
                    Text(stringResource(R.string.show_example_config))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 打开按钮
                OutlinedButton(
                    onClick = {
                        val fileExists = openConfigInExternalEditor(context)
                        if (!fileExists) {
                            showConfigNotExistDialog = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.open_external))
                }
            }
        }
    }

    // 配置应用详情弹窗
    applyDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = { applyDetail = null },
            title = { Text(stringResource(R.string.config_apply_detail_title)) },
            text = {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { applyDetail = null }) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            }
        )
    }

    // 配置文件不存在弹窗
    if (showConfigNotExistDialog) {
        AlertDialog(
            onDismissRequest = { showConfigNotExistDialog = false },
            text = { Text(stringResource(R.string.config_not_exist)) },
            confirmButton = {
                TextButton(onClick = { showConfigNotExistDialog = false }) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            }
        )
    }
}

private suspend fun downloadAndApplyConfig(context: android.content.Context, url: String): String? {
    return try {
        withContext(Dispatchers.IO) {
            val parsedUrl = URL(url)
            val connection = parsedUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val yamlContent = reader.readText()
                reader.close()
                connection.disconnect()
                yamlContent
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.config_download_failed, responseCode),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                null
            }
        }?.let { yamlContent ->
            val result = AppConfigManager.parseFromYaml(yamlContent)
            result.getOrNull()?.let { config ->
                val errors = config.ignoreFilter.validate()
                if (errors.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.config_validation_failed, errors.joinToString("\n")),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    null
                } else {
                    AppConfigManager.loadConfig(config, context)
                    val saved = AppConfigManager.saveRawYamlToFile(context, yamlContent)
                    if (saved) {
                        buildApplyDetail(context, config)
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.config_save_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        null
                    }
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.config_parse_failed, result.exceptionOrNull()?.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                null
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                context.getString(R.string.config_download_error, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
        null
    }
}

private fun buildApplyDetail(context: android.content.Context, config: com.hestudio.notifyforwarders.util.AppConfig): String {
    val sb = StringBuilder()
    sb.append(context.getString(R.string.config_downloaded_and_applied))

    // 过滤规则
    val filterAppCount = config.ignoreFilter.rules.map { it.appName }.distinct().size
    sb.append("\n").append(
        context.getString(R.string.config_apply_filter_rules, config.ignoreFilter.rules.size, filterAppCount)
    )

    // 去重过滤
    val dedup = config.dedupFilter
    if (dedup.enabled) {
        sb.append("\n").append(
            context.getString(R.string.config_apply_dedup_enabled, dedup.apps.size)
        )
    } else {
        sb.append("\n").append(context.getString(R.string.config_apply_dedup_disabled))
    }

    // API 配置
    if (config.api.endpoints != null || config.api.timeouts != null) {
        sb.append("\n").append(context.getString(R.string.config_apply_api_custom))
    } else {
        sb.append("\n").append(context.getString(R.string.config_apply_api_default))
    }

    // 图标 URL
    if (config.iconUrl.enabled) {
        sb.append("\n").append(context.getString(R.string.config_apply_icon_url_enabled))
    } else {
        sb.append("\n").append(context.getString(R.string.config_apply_icon_url_disabled))
    }

    // 镜像转发
    val mirror = config.mirror
    if (mirror.enabled) {
        val destCount = mirror.endpoints.notify.size + mirror.endpoints.clipboardText.size +
            mirror.endpoints.clipboardImage.size + mirror.endpoints.imageRaw.size
        sb.append("\n").append(
            context.getString(R.string.config_apply_mirror_enabled, destCount)
        )
    } else {
        sb.append("\n").append(context.getString(R.string.config_apply_mirror_disabled))
    }

    return sb.toString()
}

private fun openConfigInExternalEditor(context: android.content.Context): Boolean {
    try {
        val externalFile = AppConfigManager.getExternalConfigFile(context)

        // 将内部存储的配置文件复制到外部存储，供外部编辑器访问
        val internalFile = AppConfigManager.getRawYamlFile(context)
        if (!internalFile.exists()) {
            return false
        }
        externalFile.parentFile?.mkdirs()
        internalFile.copyTo(externalFile, overwrite = true)

        // 使用FileProvider获取URI
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            externalFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/plain")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return true
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.open_config_error, e.message),
            Toast.LENGTH_SHORT
        ).show()
        return true
    }
}

package com.hestudio.notifyforwarders

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.hestudio.notifyforwarders.util.IgnoreFilterConfigManager
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
        mutableStateOf(IgnoreConfigManager.getRemoteConfigUrl(context))
    }
    var isDownloading by remember { mutableStateOf(false) }
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
                    IgnoreFilterConfigManager.saveRemoteConfigUrl(context, it)
                },
                label = { Text(stringResource(R.string.config_url)) },
                placeholder = { Text(stringResource(R.string.config_url_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 示例配置按钮
                OutlinedButton(
                    onClick = onOpenExampleConfig
                ) {
                    Text(stringResource(R.string.show_example_config))
                }

                Spacer(modifier = Modifier.width(8.dp))

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
                            downloadAndApplyConfig(context, configUrl)
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

                Spacer(modifier = Modifier.width(8.dp))

                // 打开按钮
                OutlinedButton(
                    onClick = {
                        openConfigInExternalEditor(context)
                    }
                ) {
                    Text(stringResource(R.string.open_external))
                }
            }
        }
    }
}

private object IgnoreConfigManager {
    fun getRemoteConfigUrl(context: android.content.Context): String {
        return IgnoreFilterConfigManager.getRemoteConfigUrl(context)
    }
}

private suspend fun downloadAndApplyConfig(context: android.content.Context, url: String) {
    withContext(Dispatchers.IO) {
        try {
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

                // 解析配置
                val result = IgnoreFilterConfigManager.parseFromYaml(yamlContent)
                result.onSuccess { config ->
                    // 验证配置
                    val errors = config.ignoreFilter.validate()
                    if (errors.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.config_validation_failed, errors.joinToString("\n")),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@withContext
                    }

                    // 先加载到内存
                    IgnoreFilterConfigManager.loadConfig(config)

                    // 保存到文件
                    val saved = IgnoreFilterConfigManager.saveToFile(context, config)
                    if (saved) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.config_downloaded_and_applied, config.ignoreFilter.rules.size),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.config_save_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.onFailure { error ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.config_parse_failed, error.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.config_download_failed, responseCode),
                        Toast.LENGTH_SHORT
                    ).show()
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
        }
    }
}

private fun openConfigInExternalEditor(context: android.content.Context) {
    try {
        // 确保外部文件存在
        val config = IgnoreFilterConfigManager.getCurrentConfig()
        val externalFile = IgnoreFilterConfigManager.getExternalConfigFile(context)

        if (!externalFile.exists()) {
            // 如果外部文件不存在，创建它
            if (config.ignoreFilter.rules.isEmpty()) {
                // 如果内存中没有配置，创建一个空的示例配置
                IgnoreFilterConfigManager.saveToExternalFile(
                    context,
                    com.hestudio.notifyforwarders.util.IgnoreFilterConfig(
                        listOf(
                            com.hestudio.notifyforwarders.util.IgnoreFilterRule(
                                appName = "示例应用",
                                regex = listOf("/示例正则/u"),
                                text = listOf("示例文本")
                            )
                        )
                    )
                )
            } else {
                IgnoreFilterConfigManager.saveToExternalFile(context, config)
            }
        }

        // 使用FileProvider获取URI
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            externalFile
        )

        // 创建Intent打开外部编辑器
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/plain")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // 检查是否有应用可以处理这个Intent
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // 没有文本编辑器，尝试用浏览器打开
            val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (browserIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(browserIntent)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.no_app_to_open_config),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.open_config_error, e.message),
            Toast.LENGTH_SHORT
        ).show()
    }
}

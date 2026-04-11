package com.hestudio.notifyforwarders.util

import android.util.Log
import com.hestudio.notifyforwarders.constants.ApiConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * 镜像转发工具类
 * 提供向镜像目的地并行发送 HTTP 请求的共享逻辑
 */
object MirrorForwarder {

    private const val TAG = "MirrorForwarder"

    /**
     * 异步将数据转发到所有镜像目的地
     * 每个目的地在独立的协程中并行发送，互不影响
     *
     * @param scope     协程作用域
     * @param jsonBody  请求体 JSON
     * @param endpoint  API 端点路径，如 /api/notify
     */
    fun forwardToMirrors(scope: CoroutineScope, jsonBody: JSONObject, endpoint: String) {
        val destinations = AppConfigManager.getMirrorDestinations()
        if (destinations.isEmpty()) return

        for (dest in destinations) {
            scope.launch(Dispatchers.IO) {
                forwardToSingleMirror(dest, jsonBody, endpoint)
            }
        }
    }

    /**
     * 向单个镜像目的地发送请求，支持重试
     */
    private suspend fun forwardToSingleMirror(dest: MirrorDestination, jsonBody: JSONObject, endpoint: String) {
        val url = dest.buildUrl(endpoint)
        val maxAttempts = 1 + dest.retry.coerceAtLeast(0)
        for (attempt in 1..maxAttempts) {
            try {
                val success = sendHttpRequest(
                    url, jsonBody,
                    dest.connectTimeout, dest.writeTimeout,
                    dest.token, dest.verifySSL
                )
                if (success) {
                    Log.d(TAG, "镜像转发成功: $url")
                    return
                } else {
                    Log.w(TAG, "镜像转发失败: $url, 尝试 $attempt/$maxAttempts")
                }
            } catch (e: Exception) {
                Log.w(TAG, "镜像转发异常: $url, 尝试 $attempt/$maxAttempts", e)
            }
            if (attempt < maxAttempts) {
                delay(dest.retryInterval.toLong())
            }
        }
        Log.e(TAG, "镜像转发最终失败: $url, 共尝试 $maxAttempts 次")
    }

    /**
     * 发送 HTTP POST JSON 请求
     */
    fun sendHttpRequest(
        urlString: String,
        jsonBody: JSONObject,
        connectTimeout: Int,
        readTimeout: Int,
        token: String? = null,
        verifySSL: Boolean = true
    ): Boolean {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = ApiConstants.METHOD_POST
        connection.setRequestProperty("Content-Type", ApiConstants.CONTENT_TYPE_JSON)
        connection.doOutput = true
        connection.connectTimeout = connectTimeout
        connection.readTimeout = readTimeout

        if (!token.isNullOrBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }

        if (!verifySSL && connection is HttpsURLConnection) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                connection.sslSocketFactory = sslContext.socketFactory
                connection.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                Log.w(TAG, "设置SSL信任管理器失败", e)
            }
        }

        val writer = OutputStreamWriter(connection.outputStream, ApiConstants.CHARSET_UTF8)
        writer.write(jsonBody.toString())
        writer.flush()
        writer.close()

        val responseCode = connection.responseCode
        connection.disconnect()
        return responseCode == HttpURLConnection.HTTP_OK
    }
}

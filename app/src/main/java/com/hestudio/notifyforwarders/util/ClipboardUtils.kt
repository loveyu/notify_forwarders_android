package com.hestudio.notifyforwarders.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.hestudio.notifyforwarders.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * 剪贴板工具类
 * 提供复制文本到剪贴板的功能
 */
object ClipboardUtils {
    private const val TAG = "ClipboardUtils"

    /**
     * 复制文本到剪贴板
     * @param context 上下文
     * @param text 要复制的文本
     * @param label 剪贴板标签
     * @param showToast 是否显示Toast提示
     */
    fun copyToClipboard(
        context: Context,
        text: String,
        label: String = "NotifyForwarders",
        showToast: Boolean = true
    ) {
        try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(label, text)
            clipboardManager.setPrimaryClip(clipData)
            
            if (showToast) {
                Toast.makeText(
                    context,
                    context.getString(R.string.copied_to_clipboard),
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            Log.d(TAG, "文本已复制到剪贴板: $label")
        } catch (e: Exception) {
            Log.e(TAG, "复制到剪贴板失败", e)
            if (showToast) {
                Toast.makeText(
                    context,
                    context.getString(R.string.copy_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 复制通知图标到剪贴板（PNG格式，带透明）
     * @param context 上下文
     * @param iconBase64 图标的Base64数据
     */
    fun copyNotificationIcon(context: Context, iconBase64: String?) {
        if (iconBase64.isNullOrBlank()) {
            Toast.makeText(
                context,
                context.getString(R.string.no_icon_to_copy),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            // 将Base64字符串转换为Bitmap
            val imageBytes = Base64.decode(iconBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            if (bitmap == null) {
                Log.e(TAG, "无法解码图标Base64数据")
                Toast.makeText(
                    context,
                    context.getString(R.string.icon_decode_failed),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // 创建临时文件保存PNG图片
            val cacheDir = File(context.cacheDir, "clipboard_icons")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val tempFile = File(cacheDir, "icon_${System.currentTimeMillis()}.png")

            // 将Bitmap保存为PNG格式（保持透明度）
            FileOutputStream(tempFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }

            // 创建URI并复制到剪贴板
            val uri = Uri.fromFile(tempFile)
            val clipData = ClipData.newUri(context.contentResolver, "Notification Icon", uri)

            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(clipData)

            Toast.makeText(
                context,
                context.getString(R.string.icon_copied_to_clipboard),
                Toast.LENGTH_SHORT
            ).show()

            Log.d(TAG, "图标已复制到剪贴板: ${tempFile.absolutePath}")

            // 清理旧的临时文件（保留最近的5个文件）
            cleanupOldTempFiles(cacheDir)

        } catch (e: Exception) {
            Log.e(TAG, "复制图标到剪贴板失败", e)
            Toast.makeText(
                context,
                context.getString(R.string.icon_copy_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 复制通知JSON到剪贴板
     * @param context 上下文
     * @param jsonString 通知的JSON字符串
     */
    fun copyNotificationJson(context: Context, jsonString: String) {
        copyToClipboard(
            context = context,
            text = jsonString,
            label = "Notification JSON",
            showToast = true
        )
    }

    /**
     * 复制通知纯文本到剪贴板
     * @param context 上下文
     * @param plainText 通知的纯文本
     */
    fun copyNotificationText(context: Context, plainText: String) {
        copyToClipboard(
            context = context,
            text = plainText,
            label = "Notification Text",
            showToast = true
        )
    }

    /**
     * 清理旧的临时文件，保留最近的5个文件
     * @param cacheDir 缓存目录
     */
    private fun cleanupOldTempFiles(cacheDir: File) {
        try {
            val files = cacheDir.listFiles { file ->
                file.name.startsWith("icon_") && file.name.endsWith(".png")
            } ?: return

            // 按修改时间排序，保留最新的5个文件
            val sortedFiles = files.sortedByDescending { it.lastModified() }
            if (sortedFiles.size > 5) {
                sortedFiles.drop(5).forEach { file ->
                    if (file.delete()) {
                        Log.d(TAG, "已删除旧的临时文件: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理临时文件失败", e)
        }
    }
}

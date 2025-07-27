package com.hestudio.notifyforwarders.util

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * 剪贴板和图片处理工具类
 */
object ClipboardImageUtils {
    private const val TAG = "ClipboardImageUtils"

    /**
     * 剪贴板内容数据类
     */
    data class ClipboardContent(
        val type: ContentType,
        val content: String, // Base64编码的内容
        val mimeType: String? = null
    )

    /**
     * 图片数据类
     */
    data class ImageContent(
        val content: String, // Base64编码的图片
        val mimeType: String = "image/jpeg",
        val fileName: String? = null, // 文件名
        val filePath: String? = null, // 文件路径
        val dateAdded: Long? = null, // 创建时间（时间戳）
        val dateModified: Long? = null // 修改时间（时间戳）
    )

    /**
     * 内容类型枚举
     */
    enum class ContentType {
        TEXT, IMAGE, EMPTY
    }

    /**
     * 检查是否可以访问剪贴板
     */
    fun canAccessClipboard(context: Context): Boolean {
        return try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // 尝试访问剪贴板，如果抛出异常说明没有权限
            clipboardManager.primaryClip
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "剪贴板访问权限不足", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "剪贴板访问检查失败", e)
            false
        }
    }

    /**
     * 读取剪贴板内容
     */
    fun readClipboardContent(context: Context): ClipboardContent {
        try {
            // 首先检查是否可以访问剪贴板
            if (!canAccessClipboard(context)) {
                Log.w(TAG, "无法访问剪贴板，可能是权限不足或应用在后台")
                throw SecurityException("剪贴板访问权限不足")
            }

            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboardManager.primaryClip

            if (clipData == null || clipData.itemCount == 0) {
                Log.d(TAG, "剪贴板为空")
                return ClipboardContent(ContentType.EMPTY, "")
            }

            val item = clipData.getItemAt(0)

            // 检查是否是图片URI
            item.uri?.let { uri ->
                if (isImageUri(context, uri)) {
                    Log.d(TAG, "剪贴板包含图片URI")
                    val imageBase64 = uriToBase64(context, uri)
                    if (imageBase64 != null) {
                        return ClipboardContent(ContentType.IMAGE, imageBase64, "image/*")
                    }
                }
            }

            // 检查文本内容
            val text = item.text?.toString()
            if (!text.isNullOrBlank()) {
                Log.d(TAG, "剪贴板包含文本内容")
                val textBase64 = Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                return ClipboardContent(ContentType.TEXT, textBase64, "text/plain")
            }

            Log.d(TAG, "剪贴板内容无法识别")
            return ClipboardContent(ContentType.EMPTY, "")

        } catch (e: SecurityException) {
            Log.e(TAG, "剪贴板权限不足", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "读取剪贴板失败", e)
            throw e
        }
    }

    /**
     * 获取最新的图片
     */
    fun getLatestImage(context: Context): ImageContent? {
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE
            )

            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                    val displayNameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                    val id = it.getLong(idColumn)
                    val imagePath = it.getString(dataColumn)
                    val dateAdded = it.getLong(dateAddedColumn)
                    val dateModified = it.getLong(dateModifiedColumn)
                    val fileName = it.getString(displayNameColumn)
                    val mimeType = it.getString(mimeTypeColumn) ?: "image/jpeg"

                    val imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())

                    // 读取图片并转换为Base64
                    val imageBase64 = uriToBase64(context, imageUri)
                    if (imageBase64 != null) {
                        Log.d(TAG, "成功获取最新图片: $fileName")
                        return ImageContent(
                            content = imageBase64,
                            mimeType = mimeType,
                            fileName = fileName,
                            filePath = imagePath,
                            dateAdded = dateAdded,
                            dateModified = dateModified
                        )
                    }
                }
            }

            Log.d(TAG, "未找到图片")
            return null

        } catch (e: Exception) {
            Log.e(TAG, "获取最新图片失败", e)
            return null
        }
    }

    /**
     * 检查URI是否是图片
     */
    private fun isImageUri(context: Context, uri: Uri): Boolean {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            mimeType?.startsWith("image/") == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 将URI转换为Base64字符串
     */
    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                bitmapToBase64(bitmap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "URI转Base64失败", e)
            null
        }
    }

    /**
     * 将Bitmap转换为Base64字符串
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }


}

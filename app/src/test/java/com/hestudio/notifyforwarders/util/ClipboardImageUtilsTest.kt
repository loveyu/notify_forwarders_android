package com.hestudio.notifyforwarders.util

import org.junit.Test
import org.junit.Assert.*

/**
 * 测试 ClipboardImageUtils 的功能
 * 注意：这些测试专注于数据类和枚举的基本功能，不涉及Android特定的API
 */
class ClipboardImageUtilsTest {

    @Test
    fun testClipboardContentDataClass() {
        // 测试 ClipboardContent 数据类的基本功能
        val textContent = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.TEXT,
            "dGVzdCB0ZXh0", // "test text" 的 Base64 编码
            "text/plain"
        )
        val imageContent = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.IMAGE,
            "AQID", // [1, 2, 3] 的 Base64 编码
            "image/png"
        )
        val emptyContent = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.EMPTY,
            ""
        )

        // 验证类型
        assertEquals("应该是文本类型", ClipboardImageUtils.ContentType.TEXT, textContent.type)
        assertEquals("应该是图片类型", ClipboardImageUtils.ContentType.IMAGE, imageContent.type)
        assertEquals("应该是空类型", ClipboardImageUtils.ContentType.EMPTY, emptyContent.type)

        // 验证数据
        assertEquals("文本MIME类型应该匹配", "text/plain", textContent.mimeType)
        assertEquals("图片MIME类型应该匹配", "image/png", imageContent.mimeType)
        assertNotNull("文本内容不应该为空", textContent.content)
        assertNotNull("图片内容不应该为空", imageContent.content)
        assertEquals("文本内容应该匹配", "dGVzdCB0ZXh0", textContent.content)
        assertEquals("图片内容应该匹配", "AQID", imageContent.content)
    }

    @Test
    fun testClipboardContentEquality() {
        // 测试相等性
        val textContent1 = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.TEXT,
            "dGVzdA==", // "test" 的 Base64 编码
            "text/plain"
        )
        val textContent2 = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.TEXT,
            "dGVzdA==", // 相同的 Base64 编码
            "text/plain"
        )
        val textContent3 = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.TEXT,
            "ZGlmZmVyZW50", // "different" 的 Base64 编码
            "text/plain"
        )

        assertEquals("相同文本内容应该相等", textContent1, textContent2)
        assertNotEquals("不同文本内容应该不相等", textContent1, textContent3)

        // 测试空内容
        val empty1 = ClipboardImageUtils.ClipboardContent(ClipboardImageUtils.ContentType.EMPTY, "")
        val empty2 = ClipboardImageUtils.ClipboardContent(ClipboardImageUtils.ContentType.EMPTY, "")
        assertEquals("空内容应该相等", empty1, empty2)
    }

    @Test
    fun testClipboardContentToString() {
        // 测试 toString 方法
        val textContent = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.TEXT,
            "dGVzdCB0ZXh0", // "test text" 的 Base64 编码
            "text/plain"
        )
        val imageContent = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.IMAGE,
            "AQID", // [1, 2, 3] 的 Base64 编码
            "image/png"
        )
        val emptyContent = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.EMPTY,
            ""
        )

        val textString = textContent.toString()
        val imageString = imageContent.toString()
        val emptyString = emptyContent.toString()

        assertTrue("文本内容字符串应该包含类型", textString.contains("TEXT"))
        assertTrue("图片内容字符串应该包含类型", imageString.contains("IMAGE"))
        assertTrue("空内容字符串应该包含类型", emptyString.contains("EMPTY"))
        assertNotNull("空内容字符串不应该为null", emptyString)
    }

    @Test
    fun testImageContentDataClass() {
        // 测试 ImageContent 数据类
        val imageContent = ClipboardImageUtils.ImageContent(
            content = "AQID", // [1, 2, 3] 的 Base64 编码
            mimeType = "image/png",
            fileName = "test.png",
            filePath = "/path/to/test.png",
            dateAdded = 1234567890L,
            dateModified = 1234567891L
        )

        assertEquals("MIME类型应该匹配", "image/png", imageContent.mimeType)
        assertEquals("文件名应该匹配", "test.png", imageContent.fileName)
        assertEquals("文件路径应该匹配", "/path/to/test.png", imageContent.filePath)
        assertEquals("创建时间应该匹配", 1234567890L, imageContent.dateAdded)
        assertEquals("修改时间应该匹配", 1234567891L, imageContent.dateModified)
        assertEquals("内容应该匹配", "AQID", imageContent.content)
        assertNotNull("内容不应该为空", imageContent.content)
    }

    @Test
    fun testContentTypeEnum() {
        // 测试 ContentType 枚举
        val textType = ClipboardImageUtils.ContentType.TEXT
        val imageType = ClipboardImageUtils.ContentType.IMAGE
        val emptyType = ClipboardImageUtils.ContentType.EMPTY

        assertNotNull("TEXT类型不应该为空", textType)
        assertNotNull("IMAGE类型不应该为空", imageType)
        assertNotNull("EMPTY类型不应该为空", emptyType)

        // 验证枚举值
        assertEquals("TEXT枚举值", "TEXT", textType.name)
        assertEquals("IMAGE枚举值", "IMAGE", imageType.name)
        assertEquals("EMPTY枚举值", "EMPTY", emptyType.name)
    }

    @Test
    fun testBase64ContentHandling() {
        // 测试Base64内容处理（使用预编码的字符串避免Android依赖）
        val base64Text = "dGVzdCBjb250ZW50" // "test content" 的 Base64 编码

        val textContent = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.TEXT,
            base64Text,
            "text/plain"
        )

        assertEquals("Base64内容应该匹配", base64Text, textContent.content)
        assertEquals("内容类型应该是TEXT", ClipboardImageUtils.ContentType.TEXT, textContent.type)
        assertEquals("MIME类型应该匹配", "text/plain", textContent.mimeType)

        // 验证内容不为空
        assertFalse("Base64内容不应该为空", textContent.content.isEmpty())
        assertTrue("Base64内容应该是有效格式", textContent.content.matches(Regex("[A-Za-z0-9+/=]+")))
    }

    @Test
    fun testContentTypeMatching() {
        // 测试内容类型匹配
        val textContent = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.TEXT,
            "dGVzdA==", // "test" 的 Base64 编码
            "text/plain"
        )
        val imageContent = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.IMAGE,
            "AQID", // [1, 2, 3] 的 Base64 编码
            "image/png"
        )
        val emptyContent = ClipboardImageUtils.ClipboardContent(
            ClipboardImageUtils.ContentType.EMPTY,
            ""
        )

        // 使用 when 表达式验证所有情况都被覆盖
        val textResult = when (textContent.type) {
            ClipboardImageUtils.ContentType.TEXT -> "text"
            ClipboardImageUtils.ContentType.IMAGE -> "image"
            ClipboardImageUtils.ContentType.EMPTY -> "empty"
        }
        assertEquals("应该正确识别文本内容", "text", textResult)

        val imageResult = when (imageContent.type) {
            ClipboardImageUtils.ContentType.TEXT -> "text"
            ClipboardImageUtils.ContentType.IMAGE -> "image"
            ClipboardImageUtils.ContentType.EMPTY -> "empty"
        }
        assertEquals("应该正确识别图片内容", "image", imageResult)

        val emptyResult = when (emptyContent.type) {
            ClipboardImageUtils.ContentType.TEXT -> "text"
            ClipboardImageUtils.ContentType.IMAGE -> "image"
            ClipboardImageUtils.ContentType.EMPTY -> "empty"
        }
        assertEquals("应该正确识别空内容", "empty", emptyResult)
    }

    @Test
    fun testImageContentDefaults() {
        // 测试 ImageContent 的默认值
        val imageContent = ClipboardImageUtils.ImageContent(
            content = "dGVzdA=="
        )

        assertEquals("默认MIME类型应该是image/jpeg", "image/jpeg", imageContent.mimeType)
        assertNull("默认文件名应该为null", imageContent.fileName)
        assertNull("默认文件路径应该为null", imageContent.filePath)
        assertNull("默认创建时间应该为null", imageContent.dateAdded)
        assertNull("默认修改时间应该为null", imageContent.dateModified)
        assertEquals("内容应该匹配", "dGVzdA==", imageContent.content)
    }
}

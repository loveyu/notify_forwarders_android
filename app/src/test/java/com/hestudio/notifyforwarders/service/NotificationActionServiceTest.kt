package com.hestudio.notifyforwarders.service

import org.junit.Test
import org.junit.Assert.*

/**
 * 测试 NotificationActionService 的常量定义
 */
class NotificationActionServiceTest {

    @Test
    fun testActionConstants() {
        // 验证 action 常量的值是否正确
        assertEquals("com.hestudio.notifyforwarders.SEND_CLIPBOARD", NotificationActionService.ACTION_SEND_CLIPBOARD)
        assertEquals("com.hestudio.notifyforwarders.SEND_IMAGE", NotificationActionService.ACTION_SEND_IMAGE)
    }

    @Test
    fun testActionConstantsAreNotEmpty() {
        // 验证 action 常量不为空
        assertNotNull(NotificationActionService.ACTION_SEND_CLIPBOARD)
        assertNotNull(NotificationActionService.ACTION_SEND_IMAGE)
        assertTrue(NotificationActionService.ACTION_SEND_CLIPBOARD.isNotEmpty())
        assertTrue(NotificationActionService.ACTION_SEND_IMAGE.isNotEmpty())
    }

    @Test
    fun testActionConstantsAreUnique() {
        // 验证两个 action 常量是不同的
        assertNotEquals(NotificationActionService.ACTION_SEND_CLIPBOARD, NotificationActionService.ACTION_SEND_IMAGE)
    }

    @Test
    fun testActionConstantsFormat() {
        // 验证 action 常量格式符合 Android 约定
        assertTrue("剪贴板 action 应该包含包名",
            NotificationActionService.ACTION_SEND_CLIPBOARD.contains("com.hestudio.notifyforwarders"))
        assertTrue("图片 action 应该包含包名",
            NotificationActionService.ACTION_SEND_IMAGE.contains("com.hestudio.notifyforwarders"))

        // 验证 action 格式
        assertTrue("剪贴板 action 应该以 SEND_CLIPBOARD 结尾",
            NotificationActionService.ACTION_SEND_CLIPBOARD.endsWith("SEND_CLIPBOARD"))
        assertTrue("图片 action 应该以 SEND_IMAGE 结尾",
            NotificationActionService.ACTION_SEND_IMAGE.endsWith("SEND_IMAGE"))
    }

    @Test
    fun testCompanionObjectMethods() {
        // 验证伴生对象方法存在
        val companionClass = NotificationActionService.Companion::class.java

        // 检查 sendClipboard 方法
        val sendClipboardMethod = companionClass.getDeclaredMethod("sendClipboard", android.content.Context::class.java)
        assertNotNull("sendClipboard 方法应该存在", sendClipboardMethod)

        // 检查 sendLatestImage 方法
        val sendLatestImageMethod = companionClass.getDeclaredMethod("sendLatestImage", android.content.Context::class.java)
        assertNotNull("sendLatestImage 方法应该存在", sendLatestImageMethod)
    }
}

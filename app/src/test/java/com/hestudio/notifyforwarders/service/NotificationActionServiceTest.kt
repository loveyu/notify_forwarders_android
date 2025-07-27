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
}

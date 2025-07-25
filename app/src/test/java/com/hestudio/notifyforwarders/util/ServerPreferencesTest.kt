package com.hestudio.notifyforwarders.util

import android.content.Context
import android.content.SharedPreferences
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

/**
 * 测试ServerPreferences中的通知数量限制功能
 */
class ServerPreferencesTest {

    @Test
    fun testNotificationLimitConstants() {
        // 测试常量值是否正确
        assertEquals(1, ServerPreferences.getMinNotificationLimit())
        assertEquals(200, ServerPreferences.getDefaultNotificationLimit())
        assertEquals(10000, ServerPreferences.getMaxNotificationLimit())
    }

    @Test
    fun testNotificationLimitValidation() {
        val mockContext = mock(Context::class.java)
        val mockPrefs = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)
        
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        
        // 测试边界值
        ServerPreferences.saveNotificationLimit(mockContext, 0) // 应该被限制为1
        verify(mockEditor).putInt("notification_limit", 1)
        
        ServerPreferences.saveNotificationLimit(mockContext, 15000) // 应该被限制为10000
        verify(mockEditor).putInt("notification_limit", 10000)
        
        ServerPreferences.saveNotificationLimit(mockContext, 500) // 正常值
        verify(mockEditor).putInt("notification_limit", 500)
    }
}

package com.hestudio.notifyforwarders.util

import org.junit.Test
import org.junit.Assert.*

/**
 * IconCacheManager的单元测试
 */
class IconCacheManagerTest {

    @Test
    fun testCanPushIcon_initialState() {
        // 测试初始状态下可以推送图标
        val iconMd5 = "test_md5_hash"
        assertTrue("初始状态应该允许推送图标", IconCacheManager.canPushIcon(iconMd5))
    }

    @Test
    fun testCanPushIcon_afterRecord() {
        // 测试记录推送后的状态
        val iconMd5 = "test_md5_hash_2"
        
        // 记录推送
        IconCacheManager.recordIconPush(iconMd5)
        
        // 立即检查应该不能推送（10分钟限制）
        assertFalse("记录推送后应该不能立即再次推送", IconCacheManager.canPushIcon(iconMd5))
    }

    @Test
    fun testCalculateMd5() {
        // 测试MD5计算的一致性
        val input = "test_input"
        val md5_1 = calculateMd5(input)
        val md5_2 = calculateMd5(input)
        
        assertEquals("相同输入应该产生相同的MD5", md5_1, md5_2)
        assertNotNull("MD5不应该为null", md5_1)
        assertTrue("MD5应该是32位十六进制字符串", md5_1.matches(Regex("[a-f0-9]{32}")))
    }

    @Test
    fun testCalculateMd5_differentInputs() {
        // 测试不同输入产生不同MD5
        val input1 = "test_input_1"
        val input2 = "test_input_2"
        
        val md5_1 = calculateMd5(input1)
        val md5_2 = calculateMd5(input2)
        
        assertNotEquals("不同输入应该产生不同的MD5", md5_1, md5_2)
    }

    // 辅助方法：计算MD5（从IconCacheManager复制）
    private fun calculateMd5(input: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

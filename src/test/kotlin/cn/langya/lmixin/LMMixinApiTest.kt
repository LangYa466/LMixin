package cn.langya.lmixin

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LMMixinApiTest {
    @Test
    fun testApiInstance() {
        val api = LMMixinAPI.getInstance()
        assertNotNull(api)
        
        // 测试单例模式
        val api2 = LMMixinAPI.getInstance()
        assertTrue(api === api2)
    }

    @Test
    fun testGetBytes() {
        val api = LMMixinAPI.getInstance()
        val bytes = api.getBytes()
        assertTrue(bytes is Map<*, *>)
        assertTrue(bytes.isEmpty())
    }

    @Test
    fun testGetClassBytes() {
        val api = LMMixinAPI.getInstance()
        
        // 测试不存在的类
        val nonExistentBytes = api.getClassBytes("cn/langya/lmixin/NonExistentClass")
        assertNull(nonExistentBytes)
        
        // 测试空类名
        val emptyBytes = api.getClassBytes("")
        assertNull(emptyBytes)
    }

    @Test
    fun testGetProcessedClassNames() {
        val api = LMMixinAPI.getInstance()
        val classNames = api.getProcessedClassNames()
        assertTrue(classNames is List<*>)
        assertTrue(classNames.isEmpty())
    }

    @Test
    fun testIsClassProcessed() {
        val api = LMMixinAPI.getInstance()
        
        // 测试未处理的类
        assertFalse(api.isClassProcessed("cn/langya/lmixin/TestClass"))
        assertFalse(api.isClassProcessed(""))
        assertFalse(api.isClassProcessed("java/lang/String"))
    }

    @Test
    fun testGetProcessedClassCount() {
        val api = LMMixinAPI.getInstance()
        val count = api.getProcessedClassCount()
        assertTrue(count >= 0)
        assertEquals(0, count) // 初始状态应该为0
    }
    
    @Test
    fun testApiMethodsWithNullProcessor() {
        val api = LMMixinAPI()
        // 测试当mixinProcessor为null时的行为
        assertEquals(0, api.getProcessedClassCount())
        assertTrue(api.getBytes().isEmpty())
        assertTrue(api.getProcessedClassNames().isEmpty())
        assertFalse(api.isClassProcessed("test"))
        assertNull(api.getClassBytes("test"))
    }
} 
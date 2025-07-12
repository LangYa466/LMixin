package cn.langya.lmixin

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AnnotationTest {
    
    @Test
    fun testAtEnumValues() {
        assertEquals("HEAD", At.HEAD.value)
        assertEquals("TAIL", At.TAIL.value)
        assertEquals("BEFORE", At.BEFORE.value)
        assertEquals("AFTER", At.AFTER.value)
    }
    
    @Test
    fun testAtIsHead() {
        assertTrue(At.HEAD.isHead())
        assertFalse(At.TAIL.isHead())
        assertFalse(At.BEFORE.isHead())
        assertFalse(At.AFTER.isHead())
    }
    
    @Test
    fun testAtIsTail() {
        assertFalse(At.HEAD.isTail())
        assertTrue(At.TAIL.isTail())
        assertFalse(At.BEFORE.isTail())
        assertFalse(At.AFTER.isTail())
    }
    
    @Test
    fun testAtIsBefore() {
        assertFalse(At.HEAD.isBefore())
        assertFalse(At.TAIL.isBefore())
        assertTrue(At.BEFORE.isBefore())
        assertFalse(At.AFTER.isBefore())
    }
    
    @Test
    fun testAtIsAfter() {
        assertFalse(At.HEAD.isAfter())
        assertFalse(At.TAIL.isAfter())
        assertFalse(At.BEFORE.isAfter())
        assertTrue(At.AFTER.isAfter())
    }

    @Test
    fun testAtFromString() {
        assertEquals(At.HEAD, At.fromString("HEAD"))
        assertEquals(At.TAIL, At.fromString("TAIL"))
        assertEquals(At.BEFORE, At.fromString("BEFORE"))
        assertEquals(At.AFTER, At.fromString("AFTER"))
        
        // 测试大小写不敏感
        assertEquals(At.HEAD, At.fromString("head"))
        assertEquals(At.TAIL, At.fromString("tail"))
        assertEquals(At.BEFORE, At.fromString("before"))
        assertEquals(At.AFTER, At.fromString("after"))
        
        // 测试不存在的值
        assertNull(At.fromString("INVALID"))
        assertNull(At.fromString(""))
    }
    
    @Test
    fun testAtGetAllValues() {
        val allValues = At.getAllValues()
        assertEquals(4, allValues.size)
        assertTrue(allValues.contains(At.HEAD))
        assertTrue(allValues.contains(At.TAIL))
        assertTrue(allValues.contains(At.BEFORE))
        assertTrue(allValues.contains(At.AFTER))
    }
    
    @Test
    fun testAtEnumEquality() {
        val head1 = At.HEAD
        val head2 = At.HEAD
        val tail = At.TAIL
        
        assertTrue(head1 == head2)
        assertFalse(head1 == tail)
        assertTrue(head1 === head2) // 枚举是单例
    }
    
    @Test
    fun testAtEnumToString() {
        assertEquals("HEAD", At.HEAD.toString())
        assertEquals("TAIL", At.TAIL.toString())
        assertEquals("BEFORE", At.BEFORE.toString())
        assertEquals("AFTER", At.AFTER.toString())
    }
    
    @Test
    fun testAtEnumOrdinal() {
        assertEquals(0, At.HEAD.ordinal)
        assertEquals(1, At.TAIL.ordinal)
        assertEquals(2, At.BEFORE.ordinal)
        assertEquals(3, At.AFTER.ordinal)
    }
    
    @Test
    fun testAtEnumValuesMethod() {
        val values = At.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(At.HEAD))
        assertTrue(values.contains(At.TAIL))
        assertTrue(values.contains(At.BEFORE))
        assertTrue(values.contains(At.AFTER))
    }
    
    @Test
    fun testAtEnumValueOf() {
        assertEquals(At.HEAD, At.valueOf("HEAD"))
        assertEquals(At.TAIL, At.valueOf("TAIL"))
        assertEquals(At.BEFORE, At.valueOf("BEFORE"))
        assertEquals(At.AFTER, At.valueOf("AFTER"))
    }
} 
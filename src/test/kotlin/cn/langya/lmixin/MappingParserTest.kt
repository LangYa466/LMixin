package cn.langya.lmixin

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import java.io.File

class MappingParserTest {
    
    @Test
    fun testDefaultConstructor() {
        val parser = MappingParser()
        assertNotNull(parser)
        assertEquals(0, parser.getClassMapSize())
        assertEquals(0, parser.getFieldMapSize())
        assertEquals(0, parser.getMethodMapSize())
    }
    
    @Test
    fun testConstructorWithNonExistentFile() {
        val parser = MappingParser("non_existent_mappings.srg")
        assertNotNull(parser)
        assertEquals(0, parser.getClassMapSize())
        assertEquals(0, parser.getFieldMapSize())
        assertEquals(0, parser.getMethodMapSize())
    }
    
    @Test
    fun testMapClassNameWithoutMapping() {
        val parser = MappingParser()
        val result = parser.mapClassName("test/Class")
        assertEquals("test/Class", result)
    }
    
    @Test
    fun testMapFieldNameWithoutMapping() {
        val parser = MappingParser()
        val result = parser.mapFieldName("test/Class", "fieldName")
        assertEquals("fieldName", result)
    }
    
    @Test
    fun testMapMethodNameWithoutMapping() {
        val parser = MappingParser()
        val result = parser.mapMethodName("test/Class", "methodName", "()V")
        assertEquals("methodName", result)
    }
    
    @Test
    fun testMapMethodDesc() {
        val parser = MappingParser()
        val result = parser.mapMethodDesc("()V")
        assertEquals("()V", result)
    }
    
    @Test
    fun testGetClassMapSizeInitiallyZero() {
        val parser = MappingParser()
        assertEquals(0, parser.getClassMapSize())
    }
    
    @Test
    fun testGetFieldMapSizeInitiallyZero() {
        val parser = MappingParser()
        assertEquals(0, parser.getFieldMapSize())
    }
    
    @Test
    fun testGetMethodMapSizeInitiallyZero() {
        val parser = MappingParser()
        assertEquals(0, parser.getMethodMapSize())
    }
    
    @Test
    fun testHasClassMappingNonExistent() {
        val parser = MappingParser()
        assertFalse(parser.hasClassMapping("test/Class"))
    }
    
    @Test
    fun testHasFieldMappingNonExistent() {
        val parser = MappingParser()
        assertFalse(parser.hasFieldMapping("test/Class", "fieldName"))
    }
    
    @Test
    fun testHasMethodMappingNonExistent() {
        val parser = MappingParser()
        assertFalse(parser.hasMethodMapping("test/Class", "methodName", "()V"))
    }
    
    @Test
    fun testMapMethodNameWithValidMapping() {
        // 创建临时映射文件
        val tempFile = File.createTempFile("test_mappings", ".srg")
        tempFile.deleteOnExit()
        
        tempFile.writeText("MD: test/Class/method ()V test/Class/mappedMethod ()V")
        
        val parser = MappingParser(tempFile.absolutePath)
        
        // 验证映射存在
        assertTrue(parser.hasMethodMapping("test/Class", "method", "()V"))
        
        // 验证映射结果
        val result = parser.mapMethodName("test/Class", "method", "()V")
        assertEquals("mappedMethod", result)
    }
    
    @Test
    fun testLoadMappingsWithValidFile() {
        // 创建临时映射文件
        val tempFile = File.createTempFile("test_mappings", ".srg")
        tempFile.deleteOnExit()
        
        tempFile.writeText("""
            CL: test/Class test/MappedClass
            FD: test/Class/field test/Class/mappedField
            MD: test/Class/method ()V test/Class/mappedMethod ()V
        """.trimIndent())
        
        val parser = MappingParser(tempFile.absolutePath)
        
        assertEquals(1, parser.getClassMapSize())
        assertEquals(1, parser.getFieldMapSize())
        assertEquals(1, parser.getMethodMapSize())
        
        assertTrue(parser.hasClassMapping("test/Class"))
        assertTrue(parser.hasFieldMapping("test/Class", "field"))
        assertTrue(parser.hasMethodMapping("test/Class", "method", "()V"))
        
        assertEquals("test/MappedClass", parser.mapClassName("test/Class"))
        assertEquals("mappedField", parser.mapFieldName("test/Class", "field"))
        // mapMethodName应该返回映射后的方法名
        assertEquals("mappedMethod", parser.mapMethodName("test/Class", "method", "()V"))
    }
    
    @Test
    fun testLoadMappingsWithInvalidLines() {
        val tempFile = File.createTempFile("test_mappings", ".srg")
        tempFile.deleteOnExit()
        
        tempFile.writeText("""
            CL: test/Class
            FD: test/Class/field
            MD: test/Class/method
            INVALID: line
            CL: test/Class test/MappedClass extra
        """.trimIndent())
        
        val parser = MappingParser(tempFile.absolutePath)
        
        // 只有有效的行应该被解析
        assertEquals(1, parser.getClassMapSize())
        assertEquals(0, parser.getFieldMapSize())
        assertEquals(0, parser.getMethodMapSize())
    }
    
    @Test
    fun testLoadMappingsWithEmptyFile() {
        val tempFile = File.createTempFile("test_mappings", ".srg")
        tempFile.deleteOnExit()
        
        val parser = MappingParser(tempFile.absolutePath)
        
        assertEquals(0, parser.getClassMapSize())
        assertEquals(0, parser.getFieldMapSize())
        assertEquals(0, parser.getMethodMapSize())
    }
    
    @Test
    fun testMapMethodNameWithComplexDescriptor() {
        val parser = MappingParser()
        val result = parser.mapMethodName("test/Class", "method", "(ILjava/lang/String;)V")
        assertEquals("method", result)
    }
    
    @Test
    fun testMapMethodNameWithEmptyDescriptor() {
        val parser = MappingParser()
        val result = parser.mapMethodName("test/Class", "method", "")
        assertEquals("method", result)
    }
    
    @Test
    fun testMapClassNameWithEmptyString() {
        val parser = MappingParser()
        val result = parser.mapClassName("")
        assertEquals("", result)
    }
    
    @Test
    fun testMapFieldNameWithEmptyOwner() {
        val parser = MappingParser()
        val result = parser.mapFieldName("", "fieldName")
        assertEquals("fieldName", result)
    }
    
    @Test
    fun testMapFieldNameWithEmptyFieldName() {
        val parser = MappingParser()
        val result = parser.mapFieldName("test/Class", "")
        assertEquals("", result)
    }
} 
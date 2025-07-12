package cn.langya.lmixin

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import java.io.File
import java.io.ByteArrayInputStream

class MixinProcessorTest {
    
    @Test
    fun testDefaultConstructor() {
        val processor = MixinProcessor()
        assertNotNull(processor)
        assertTrue(processor.mixinMap.isEmpty())
    }
    
    @Test
    fun testConstructorWithMappingParser() {
        val mappingParser = MappingParser()
        val processor = MixinProcessor(mappingParser)
        assertNotNull(processor)
        assertTrue(processor.mixinMap.isEmpty())
    }
    
    @Test
    fun testConstructorWithSrgFilePath() {
        val processor = MixinProcessor("test_mappings.srg")
        assertNotNull(processor)
        assertTrue(processor.mixinMap.isEmpty())
    }
    
    @Test
    fun testGetBytesInitiallyEmpty() {
        val processor = MixinProcessor()
        val bytes = processor.getBytes()
        assertTrue(bytes.isEmpty())
    }
    
    @Test
    fun testGetClassBytesNonExistent() {
        val processor = MixinProcessor()
        val bytes = processor.getClassBytes("cn/langya/lmixin/NonExistentClass")
        assertNull(bytes)
    }
    
    @Test
    fun testGetProcessedClassNamesInitiallyEmpty() {
        val processor = MixinProcessor()
        val classNames = processor.getProcessedClassNames()
        assertTrue(classNames.isEmpty())
    }
    
    @Test
    fun testIsClassProcessedNonExistent() {
        val processor = MixinProcessor()
        assertFalse(processor.isClassProcessed("cn/langya/lmixin/TestClass"))
        assertFalse(processor.isClassProcessed(""))
    }
    
    @Test
    fun testGetProcessedClassCountInitiallyZero() {
        val processor = MixinProcessor()
        assertEquals(0, processor.getProcessedClassCount())
    }
    
    @Test
    fun testGetMixinMapSizeInitiallyZero() {
        val processor = MixinProcessor()
        assertEquals(0, processor.getMixinMapSize())
    }
    
    @Test
    fun testGetMixinsForClassNonExistent() {
        val processor = MixinProcessor()
        val mixins = processor.getMixinsForClass("cn/langya/lmixin/TestClass")
        assertNull(mixins)
    }
    
    @Test
    fun testHasMixinsForClassNonExistent() {
        val processor = MixinProcessor()
        assertFalse(processor.hasMixinsForClass("cn/langya/lmixin/TestClass"))
    }
    
    @Test
    fun testProcessMixinsFromDirectoryNonExistent() {
        val processor = MixinProcessor()
        // 测试不存在的目录
        processor.processMixinsFromDirectory("/non/existent/directory")
        // 不应该抛出异常
        assertTrue(processor.getBytes().isEmpty())
    }
    
    @Test
    fun testProcessMixinsFromJarNonExistent() {
        val processor = MixinProcessor()
        // 测试不存在的JAR文件
        processor.processMixinsFromJar("/non/existent/file.jar")
        // 不应该抛出异常
        assertTrue(processor.getBytes().isEmpty())
    }
    
    @Test
    fun testProcessMixinsFromDirectoryFile() {
        val processor = MixinProcessor()
        // 测试文件而不是目录
        val tempFile = File.createTempFile("test", ".txt")
        tempFile.deleteOnExit()
        
        processor.processMixinsFromDirectory(tempFile.absolutePath)
        // 不应该抛出异常
        assertTrue(processor.getBytes().isEmpty())
    }
} 
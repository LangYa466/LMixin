package cn.langya.lmixin

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class MixinTransformerTest {
    
    @Test
    fun testDefaultConstructor() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        val transformer = MixinTransformer(classWriter, mixinInfo)
        
        assertNotNull(transformer)
        assertEquals("test.MixinClass", transformer.getMixinClassName())
        assertEquals("test.TargetClass", transformer.getTargetClassName())
    }
    
    @Test
    fun testConstructorWithApi() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        val transformer = MixinTransformer(Opcodes.ASM9, classWriter, mixinInfo)
        
        assertNotNull(transformer)
        assertEquals("test.MixinClass", transformer.getMixinClassName())
        assertEquals("test.TargetClass", transformer.getTargetClassName())
    }
    
    @Test
    fun testGetMixinInfo() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        val transformer = MixinTransformer(classWriter, mixinInfo)
        
        val result = transformer.getMixinInfo()
        assertEquals(mixinInfo, result)
    }
    
    @Test
    fun testGetTargetClassName() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        val transformer = MixinTransformer(classWriter, mixinInfo)
        
        assertEquals("test.TargetClass", transformer.getTargetClassName())
    }
    
    @Test
    fun testGetMixinClassName() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        val transformer = MixinTransformer(classWriter, mixinInfo)
        
        assertEquals("test.MixinClass", transformer.getMixinClassName())
    }
    
    @Test
    fun testGetInterfaceCount() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val mixinInfo = MixinInfo(
            "test.MixinClass",
            "test.TargetClass",
            listOf("test.Interface1", "test.Interface2")
        )
        val transformer = MixinTransformer(classWriter, mixinInfo)
        
        assertEquals(2, transformer.getInterfaceCount())
    }
    
    @Test
    fun testGetNewFieldCount() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val newFields = listOf(
            FieldNode(Opcodes.ACC_PUBLIC, "field1", "Ljava/lang/String;", null, null),
            FieldNode(Opcodes.ACC_PRIVATE, "field2", "I", null, null)
        )
        val mixinInfo = MixinInfo(
            "test.MixinClass",
            "test.TargetClass",
            newFields = newFields
        )
        val transformer = MixinTransformer(classWriter, mixinInfo)
        
        assertEquals(2, transformer.getNewFieldCount())
    }
    
    @Test
    fun testGetNewMethodCount() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val newMethods = listOf(
            MethodNode(Opcodes.ACC_PUBLIC, "method1", "()V", null, null),
            MethodNode(Opcodes.ACC_PRIVATE, "method2", "(I)V", null, null)
        )
        val mixinInfo = MixinInfo(
            "test.MixinClass",
            "test.TargetClass",
            newMethods = newMethods
        )
        val transformer = MixinTransformer(classWriter, mixinInfo)
        
        assertEquals(2, transformer.getNewMethodCount())
    }
    
    @Test
    fun testGetShadowFieldCount() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val shadowFields = listOf(
            MixinInfo.ShadowField(
                "mixinField1",
                "targetField1",
                FieldNode(Opcodes.ACC_PUBLIC, "mixinField1", "Ljava/lang/String;", null, null)
            ),
            MixinInfo.ShadowField(
                "mixinField2",
                "targetField2",
                FieldNode(Opcodes.ACC_PRIVATE, "mixinField2", "I", null, null)
            )
        )
        val mixinInfo = MixinInfo(
            "test.MixinClass",
            "test.TargetClass",
            shadowFields = shadowFields
        )
        val transformer = MixinTransformer(classWriter, mixinInfo)
        
        assertEquals(2, transformer.getShadowFieldCount())
    }
    
    @Test
    fun testGetShadowMethodCount() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val shadowMethods = listOf(
            MixinInfo.ShadowMethod(
                "mixinMethod1",
                "()V",
                "targetMethod1",
                "()V",
                MethodNode(Opcodes.ACC_PUBLIC, "mixinMethod1", "()V", null, null)
            ),
            MixinInfo.ShadowMethod(
                "mixinMethod2",
                "(I)V",
                "targetMethod2",
                "(I)V",
                MethodNode(Opcodes.ACC_PRIVATE, "mixinMethod2", "(I)V", null, null)
            )
        )
        val mixinInfo = MixinInfo(
            "test.MixinClass",
            "test.TargetClass",
            shadowMethods = shadowMethods
        )
        val transformer = MixinTransformer(classWriter, mixinInfo)
        
        assertEquals(2, transformer.getShadowMethodCount())
    }
    
    @Test
    fun testGetOverwriteMethodCount() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val overwriteMethods = listOf(
            MixinInfo.OverwriteMethod(
                "overwriteMethod1",
                "()V",
                "targetMethod1",
                "()V",
                MethodNode(Opcodes.ACC_PUBLIC, "overwriteMethod1", "()V", null, null)
            ),
            MixinInfo.OverwriteMethod(
                "overwriteMethod2",
                "(I)V",
                "targetMethod2",
                "(I)V",
                MethodNode(Opcodes.ACC_PRIVATE, "overwriteMethod2", "(I)V", null, null)
            )
        )
        val mixinInfo = MixinInfo(
            "test.MixinClass",
            "test.TargetClass",
            overwriteMethods = overwriteMethods
        )
        val transformer = MixinTransformer(classWriter, mixinInfo)
        
        assertEquals(2, transformer.getOverwriteMethodCount())
    }
    
    @Test
    fun testGetInjectMethodCount() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val injectMethods = listOf(
            MixinInfo.InjectMethod(
                "injectMethod1",
                "()V",
                "targetMethod1",
                "()V",
                At.HEAD,
                "injectionPoint1",
                0,
                MethodNode(Opcodes.ACC_PUBLIC, "injectMethod1", "()V", null, null)
            ),
            MixinInfo.InjectMethod(
                "injectMethod2",
                "(I)V",
                "targetMethod2",
                "(I)V",
                At.TAIL,
                "injectionPoint2",
                1,
                MethodNode(Opcodes.ACC_PRIVATE, "injectMethod2", "(I)V", null, null)
            )
        )
        val mixinInfo = MixinInfo(
            "test.MixinClass",
            "test.TargetClass",
            injectMethods = injectMethods
        )
        val transformer = MixinTransformer(classWriter, mixinInfo)
        
        assertEquals(2, transformer.getInjectMethodCount())
    }
    
    @Test
    fun testTransformerWithEmptyMixinInfo() {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        val transformer = MixinTransformer(classWriter, mixinInfo)
        
        assertEquals(0, transformer.getInterfaceCount())
        assertEquals(0, transformer.getNewFieldCount())
        assertEquals(0, transformer.getNewMethodCount())
        assertEquals(0, transformer.getShadowFieldCount())
        assertEquals(0, transformer.getShadowMethodCount())
        assertEquals(0, transformer.getOverwriteMethodCount())
        assertEquals(0, transformer.getInjectMethodCount())
    }
} 
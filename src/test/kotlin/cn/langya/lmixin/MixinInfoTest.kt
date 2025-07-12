package cn.langya.lmixin

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.Opcodes

class MixinInfoTest {
    
    @Test
    fun testDefaultConstructor() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertNotNull(mixinInfo)
        assertEquals("test.MixinClass", mixinInfo.mixinClassName)
        assertEquals("test.TargetClass", mixinInfo.targetClassName)
        assertTrue(mixinInfo.interfaces.isEmpty())
        assertTrue(mixinInfo.shadowFields.isEmpty())
        assertTrue(mixinInfo.shadowMethods.isEmpty())
        assertTrue(mixinInfo.overwriteMethods.isEmpty())
        assertTrue(mixinInfo.injectMethods.isEmpty())
        assertTrue(mixinInfo.newFields.isEmpty())
        assertTrue(mixinInfo.newMethods.isEmpty())
    }
    
    @Test
    fun testFullConstructor() {
        val interfaces = listOf("test.Interface1", "test.Interface2")
        val mixinInfo = MixinInfo(
            "test.MixinClass",
            "test.TargetClass",
            interfaces,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList()
        )
        
        assertEquals("test.MixinClass", mixinInfo.mixinClassName)
        assertEquals("test.TargetClass", mixinInfo.targetClassName)
        assertEquals(2, mixinInfo.interfaces.size)
        assertTrue(mixinInfo.interfaces.contains("test.Interface1"))
        assertTrue(mixinInfo.interfaces.contains("test.Interface2"))
    }
    
    @Test
    fun testGetInterfaceCount() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertEquals(0, mixinInfo.getInterfaceCount())
        
        val mixinInfoWithInterfaces = MixinInfo(
            "test.MixinClass",
            "test.TargetClass",
            listOf("test.Interface1", "test.Interface2", "test.Interface3")
        )
        assertEquals(3, mixinInfoWithInterfaces.getInterfaceCount())
    }
    
    @Test
    fun testGetShadowFieldCount() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertEquals(0, mixinInfo.getShadowFieldCount())
    }
    
    @Test
    fun testGetShadowMethodCount() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertEquals(0, mixinInfo.getShadowMethodCount())
    }
    
    @Test
    fun testGetOverwriteMethodCount() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertEquals(0, mixinInfo.getOverwriteMethodCount())
    }
    
    @Test
    fun testGetInjectMethodCount() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertEquals(0, mixinInfo.getInjectMethodCount())
    }
    
    @Test
    fun testGetNewFieldCount() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertEquals(0, mixinInfo.getNewFieldCount())
    }
    
    @Test
    fun testGetNewMethodCount() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertEquals(0, mixinInfo.getNewMethodCount())
    }
    
    @Test
    fun testHasInterfaces() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertFalse(mixinInfo.hasInterfaces())
        
        val mixinInfoWithInterfaces = MixinInfo(
            "test.MixinClass",
            "test.TargetClass",
            listOf("test.Interface1")
        )
        assertTrue(mixinInfoWithInterfaces.hasInterfaces())
    }
    
    @Test
    fun testHasShadowFields() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertFalse(mixinInfo.hasShadowFields())
    }
    
    @Test
    fun testHasShadowMethods() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertFalse(mixinInfo.hasShadowMethods())
    }
    
    @Test
    fun testHasOverwriteMethods() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertFalse(mixinInfo.hasOverwriteMethods())
    }
    
    @Test
    fun testHasInjectMethods() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertFalse(mixinInfo.hasInjectMethods())
    }
    
    @Test
    fun testHasNewFields() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertFalse(mixinInfo.hasNewFields())
    }
    
    @Test
    fun testHasNewMethods() {
        val mixinInfo = MixinInfo("test.MixinClass", "test.TargetClass")
        assertFalse(mixinInfo.hasNewMethods())
    }
    
    @Test
    fun testShadowField() {
        val fieldNode = FieldNode(Opcodes.ACC_PUBLIC, "testField", "Ljava/lang/String;", null, null)
        val shadowField = MixinInfo.ShadowField("mixinField", "targetField", fieldNode)
        
        assertEquals("mixinField", shadowField.mixinFieldName)
        assertEquals("targetField", shadowField.targetFieldName)
        assertEquals(fieldNode, shadowField.fieldNode)
        
        assertTrue(shadowField.matchesFieldName("mixinField"))
        assertTrue(shadowField.matchesFieldName("targetField"))
        assertFalse(shadowField.matchesFieldName("otherField"))
    }
    
    @Test
    fun testShadowMethod() {
        val methodNode = MethodNode(Opcodes.ACC_PUBLIC, "testMethod", "()V", null, null)
        val shadowMethod = MixinInfo.ShadowMethod(
            "mixinMethod",
            "()V",
            "targetMethod",
            "()V",
            methodNode
        )
        
        assertEquals("mixinMethod", shadowMethod.mixinMethodName)
        assertEquals("()V", shadowMethod.mixinMethodDesc)
        assertEquals("targetMethod", shadowMethod.targetMethodName)
        assertEquals("()V", shadowMethod.targetMethodDesc)
        assertEquals(methodNode, shadowMethod.methodNode)
        
        assertTrue(shadowMethod.matchesMethod("mixinMethod", "()V"))
        assertTrue(shadowMethod.matchesMethod("targetMethod", "()V"))
        assertFalse(shadowMethod.matchesMethod("otherMethod", "()V"))
        assertFalse(shadowMethod.matchesMethod("mixinMethod", "(I)V"))
    }
    
    @Test
    fun testInjectMethod() {
        val methodNode = MethodNode(Opcodes.ACC_PUBLIC, "injectMethod", "()V", null, null)
        val injectMethod = MixinInfo.InjectMethod(
            "injectMethod",
            "()V",
            "targetMethod",
            "()V",
            At.HEAD,
            "injectionPoint",
            0,
            methodNode
        )
        
        assertEquals("injectMethod", injectMethod.mixinMethodName)
        assertEquals("()V", injectMethod.mixinMethodDesc)
        assertEquals("targetMethod", injectMethod.targetMethodName)
        assertEquals("()V", injectMethod.targetMethodDesc)
        assertEquals(At.HEAD, injectMethod.at)
        assertEquals("injectionPoint", injectMethod.injectionPointMethodName)
        assertEquals(0, injectMethod.ordinal)
        assertEquals(methodNode, injectMethod.methodNode)
        
        assertTrue(injectMethod.matchesMethod("injectMethod", "()V"))
        assertFalse(injectMethod.matchesMethod("otherMethod", "()V"))
        assertFalse(injectMethod.matchesMethod("injectMethod", "(I)V"))
    }
    
    @Test
    fun testOverwriteMethod() {
        val methodNode = MethodNode(Opcodes.ACC_PUBLIC, "overwriteMethod", "()V", null, null)
        val overwriteMethod = MixinInfo.OverwriteMethod(
            "overwriteMethod",
            "()V",
            "targetMethod",
            "()V",
            methodNode
        )
        
        assertEquals("overwriteMethod", overwriteMethod.mixinMethodName)
        assertEquals("()V", overwriteMethod.mixinMethodDesc)
        assertEquals("targetMethod", overwriteMethod.targetMethodName)
        assertEquals("()V", overwriteMethod.targetMethodDesc)
        assertEquals(methodNode, overwriteMethod.methodNode)
        
        assertTrue(overwriteMethod.matchesMethod("overwriteMethod", "()V"))
        assertTrue(overwriteMethod.matchesMethod("targetMethod", "()V"))
        assertFalse(overwriteMethod.matchesMethod("otherMethod", "()V"))
        assertFalse(overwriteMethod.matchesMethod("overwriteMethod", "(I)V"))
    }
} 
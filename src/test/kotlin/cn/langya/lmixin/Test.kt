package cn.langya.lmixin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.lang.instrument.Instrumentation
import java.lang.reflect.Method
import java.nio.file.Files
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.tree.ClassNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse

class MappingParserTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testMappingParser() {
        val srgContent = """
            CL: a/b/C Cpp
            FD: a/b/C/d e
            MD: a/b/C/f (Ljava/lang/String;)V a/b/C/g (Ljava/lang/String;)V
        """.trimIndent()
        val srgFile = tempDir.resolve("test.srg").toFile()
        srgFile.writeText(srgContent)

        val parser = MappingParser(srgFile.absolutePath)

        assertEquals("Cpp", parser.mapClassName("a/b/C"))
        assertEquals("e", parser.mapFieldName("a/b/C", "d"))
        assertEquals("g", parser.mapMethodName("a/b/C", "f", "(Ljava/lang/String;)V"))
    }
}

class TargetClass {
    fun originalMethod(): String {
        return "Original"
    }

    fun methodToInject(): String {
        return "OriginalInject"
    }
}

@Mixin(TargetClass::class)
class SimpleMixin {
    @Overwrite
    fun originalMethod(): String {
        return "Overwritten"
    }

    @Inject(method = "methodToInject", at = At.HEAD)
    fun injectHead() {
        println("Inject at head")
    }

    @Inject(method = "methodToInject", at = At.TAIL)
    fun injectTail() {
        println("Inject at tail")
    }

    @Inject(method = "originalMethod", at = At.BEFORE, ordinal = 1)
    fun injectBefore() {
        println("Inject before originalMethod")
    }

    @Inject(method = "originalMethod", at = At.AFTER, ordinal = 0)
    fun injectAfter() {
        println("Inject after method call in originalMethod")
    }
}

@TestInstance(Lifecycle.PER_CLASS)
class MixinTransformerTest {
    @Test
    fun testOverwriteMethod() {
        val inst = AgentTest.MockInstrumentation()
        LMMixinAgent.init(inst, "")

        val originalTargetClassBytes = getBytesFromClass(TargetClass::class.java)
        val transformedBytes = inst.transformers.first().transform(
            TargetClass::class.java.classLoader,
            TargetClass::class.java.canonicalName.replace('.', '/'),
            TargetClass::class.java, null, originalTargetClassBytes
        )

        val customClassLoader = object : ClassLoader(this.javaClass.classLoader) {
            override fun findClass(name: String): Class<*> {
                return if (name == TargetClass::class.java.canonicalName) {
                     defineClass(name, transformedBytes, 0, transformedBytes.size)
                 } else {
                    super.findClass(name)
                }
            }
        }

        val transformedClass = customClassLoader.loadClass(TargetClass::class.java.canonicalName)
        val instance = transformedClass.getDeclaredConstructor().newInstance()
        val method = transformedClass.getMethod("originalMethod")
        val result = method.invoke(instance) as String

        assertEquals("Overwritten", result)
    }

    @Test
    fun testInjectMethods() {
        val inst = AgentTest.MockInstrumentation()
        LMMixinAgent.init(inst, "")

        val originalTargetClassBytes = getBytesFromClass(TargetClass::class.java)
        val transformedBytes = inst.transformers.first().transform(
            TargetClass::class.java.classLoader,
            TargetClass::class.java.canonicalName.replace('.', '/'),
            TargetClass::class.java, null, originalTargetClassBytes
        )

        val customClassLoader = object : ClassLoader(this.javaClass.classLoader) {
            override fun findClass(name: String): Class<*> {
                return if (name == TargetClass::class.java.canonicalName) {
                    defineClass(name, transformedBytes, 0, transformedBytes.size)
                } else {
                    super.findClass(name)
                }
            }
        }

        val transformedClass = customClassLoader.loadClass(TargetClass::class.java.canonicalName)
        val instance = transformedClass.getDeclaredConstructor().newInstance()

        val methodToInject = transformedClass.getMethod("methodToInject")
        methodToInject.invoke(instance)

        val originalMethod = transformedClass.getMethod("originalMethod")
        originalMethod.invoke(instance)
    }

    private fun getBytesFromClass(clazz: Class<*>): ByteArray {
        val resourceStream = clazz.classLoader.getResourceAsStream(clazz.name.replace('.', '/') + ".class")
        return resourceStream?.use { it.readBytes() } ?: ByteArray(0)
    }
}



class AgentTest {

    class MockInstrumentation : Instrumentation {
        val transformers = mutableListOf<java.lang.instrument.ClassFileTransformer>()

        override fun addTransformer(transformer: java.lang.instrument.ClassFileTransformer, canRetransform: Boolean) {
            transformers.add(transformer)
        }

        override fun addTransformer(transformer: java.lang.instrument.ClassFileTransformer) {
            transformers.add(transformer)
        }

        override fun removeTransformer(transformer: java.lang.instrument.ClassFileTransformer): Boolean {
            return transformers.remove(transformer)
        }

        override fun isRetransformClassesSupported(): Boolean = true
        override fun isRedefineClassesSupported(): Boolean = true
        override fun isNativeMethodPrefixSupported(): Boolean = false
        override fun appendToBootstrapClassLoaderSearch(jarfile: java.util.jar.JarFile?) {}
        override fun appendToSystemClassLoaderSearch(jarfile: java.util.jar.JarFile?) {}
        override fun getAllLoadedClasses(): Array<Class<*>> = arrayOf()
        override fun getInitiatedClasses(loader: ClassLoader?): Array<Class<*>> = arrayOf()
        override fun getObjectSize(obj: Any?): Long = 0L
        override fun redefineClasses(vararg classDefinition: java.lang.instrument.ClassDefinition?) {}
        override fun retransformClasses(vararg classes: Class<*>?) {}
        override fun setNativeMethodPrefix(transformer: java.lang.instrument.ClassFileTransformer?, prefix: String?) {}
        override fun isModifiableClass(clazz: Class<*>?): Boolean = true
    }

    @Test
    fun testAgentLoadAndMixinApplication() {
        val inst = MockInstrumentation()
        LMMixinAgent.init(inst)

        val originalTargetClassBytes = getBytesFromClass(TargetClass::class.java)
        val transformedBytes = inst.transformers.first().transform(
            TargetClass::class.java.classLoader,
            TargetClass::class.java.canonicalName.replace('.', '/'),
            TargetClass::class.java, null, originalTargetClassBytes
        )

        val customClassLoader = object : ClassLoader(this.javaClass.classLoader) {
            override fun findClass(name: String): Class<*> {
                return if (name == TargetClass::class.java.canonicalName) {
                    defineClass(name, transformedBytes, 0, transformedBytes.size)
                } else {
                    super.findClass(name)
                }
            }
        }

        val transformedClass = customClassLoader.loadClass(TargetClass::class.java.canonicalName)
        val instance = transformedClass.getDeclaredConstructor().newInstance()
        val method = transformedClass.getMethod("originalMethod")
        val result = method.invoke(instance) as String

        assertEquals("Overwritten", result)
    }

    private fun getBytesFromClass(clazz: Class<*>): ByteArray {
        val resourceStream = clazz.classLoader.getResourceAsStream(clazz.name.replace('.', '/') + ".class")
        return resourceStream?.use { it.readBytes() } ?: ByteArray(0)
    }
}

/**
 * @Shadow注解测试示例
 */
class ShadowTest {
    
    // 测试目标类
    class ShadowTargetClass {
        private val privateField = "private value"
        protected val protectedField = "protected value"
        
        private fun privateMethod(): String = "private method"
        protected fun protectedMethod(): String = "protected method"
        
        fun publicMethod(): String = "public method"
    }
    
    // 基本@Shadow注解测试
    @Mixin(ShadowTargetClass::class)
    class BasicShadowMixin {
        
        // 基本@Shadow字段 - 引用目标类的字段
        @Shadow
        private val privateField: String = ""
        
        // 带重映射的@Shadow字段 - 指定目标字段名
        @Shadow("protectedField")
        private val myProtectedField: String = ""
        
        // 禁用重映射的@Shadow字段
        @Shadow(remap = false)
        private val directField: String = ""
        
        // 基本@Shadow方法 - 引用目标类的方法
        @Shadow
        private fun privateMethod(): String = ""
        
        // 带重映射的@Shadow方法 - 指定目标方法名
        @Shadow("protectedMethod")
        private fun myProtectedMethod(): String = ""
        
        // 禁用重映射的@Shadow方法
        @Shadow(remap = false)
        private fun directMethod(): String = ""
        
        // 使用@Shadow字段和方法的示例方法
        fun exampleUsage() {
            // 访问@Shadow字段
            println("Private field: $privateField")
            println("Protected field: $myProtectedField")
            println("Direct field: $directField")
            
            // 调用@Shadow方法
            println("Private method: ${privateMethod()}")
            println("Protected method: ${myProtectedMethod()}")
            println("Direct method: ${directMethod()}")
        }
        
        // 使用@Overwrite重写目标类的方法
        @Overwrite
        fun publicMethod(): String {
            // 在重写的方法中使用@Shadow成员
            val originalResult = privateMethod()
            return "Modified: $originalResult"
        }
        
        // 使用@Inject注入代码
        @Inject(method = "publicMethod", at = At.HEAD)
        fun injectAtHead() {
            println("Injected at head of publicMethod")
        }
        
        @Inject(method = "publicMethod", at = At.TAIL)
        fun injectAtTail() {
            println("Injected at tail of publicMethod")
        }
    }
    
    // 高级@Shadow注解测试
    @Mixin(ShadowTargetClass::class)
    class AdvancedShadowMixin {
        
        // 引用不同签名的字段
        @Shadow("fieldWithDifferentName")
        private val myField: String = ""
        
        // 引用不同签名的方法
        @Shadow("methodWithDifferentName")
        private fun myMethod(param: Int): String = ""
        
        // 在构造函数中使用@Shadow
        @Inject(method = "<init>", at = At.TAIL)
        fun constructorInjection() {
            println("Constructor injection using shadow members")
            // 可以在这里使用@Shadow字段和方法
        }
        
        // 条件性使用@Shadow成员
        fun conditionalUsage(condition: Boolean) {
            if (condition) {
                // 使用@Shadow成员
                val value = myField
                val result = myMethod(42)
                println("Conditional usage: $value, $result")
            }
        }
    }
    
    // 最佳实践测试
    @Mixin(ShadowTargetClass::class)
    class BestPracticeShadowMixin {
        
        // 好的做法：提供默认值，使用private修饰符
        @Shadow
        private val targetField: String = ""
        
        // 好的做法：指定目标成员名
        @Shadow("specificFieldName")
        private val myField: String = ""
        
        // 好的做法：禁用重映射（当目标成员名已知时）
        @Shadow(remap = false)
        private val knownField: String = ""
        
        // 好的做法：在方法中使用@Shadow成员
        fun demonstrateUsage() {
            // 使用@Shadow字段
            val fieldValue = targetField
            
            // 使用@Shadow方法
            val methodResult = targetMethod()
            
            // 处理结果
            println("Field: $fieldValue, Method: $methodResult")
        }
        
        @Shadow
        private fun targetMethod(): String = ""
    }
    
    @Test
    fun testShadowAnnotation() {
        val inst = AgentTest.MockInstrumentation()
        LMMixinAgent.init(inst, "")
        
        // 测试@Shadow注解的基本功能
        val originalTargetClassBytes = getBytesFromClass(ShadowTargetClass::class.java)
        val transformedBytes = inst.transformers.first().transform(
            ShadowTargetClass::class.java.classLoader,
            ShadowTargetClass::class.java.canonicalName.replace('.', '/'),
            ShadowTargetClass::class.java, null, originalTargetClassBytes
        )
        
        val customClassLoader = object : ClassLoader(this.javaClass.classLoader) {
            override fun findClass(name: String): Class<*> {
                return if (name == ShadowTargetClass::class.java.canonicalName) {
                    defineClass(name, transformedBytes, 0, transformedBytes.size)
                } else {
                    super.findClass(name)
                }
            }
        }
        
        val transformedClass = customClassLoader.loadClass(ShadowTargetClass::class.java.canonicalName)
        val instance = transformedClass.getDeclaredConstructor().newInstance()
        
        // 测试重写的方法
        val method = transformedClass.getMethod("publicMethod")
        val result = method.invoke(instance) as String
        
        // 验证结果包含@Shadow方法调用的结果
        assertEquals(true, result.contains("Modified:"))
        assertEquals(true, result.contains("private method"))
    }
    
    private fun getBytesFromClass(clazz: Class<*>): ByteArray {
        val resourceStream = clazz.classLoader.getResourceAsStream(clazz.name.replace('.', '/') + ".class")
        return resourceStream?.use { it.readBytes() } ?: ByteArray(0)
    }
}

class GetBytesTest {
    
    @Test
    fun testGetBytesMethod() {
        val inst = AgentTest.MockInstrumentation()
        LMMixinAgent.init(inst, "")
        
        // 测试Java API
        val bytesMap = LMMixinJavaAPI.getBytes()
        assertNotNull(bytesMap)
        
        // 测试获取指定类字节码
        val targetClassBytes = LMMixinJavaAPI.getClassBytes("cn/langya/lmixin/TargetClass")
        assertNotNull(targetClassBytes)
        assertTrue(targetClassBytes!!.isNotEmpty())
        
        // 测试获取处理过的类名列表
        val classNames = LMMixinJavaAPI.getProcessedClassNames()
        assertNotNull(classNames)
        assertTrue(classNames.isNotEmpty())
        
        // 测试检查类是否已处理
        assertTrue(LMMixinJavaAPI.isClassProcessed("cn/langya/lmixin/TargetClass"))
        assertFalse(LMMixinJavaAPI.isClassProcessed("cn/langya/lmixin/NonExistentClass"))
        
        // 测试获取处理过的类数量
        val count = LMMixinJavaAPI.getProcessedClassCount()
        assertTrue(count > 0)
    }
    
    @Test
    fun testGetBytesFromMixinProcessor() {
        val inst = AgentTest.MockInstrumentation()
        LMMixinAgent.init(inst, "")
        
        // 通过反射获取mixinProcessor实例来测试getBytes方法
        val agent = inst.transformers.first() as LMMixinAgent
        val mixinProcessorField = LMMixinAgent::class.java.getDeclaredField("mixinProcessor")
        mixinProcessorField.isAccessible = true
        val mixinProcessor = mixinProcessorField.get(agent) as MixinProcessor
        
        val bytesMap = mixinProcessor.getBytes()
        assertNotNull(bytesMap)
        assertTrue(bytesMap.isNotEmpty())
        
        // 验证包含目标类
        assertTrue(bytesMap.containsKey("cn/langya/lmixin/TargetClass"))
        assertTrue(bytesMap.containsKey("cn/langya/lmixin/SimpleMixin"))
    }
    
    @Test
    fun testGetBytesContent() {
        val inst = AgentTest.MockInstrumentation()
        LMMixinAgent.init(inst, "")
        
        val bytesMap = LMMixinJavaAPI.getBytes()
        
        // 验证字节码内容
        bytesMap.forEach { (className, bytes) ->
            assertTrue(bytes.isNotEmpty(), "Class $className should have non-empty bytecode")
            
            // 验证字节码以0xCAFEBABE开头（Java类文件魔数）
            assertEquals(0xCA.toByte(), bytes[0], "Class $className should start with Java class file magic number")
            assertEquals(0xFE.toByte(), bytes[1], "Class $className should start with Java class file magic number")
            assertEquals(0xBA.toByte(), bytes[2], "Class $className should start with Java class file magic number")
            assertEquals(0xBE.toByte(), bytes[3], "Class $className should start with Java class file magic number")
        }
    }
}
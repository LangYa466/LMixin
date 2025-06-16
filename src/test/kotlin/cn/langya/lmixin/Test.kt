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
}

fun createMockMixinInfo(mixinClassName: String, targetClassName: String): MixinInfo {
    val mixinClassReader = ClassReader(SimpleMixin::class.java.canonicalName.replace('.', '/'))
    val mixinClassNode = ClassNode()
    mixinClassReader.accept(mixinClassNode, 0)

    val overwriteMethodNode = mixinClassNode.methods.find { it.name == "originalMethod" && it.desc == "()Ljava/lang/String;" }!!
    return MixinInfo(
        mixinClassName = mixinClassName,
        targetClassName = targetClassName,
        overwriteMethods = listOf(
            MixinInfo.OverwriteMethod(
                mixinMethodName = "originalMethod",
                mixinMethodDesc = "()Ljava/lang/String;",
                targetMethodName = "originalMethod",
                targetMethodDesc = "()Ljava/lang/String;",
                methodNode = overwriteMethodNode
            )
        )
    )
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

    private fun getBytesFromClass(clazz: Class<*>): ByteArray {
        val resourceStream = clazz.classLoader.getResourceAsStream(clazz.name.replace('.', '/') + ".class")
        return resourceStream?.use { it.readBytes() } ?: ByteArray(0)
    }
}

// 搞笑奇异
// fun createMockMixinInfo(mixinClassName: String, targetClassName: String): MixinInfo {
//    val mixinClassReader = ClassReader(SimpleMixin::class.java.canonicalName.replace('.', '/'))
//    val mixinClassNode = ClassNode()
//    mixinClassReader.accept(mixinClassNode, 0)
//
//    val overwriteMethodNode = mixinClassNode.methods.find { it.name == "originalMethod" && it.desc == "()Ljava/lang/String;" }!!
//    return MixinInfo(
//        mixinClassName = mixinClassName,
//        targetClassName = targetClassName,
//        overwriteMethods = listOf(
//            MixinInfo.OverwriteMethod(
//                mixinMethodName = "originalMethod",
//                mixinMethodDesc = "()Ljava/lang/String;",
//                targetMethodName = "originalMethod",
//                targetMethodDesc = "()Ljava/lang/String;",
//                methodNode = overwriteMethodNode
//            )
//        )
//    )
// }

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
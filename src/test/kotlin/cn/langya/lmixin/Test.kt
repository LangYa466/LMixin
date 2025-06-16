package cn.langya.lmixin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
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

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testOverwriteMethod() {
        val targetClassName = "TargetClass"
        val mixinClassName = "SimpleMixin"
        val mixinInfo = createMockMixinInfo(mixinClassName, targetClassName)

        val targetClassBytes = getBytesFromClass(TargetClass::class.java)
        val classReader = ClassReader(targetClassBytes)
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)

        val mixinTransformer = MixinTransformer(Opcodes.ASM9, classWriter, mixinInfo)
        classReader.accept(mixinTransformer, 0)

        val transformedBytes = classWriter.toByteArray()

        val customClassLoader = object : ClassLoader(this.javaClass.classLoader) {
            override fun findClass(name: String): Class<*> {
                return if (name == targetClassName) {
                    defineClass(name, transformedBytes, 0, transformedBytes.size)
                } else {
                    super.findClass(name)
                }
            }
        }

        val transformedClass = customClassLoader.loadClass(targetClassName)
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
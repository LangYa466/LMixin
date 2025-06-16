package cn.langya.lmixin

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.io.InputStream
import java.net.URLClassLoader
import java.util.jar.JarFile

class MixinProcessor(private val mappingParser: MappingParser) {

    val mixinMap = mutableMapOf<String, MutableList<MixinInfo>>()

    fun processMixins() {
        val classLoader = ClassLoader.getSystemClassLoader()
        if (classLoader is URLClassLoader) {
            classLoader.urLs.forEach { url ->
                when (url.protocol) {
                    "file" -> {
                        val file = File(url.toURI())
                        if (file.isDirectory) {
                            file.walkTopDown()
                                .filter { it.isFile && it.extension == "class" }
                                .forEach { processClassFile(it.inputStream()) }
                        } else if (file.isFile && file.extension == "jar") {
                            processJarFile(JarFile(file))
                        }
                    }
                }
            }
        }
    }

    private fun processJarFile(jarFile: JarFile) {
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.endsWith(".class")) {
                jarFile.getInputStream(entry).use { processClassFile(it) }
            }
        }
    }

    private fun processClassFile(inputStream: InputStream) {
        val classReader = ClassReader(inputStream)
        val classNode = ClassNode()
        classReader.accept(classNode, 0)

        if (classNode.visibleAnnotations?.any { it.desc == "Lcn/langya/lmixin/Mixin;" } == true) {
            parseMixinClass(classNode)
        }
    }

    private fun parseMixinClass(mixinClassNode: ClassNode) {
        val mixinAnnotation = mixinClassNode.visibleAnnotations.find { it.desc == "Lcn/langya/lmixin/Mixin;" }
        val targetClassType = mixinAnnotation?.values?.get(1) as? Type ?: return
        val targetClassName = mappingParser.mapClassName(targetClassType.internalName)

        val interfaces = mixinClassNode.interfaces?.map { it } ?: emptyList()

        val shadowFields = mutableListOf<MixinInfo.ShadowField>()
        val shadowMethods = mutableListOf<MixinInfo.ShadowMethod>()
        val overwriteMethods = mutableListOf<MixinInfo.OverwriteMethod>()
        val newFields = mutableListOf<FieldNode>()
        val newMethods = mutableListOf<MethodNode>()

        mixinClassNode.fields.forEach { fieldNode ->
            newFields.add(fieldNode)
        }

        mixinClassNode.methods.forEach { methodNode ->
            when {
                methodNode.visibleAnnotations?.any { it.desc == "Lcn/langya/lmixin/Overwrite;" } == true -> {
                    overwriteMethods.add(
                        MixinInfo.OverwriteMethod(
                            methodNode.name,
                            methodNode.desc,
                            methodNode.name,
                            methodNode.desc,
                            methodNode
                        )
                    )
                }

                else -> {
                    newMethods.add(methodNode)
                }
            }
        }

        val mixinInfo = MixinInfo(
            mixinClassNode.name,
            targetClassName,
            interfaces,
            shadowFields,
            shadowMethods,
            overwriteMethods,
            newFields,
            newMethods
        )

        mixinMap.computeIfAbsent(targetClassName) { mutableListOf() }.add(mixinInfo)
    }
}
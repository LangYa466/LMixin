package cn.langya.lmixin

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.io.InputStream
import java.net.URLClassLoader
import java.util.jar.JarFile

class MixinProcessor(private val mappingParser: MappingParser) {

    val mixinMap = mutableMapOf<String, MutableList<MixinInfo>>()
    
    // 存储所有处理过的类的字节码
    private val processedClasses = mutableMapOf<String, ByteArray>()

    /**
     * 默认构造函数，使用默认的MappingParser
     */
    constructor() : this(MappingParser())
    
    /**
     * 使用指定映射文件路径的构造函数
     */
    constructor(srgFilePath: String) : this(MappingParser(srgFilePath))

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
                                .forEach {
                                    it.inputStream().use { stream -> processClassFile(stream) }
                                }
                        } else if (file.isFile && file.extension == "jar") {
                            processJarFile(JarFile(file))
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 处理指定目录中的类文件
     * @param directory 目录路径
     */
    fun processMixinsFromDirectory(directory: String) {
        val dir = File(directory)
        if (!dir.exists() || !dir.isDirectory) {
            println("Warning: Directory $directory does not exist or is not a directory")
            return
        }
        
        dir.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach {
                it.inputStream().use { stream -> processClassFile(stream) }
            }
    }
    
    /**
     * 处理指定JAR文件中的类文件
     * @param jarFilePath JAR文件路径
     */
    fun processMixinsFromJar(jarFilePath: String) {
        val jarFile = File(jarFilePath)
        if (!jarFile.exists() || !jarFile.isFile) {
            println("Warning: JAR file $jarFilePath does not exist or is not a file")
            return
        }
        
        processJarFile(JarFile(jarFile))
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
        val bytes = inputStream.readBytes()
        val classReader = ClassReader(bytes)
        val classNode = ClassNode()
        classReader.accept(classNode, ClassReader.SKIP_CODE)

        if (classNode.visibleAnnotations?.any { it.desc == "Lcn/langya/lmixin/Mixin;" } == true) {
            val fullClassReader = ClassReader(bytes)
            val fullClassNode = ClassNode()
            fullClassReader.accept(fullClassNode, 0)
            parseMixinClass(fullClassNode)
        }
        
        // 保存处理过的类字节码
        processedClasses[classNode.name] = bytes
    }

    private fun <T> getAnnotationValue(node: AnnotationNode, key: String): T? {
        val values = node.values ?: return null
        val keyIndex = values.indexOf(key)
        if (keyIndex != -1 && keyIndex + 1 < values.size) {
            @Suppress("UNCHECKED_CAST")
            return values[keyIndex + 1] as? T
        }
        return null
    }

    private fun parseMixinClass(mixinClassNode: ClassNode) {
        val mixinAnnotation = mixinClassNode.visibleAnnotations.find { it.desc == "Lcn/langya/lmixin/Mixin;" }
        val targetClassType = getAnnotationValue<Type>(mixinAnnotation!!, "value") ?: return
        val targetClassName = mappingParser.mapClassName(targetClassType.internalName)

        val interfaces = mixinClassNode.interfaces?.map { it } ?: emptyList()

        val shadowFields = mutableListOf<MixinInfo.ShadowField>()
        val shadowMethods = mutableListOf<MixinInfo.ShadowMethod>()
        val overwriteMethods = mutableListOf<MixinInfo.OverwriteMethod>()
        val injectMethods = mutableListOf<MixinInfo.InjectMethod>()
        val newFields = mutableListOf<FieldNode>()
        val newMethods = mutableListOf<MethodNode>()

        mixinClassNode.fields.forEach { fieldNode ->
            val shadowAnnotation = fieldNode.visibleAnnotations?.find { it.desc == "Lcn/langya/lmixin/Shadow;" }
            if (shadowAnnotation != null) {
                val targetFieldName = getAnnotationValue<String>(shadowAnnotation, "value") ?: fieldNode.name
                val remap = getAnnotationValue<Boolean>(shadowAnnotation, "remap") ?: true
                
                shadowFields.add(
                    MixinInfo.ShadowField(
                        mixinFieldName = fieldNode.name,
                        targetFieldName = if (remap) mappingParser.mapFieldName(targetClassName, targetFieldName) else targetFieldName,
                        fieldNode = fieldNode
                    )
                )
            } else {
                newFields.add(fieldNode)
            }
        }

        mixinClassNode.methods.forEach { methodNode ->
            if (methodNode.name == "<init>" || methodNode.name == "<clinit>") {
                return@forEach
            }

            when {
                methodNode.visibleAnnotations?.any { it.desc == "Lcn/langya/lmixin/Shadow;" } == true -> {
                    val shadowAnnotation = methodNode.visibleAnnotations.find { it.desc == "Lcn/langya/lmixin/Shadow;" }!!
                    val targetMethodName = getAnnotationValue<String>(shadowAnnotation, "value") ?: methodNode.name
                    val remap = getAnnotationValue<Boolean>(shadowAnnotation, "remap") ?: true
                    
                    shadowMethods.add(
                        MixinInfo.ShadowMethod(
                            methodNode.name,
                            methodNode.desc,
                            if (remap) mappingParser.mapMethodName(targetClassName, targetMethodName, methodNode.desc) else targetMethodName,
                            if (remap) mappingParser.mapMethodDesc(methodNode.desc) else methodNode.desc,
                            methodNode
                        )
                    )
                }

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

                methodNode.visibleAnnotations?.any { it.desc == "Lcn/langya/lmixin/Inject;" } == true -> {
                    val injectAnnotation =
                        methodNode.visibleAnnotations.find { it.desc == "Lcn/langya/lmixin/Inject;" }!!

                    val targetMethodName = getAnnotationValue<String>(injectAnnotation, "method") ?: 
                                         getAnnotationValue<String>(injectAnnotation, "target") ?: ""
                    if (targetMethodName.isEmpty()) {
                        println("Warning: @Inject in ${mixinClassNode.name}.${methodNode.name} is missing 'method' property. Skipping.")
                        return@forEach
                    }

                    val atValue = getAnnotationValue<Array<*>>(injectAnnotation, "at")
                    val at = At.valueOf((atValue?.get(1) as? String) ?: "HEAD")

                    val injectionPoint = getAnnotationValue<String>(injectAnnotation, "injectionPoint") ?: ""
                    val ordinal = getAnnotationValue<Int>(injectAnnotation, "ordinal") ?: 0

                    injectMethods.add(
                        MixinInfo.InjectMethod(
                            methodNode.name,
                            methodNode.desc,
                            targetMethodName,
                            methodNode.desc,
                            at,
                            injectionPoint,
                            ordinal,
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
            injectMethods,
            newFields,
            newMethods
        )

        mixinMap.computeIfAbsent(targetClassName) { mutableListOf() }.add(mixinInfo)
    }
    
    /**
     * 获取所有处理过的类的字节码映射
     * @return Map<String, ByteArray> 类名到字节码的映射
     */
    fun getBytes(): Map<String, ByteArray> {
        return processedClasses.toMap()
    }
    
    /**
     * 获取指定类的字节码
     * @param className 类名
     * @return ByteArray? 类的字节码，如果不存在则返回null
     */
    fun getClassBytes(className: String): ByteArray? {
        return processedClasses[className]
    }
    
    /**
     * 获取所有处理过的类名列表
     * @return List<String> 类名列表
     */
    fun getProcessedClassNames(): List<String> {
        return processedClasses.keys.toList()
    }
    
    /**
     * 检查指定类是否已被处理
     * @param className 类名
     * @return Boolean 是否已处理
     */
    fun isClassProcessed(className: String): Boolean {
        return processedClasses.containsKey(className)
    }
    
    /**
     * 获取处理过的类数量
     * @return Int 类数量
     */
    fun getProcessedClassCount(): Int {
        return processedClasses.size
    }
    
    /**
     * 获取mixin映射数量
     * @return Int mixin映射数量
     */
    fun getMixinMapSize(): Int {
        return mixinMap.size
    }
    
    /**
     * 获取指定目标类的mixin信息列表
     * @param targetClassName 目标类名
     * @return List<MixinInfo>? mixin信息列表，如果不存在则返回null
     */
    fun getMixinsForClass(targetClassName: String): List<MixinInfo>? {
        return mixinMap[targetClassName]
    }
    
    /**
     * 检查指定目标类是否有mixin
     * @param targetClassName 目标类名
     * @return Boolean 是否有mixin
     */
    fun hasMixinsForClass(targetClassName: String): Boolean {
        return mixinMap.containsKey(targetClassName) && mixinMap[targetClassName]?.isNotEmpty() == true
    }
}
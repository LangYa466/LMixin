package cn.langya.lmixin

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

class LMMixinAgent : ClassFileTransformer {

    internal lateinit var mappingParser: MappingParser
    internal lateinit var mixinProcessor: MixinProcessor

    companion object {
        private var currentAgent: LMMixinAgent? = null
        
        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            init(inst, agentArgs ?: "mappings.srg")
        }

        @JvmStatic
        fun init(inst: Instrumentation, srgFilePath: String = "mappings.srg") {
            val agent = LMMixinAgent()
            agent.mappingParser = MappingParser(srgFilePath)
            agent.mixinProcessor = MixinProcessor(agent.mappingParser)
            agent.mixinProcessor.processMixins()
            inst.addTransformer(agent)
            
            // 保存当前agent实例的引用
            currentAgent = agent
            
            LMMixinAPI.init(inst, srgFilePath)
        }
        
        /**
         * 获取当前agent实例
         * @return LMMixinAgent? 当前agent实例，如果未初始化则返回null
         */
        @JvmStatic
        fun getCurrentAgent(): LMMixinAgent? {
            return currentAgent
        }
        
        /**
         * 获取所有处理过的类的字节码映射
         * @return Map<String, ByteArray> 类名到字节码的映射
         */
        @JvmStatic
        fun getProcessedClassesBytes(): Map<String, ByteArray> {
            return currentAgent?.getProcessedClassesBytesInstance() ?: emptyMap()
        }
    }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray? {
        val transformedClassName = mappingParser.mapClassName(className)
        val mixinsForClass = mixinProcessor.mixinMap[transformedClassName]

        if (mixinsForClass != null && mixinsForClass.isNotEmpty()) {
            val classReader = ClassReader(classfileBuffer)
            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)

            var currentCv = classWriter as ClassVisitor

            mixinsForClass.forEach { mixinInfo ->
                currentCv = MixinTransformer(Opcodes.ASM9, currentCv, mixinInfo)
            }
            classReader.accept(currentCv, 0)
            return classWriter.toByteArray()
        }
        return null
    }
    
    /**
     * 获取所有处理过的类的字节码映射（实例方法）
     * @return Map<String, ByteArray> 类名到字节码的映射
     */
    fun getProcessedClassesBytesInstance(): Map<String, ByteArray> {
        return mixinProcessor.getBytes()
    }
}
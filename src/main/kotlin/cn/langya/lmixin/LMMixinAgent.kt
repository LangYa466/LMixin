package cn.langya.lmixin

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

class LMMixinAgent : ClassFileTransformer {

    private lateinit var mappingParser: MappingParser
    private lateinit var mixinProcessor: MixinProcessor

    companion object {
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
}
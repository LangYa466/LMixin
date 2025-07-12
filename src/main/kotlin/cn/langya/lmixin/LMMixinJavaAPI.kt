package cn.langya.lmixin

import java.lang.instrument.Instrumentation
import java.util.*

object LMMixinJavaAPI {
    
    private var mixinProcessor: MixinProcessor? = null
    
    /**
     * 初始化LMixin Agent
     * @param inst Instrumentation实例
     * @param srgFilePath SRG映射文件路径
     */
    @JvmStatic
    fun init(inst: Instrumentation, srgFilePath: String = "mappings.srg") {
        val agent = LMMixinAgent()
        agent.mappingParser = MappingParser(srgFilePath)
        agent.mixinProcessor = MixinProcessor(agent.mappingParser)
        agent.mixinProcessor.processMixins()
        inst.addTransformer(agent)
        
        // 保存mixinProcessor引用以供Java API使用
        this.mixinProcessor = agent.mixinProcessor
    }
    
    /**
     * 获取所有处理过的类的字节码映射
     * @return Map<String, ByteArray> 类名到字节码的映射
     */
    @JvmStatic
    fun getBytes(): Map<String, ByteArray> {
        return mixinProcessor?.getBytes() ?: emptyMap()
    }
    
    /**
     * 获取指定类的字节码
     * @param className 类名（内部格式，如：cn/langya/lmixin/ExampleClass）
     * @return ByteArray? 类的字节码，如果不存在则返回null
     */
    @JvmStatic
    fun getClassBytes(className: String): ByteArray? {
        return mixinProcessor?.getBytes()?.get(className)
    }
    
    /**
     * 获取所有处理过的类名列表
     * @return List<String> 类名列表
     */
    @JvmStatic
    fun getProcessedClassNames(): List<String> {
        return mixinProcessor?.getBytes()?.keys?.toList() ?: emptyList()
    }
    
    /**
     * 检查指定类是否已被处理
     * @param className 类名
     * @return Boolean 是否已处理
     */
    @JvmStatic
    fun isClassProcessed(className: String): Boolean {
        return mixinProcessor?.getBytes()?.containsKey(className) ?: false
    }
    
    /**
     * 获取处理过的类数量
     * @return Int 类数量
     */
    @JvmStatic
    fun getProcessedClassCount(): Int {
        return mixinProcessor?.getBytes()?.size ?: 0
    }
} 
package cn.langya.lmixin

import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

data class MixinInfo(
    val mixinClassName: String,
    val targetClassName: String,
    val interfaces: List<String> = emptyList(),
    val shadowFields: List<ShadowField> = emptyList(),
    val shadowMethods: List<ShadowMethod> = emptyList(),

    val overwriteMethods: List<OverwriteMethod> = emptyList(),
    val injectMethods: List<InjectMethod> = emptyList(),
    val newFields: List<FieldNode> = emptyList(),
    val newMethods: List<MethodNode> = emptyList()
) {
    /**
     * 使用默认参数的构造函数
     */
    constructor(mixinClassName: String, targetClassName: String) : this(
        mixinClassName = mixinClassName,
        targetClassName = targetClassName,
        interfaces = emptyList(),
        shadowFields = emptyList(),
        shadowMethods = emptyList(),
        overwriteMethods = emptyList(),
        injectMethods = emptyList(),
        newFields = emptyList(),
        newMethods = emptyList()
    )
    
    /**
     * 获取接口数量
     * @return Int 接口数量
     */
    fun getInterfaceCount(): Int {
        return interfaces.size
    }
    
    /**
     * 获取Shadow字段数量
     * @return Int Shadow字段数量
     */
    fun getShadowFieldCount(): Int {
        return shadowFields.size
    }
    
    /**
     * 获取Shadow方法数量
     * @return Int Shadow方法数量
     */
    fun getShadowMethodCount(): Int {
        return shadowMethods.size
    }
    
    /**
     * 获取Overwrite方法数量
     * @return Int Overwrite方法数量
     */
    fun getOverwriteMethodCount(): Int {
        return overwriteMethods.size
    }
    
    /**
     * 获取Inject方法数量
     * @return Int Inject方法数量
     */
    fun getInjectMethodCount(): Int {
        return injectMethods.size
    }
    
    /**
     * 获取新字段数量
     * @return Int 新字段数量
     */
    fun getNewFieldCount(): Int {
        return newFields.size
    }
    
    /**
     * 获取新方法数量
     * @return Int 新方法数量
     */
    fun getNewMethodCount(): Int {
        return newMethods.size
    }
    
    /**
     * 检查是否有接口
     * @return Boolean 是否有接口
     */
    fun hasInterfaces(): Boolean {
        return interfaces.isNotEmpty()
    }
    
    /**
     * 检查是否有Shadow字段
     * @return Boolean 是否有Shadow字段
     */
    fun hasShadowFields(): Boolean {
        return shadowFields.isNotEmpty()
    }
    
    /**
     * 检查是否有Shadow方法
     * @return Boolean 是否有Shadow方法
     */
    fun hasShadowMethods(): Boolean {
        return shadowMethods.isNotEmpty()
    }
    
    /**
     * 检查是否有Overwrite方法
     * @return Boolean 是否有Overwrite方法
     */
    fun hasOverwriteMethods(): Boolean {
        return overwriteMethods.isNotEmpty()
    }
    
    /**
     * 检查是否有Inject方法
     * @return Boolean 是否有Inject方法
     */
    fun hasInjectMethods(): Boolean {
        return injectMethods.isNotEmpty()
    }
    
    /**
     * 检查是否有新字段
     * @return Boolean 是否有新字段
     */
    fun hasNewFields(): Boolean {
        return newFields.isNotEmpty()
    }
    
    /**
     * 检查是否有新方法
     * @return Boolean 是否有新方法
     */
    fun hasNewMethods(): Boolean {
        return newMethods.isNotEmpty()
    }

    data class ShadowField(
        val mixinFieldName: String,
        val targetFieldName: String,
        val fieldNode: FieldNode
    ) {
        /**
         * 检查字段名是否匹配
         * @param name 字段名
         * @return Boolean 是否匹配
         */
        fun matchesFieldName(name: String): Boolean {
            return mixinFieldName == name || targetFieldName == name
        }
    }

    data class ShadowMethod(
        val mixinMethodName: String,
        val mixinMethodDesc: String,
        val targetMethodName: String,
        val targetMethodDesc: String,
        val methodNode: MethodNode
    ) {
        /**
         * 检查方法是否匹配
         * @param name 方法名
         * @param desc 方法描述符
         * @return Boolean 是否匹配
         */
        fun matchesMethod(name: String, desc: String): Boolean {
            return (mixinMethodName == name && mixinMethodDesc == desc) ||
                   (targetMethodName == name && targetMethodDesc == desc)
        }
    }

    data class InjectMethod(
        val mixinMethodName: String,
        val mixinMethodDesc: String,
        val targetMethodName: String,
        val targetMethodDesc: String,
        val at: At,
        val injectionPointMethodName: String,
        val ordinal: Int,
        val methodNode: MethodNode
    ) {
        /**
         * 检查注入方法是否匹配
         * @param name 方法名
         * @param desc 方法描述符
         * @return Boolean 是否匹配
         */
        fun matchesMethod(name: String, desc: String): Boolean {
            return mixinMethodName == name && mixinMethodDesc == desc
        }
    }

    data class OverwriteMethod(
        val mixinMethodName: String,
        val mixinMethodDesc: String,
        val targetMethodName: String,
        val targetMethodDesc: String,
        val methodNode: MethodNode
    ) {
        /**
         * 检查重写方法是否匹配
         * @param name 方法名
         * @param desc 方法描述符
         * @return Boolean 是否匹配
         */
        fun matchesMethod(name: String, desc: String): Boolean {
            return (mixinMethodName == name && mixinMethodDesc == desc) ||
                   (targetMethodName == name && targetMethodDesc == desc)
        }
    }
}
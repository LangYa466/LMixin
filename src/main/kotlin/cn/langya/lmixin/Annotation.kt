package cn.langya.lmixin

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Mixin(val value: KClass<*>)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Overwrite

enum class At(val value: String) {
    HEAD("HEAD"),
    TAIL("TAIL"),
    BEFORE("BEFORE"),
    AFTER("AFTER"),
    ;
    
    /**
     * 检查是否为头部注入
     * @return Boolean 是否为头部注入
     */
    fun isHead(): Boolean {
        return this == HEAD
    }
    
    /**
     * 检查是否为尾部注入
     * @return Boolean 是否为尾部注入
     */
    fun isTail(): Boolean {
        return this == TAIL
    }
    
    /**
     * 检查是否为前置注入
     * @return Boolean 是否为前置注入
     */
    fun isBefore(): Boolean {
        return this == BEFORE
    }
    
    /**
     * 检查是否为后置注入
     * @return Boolean 是否为后置注入
     */
    fun isAfter(): Boolean {
        return this == AFTER
    }
    
    /**
     * 获取注入位置的描述
     * @return String 注入位置描述
     */
    fun getDescription(): String {
        return when (this) {
            HEAD -> "方法开始处"
            TAIL -> "方法结束处"
            BEFORE -> "指定调用之前"
            AFTER -> "指定调用之后"
        }
    }
    
    companion object {
        /**
         * 从字符串创建At枚举
         * @param value 字符串值
         * @return At? 对应的枚举值，如果不存在则返回null
         */
        fun fromString(value: String): At? {
            return entries.find { it.value == value.uppercase() }
        }
        
        /**
         * 获取所有可用的注入位置
         * @return Array<At> 所有注入位置
         */
        fun getAllValues(): Array<At> {
            return entries.toTypedArray()
        }
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject(
    val method: String, 
    val at: At = At.HEAD, 
    val ordinal: Int = 0,
    val target: String = "",
    val injectionPoint: String = ""
)

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Shadow(
    val value: String = "",
    val remap: Boolean = true
)
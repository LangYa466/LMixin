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
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject(val method: String, val at: At = At.HEAD, val ordinal: Int = 0)

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Shadow
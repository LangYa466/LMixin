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
    val newFields: List<FieldNode> = emptyList(),
    val newMethods: List<MethodNode> = emptyList()
) {
    data class ShadowField(
        val mixinFieldName: String,
        val targetFieldName: String,
        val fieldNode: FieldNode
    )

    data class ShadowMethod(
        val mixinMethodName: String,
        val mixinMethodDesc: String,
        val targetMethodName: String,
        val targetMethodDesc: String,
        val methodNode: MethodNode
    )

    data class OverwriteMethod(
        val mixinMethodName: String,
        val mixinMethodDesc: String,
        val targetMethodName: String,
        val targetMethodDesc: String,
        val methodNode: MethodNode
    )
}
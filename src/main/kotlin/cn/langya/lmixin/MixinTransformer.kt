package cn.langya.lmixin

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*


class MixinTransformer(
    api: Int,
    private val cv: ClassVisitor,
    private val mixinInfo: MixinInfo
) : ClassNode(api) {

    /**
     * 使用默认ASM版本的构造函数
     */
    constructor(cv: ClassVisitor, mixinInfo: MixinInfo) : this(Opcodes.ASM9, cv, mixinInfo)

    private fun remapMethodNode(methodNode: MethodNode) {
        val instructions = methodNode.instructions ?: return
        for (insn in instructions) {
            when (insn) {
                is FieldInsnNode -> {
                    if (insn.owner == mixinInfo.mixinClassName) {
                        insn.owner = mixinInfo.targetClassName
                        val shadowField = mixinInfo.shadowFields.find { it.mixinFieldName == insn.name }
                        if (shadowField != null) {
                            insn.name = shadowField.targetFieldName
                        }
                    }
                }

                is MethodInsnNode -> {
                    if (insn.owner == mixinInfo.mixinClassName) {
                        insn.owner = mixinInfo.targetClassName
                        val shadowMethod = mixinInfo.shadowMethods.find { 
                            it.mixinMethodName == insn.name && it.mixinMethodDesc == insn.desc 
                        }
                        if (shadowMethod != null) {
                            insn.name = shadowMethod.targetMethodName
                            insn.desc = shadowMethod.targetMethodDesc
                        }
                    }
                }
            }
        }
    }

    override fun visitEnd() {
        mixinInfo.interfaces.forEach { iface ->
            if (!interfaces.contains(iface)) {
                interfaces.add(iface)
            }
        }

        mixinInfo.newFields.forEach { field ->
            if (fields.none { it.name == field.name }) {
                field.access =
                    (field.access and Opcodes.ACC_PRIVATE.inv() and Opcodes.ACC_PROTECTED.inv()) or Opcodes.ACC_PUBLIC
                fields.add(field)
            }
        }

        val methodsToAdd = mutableListOf<MethodNode>()
        mixinInfo.newMethods.forEach {
            remapMethodNode(it)
            methodsToAdd.add(it)
        }

        mixinInfo.overwriteMethods.forEach { overwrite ->
            val wasRemoved =
                methods.removeIf { it.name == overwrite.targetMethodName && it.desc == overwrite.targetMethodDesc }
            if (wasRemoved) {
                remapMethodNode(overwrite.methodNode)
                overwrite.methodNode.name = overwrite.targetMethodName
                overwrite.methodNode.desc = overwrite.targetMethodDesc
                methodsToAdd.add(overwrite.methodNode)
            }
        }

        mixinInfo.injectMethods.forEach { inject ->
            methods.find { it.name == inject.targetMethodName && it.desc == inject.targetMethodDesc }
                ?.let { targetMethod ->
                    val instructionsToInject = InsnList()
                    val tempMethodNode = MethodNode(Opcodes.ASM9, "temp", "()V", null, null).apply {
                        instructions = instructionsToInject
                    }
                    inject.methodNode.instructions.forEach { insn -> instructionsToInject.add(insn.clone(emptyMap())) }
                    remapMethodNode(tempMethodNode)

                    when (inject.at) {
                        At.HEAD -> {
                            targetMethod.instructions.insert(tempMethodNode.instructions)
                        }

                        At.TAIL -> {
                            val returnInsn =
                                targetMethod.instructions.lastOrNull { it.opcode >= Opcodes.IRETURN && it.opcode <= Opcodes.RETURN }
                            if (returnInsn != null) {
                                targetMethod.instructions.insertBefore(returnInsn, tempMethodNode.instructions)
                            } else {
                                targetMethod.instructions.add(tempMethodNode.instructions)
                            }
                        }

                        At.BEFORE -> {
                            val targetCallNodes = targetMethod.instructions.filterIsInstance<MethodInsnNode>()
                                .filter { it.name == inject.injectionPointMethodName }

                            if (inject.ordinal < targetCallNodes.size) {
                                val injectionPoint = targetCallNodes[inject.ordinal]
                                targetMethod.instructions.insertBefore(injectionPoint, tempMethodNode.instructions)
                            }
                        }

                        At.AFTER -> {
                            val targetCallNodes = targetMethod.instructions.filterIsInstance<MethodInsnNode>()
                                .filter { it.name == inject.injectionPointMethodName }

                            if (inject.ordinal < targetCallNodes.size) {
                                val injectionPoint = targetCallNodes[inject.ordinal]
                                targetMethod.instructions.insert(injectionPoint, tempMethodNode.instructions)
                            }
                        }
                    }
                }
        }

        methodsToAdd.forEach { node ->
            if (methods.none { it.name == node.name && it.desc == node.desc }) {
                methods.add(node)
            }
        }

        accept(cv)
    }
    
    /**
     * 获取mixin信息
     * @return MixinInfo mixin信息
     */
    fun getMixinInfo(): MixinInfo {
        return mixinInfo
    }
    
    /**
     * 获取目标类名
     * @return String 目标类名
     */
    fun getTargetClassName(): String {
        return mixinInfo.targetClassName
    }
    
    /**
     * 获取mixin类名
     * @return String mixin类名
     */
    fun getMixinClassName(): String {
        return mixinInfo.mixinClassName
    }
    
    /**
     * 获取接口数量
     * @return Int 接口数量
     */
    fun getInterfaceCount(): Int {
        return mixinInfo.interfaces.size
    }
    
    /**
     * 获取新字段数量
     * @return Int 新字段数量
     */
    fun getNewFieldCount(): Int {
        return mixinInfo.newFields.size
    }
    
    /**
     * 获取新方法数量
     * @return Int 新方法数量
     */
    fun getNewMethodCount(): Int {
        return mixinInfo.newMethods.size
    }
    
    /**
     * 获取Shadow字段数量
     * @return Int Shadow字段数量
     */
    fun getShadowFieldCount(): Int {
        return mixinInfo.shadowFields.size
    }
    
    /**
     * 获取Shadow方法数量
     * @return Int Shadow方法数量
     */
    fun getShadowMethodCount(): Int {
        return mixinInfo.shadowMethods.size
    }
    
    /**
     * 获取Overwrite方法数量
     * @return Int Overwrite方法数量
     */
    fun getOverwriteMethodCount(): Int {
        return mixinInfo.overwriteMethods.size
    }
    
    /**
     * 获取Inject方法数量
     * @return Int Inject方法数量
     */
    fun getInjectMethodCount(): Int {
        return mixinInfo.injectMethods.size
    }
}
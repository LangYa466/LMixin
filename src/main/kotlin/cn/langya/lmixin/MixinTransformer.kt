package cn.langya.lmixin

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*


class MixinTransformer(
    api: Int,
    private val cv: ClassVisitor,
    private val mixinInfo: MixinInfo
) : ClassNode(api) {

    private fun remapMethodNode(methodNode: MethodNode) {
        val instructions = methodNode.instructions ?: return
        for (insn in instructions) {
            when (insn) {
                is FieldInsnNode -> {
                    if (insn.owner == mixinInfo.mixinClassName) {
                        insn.owner = mixinInfo.targetClassName
                    }
                }

                is MethodInsnNode -> {
                    if (insn.owner == mixinInfo.mixinClassName) {
                        insn.owner = mixinInfo.targetClassName
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
}
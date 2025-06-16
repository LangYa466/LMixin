package cn.langya.lmixin

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class MixinTransformer(
    api: Int,
    private val cv: ClassVisitor,
    private val mixinInfo: MixinInfo
) : ClassNode(api) {

    override fun visitEnd() {
        super.visitEnd()

        mixinInfo.interfaces.forEach { iface ->
            if (!interfaces.contains(iface)) interfaces.add(iface)
        }

        mixinInfo.newFields.forEach { field ->
            if (fields.none { it.name == field.name }) {
                field.access = field.access and Opcodes.ACC_PRIVATE.inv() or Opcodes.ACC_PUBLIC
                fields.add(field)
            }
        }

        val methodsToAdd = mutableListOf<MethodNode>().apply { addAll(mixinInfo.newMethods) }

        mixinInfo.overwriteMethods.forEach { overwrite ->
            methods.find { it.name == overwrite.targetMethodName && it.desc == overwrite.targetMethodDesc }?.let {
                val src = overwrite.methodNode
                val clone = MethodNode(src.access, src.name, src.desc, src.signature, src.exceptions.toTypedArray())
                src.accept(clone)
                methods.removeIf { it.name == overwrite.targetMethodName && it.desc == overwrite.targetMethodDesc }
                methodsToAdd.add(clone)
            }
        }

        methodsToAdd.forEach { node ->
            if (methods.none { it.name == node.name && it.desc == node.desc }) {
                methods.add(node)
            }
        }

        this.name = mixinInfo.targetClassName

        accept(cv)
    }
}
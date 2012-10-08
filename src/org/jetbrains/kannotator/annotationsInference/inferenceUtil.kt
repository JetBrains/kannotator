package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.declarations.MethodId
import org.jetbrains.kannotator.controlFlow.builder.AsmInstructionMetadata
import org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method

fun DeclarationIndex.findMethodByInstruction(instruction: Instruction): Method? {
    val methodInsnNode = (instruction.metadata as? AsmInstructionMetadata)?.asmInstruction as? MethodInsnNode
    return if (methodInsnNode == null) null else this.findMethodByMethodInsnNode(methodInsnNode)
}

fun DeclarationIndex.findMethodByMethodInsnNode(methodInsnNode: MethodInsnNode): Method? {
    return this.findMethod(ClassName.fromInternalName(methodInsnNode.owner!!), methodInsnNode.name!!, methodInsnNode.desc)
}

fun getMethodIdByInstruction(instruction: Instruction): MethodId? {
    val methodInsnNode = (instruction.metadata as? AsmInstructionMetadata)?.asmInstruction as? MethodInsnNode
    return if (methodInsnNode == null) null else MethodId(methodInsnNode.name!!, methodInsnNode.desc)
}

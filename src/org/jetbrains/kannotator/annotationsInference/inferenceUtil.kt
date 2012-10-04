package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.declarations.MethodId
import org.jetbrains.kannotator.controlFlowBuilder.AsmInstructionMetadata
import org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.kannotator.declarations.getArgumentTypes

fun getMethodIdByInstruction(instruction: Instruction): MethodId? {
    val methodInsnNode = (instruction.metadata as? AsmInstructionMetadata)?.asmInstruction as? MethodInsnNode
    return if (methodInsnNode == null) null else MethodId(methodInsnNode.name!!, methodInsnNode.desc)
}

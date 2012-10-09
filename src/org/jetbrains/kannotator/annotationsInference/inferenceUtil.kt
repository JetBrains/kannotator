package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.builder.AsmInstructionMetadata
import org.jetbrains.kannotator.controlFlow.builder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.MethodId
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.jetbrains.kannotator.declarations.getArgumentCount
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.MethodInsnNode

trait Annotation

fun ControlFlowGraph.traverseInstructions(f: (Instruction) -> Unit) {
    for (instruction in instructions) {
        if (instruction[STATE_BEFORE] == null) continue // dead instructions
        f(instruction)
    }
}

public fun <A: Annotation> generateAssertsForCallArguments(
        instruction: Instruction,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<A>,
        addAssertForStackValue: (Int) -> Unit,
        needGenerateAssertForThis: Boolean,
        needGenerateAssertForArgument: (A) -> Boolean
) {
    val methodId = getMethodIdByInstruction(instruction)
    val hasThis = instruction.getOpcode() != INVOKESTATIC
    val thisSlots = if (hasThis) 1 else 0
    val parametersCount = methodId.getArgumentCount() + thisSlots

    fun addAssertForArgumentOnStack(index: Int) {
        addAssertForStackValue(parametersCount - index - 1)
    }

    if (hasThis && needGenerateAssertForThis) {
        addAssertForArgumentOnStack(0)
    }
    if (instruction.getOpcode() != INVOKEDYNAMIC) {
        val method = declarationIndex.findMethodByInstruction(instruction)
        if (method != null) {
            val positions = PositionsWithinMember(method)
            for (paramIndex in thisSlots..parametersCount - 1) {
                val paramAnnotation = annotations[positions.forParameter(paramIndex).position]
                if (paramAnnotation != null && needGenerateAssertForArgument(paramAnnotation)) {
                    addAssertForArgumentOnStack(paramIndex)
                }
            }
        }
    }
}

fun DeclarationIndex.findMethodByInstruction(instruction: Instruction): Method? {
    val methodInsnNode = (instruction.metadata as? AsmInstructionMetadata)?.asmInstruction as? MethodInsnNode
    return if (methodInsnNode == null) null else this.findMethodByMethodInsnNode(methodInsnNode)
}

fun DeclarationIndex.findMethodByMethodInsnNode(methodInsnNode: MethodInsnNode): Method? {
    return this.findMethod(ClassName.fromInternalName(methodInsnNode.owner!!), methodInsnNode.name!!, methodInsnNode.desc)
}

fun getMethodIdByInstruction(instruction: Instruction): MethodId {
    val methodInsnNode = (instruction.metadata as AsmInstructionMetadata).asmInstruction as MethodInsnNode
    return MethodId(methodInsnNode.name!!, methodInsnNode.desc)
}

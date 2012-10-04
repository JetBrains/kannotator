package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.getArgumentCount
import org.jetbrains.kannotator.nullability.Nullability
import org.objectweb.asm.Opcodes.*
import org.jetbrains.kannotator.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.declarations.ClassName
import org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.kannotator.controlFlowBuilder.AsmInstructionMetadata
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.Positions

fun generateNullabilityAsserts(
        instruction: Instruction,
        annotations: Annotations<NullabilityAnnotation>,
        declarationIndex: DeclarationIndex
) : Set<Assert<Nullability>> {
    val state = instruction[STATE_BEFORE]!!
    val result = hashSet<Assert<Nullability>>()

    fun addAssertForStackValue(indexFromTop: Int) {
        val valueSet = state.stack[indexFromTop]
        for (value in valueSet) {
            result.add(Assert(value))
        }
    }

    when (instruction.getOpcode()) {
        INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC, INVOKESTATIC -> {
            val methodId = getMethodIdByInstruction(instruction)
            val hasThis = instruction.getOpcode() != INVOKESTATIC
            val nonThisParametersCount = methodId!!.getArgumentCount() // excluding this
            if (hasThis) {
                addAssertForStackValue(nonThisParametersCount)
            }
            if (instruction.getOpcode() != INVOKEDYNAMIC) {
                val method = declarationIndex.findMethodByInstruction(instruction)
                if (method != null) {
                    val positions = Positions(method)
                    val parameterIndices = if (hasThis) 1..nonThisParametersCount else 0..nonThisParametersCount - 1
                    for (paramIndex in parameterIndices) {
                        val paramAnnotation = annotations[positions.forParameter(paramIndex).position]
                        if (paramAnnotation == NullabilityAnnotation.NOT_NULL) {
                            addAssertForStackValue(nonThisParametersCount - paramIndex)
                        }
                    }
                }
            }
        }
        GETFIELD, ARRAYLENGTH, ATHROW,
        MONITORENTER, MONITOREXIT -> {
            addAssertForStackValue(0)
        }
        AALOAD, BALOAD, IALOAD, CALOAD, SALOAD, FALOAD, LALOAD, DALOAD,
        PUTFIELD -> {
            addAssertForStackValue(1)
        }
        AASTORE, BASTORE, IASTORE, CASTORE, SASTORE, FASTORE, LASTORE, DASTORE -> {
            addAssertForStackValue(2)
        }
        else -> {}
    }
    return result
}

fun DeclarationIndex.findMethodByInstruction(instruction: Instruction): Method? {
    val methodInsnNode = (instruction.metadata as? AsmInstructionMetadata)?.asmInstruction as? MethodInsnNode
    if (methodInsnNode == null) return null
    return this.findMethod(ClassName.fromInternalName(methodInsnNode.owner!!), methodInsnNode.name!!, methodInsnNode.desc)
}

package org.jetbrains.kannotator.annotationsInference

import java.util.Collections
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlowBuilder.AsmInstructionMetadata
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.objectweb.asm.Opcodes.*

class NullabilityAssert(val shouldBeNotNullValue: Value)

fun generateAsserts(instruction: Instruction) : Set<NullabilityAssert> {
    val state = instruction[STATE_BEFORE]
    if (state == null) return Collections.emptySet()

    val result = hashSet<NullabilityAssert>()
    val instructionMetadata = instruction.metadata
    if (instructionMetadata is AsmInstructionMetadata) {
        when (instructionMetadata.asmInstruction.getOpcode()) {
            INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC -> {
                val valueSet = state.stack[0]
                for (value in valueSet) {
                    result.add(NullabilityAssert(value))
                }
            }
            else -> Unit.VALUE
        }
    }
    return result
}


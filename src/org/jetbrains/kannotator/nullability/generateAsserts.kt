package org.jetbrains.kannotator.annotationsInference

import java.util.Collections
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.objectweb.asm.Opcodes.*
import org.jetbrains.kannotator.nullability.Nullability
import org.jetbrains.kannotator.asm.util.getOpcode

fun generateNullabilityAsserts(instruction: Instruction) : Set<Assert<Nullability>> {
    val state = instruction[STATE_BEFORE]!!

    val result = hashSet<Assert<Nullability>>()
    when (instruction.getOpcode()) {
        INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC,
        GETFIELD, PUTFIELD,
        AALOAD, AASTORE -> {
            val valueSet = state.stack[0]
            for (value in valueSet) {
                result.add(Assert(value))
            }
        }
        // TODO other interesting instructions
        else -> {}
    }
    return result
}


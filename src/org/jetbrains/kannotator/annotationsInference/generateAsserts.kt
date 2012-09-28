package org.jetbrains.kannotator.annotationsInference

import java.util.Collections
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.objectweb.asm.Opcodes.*

class NullabilityAssert(val shouldBeNotNullValue: Value)

fun generateAsserts(instruction: Instruction) : Set<NullabilityAssert> {
    val state = instruction[STATE_BEFORE]!!

    val result = hashSet<NullabilityAssert>()
    when (instruction.getOpcode()) {
        INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC,
        AALOAD, AASTORE -> {
            val valueSet = state.stack[0]
            for (value in valueSet) {
                result.add(NullabilityAssert(value))
            }
        }
        // TODO other interesting instructions
        else -> {}
    }
    return result
}


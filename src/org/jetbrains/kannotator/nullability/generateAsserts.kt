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

    fun addAssertForStackValue(indexFromTop: Int) {
        val valueSet = state.stack[indexFromTop]
        for (value in valueSet) {
            result.add(Assert(value))
        }
    }

    when (instruction.getOpcode()) {
        INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC -> {
            // TODO depending on number of parameters
            addAssertForStackValue(0)
        }
        GETFIELD, ARRAYLENGTH -> {
            addAssertForStackValue(0)
        }
        AALOAD, PUTFIELD -> {
            addAssertForStackValue(1)
        }
        AASTORE -> {
            addAssertForStackValue(2)
        }
        // TODO other interesting instructions
        else -> {}
    }
    return result
}


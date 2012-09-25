package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.jetbrains.kannotator.controlFlow.Value
import org.objectweb.asm.Opcodes.*
import org.jetbrains.kannotator.nullability.NullabilityValueInfo
import org.jetbrains.kannotator.nullability.NullabilityValueInfo.*
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlowBuilder.AsmInstructionMetadata
import org.jetbrains.kannotator.nullability.merge
import javax.jnlp.SingleInstanceService
import org.objectweb.asm.tree.*
import java.util.LinkedHashMap
import org.jetbrains.kannotator.controlFlowBuilder.TypedValue
import org.jetbrains.kannotator.controlFlow.ControlFlowEdge
import kotlin.test.assertNull

fun inferAnnotations(graph: ControlFlowGraph) : List<NullabilityValueInfo> {
    val parametersValueInfo = hashMap<Value, NullabilityValueInfo>()
    val returnValueInfo = arrayList<NullabilityValueInfo>()
    for (instruction in graph.instructions) {
        analyzeInstruction(instruction, parametersValueInfo, returnValueInfo)
    }
    val result = arrayList<NullabilityValueInfo>(returnValueInfo.merge())
    result.addAll(parametersValueInfo.values())
    return result
}

fun analyzeInstruction(
        instruction: Instruction,
        parametersValueInfo: MutableMap<Value, NullabilityValueInfo>,
        returnValueInfo: MutableList<NullabilityValueInfo>)
{
    val state = instruction[STATE_BEFORE]
    if (state == null) return

    val nullabilityInfosForInstruction = computeNullabilityInfosForInstruction(instruction)

    val instructionMetadata = instruction.metadata
    if (instructionMetadata is AsmInstructionMetadata) {
        when (instructionMetadata.asmInstruction.getOpcode()) {
            ARETURN -> {
                val valueSet = state.stack[0]
                val nullabilityValueInfo = valueSet
                        .map { it -> getNullabilityInfo(nullabilityInfosForInstruction, it) }.merge()
                returnValueInfo.add(nullabilityValueInfo)
            }
            INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC -> {
                val valueSet = state.stack[0]
                for (value in valueSet) {
                    if (value.interesting) {
                        parametersValueInfo[value] = NOT_NULL
                    }
                }
            }
            else -> Unit.VALUE
        }
    }
}

val nullabilityInfosForEdges : MutableMap<ControlFlowEdge, Map<Value, NullabilityValueInfo>> = hashMap()

fun getNullabilityInfo(nullabilityInfos: Map<Value, NullabilityValueInfo>, value: Value) : NullabilityValueInfo {
    val initialInfo = getInitialNullabilityInfo(value)
    val currentInfo = nullabilityInfos[value]
    return if (currentInfo == null) initialInfo else initialInfo merge currentInfo
}

fun computeNullabilityInfosForInstruction(instruction: Instruction) : Map<Value, NullabilityValueInfo> {
    val result = hashMap<Value, NullabilityValueInfo>()

    // merge from incoming edges
    for (incomingEdge in instruction.incomingEdges) {
        val incomingEdgeMap: Map<Value, NullabilityValueInfo>? = nullabilityInfosForEdges[incomingEdge.from]
        if (incomingEdgeMap == null) continue
        for ((value, info) in incomingEdgeMap) {
            val currentInfo = result[value]
            result[value] = if (currentInfo == null) info else info merge currentInfo
        }

    }

    // propagate to outgoing edges
    for (outgoingEdge in instruction.outgoingEdges) {
        assertNull(nullabilityInfosForEdges[outgoingEdge])
        nullabilityInfosForEdges[outgoingEdge] = result
    }

    return result
}

fun getInitialNullabilityInfo(value: Value): NullabilityValueInfo {
    val typedValue = value as TypedValue
    val createdAtInsn = typedValue.createdAtInsn

    // TODO this is a hack, must create value info depending on current command type
    if (value == org.jetbrains.kannotator.controlFlowBuilder.NULL_VALUE) {
        return NULL
    }
    if (createdAtInsn == null) return UNKNOWN // todo parameter
    return when (createdAtInsn.getOpcode()) {
        NEW -> NOT_NULL
        ACONST_NULL -> NULL
        else -> UNKNOWN
    }
}
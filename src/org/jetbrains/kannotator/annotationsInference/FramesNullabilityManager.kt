package org.jetbrains.kannotator.annotationsInference

import org.objectweb.asm.Opcodes.*
import org.jetbrains.kannotator.nullability.NullabilityValueInfo
import org.jetbrains.kannotator.nullability.NullabilityValueInfo.*
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlow.ControlFlowEdge
import kotlin.test.assertNull
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.State
import org.jetbrains.kannotator.controlFlowBuilder.AsmInstructionMetadata
import kotlin.test.assertFalse
import org.jetbrains.kannotator.controlFlowBuilder.TypedValue
import java.util.HashMap

public class FramesNullabilityManager {
    private val nullabilityInfosForEdges : MutableMap<ControlFlowEdge, Map<Value, NullabilityValueInfo>> = hashMap()

    fun getNullabilityInfo(nullabilityInfos: Map<Value, NullabilityValueInfo>, value: Value) : NullabilityValueInfo {
        val initialInfo = getInitialNullabilityInfo(value)
        val currentInfo = nullabilityInfos[value]
        return if (currentInfo == null) initialInfo else currentInfo
    }

    fun setValueInfosForEdge(edge: ControlFlowEdge, infos: Map<Value, NullabilityValueInfo>) {
        assertNull(nullabilityInfosForEdges[edge])
        nullabilityInfosForEdges[edge] = infos
    }

    fun computeNullabilityInfosForInstruction(instruction: Instruction, state: State<Unit>) : Map<Value, NullabilityValueInfo> {
        val infosForInstruction = hashMap<Value, NullabilityValueInfo>()

        // merge from incoming edges
        for (incomingEdge in instruction.incomingEdges) {
            val incomingEdgeMap: Map<Value, NullabilityValueInfo>? = nullabilityInfosForEdges[incomingEdge]
            if (incomingEdgeMap == null) continue
            for ((value, info) in incomingEdgeMap) {
                val currentInfo = infosForInstruction[value]
                infosForInstruction[value] = if (currentInfo == null) info else info merge currentInfo
            }
        }

        fun propagateTransformedValueInfos(
                outgoingEdge: ControlFlowEdge,
                transformStackValueInfo: (NullabilityValueInfo?) -> NullabilityValueInfo // TODO remove '?'
        ) {
            val infosForEdge = HashMap(infosForInstruction)
            for (value in state.stack[0]) {
                infosForEdge[value] = transformStackValueInfo(infosForInstruction[value])
            }
            setValueInfosForEdge(outgoingEdge, infosForEdge)
        }

        var propagateAsIs = false
        val instructionMetadata = instruction.metadata
        if (instructionMetadata is AsmInstructionMetadata) {
            val opcode: Int = instructionMetadata.asmInstruction.getOpcode()
            when (opcode) {
                IFNULL, IFNONNULL -> {
                    // first outgoing edge is 'false', second is 'true'
                    // this order is is provided by code in ASM's Analyzer
                    val it = instruction.outgoingEdges.iterator()
                    val (falseEdge, trueEdge) = Pair(it.next(), it.next())
                    assertFalse(it.hasNext()) // should be exactly two edges!

                    val (nullEdge, notNullEdge) = if (opcode == IFNULL)
                        Pair(trueEdge, falseEdge)
                    else Pair(falseEdge, trueEdge)

                    propagateTransformedValueInfos(nullEdge) { wasInfo ->
                        if (wasInfo == CONFLICT || wasInfo == NOT_NULL) CONFLICT else NULL }

                    propagateTransformedValueInfos(notNullEdge) { wasInfo ->
                        if (wasInfo == CONFLICT || wasInfo == NULL) CONFLICT else NOT_NULL }
                }
                else -> {
                    propagateAsIs = true
                }
            }
        }
        else {
            propagateAsIs = true
        }

        // propagate to outgoing edges as is
        if (propagateAsIs) {
            instruction.outgoingEdges.forEach { edge -> setValueInfosForEdge(edge, infosForInstruction) }
        }

        return infosForInstruction
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
}
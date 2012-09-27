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
import kotlin.test.assertEquals

import org.jetbrains.kannotator.controlFlowBuilder

public class FramesNullabilityManager {
    private val nullabilityInfosForEdges : MutableMap<ControlFlowEdge, Map<Value, NullabilityValueInfo>> = hashMap()

    fun getNullabilityInfo(nullabilityInfos: ValueNullabilityMap, value: Value) : NullabilityValueInfo {
        return nullabilityInfos[value]
    }

    fun setValueInfosForEdge(edge: ControlFlowEdge, infos: Map<Value, NullabilityValueInfo>) {
        assertNull(nullabilityInfosForEdges[edge])
        nullabilityInfosForEdges[edge] = infos
    }

    fun mergeInfosFromIncomingEdges(instruction: Instruction) : ValueNullabilityMap {
        val result = ValueNullabilityMap()

        for (incomingEdge in instruction.incomingEdges) {
            val incomingEdgeMap: Map<Value, NullabilityValueInfo>? = nullabilityInfosForEdges[incomingEdge]
            if (incomingEdgeMap == null) continue
            for ((value, info) in incomingEdgeMap) {
                result[value] = info merge result[value]
            }
        }

        return result
    }

    fun computeNullabilityInfosForInstruction(instruction: Instruction, state: State<Unit>) : ValueNullabilityMap {
        val infosForInstruction = mergeInfosFromIncomingEdges(instruction)

        fun propagateTransformedValueInfos(
                outgoingEdge: ControlFlowEdge,
                transformStackValueInfo: (NullabilityValueInfo) -> NullabilityValueInfo
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
}

public class ValueNullabilityMap: HashMap<Value, NullabilityValueInfo>() {
    override fun get(key: Any?): NullabilityValueInfo {
        val fromSuper = super.get(key)
        if (fromSuper != null) return fromSuper

        if (key !is TypedValue) throw IllegalStateException("Trying to get with key which is not TypedValue")

        val createdAtInsn = key.createdAtInsn

        if (createdAtInsn != null) {
            return when (createdAtInsn.getOpcode()) {
                NEW -> NOT_NULL
                ACONST_NULL -> NULL
                INVOKEDYNAMIC, INVOKEINTERFACE, INVOKESTATIC, INVOKESPECIAL, INVOKEVIRTUAL ->
                    UNKNOWN // TODO load from annotations
                else -> throw UnsupportedOperationException("Unsupported opcode=${createdAtInsn.getOpcode()}")
            }
        }
        else if (key.interesting) {
            return UNKNOWN // todo read from parameter annotation
        }
        else return when (key) {
            controlFlowBuilder.NULL_VALUE -> NULL
            controlFlowBuilder.PRIMITIVE_VALUE_SIZE_1, controlFlowBuilder.PRIMITIVE_VALUE_SIZE_2 ->
                throw IllegalStateException("trying to get nullabilty info for primitive")
            else -> NOT_NULL // this is either "this" or caught exception
        }
    }
}
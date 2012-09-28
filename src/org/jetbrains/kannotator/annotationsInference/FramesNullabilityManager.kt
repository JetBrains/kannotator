package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
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
import java.util.Collections

class FramesNullabilityManager {
    private val nullabilityInfosForEdges : MutableMap<ControlFlowEdge, ValueNullabilityMap> = hashMap()

    fun getNullabilityInfo(nullabilityInfos: ValueNullabilityMap, value: Value) : NullabilityValueInfo {
        return nullabilityInfos[value]
    }

    private fun setValueInfosForEdge(edge: ControlFlowEdge, infos: ValueNullabilityMap) {
        assertNull(nullabilityInfosForEdges[edge])
        nullabilityInfosForEdges[edge] = infos
    }

    private fun mergeInfosFromIncomingEdges(instruction: Instruction) : ValueNullabilityMap {
        val result = ValueNullabilityMap()

        for (incomingEdge in instruction.incomingEdges) {
            val incomingEdgeMap: Map<Value, NullabilityValueInfo>? = nullabilityInfosForEdges[incomingEdge]
            if (incomingEdgeMap == null) continue
            for ((value, info) in incomingEdgeMap) {
                result[value] = if (result.containsKey(value)) info merge result[value] else info
            }
        }

        return result
    }

    fun computeNullabilityInfosForInstruction(instruction: Instruction) : ValueNullabilityMap {
        val state = instruction[STATE_BEFORE]!!

        val infosForInstruction = mergeInfosFromIncomingEdges(instruction)

        fun getFalseTrueEdges(): Pair<ControlFlowEdge, ControlFlowEdge> {
            // first outgoing edge is 'false', second is 'true'
            // this order is is provided by code in ASM's Analyzer
            val it = instruction.outgoingEdges.iterator()
            val result = Pair(it.next(), it.next())
            assertFalse(it.hasNext()) // should be exactly two edges!
            return result
        }

        fun propagateTransformedValueInfos(
                outgoingEdge: ControlFlowEdge,
                state: State<Unit>,
                transformStackValueInfo: ((NullabilityValueInfo) -> NullabilityValueInfo)?
        ) {
            if (transformStackValueInfo == null) {
                setValueInfosForEdge(outgoingEdge, infosForInstruction)
                return
            }

            val infosForEdge = ValueNullabilityMap(infosForInstruction)
            for (value in state.stack[0]) {
                infosForEdge[value] = transformStackValueInfo(infosForInstruction[value])
            }
            setValueInfosForEdge(outgoingEdge, infosForEdge)
        }

        val instructionMetadata = instruction.metadata
        if (instructionMetadata is AsmInstructionMetadata) {
            val opcode: Int = instructionMetadata.asmInstruction.getOpcode()
            when (opcode) {
                IFNULL, IFNONNULL -> {
                    val (falseEdge, trueEdge) = getFalseTrueEdges()

                    val (nullEdge, notNullEdge) =
                        if (opcode == IFNULL) Pair(trueEdge, falseEdge) else Pair(falseEdge, trueEdge)

                    propagateTransformedValueInfos(nullEdge, state) { wasInfo ->
                        if (wasInfo == CONFLICT || wasInfo == NOT_NULL) CONFLICT else NULL }

                    propagateTransformedValueInfos(notNullEdge, state) { wasInfo ->
                        if (wasInfo == CONFLICT || wasInfo == NULL) CONFLICT else NOT_NULL }

                    return infosForInstruction
                }
                IFEQ, IFNE -> {
                    if (instruction.incomingEdges.size == 1) {
                        val previousInstruction = instruction.incomingEdges.first().from
                        val previousMetadata = previousInstruction.metadata
                        if (previousMetadata is AsmInstructionMetadata
                                && previousMetadata.asmInstruction.getOpcode() == INSTANCEOF) {
                            val (falseEdge, trueEdge) = getFalseTrueEdges()

                            val (instanceOfEdge, notInstanceOfEdge) =
                                if (opcode == IFNE) Pair(trueEdge, falseEdge) else Pair(falseEdge, trueEdge)

                            propagateTransformedValueInfos(instanceOfEdge, previousInstruction[STATE_BEFORE]!!)
                                { wasInfo -> if (wasInfo == CONFLICT || wasInfo == NULL) CONFLICT else NOT_NULL }

                            propagateTransformedValueInfos(notInstanceOfEdge, previousInstruction[STATE_BEFORE]!!, null)

                            return infosForInstruction
                        }
                    }
                }
                else -> Unit.VALUE
            }
        }

        // propagate to outgoing edges as is
        instruction.outgoingEdges.forEach { edge -> setValueInfosForEdge(edge, infosForInstruction) }

        return infosForInstruction
    }
}

public class ValueNullabilityMap(m: Map<Value, NullabilityValueInfo> = Collections.emptyMap()): HashMap<Value, NullabilityValueInfo>(m) {
    override fun get(key: Any?): NullabilityValueInfo {
        val fromSuper = super.get(key)
        if (fromSuper != null) return fromSuper

        if (key !is TypedValue) throw IllegalStateException("Trying to get with key which is not TypedValue")

        val createdAtInsn = key.createdAtInsn

        if (createdAtInsn != null) {
            return when (createdAtInsn.getOpcode()) {
                NEW, NEWARRAY, ANEWARRAY, MULTIANEWARRAY -> NOT_NULL
                ACONST_NULL -> NULL
                LDC, 19 /* LDC_W */ -> NOT_NULL
                INVOKEDYNAMIC, INVOKEINTERFACE, INVOKESTATIC, INVOKESPECIAL, INVOKEVIRTUAL ->
                    UNKNOWN // TODO load from annotations
                GETFIELD, GETSTATIC -> UNKNOWN // TODO load from annotations
                AALOAD -> UNKNOWN
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
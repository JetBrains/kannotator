package org.jetbrains.kannotator.annotationsInference.nullability

import org.objectweb.asm.Opcodes.*
import java.util.Collections
import java.util.HashMap
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.jetbrains.kannotator.annotationsInference.findMethodByMethodInsnNode
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityValueInfo.*
import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.controlFlow.ControlFlowEdge
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.State
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlow.builder
import org.jetbrains.kannotator.controlFlow.builder.STATE_BEFORE
import org.jetbrains.kannotator.controlFlow.builder.TypedValue
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.kannotator.annotationsInference.generateAssertsForCallArguments
import org.jetbrains.kannotator.annotationsInference.forInterestingValue

class FramesNullabilityManager(
        val positions: PositionsForMethod,
        val annotations: Annotations<NullabilityAnnotation>,
        val declarationIndex: DeclarationIndex
) {
    private val nullabilityInfosForEdges : MutableMap<ControlFlowEdge, ValueNullabilityMap> = hashMap()

    private fun setValueInfosForEdge(edge: ControlFlowEdge, infos: ValueNullabilityMap) {
        assertNull(nullabilityInfosForEdges[edge])
        nullabilityInfosForEdges[edge] = infos
    }

    private fun mergeInfosFromIncomingEdges(instruction: Instruction) : ValueNullabilityMap {
        val incomingEdgesMaps = instruction.incomingEdges.map { e -> nullabilityInfosForEdges[e] }.filterNotNull()
        return mergeValueNullabilityMaps(positions, annotations, declarationIndex, incomingEdgesMaps)
    }

    data class BranchingInstructionEdges (val falseEdge: ControlFlowEdge, val trueEdge: ControlFlowEdge, val exceptionEdges: Collection<ControlFlowEdge>)

    fun computeNullabilityInfosForInstruction(instruction: Instruction) : ValueNullabilityMap {
        val state = instruction[STATE_BEFORE]!!

        val infosForInstruction = mergeInfosFromIncomingEdges(instruction)

        fun getFalseTrueEdges(): BranchingInstructionEdges {
            // first outgoing edge is 'false', second is 'true', remaining (if any) are exceptions-related
            // this order is provided by code in ASM's Analyzer

            val (exceptionEdges, falseTrueEdges) = instruction.outgoingEdges.partition { e -> e.exception }

            assertTrue(falseTrueEdges.size() in 1..2,  "Instruction must have one 'true' and one optional 'false' outgoing edge: $instruction")

            // If there is only one non-exceptional edge, use it as both 'false' and 'true' edge (it means that corresponding 'if' has empty body)
            val it = falseTrueEdges.iterator()
            val falseEdge = it.next()
            val trueEdge = if (it.hasNext()) it.next() else falseEdge

            return BranchingInstructionEdges(falseEdge, trueEdge, exceptionEdges)
        }

        fun propagateConditionInfo(
                outgoingEdge: ControlFlowEdge,
                state: State,
                transform: ((NullabilityValueInfo) -> NullabilityValueInfo)?
        ) {
            if (transform == null) {
                setValueInfosForEdge(outgoingEdge, infosForInstruction)
                return
            }

            val infosForEdge = ValueNullabilityMap(positions, annotations, declarationIndex, infosForInstruction)
            val conditionSubjects = state.stack[0]
            for (value in conditionSubjects) {
                infosForEdge[value] = transform(infosForInstruction[value])
            }
            setValueInfosForEdge(outgoingEdge, infosForEdge)
        }

        // Note: propagator is used in the case of distinct 'false' (1st argument) and 'true' edges (2nd argument)
        fun propagateBranchingInstructionEdges(
                edges: BranchingInstructionEdges,
                propagator: (ControlFlowEdge, ControlFlowEdge) -> Unit
        ) {
            val (falseEdge, trueEdge, remainingEdges) = getFalseTrueEdges()
            if (trueEdge != falseEdge) {
                propagator(falseEdge, trueEdge)
            } else {
                setValueInfosForEdge(falseEdge, infosForInstruction)
            }

            remainingEdges.forEach { edge -> setValueInfosForEdge(edge, infosForInstruction) }
        }

        addInfoForDereferencingInstruction(instruction, infosForInstruction)

        val opcode = instruction.getOpcode()
        when (opcode) {
            IFNULL, IFNONNULL -> {
                propagateBranchingInstructionEdges(
                        getFalseTrueEdges(),
                        {(falseEdge, trueEdge) ->
                            val (nullEdge, notNullEdge) =
                            if (opcode == IFNULL) Pair(trueEdge, falseEdge) else Pair(falseEdge, trueEdge)

                            propagateConditionInfo(nullEdge, state) { wasInfo ->
                                if (wasInfo == CONFLICT || wasInfo == NOT_NULL) CONFLICT else NULL }

                            propagateConditionInfo(notNullEdge, state) { wasInfo ->
                                if (wasInfo == CONFLICT || wasInfo == NULL) CONFLICT else NOT_NULL }
                        }
                )
                return infosForInstruction
            }
            IFEQ, IFNE -> {
                if (instruction.incomingEdges.size == 1) {
                    val previousInstruction = instruction.incomingEdges.first().from
                    if (previousInstruction.getOpcode() == INSTANCEOF) {
                        propagateBranchingInstructionEdges(
                                getFalseTrueEdges(),
                                {(falseEdge, trueEdge) ->
                                    val (instanceOfEdge, notInstanceOfEdge) =
                                    if (opcode == IFNE) Pair(trueEdge, falseEdge) else Pair(falseEdge, trueEdge)

                                    propagateConditionInfo(instanceOfEdge, previousInstruction[STATE_BEFORE]!!)
                                    { wasInfo -> if (wasInfo == CONFLICT || wasInfo == NULL) CONFLICT else NOT_NULL }

                                    propagateConditionInfo(notInstanceOfEdge, previousInstruction[STATE_BEFORE]!!, null)
                                }
                        )
                        return infosForInstruction
                    }
                }
            }
            else -> {}
        }

        // propagate to outgoing edges as is
        instruction.outgoingEdges.forEach { edge -> setValueInfosForEdge(edge, infosForInstruction) }

        return infosForInstruction
    }

    private fun addInfoForDereferencingInstruction(instruction: Instruction, infosForInstruction: ValueNullabilityMap) {
        val state = instruction[STATE_BEFORE]!!

        fun markStackValueAsNotNull(indexFromTop: Int) {
            val valueSet = state.stack[indexFromTop]
            for (value in valueSet) {
                val wasInfo = infosForInstruction[value]
                infosForInstruction[value] = if (wasInfo == CONFLICT || wasInfo == NULL) wasInfo else NOT_NULL
            }
        }

        when (instruction.getOpcode()) {
            INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC, INVOKESTATIC -> {
                generateAssertsForCallArguments(instruction, declarationIndex, annotations,
                        { indexFromTop -> markStackValueAsNotNull(indexFromTop) },
                        true,
                        { paramAnnotation -> paramAnnotation == NullabilityAnnotation.NOT_NULL })
            }
            GETFIELD, ARRAYLENGTH, ATHROW,
            MONITORENTER, MONITOREXIT -> {
                markStackValueAsNotNull(0)
            }
            AALOAD, BALOAD, IALOAD, CALOAD, SALOAD, FALOAD, LALOAD, DALOAD,
            PUTFIELD -> {
                markStackValueAsNotNull(1)
            }
            AASTORE, BASTORE, IASTORE, CASTORE, SASTORE, FASTORE, LASTORE, DASTORE -> {
                markStackValueAsNotNull(2)
            }
            else -> {}
        }
    }
}

public class ValueNullabilityMap(
        val positions: PositionsForMethod,
        val annotations: Annotations<NullabilityAnnotation>,
        val declarationIndex: DeclarationIndex,
        m: Map<Value, NullabilityValueInfo> = Collections.emptyMap()
): HashMap<Value, NullabilityValueInfo>(m) {
    override fun get(key: Any?): NullabilityValueInfo {
        val fromSuper = super.get(key)
        if (fromSuper != null) return fromSuper

        if (key !is TypedValue) throw IllegalStateException("Trying to get with key which is not TypedValue")

        val createdAtInsn = key.createdAtInsn

        if (createdAtInsn != null) {
            return when (createdAtInsn.getOpcode()) {
                NEW, NEWARRAY, ANEWARRAY, MULTIANEWARRAY -> NOT_NULL
                ACONST_NULL -> NULL
                LDC -> NOT_NULL
                INVOKEDYNAMIC, INVOKEINTERFACE, INVOKESTATIC, INVOKESPECIAL, INVOKEVIRTUAL -> {
                    if (createdAtInsn.getOpcode() == INVOKEDYNAMIC)
                        UNKNOWN
                    else {
                        val method = declarationIndex.findMethodByMethodInsnNode(createdAtInsn as MethodInsnNode)
                        if (method != null) {
                            val positions = PositionsForMethod(method)
                            val paramAnnotation = annotations[positions.forReturnType().position]
                            paramAnnotation.toValueInfo()
                        }
                        else {
                            UNKNOWN
                        }
                    }
                }
                GETFIELD, GETSTATIC -> UNKNOWN // TODO load from annotations
                AALOAD -> UNKNOWN
                else -> throw UnsupportedOperationException("Unsupported opcode=${createdAtInsn.getOpcode()}")
            }
        }
        if (key.interesting) {
            return annotations[positions.forInterestingValue(key)].toValueInfo()
        }
        return when (key) {
            builder.NULL_VALUE -> NULL
            builder.PRIMITIVE_VALUE_SIZE_1, builder.PRIMITIVE_VALUE_SIZE_2 ->
                throw IllegalStateException("trying to get nullabilty info for primitive")
            else -> NOT_NULL // this is either "this" or caught exception
        }
    }
}

fun mergeValueNullabilityMaps(
        positions: PositionsForMethod,
        annotations: Annotations<NullabilityAnnotation>,
        declarationIndex: DeclarationIndex,
        maps: Collection<ValueNullabilityMap>
): ValueNullabilityMap {
    val result = ValueNullabilityMap(positions, annotations, declarationIndex)
    val affectedValues = maps.flatMap { m -> m.keySet() }.toSet()

    for (m in maps) {
        for (value in affectedValues) {
            result[value] =  if (result.containsKey(value)) m[value] merge result[value] else m[value]
        }
    }
    return result
}
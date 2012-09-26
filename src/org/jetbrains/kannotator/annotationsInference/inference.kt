package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlow.State
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import java.util.HashMap
import java.util.Collections

class NullabilityAssert(val shouldBeNotNullValue: Value)

class NullabilityAnnotation {
    val parametersValueInfo = hashMap<Value, NullabilityValueInfo>()
    val returnValueInfo = arrayList<NullabilityValueInfo>()

    fun addParameterValueInfo(value: Value, valueInfo: NullabilityValueInfo) {
        parametersValueInfo[value] = valueInfo
    }

    fun addReturnValueInfo(valueInfo: NullabilityValueInfo) {
        returnValueInfo.add(valueInfo)
    }
}

fun NullabilityAnnotation.addAssert(assert: NullabilityAssert) {
    addParameterValueInfo(assert.shouldBeNotNullValue, NOT_NULL)
}

fun inferAnnotations(graph: ControlFlowGraph) : List<NullabilityValueInfo> {
    val annotation = NullabilityAnnotation()
    for (instruction in graph.instructions) {
        analyzeInstruction(instruction, annotation)
    }
    val result = arrayList<NullabilityValueInfo>(annotation.returnValueInfo.merge())
    result.addAll(annotation.parametersValueInfo.values())
    return result
}

fun analyzeInstruction(instruction: Instruction, annotation: NullabilityAnnotation) {
    val state = instruction[STATE_BEFORE]
    if (state == null) return

    val nullabilityInfosForInstruction = computeNullabilityInfosForInstruction(instruction, state)

    val asserts = generateAsserts(instruction)
    for (assert in asserts) {
        if (!checkAssertionIsSatisfied(assert, {value -> getNullabilityInfo(nullabilityInfosForInstruction, value)})) {
            annotation.addAssert(assert)
        }
    }
}

fun checkReturnInstruction(instruction: Instruction, annotation: NullabilityAnnotation, nullabilityInfosForInstruction: Map<Value, NullabilityValueInfo>) {
    fun checkAllValuesOnReturn() {
        for ((value, nullabilityValueInfo) in nullabilityInfosForInstruction) {
            if (value.interesting && nullabilityValueInfo == NULL) {
                annotation.addParameterValueInfo(value, NULLABLE)
            }
        }
    }

    val state = instruction[STATE_BEFORE]
    if (state == null) return

    val instructionMetadata = instruction.metadata
    if (instructionMetadata is AsmInstructionMetadata) {
        when (instructionMetadata.asmInstruction.getOpcode()) {
            ARETURN -> {
                val valueSet = state.stack[0]
                val nullabilityValueInfo = valueSet
                        .map { it -> getNullabilityInfo(nullabilityInfosForInstruction, it) }.merge()
                annotation.addReturnValueInfo(nullabilityValueInfo)
                checkAllValuesOnReturn()
            }
            RETURN, IRETURN, LRETURN, DRETURN, FRETURN -> {
                checkAllValuesOnReturn()
            }
            else -> Unit.VALUE
        }
    }
}

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

fun checkAssertionIsSatisfied(assert: NullabilityAssert, nullabilityValueInfoMapping: (Value) -> NullabilityValueInfo) : Boolean {
    if (!assert.shouldBeNotNullValue.interesting) return true

    val valueInfo = nullabilityValueInfoMapping(assert.shouldBeNotNullValue)
    return valueInfo == NOT_NULL || valueInfo == CONFLICT
}

val nullabilityInfosForEdges : MutableMap<ControlFlowEdge, Map<Value, NullabilityValueInfo>> = hashMap()

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
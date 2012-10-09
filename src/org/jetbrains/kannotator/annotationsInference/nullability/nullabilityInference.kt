package org.jetbrains.kannotator.annotationsInference.nullability

import org.objectweb.asm.Opcodes.*
import kotlin.test.assertTrue
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityValueInfo.*
import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlow.builder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.jetbrains.kannotator.declarations.TypePosition
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.annotationsInference.generateAssertsForCallArguments
import kotlinlib.emptyList
import org.jetbrains.kannotator.annotationsInference.traverseInstructions
import org.jetbrains.kannotator.annotationsInference.forInterestingValue
import org.jetbrains.kannotator.declarations.MutableAnnotations
import org.jetbrains.kannotator.declarations.setIfNotNull

fun buildNullabilityAnnotations(
        graph: ControlFlowGraph,
        positions: PositionsWithinMember,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<NullabilityAnnotation>
): Annotations<NullabilityAnnotation> {

    val framesManager = FramesNullabilityManager(positions, annotations, declarationIndex)
    val valueNullabilityMapsOnReturn = arrayList<ValueNullabilityMap>()
    var returnValueInfo : NullabilityValueInfo? = null

    fun collectValueInfoForReturnInstruction(
            instruction: Instruction,
            nullabilityInfos: ValueNullabilityMap
    ) {
        if (instruction.getOpcode() == ARETURN) {
            val state = instruction[STATE_BEFORE]!!
            val returnValues = state.stack[0]
            val nullabilityValueInfo = returnValues.map { it -> nullabilityInfos[it] }.merge()
            returnValueInfo = nullabilityValueInfo.mergeWithNullable(returnValueInfo)
        }
        valueNullabilityMapsOnReturn.add(nullabilityInfos)
    }

    fun createAnnotations(): Annotations<NullabilityAnnotation> {
        val result = AnnotationsImpl<NullabilityAnnotation>()
        result.setIfNotNull(positions.forReturnType().position, returnValueInfo?.toAnnotation())

        val mapOnReturn = mergeValueNullabilityMaps(positions, result, declarationIndex, valueNullabilityMapsOnReturn)
        for ((value, valueInfo) in mapOnReturn) {
            if (value.interesting) {
                result.setIfNotNull(positions.forInterestingValue(value), valueInfo.toAnnotation())
            }
        }
        return result
    }


    graph.traverseInstructions {
        instruction ->
        val valueNullabilityMap = framesManager.computeNullabilityInfosForInstruction(instruction)
        if (instruction.getOpcode() in RETURN_OPCODES) {
            collectValueInfoForReturnInstruction(instruction, valueNullabilityMap)
        }
    }

    return createAnnotations()
}

private val RETURN_OPCODES = hashSet(ARETURN, RETURN, IRETURN, LRETURN, DRETURN, FRETURN)


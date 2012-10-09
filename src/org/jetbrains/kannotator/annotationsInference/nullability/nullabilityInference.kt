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

fun buildNullabilityAnnotations(
        graph: ControlFlowGraph,
        positions: PositionsWithinMember,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<NullabilityAnnotation>
) : Annotations<NullabilityAnnotation> {

    val framesManager = FramesNullabilityManager(positions, annotations, declarationIndex)
    val valueNullabilityMapsOnReturn = arrayList<ValueNullabilityMap>()
    var returnValueInfo : NullabilityValueInfo? = null

    fun checkReturnInstruction(
            instruction: Instruction,
            nullabilityInfos: ValueNullabilityMap
    ) {
        val state = instruction[STATE_BEFORE]!!

        when (instruction.getOpcode()) {
            ARETURN -> {
                val valueSet = state.stack[0]
                val nullabilityValueInfo = valueSet.map { it -> nullabilityInfos[it] }.merge()
                returnValueInfo = nullabilityValueInfo.mergeWithNullable(returnValueInfo)
                valueNullabilityMapsOnReturn.add(nullabilityInfos)
            }
            RETURN, IRETURN, LRETURN, DRETURN, FRETURN -> {
                valueNullabilityMapsOnReturn.add(nullabilityInfos)
            }
            else -> {}
        }
    }

    graph.traverseInstructions { instruction ->
        framesManager.computeNullabilityInfosForInstruction(instruction)
    }

    framesManager.clear()

    graph.traverseInstructions { instruction ->
        checkReturnInstruction(instruction, framesManager.computeNullabilityInfosForInstruction(instruction))
    }

    return run {
        val result = AnnotationsImpl<NullabilityAnnotation>()
        fun setAnnotation(position: TypePosition, annotation: NullabilityAnnotation?) {
            if (annotation != null) {
                result[position] = annotation
            }
        }
        setAnnotation(positions.forReturnType().position, returnValueInfo?.toAnnotation())

        val mapOnReturn = mergeValueNullabilityMaps(positions, result, declarationIndex, valueNullabilityMapsOnReturn)
        for ((value, valueInfo) in mapOnReturn) {
            if (value.interesting) {
                setAnnotation(positions.forInterestingValue(value), valueInfo.toAnnotation())
            }
        }
        result
    }
}

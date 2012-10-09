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

class NullabilityAnnotationInferrer(
        private val graph: ControlFlowGraph,
        annotations: Annotations<NullabilityAnnotation>,
        val positions: PositionsWithinMember,
        val declarationIndex: DeclarationIndex
) {
    private val framesManager = FramesNullabilityManager(positions, annotations, declarationIndex)
    val valueNullabilityMapsOnReturn = arrayList<ValueNullabilityMap>()
    var returnValueInfo : NullabilityValueInfo? = null

    fun buildAnnotations() : Annotations<NullabilityAnnotation> {
        graph.traverseInstructions { instruction ->
            framesManager.computeNullabilityInfosForInstruction(instruction) }

        framesManager.clear()

        graph.traverseInstructions { instruction ->
            checkReturnInstruction(instruction, framesManager.computeNullabilityInfosForInstruction(instruction))
        }
        return toAnnotations()
    }

    private fun checkReturnInstruction(
            instruction: Instruction,
            nullabilityInfos: ValueNullabilityMap
    ) {
        val state = instruction[STATE_BEFORE]!!

        when (instruction.getOpcode()) {
            ARETURN -> {
                val valueSet = state.stack[0]
                val nullabilityValueInfo = valueSet.map { it -> nullabilityInfos[it] }.merge()
                addReturnValueInfo(nullabilityValueInfo)

                addValueNullabilityMapOnReturn(nullabilityInfos)
            }
            RETURN, IRETURN, LRETURN, DRETURN, FRETURN -> {
                addValueNullabilityMapOnReturn(nullabilityInfos)
            }
            else -> Unit.VALUE
        }
    }

    fun addValueNullabilityMapOnReturn(map: ValueNullabilityMap) {
        valueNullabilityMapsOnReturn.add(map)
    }

    fun addReturnValueInfo(valueInfo: NullabilityValueInfo) {
        val current = returnValueInfo
        returnValueInfo = valueInfo.mergeWithNullable(current)
    }

    private fun Value.getParameterPosition() = positions.forParameter(this.parameterIndex!!).position

    fun toAnnotations(): Annotations<NullabilityAnnotation> {
        val annotations = AnnotationsImpl<NullabilityAnnotation>()
        fun setAnnotation(position: TypePosition, annotation: NullabilityAnnotation?) {
            if (annotation != null) {
                annotations[position] = annotation
            }
        }
        setAnnotation(positions.forReturnType().position, returnValueInfo?.toAnnotation())

        val mapOnReturn = mergeValueNullabilityMaps(positions, annotations, declarationIndex, valueNullabilityMapsOnReturn)
        for ((value, valueInfo) in mapOnReturn) {
            if (value.interesting) {
                setAnnotation(value.getParameterPosition(), valueInfo.toAnnotation())
            }
        }
        return annotations
    }
}

package org.jetbrains.kannotator.annotationsInference.nullability

import org.objectweb.asm.Opcodes.*
import kotlin.test.assertTrue
import org.jetbrains.kannotator.annotationsInference.AbstractAnnotationInferrer
import org.jetbrains.kannotator.annotationsInference.AnnotationManager
import org.jetbrains.kannotator.annotationsInference.Assert
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

class NullabilityAnnotationInferrer(
        graph: ControlFlowGraph,
        annotations: Annotations<NullabilityAnnotation>,
        positions: PositionsWithinMember,
        declarationIndex: DeclarationIndex
) : AbstractAnnotationInferrer<NullabilityAnnotation, NullabilityValueInfo>(graph, annotations, positions, declarationIndex,
        NullabilityAnnotationManager(annotations, declarationIndex, positions)) {

    //todo make property without backing field (after KT-2892)
    private val nullabilityAnnotationManager : NullabilityAnnotationManager = annotationManager as NullabilityAnnotationManager
    private val framesManager = FramesNullabilityManager(nullabilityAnnotationManager, annotations, declarationIndex)

    override fun computeValueInfos(instruction: Instruction) : ValueNullabilityMap =
            framesManager.computeNullabilityInfosForInstruction(instruction)

    override fun isAnnotationNecessary(
            assert: Assert,
            valueInfos: Map<Value, NullabilityValueInfo>
    ): Boolean {
        if (!assert.value.interesting) return false

        val valueInfo = valueInfos[assert.value]
        return valueInfo == NULLABLE || valueInfo == UNKNOWN
    }


    override fun postProcess() {
        framesManager.clear()
        traverseInstructions { instruction ->
            checkReturnInstruction(instruction, computeValueInfos(instruction))
        }
    }

    protected override fun generateAsserts(instruction: Instruction): Collection<Assert> {
        return emptyList()
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
                nullabilityAnnotationManager.addReturnValueInfo(nullabilityValueInfo)

                nullabilityAnnotationManager.addValueNullabilityMapOnReturn(nullabilityInfos)
            }
            RETURN, IRETURN, LRETURN, DRETURN, FRETURN -> {
                nullabilityAnnotationManager.addValueNullabilityMapOnReturn(nullabilityInfos)
            }
            else -> Unit.VALUE
        }
    }
}

private class NullabilityAnnotationManager(
        val annotations: Annotations<NullabilityAnnotation>,
        val declarationIndex: DeclarationIndex,
        val positions: PositionsWithinMember
) : AnnotationManager<NullabilityAnnotation>() {

    val valueNullabilityMapsOnReturn = arrayList<ValueNullabilityMap>()
    var returnValueInfo : NullabilityValueInfo? = null

    fun addValueNullabilityMapOnReturn(map: ValueNullabilityMap) {
        valueNullabilityMapsOnReturn.add(map)
    }

    fun addReturnValueInfo(valueInfo: NullabilityValueInfo) {
        val current = returnValueInfo
        returnValueInfo = valueInfo.mergeWithNullable(current)
    }

    override fun addAssert(assert: Assert) {
    }

    fun getParameterAnnotation(value: Value) : NullabilityAnnotation? {
        assertTrue(value.interesting)
        return annotations[value.getParameterPosition()]
    }

    private fun Value.getParameterPosition() = positions.forParameter(this.parameterIndex!!).position

    override fun toAnnotations(): Annotations<NullabilityAnnotation> {
        val annotations = AnnotationsImpl<NullabilityAnnotation>()
        fun setAnnotation(position: TypePosition, annotation: NullabilityAnnotation?) {
            if (annotation != null) {
                annotations[position] = annotation
            }
        }
        setAnnotation(positions.forReturnType().position, returnValueInfo?.toAnnotation())

        val mapOnReturn = mergeValueNullabilityMaps(this, annotations, declarationIndex, valueNullabilityMapsOnReturn)
        for ((value, valueInfo) in mapOnReturn) {
            if (value.interesting) {
                setAnnotation(value.getParameterPosition(), valueInfo.toAnnotation())
            }
        }
        return annotations
    }
}

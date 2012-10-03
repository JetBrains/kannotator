package org.jetbrains.kannotator.annotationsInference

import org.objectweb.asm.Opcodes.*
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlowBuilder.AsmInstructionMetadata
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.Positions
import org.jetbrains.kannotator.declarations.TypePosition
import org.jetbrains.kannotator.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.nullability.NullabilityValueInfo
import org.jetbrains.kannotator.nullability.NullabilityValueInfo.*
import org.jetbrains.kannotator.nullability.merge
import org.jetbrains.kannotator.nullability.toAnnotation
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.nullability.Nullability
import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.nullability.mergeWithNullable

class NullabilityAnnotationsInference(
        graph: ControlFlowGraph
) : AnnotationsInference<Nullability>(graph, NullabilityAnnotationsManager()) {

    private val framesManager = FramesNullabilityManager()
    //todo make property without backing field (after KT-2892)
    private val nullabilityAnnotationManager : NullabilityAnnotationsManager = annotationsManager as NullabilityAnnotationsManager

    override fun computeValueInfos(instruction: Instruction) : ValueNullabilityMap =
            framesManager.computeNullabilityInfosForInstruction(instruction)

    override fun postProcess(instruction: Instruction, valueInfos: Map<Value, ValueInfo<Nullability>>) =
        checkReturnInstruction(instruction, valueInfos as ValueNullabilityMap)

    override fun isAnnotationNecessary(
            assert: Assert<Nullability>,
            valueInfos: Map<Value, ValueInfo<Nullability>>
    ): Boolean {
        if (!assert.value.interesting) return false

        val valueInfo = valueInfos[assert.value]
        return valueInfo == NULLABLE || valueInfo == UNKNOWN
    }

    protected override fun generateAsserts(instruction: Instruction): Collection<Assert<Nullability>> =
        generateNullabilityAsserts(instruction)

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

private class NullabilityAnnotationsManager : AnnotationsManager<Nullability>() {
    val parameterAnnotations = hashMap<Value, NullabilityAnnotation>()
    val valueNullabilityMapsOnReturn = arrayList<ValueNullabilityMap>()
    var returnValueInfo : NullabilityValueInfo? = null

    fun addParameterAnnotation(value: Value, annotation: NullabilityAnnotation) {
        parameterAnnotations[value] = annotation
    }

    fun addValueNullabilityMapOnReturn(map: ValueNullabilityMap) {
        valueNullabilityMapsOnReturn.add(map)
    }

    fun addReturnValueInfo(valueInfo: NullabilityValueInfo) {
        val current = returnValueInfo
        returnValueInfo = valueInfo.mergeWithNullable(current)
    }

    override fun addAssert(assert: Assert<Nullability>) {
        addParameterAnnotation(assert.value, NullabilityAnnotation.NOT_NULL)
    }

    override fun toAnnotations(positions: Positions): Annotations<Annotation<Nullability>> {
        val annotations = AnnotationsImpl<NullabilityAnnotation>()
        fun setAnnotation(position: TypePosition, annotation: NullabilityAnnotation?) {
            if (annotation != null) {
                annotations[position] = annotation
            }
        }
        fun Value.getParameterPosition() = positions.forParameter(this.parameterIndex!!).position

        setAnnotation(positions.forReturnType().position, returnValueInfo?.toAnnotation())

        val mapOnReturn = mergeValueNullabilityMaps(valueNullabilityMapsOnReturn)
        for ((value, valueInfo) in mapOnReturn) {
            if (value.interesting) {
                setAnnotation(value.getParameterPosition(), valueInfo.toAnnotation())
            }
        }
        for ((value, annotation) in parameterAnnotations) {
            setAnnotation(value.getParameterPosition(), annotation)
        }
        return annotations
    }
}

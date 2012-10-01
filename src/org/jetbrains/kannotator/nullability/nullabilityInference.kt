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

class NullabilityAnnotationsInference(
        graph: ControlFlowGraph
) : AnnotationsInference<Nullability>(graph, NullabilityAnnotationsManager()) {

    private val framesManager = FramesNullabilityManager()
    //todo make property without backing field (compiler's bug)
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
            nullabilityInfosForInstruction: ValueNullabilityMap
    ) {
        fun checkAllValuesOnReturn() {
            // this function is invoked for each *RETURN instruction:
            // if parameter becomes NULL here, then it should be annotated as nullable
            for ((value, nullabilityValueInfo) in nullabilityInfosForInstruction) {
                if (value.interesting && nullabilityValueInfo == NULL) {
                    nullabilityAnnotationManager.addParameterAnnotation(value, NullabilityAnnotation.NULLABLE)
                }
            }
        }

        val state = instruction[STATE_BEFORE]!!

        when (instruction.getOpcode()) {
            ARETURN -> {
                val valueSet = state.stack[0]
                val nullabilityValueInfo = valueSet.map { it -> nullabilityInfosForInstruction[it] }.merge()
                nullabilityAnnotationManager.addReturnValueInfo(nullabilityValueInfo)
                checkAllValuesOnReturn()
            }
            RETURN, IRETURN, LRETURN, DRETURN, FRETURN -> {
                checkAllValuesOnReturn()
            }
            else -> Unit.VALUE
        }
    }
}

private class NullabilityAnnotationsManager : AnnotationsManager<Nullability>() {
    val parameterAnnotations = hashMap<Value, NullabilityAnnotation>()
    var returnValueInfo : NullabilityValueInfo? = null

    fun addParameterAnnotation(value: Value, annotation: NullabilityAnnotation) {
        parameterAnnotations[value] = annotation
    }

    fun addReturnValueInfo(valueInfo: NullabilityValueInfo) {
        val current = returnValueInfo
        returnValueInfo = if (current == null) valueInfo else current.merge(valueInfo)
    }

    override fun addAssert(assert: Assert<Nullability>) {
        addParameterAnnotation(assert.value, NullabilityAnnotation.NOT_NULL)
    }

    override fun toAnnotations(positions: Positions): Annotations<Annotation<Nullability>> {
        val annotations = AnnotationsImpl<NullabilityAnnotation>()
        fun setAnnotation(position: TypePosition, annotation: NullabilityAnnotation?) {
            if (annotation != null) {
                annotations.set(position, annotation)
            }
        }
        setAnnotation(positions.forReturnType().position, returnValueInfo?.toAnnotation())
        for ((value, annotation) in parameterAnnotations) {
            setAnnotation(positions.forParameter(value.parameterIndex!!).position, annotation)
        }
        return annotations
    }
}

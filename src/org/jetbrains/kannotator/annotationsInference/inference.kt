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
import org.jetbrains.kannotator.funDependecy.GlobalMethodSearcher

fun buildAnnotations(
        graph: ControlFlowGraph,
        positions: Positions,
        methodSearcher: GlobalMethodSearcher,
        annotations: Annotations<NullabilityAnnotation>
) : Annotations<NullabilityAnnotation> {

    val annotationsInference = AnnotationsInference(graph)
    annotationsInference.process()
    return annotationsInference.getResult().toAnnotations(positions)
}

class AnnotationsInference(private val graph: ControlFlowGraph) {
    private val framesManager = FramesNullabilityManager()
    private val annotationsManager = NullabilityAnnotationsManager()

    fun process() {
        for (instruction in graph.instructions) {
            analyzeInstruction(instruction, annotationsManager)
        }
    }

    fun getResult(): NullabilityAnnotationsManager = annotationsManager

    private fun analyzeInstruction(instruction: Instruction, annotation: NullabilityAnnotationsManager) {
        if (instruction[STATE_BEFORE] == null) return

        val nullabilityInfosForInstruction = framesManager.computeNullabilityInfosForInstruction(instruction)

        val asserts = generateAsserts(instruction)
        for (assert in asserts) {
            if (!checkAssertionIsSatisfied(assert, nullabilityInfosForInstruction)) {
                annotation.addAssert(assert)
            }
        }

        checkReturnInstruction(instruction, annotation, nullabilityInfosForInstruction)
    }

    private fun checkAssertionIsSatisfied(
            assert: NullabilityAssert,
            nullabilityInfosForInstruction: ValueNullabilityMap
    ): Boolean {
        if (!assert.shouldBeNotNullValue.interesting) return true

        val valueInfo = nullabilityInfosForInstruction[assert.shouldBeNotNullValue]
        return valueInfo == NOT_NULL || valueInfo == CONFLICT
    }

    private fun checkReturnInstruction(
            instruction: Instruction,
            annotationManager: NullabilityAnnotationsManager,
            nullabilityInfosForInstruction: ValueNullabilityMap
    ) {
        fun checkAllValuesOnReturn() {
            for ((value, nullabilityValueInfo) in nullabilityInfosForInstruction) {
                if (value.interesting && nullabilityValueInfo == NULL) {
                    annotationManager.addParameterAnnotation(value, NullabilityAnnotation.NULLABLE)
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
                    val nullabilityValueInfo = valueSet.map { it -> nullabilityInfosForInstruction[it] }.merge()
                    annotationManager.addReturnValueInfo(nullabilityValueInfo)
                    checkAllValuesOnReturn()
                }
                RETURN, IRETURN, LRETURN, DRETURN, FRETURN -> {
                    checkAllValuesOnReturn()
                }
                else -> Unit.VALUE
            }
        }
    }
}

private class NullabilityAnnotationsManager {
    val parameterAnnotations = hashMap<Value, NullabilityAnnotation>()
    var returnValueInfo : NullabilityValueInfo? = null

    fun addParameterAnnotation(value: Value, annotation: NullabilityAnnotation) {
        parameterAnnotations[value] = annotation
    }

    fun addReturnValueInfo(valueInfo: NullabilityValueInfo) {
        val current = returnValueInfo
        returnValueInfo = if (current == null) valueInfo else current.merge(valueInfo)
    }
}

private fun NullabilityAnnotationsManager.addAssert(assert: NullabilityAssert) {
    addParameterAnnotation(assert.shouldBeNotNullValue, NullabilityAnnotation.NOT_NULL)
}

private fun NullabilityAnnotationsManager.toAnnotations(positions: Positions): Annotations<NullabilityAnnotation> {
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

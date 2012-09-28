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

        val valueInfo = framesManager.getNullabilityInfo(nullabilityInfosForInstruction, assert.shouldBeNotNullValue)
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
                    annotationManager.addParameterValueInfo(value, NULLABLE)
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
    val parametersValueInfo = hashMap<Value, NullabilityValueInfo>()
    val returnValueInfo = arrayList<NullabilityValueInfo>()

    fun addParameterValueInfo(value: Value, valueInfo: NullabilityValueInfo) {
        parametersValueInfo[value] = valueInfo
    }

    fun addReturnValueInfo(valueInfo: NullabilityValueInfo) {
        returnValueInfo.add(valueInfo)
    }
}

private fun NullabilityAnnotationsManager.addAssert(assert: NullabilityAssert) {
    addParameterValueInfo(assert.shouldBeNotNullValue, NOT_NULL)
}

fun NullabilityAnnotationsManager.toAnnotations(positions: Positions): Annotations<NullabilityAnnotation> {
    val annotations = AnnotationsImpl<NullabilityAnnotation>()
    fun setAnnotation(position: TypePosition, annotation: NullabilityAnnotation?) {
        if (annotation != null) {
            annotations.set(position, annotation)
        }
    }
    setAnnotation(positions.forReturnType().position, returnValueInfo.merge().toAnnotation())
    for ((value, valueInfo) in parametersValueInfo) {
        setAnnotation(positions.forParameter(value.parameterIndex!!).position, valueInfo.toAnnotation())
    }
    return annotations
}

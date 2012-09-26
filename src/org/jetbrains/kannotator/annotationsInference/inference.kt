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
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.Positions
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.TypePosition
import org.jetbrains.kannotator.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.nullability.toAnnotation

class AnnotationsInference {
    private val framesManager = FramesNullabilityManager()

    class NullabilityAnnotationsManager {
        val parametersValueInfo = hashMap<Value, NullabilityValueInfo>()
        val returnValueInfo = arrayList<NullabilityValueInfo>()

        fun addParameterValueInfo(value: Value, valueInfo: NullabilityValueInfo) {
            parametersValueInfo[value] = valueInfo
        }

        fun addReturnValueInfo(valueInfo: NullabilityValueInfo) {
            returnValueInfo.add(valueInfo)
        }
    }

    fun NullabilityAnnotationsManager.addAssert(assert: NullabilityAssert) {
        addParameterValueInfo(assert.shouldBeNotNullValue, NOT_NULL)
    }

    fun NullabilityAnnotationsManager.toAnnotations(positions: Positions) : Annotations<NullabilityAnnotation> {
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

    fun inferAnnotations(graph: ControlFlowGraph, positions: Positions) : Annotations<NullabilityAnnotation> {
        val annotationsManager = NullabilityAnnotationsManager()
        for (instruction in graph.instructions) {
            analyzeInstruction(instruction, annotationsManager)
        }
        val result = arrayList<NullabilityValueInfo>(annotationsManager.returnValueInfo.merge())
        result.addAll(annotationsManager.parametersValueInfo.values())
        return annotationsManager.toAnnotations(positions)
    }

    fun analyzeInstruction(instruction: Instruction, annotation: NullabilityAnnotationsManager) {
        val state = instruction[STATE_BEFORE]
        if (state == null) return

        val nullabilityInfosForInstruction = framesManager.computeNullabilityInfosForInstruction(instruction, state)

        val asserts = generateAsserts(instruction)
        for (assert in asserts) {
            if (!checkAssertionIsSatisfied(assert, {value -> framesManager.getNullabilityInfo(nullabilityInfosForInstruction, value)})) {
                annotation.addAssert(assert)
            }
        }

        checkReturnInstruction(instruction, annotation, nullabilityInfosForInstruction)
    }

    fun checkReturnInstruction(instruction: Instruction, annotation: NullabilityAnnotationsManager, nullabilityInfosForInstruction: Map<Value, NullabilityValueInfo>) {
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
                            .map { it -> framesManager.getNullabilityInfo(nullabilityInfosForInstruction, it) }.merge()
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

    fun checkAssertionIsSatisfied(assert: NullabilityAssert, nullabilityValueInfoMapping: (Value) -> NullabilityValueInfo) : Boolean {
        if (!assert.shouldBeNotNullValue.interesting) return true

        val valueInfo = nullabilityValueInfoMapping(assert.shouldBeNotNullValue)
        return valueInfo == NOT_NULL || valueInfo == CONFLICT
    }
}
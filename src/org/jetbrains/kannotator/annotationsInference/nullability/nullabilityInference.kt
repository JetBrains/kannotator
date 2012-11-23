package org.jetbrains.kannotator.annotationsInference.nullability

import org.jetbrains.kannotator.annotationsInference.forInterestingValue
import org.jetbrains.kannotator.annotationsInference.traverseInstructions
import org.jetbrains.kannotator.asm.util.getAsmInstructionNode
import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.builder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.FieldInsnNode
import util.controlFlow.buildControlFlowGraph
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.asm.util.isPrimitiveOrVoidType
import org.jetbrains.kannotator.controlFlow.State

fun buildMethodNullabilityAnnotations(
        graph: ControlFlowGraph,
        positions: PositionsForMethod,
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

fun buildFieldNullabilityAnnotations(
        fieldInfo: FieldDependencyInfo,
        controlFlowGraphBuilder: (Method) -> ControlFlowGraph,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<NullabilityAnnotation>): Annotations<NullabilityAnnotation> {

    val fieldAnnotations = AnnotationsImpl(annotations)
    val field = fieldInfo.field

    if (field.getType().isPrimitiveOrVoidType()) {
        return AnnotationsImpl()
    }

    if (field.isFinal()) {
        if (field.isStatic()) {
            if (field.value != null) {
                fieldAnnotations[getFieldAnnotatedType(field).position] = NullabilityAnnotation.NOT_NULL
                return fieldAnnotations
            }
            else {
                // Get in class static initializer -> Infer nullability
                // TODO: Initializer can call other static functions and and use values of other fields
                val method = declarationIndex.findMethod(field.declaringClass, "<clinit>", "()V")
                if (method != null) {
                    val graph = controlFlowGraphBuilder(method)
                    return collectFieldInformationFromMethod(graph, field, PositionsForMethod(method), declarationIndex, annotations)
                }

                throw IllegalStateException("Static field ${fieldInfo} is not initialized and no static initializer found")
            }
        }
    }

    return AnnotationsImpl()
}

fun collectFieldInformationFromMethod(
        graph: ControlFlowGraph,
        field: Field,
        positions: PositionsForMethod,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<NullabilityAnnotation>): Annotations<NullabilityAnnotation> {
    val framesManager = FramesNullabilityManager(positions, annotations, declarationIndex)

    var fieldValueInfo: NullabilityValueInfo? = null

    fun collectValueInfoForFieldInitInstruction(instruction: Instruction, nullabilityInfos: ValueNullabilityMap) {
        if (instruction.getOpcode() == PUTSTATIC) {
            val fieldNode = instruction.getAsmInstructionNode() as FieldInsnNode
            if (field.name == fieldNode.name) {
                val state = instruction[STATE_BEFORE]!!
                val fieldValues = state.stack[0]
                val nullabilityValueInfo = fieldValues.map { it -> nullabilityInfos[it] }.merge()
                fieldValueInfo = nullabilityValueInfo.mergeWithNullable(fieldValueInfo)
            }
        }
    }

    fun createAnnotations(): Annotations<NullabilityAnnotation> {
        val result = AnnotationsImpl<NullabilityAnnotation>()
        result.setIfNotNull(getFieldAnnotatedType(field).position, fieldValueInfo?.toAnnotation())
        return result
    }

    graph.traverseInstructions {
        val valueNullabilityMap = framesManager.computeNullabilityInfosForInstruction(it)
        collectValueInfoForFieldInitInstruction(it, valueNullabilityMap)
    }

    return createAnnotations()
}


package org.jetbrains.kannotator.annotationsInference.mutability

import java.util.HashMap
import org.jetbrains.kannotator.annotationsInference.generateAssertsForCallArguments
import org.jetbrains.kannotator.annotationsInference.getReceiverValues
import org.jetbrains.kannotator.annotationsInference.traverseInstructions
import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlow.builder.AsmInstructionMetadata
import org.jetbrains.kannotator.controlFlow.builder.STATE_BEFORE
import org.jetbrains.kannotator.controlFlow.builder.TypedValue
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodInsnNode

fun buildMutabilityAnnotations(
        graph: ControlFlowGraph,
        positions: PositionsWithinMember,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<MutabilityAnnotation>
) : Annotations<MutabilityAnnotation> {

    val asm2GraphInstruction = run {
        val map = HashMap<AbstractInsnNode, Instruction>()
        graph.traverseInstructions { instruction ->
            val metadata = instruction.metadata
            if (metadata is AsmInstructionMetadata) {
                map[metadata.asmInstruction] = instruction
            }
        }
        map
    }

    val valueAnnotations = HashMap<Value, MutabilityAnnotation>()

    fun markMutable(value: Value) {
        if (value !is TypedValue || value._type == null) return
        valueAnnotations[value] = MutabilityAnnotation.MUTABLE

        val createdAtInsn = value.createdAtInsn
        if (createdAtInsn is MethodInsnNode && createdAtInsn.isMutabilityPropagatingInvocation()) {
            val instruction = asm2GraphInstruction[createdAtInsn]!!
            for (receiverValue in instruction.getReceiverValues()) {
                markMutable(receiverValue)
            }
        }
    }

    fun computeMutabilityForInstruction(instruction: Instruction) {
        val state = instruction[STATE_BEFORE]!!
        val asmInstruction = (instruction.metadata as? AsmInstructionMetadata)?.asmInstruction
        if (!(asmInstruction is MethodInsnNode)) return
        if (instruction.getOpcode() == INVOKEINTERFACE) {
            if (asmInstruction.isMutatingInvocation()) {
                for (receiverValue in instruction.getReceiverValues()) {
                    markMutable(receiverValue)
                }
            }
        }
        generateAssertsForCallArguments(instruction, declarationIndex, annotations,
                { indexFromTop -> state.stack[indexFromTop].forEach { value -> markMutable(value) }},
                false,
                { paramAnnotation -> paramAnnotation == MutabilityAnnotation.MUTABLE })
    }

    fun createAnnotations(): Annotations<MutabilityAnnotation> {
        val result = AnnotationsImpl<MutabilityAnnotation>()
        for ((value, annotation) in valueAnnotations) {
            if (value.interesting) {
                result[positions.forParameter(value.parameterIndex!!).position] = annotation
            }
        }
        return result
    }


    graph.traverseInstructions { instruction -> computeMutabilityForInstruction(instruction) }
    return createAnnotations()
}
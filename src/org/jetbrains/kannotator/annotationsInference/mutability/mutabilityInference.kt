package org.jetbrains.kannotator.annotationsInference.mutability

import java.util.Collections
import kotlinlib.emptyList
import org.jetbrains.kannotator.annotationsInference.getMethodIdByInstruction
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
import org.jetbrains.kannotator.declarations.TypePosition
import org.jetbrains.kannotator.declarations.getArgumentCount
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.kannotator.annotationsInference.generateAssertsForCallArguments
import org.jetbrains.kannotator.annotationsInference.traverseInstructions
import java.util.HashMap

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
        valueAnnotations[value] = MutabilityAnnotation.MUTABLE
        //todo propagate mutability
    }

    fun propagateMutability(createdAtInsn: MethodInsnNode) {
        if (!createdAtInsn.isMutabilityPropagatingInvocation()) return
        val instruction = asm2GraphInstruction[createdAtInsn]!!
        //todo bug
        val receiverValues = instruction[STATE_BEFORE]!!.stack[0]
        for (receiverValue in receiverValues) {
            markMutable(receiverValue)
        }
    }

    fun computeMutabilityForInstruction(instruction: Instruction) {
        val state = instruction[STATE_BEFORE]!!
        val asmInstruction = (instruction.metadata as? AsmInstructionMetadata)?.asmInstruction
        if (!(asmInstruction is MethodInsnNode)) return
        if (instruction.getOpcode() == INVOKEINTERFACE) {
            val methodId = getMethodIdByInstruction(instruction)
            val receiverValues = state.stack[methodId.getArgumentCount()]
            for (receiverValue in receiverValues) {
                if (receiverValue !is TypedValue || receiverValue._type == null) continue
                if (asmInstruction.isMutatingInvocation()) {
                    markMutable(receiverValue)
                    if (receiverValue.createdAtInsn is MethodInsnNode) {
                        propagateMutability(receiverValue.createdAtInsn)
                    }
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

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

fun buildMutabilityAnnotations(
        graph: ControlFlowGraph,
        positions: PositionsWithinMember,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<MutabilityAnnotation>
) : Annotations<MutabilityAnnotation> {

    val asm2GraphInstructionMap = run {
        val map = hashMap<AbstractInsnNode, Instruction>()
        graph.traverseInstructions { instruction ->
            val metadata = instruction.metadata
            if (metadata is AsmInstructionMetadata) {
                map[metadata.asmInstruction] = instruction
            }
        }
        map
    }

    val parameterAnnotations = hashMap<Value, MutabilityAnnotation>()

    fun generateAssert(value: Value) {
        parameterAnnotations[value] = MutabilityAnnotation.MUTABLE
    }

    fun generatePropagatingMutabilityAsserts(createdAtInsn: MethodInsnNode) {
        if (!createdAtInsn.isPropagatingMutability()) return
        val instruction = asm2GraphInstructionMap[createdAtInsn]!!
        val valueSet = instruction[STATE_BEFORE]!!.stack[0]
        for (value in valueSet) {
            generateAssert(value)
        }
    }

    fun generateAsserts(instruction: Instruction) {
        val state = instruction[STATE_BEFORE]!!
        val asmInstruction = (instruction.metadata as? AsmInstructionMetadata)?.asmInstruction
        if (!(asmInstruction is MethodInsnNode)) return
        if (instruction.getOpcode() == INVOKEINTERFACE) {
            val methodId = getMethodIdByInstruction(instruction)
            val valueSet = state.stack[methodId.getArgumentCount()]
            for (value in valueSet) {
                if (!(value is TypedValue) || value._type == null) continue;
                if (asmInstruction.isInvocationRequiredMutability()) {
                    generateAssert(value)
                    if (value.createdAtInsn is MethodInsnNode) {
                        generatePropagatingMutabilityAsserts(value.createdAtInsn)
                    }
                }
            }
        }
        generateAssertsForCallArguments(instruction, declarationIndex, annotations,
                { indexFromTop ->
                    state.stack[indexFromTop].forEach { value -> generateAssert(value) }
                },
                false,  { paramAnnotation -> paramAnnotation == MutabilityAnnotation.MUTABLE } )
    }

    graph.traverseInstructions { insn -> generateAsserts(insn) }
    return run {
        val result = AnnotationsImpl<MutabilityAnnotation>()
        for ((value, annotation) in parameterAnnotations) {
            if (value.interesting) {
                result[positions.forParameter(value.parameterIndex!!).position] = annotation
            }
        }
        result
    }
}

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

class MutabilityAnnotationInferrer(
        val graph: ControlFlowGraph,
        val annotations: Annotations<MutabilityAnnotation>,
        val positions: PositionsWithinMember,
        val declarationIndex: DeclarationIndex
) {
    private val asm2GraphInstructionMap = createInstructionMap()
    private val parameterAnnotations = hashMap<Value, MutabilityAnnotation>()

    private fun createInstructionMap() : Map<AbstractInsnNode, Instruction> {
        val map = hashMap<AbstractInsnNode, Instruction>()
        graph.traverseInstructions { instruction ->
            val metadata = instruction.metadata
            if (metadata is AsmInstructionMetadata) {
                map[metadata.asmInstruction] = instruction
            }
        }
        return map
    }

    fun buildAnnotations() : Annotations<MutabilityAnnotation> {
        graph.traverseInstructions { insn -> generateAsserts(insn) }
        return toAnnotations()
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
                if (isInvocationRequiredMutability(asmInstruction)) {
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

    private fun generateAssert(value: Value) {
        parameterAnnotations[value] = MutabilityAnnotation.MUTABLE
    }

    private fun isInvocationRequiredMutability(instruction: MethodInsnNode) : Boolean =
            mutableInterfaces.containsInvocation(instruction)

    private fun isPropagatingMutability(instruction: MethodInsnNode) : Boolean =
            propagatingMutability.containsInvocation(instruction)

    private fun Map<String, List<String>>.containsInvocation(instruction: MethodInsnNode) : Boolean {
        val className = instruction.owner!!.replace("/", ".")
        val methodName = instruction.name!!
        return this[className]?.contains(methodName) ?: false
    }

    private fun generatePropagatingMutabilityAsserts(createdAtInsn: MethodInsnNode) {
        if (!isPropagatingMutability(createdAtInsn)) return
        val instruction = asm2GraphInstructionMap[createdAtInsn]!!
        val valueSet = instruction[STATE_BEFORE]!!.stack[0]
        for (value in valueSet) {
            generateAssert(value)
        }
    }

    private fun toAnnotations(): Annotations<MutabilityAnnotation> {
        val annotations = AnnotationsImpl<MutabilityAnnotation>()
        fun setAnnotation(position: TypePosition, annotation: MutabilityAnnotation?) {
            if (annotation != null) {
                annotations.set(position, annotation)
            }
        }
        for ((value, annotation) in parameterAnnotations) {
            if (value.interesting) {
                setAnnotation(positions.forParameter(value.parameterIndex!!).position, annotation)
            }
        }
        return annotations
    }
}

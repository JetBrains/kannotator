package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.declarations.Positions
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.TypePosition
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.mutability.MutabilityAssert
import java.util.Collections
import org.objectweb.asm.Opcodes.*
import org.jetbrains.kannotator.controlFlowBuilder.AsmInstructionMetadata
import org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.kannotator.controlFlowBuilder.TypedValue
import org.objectweb.asm.Type
import org.jetbrains.kannotator.mutability.mutableInterfaces
import org.jetbrains.kannotator.mutability.propagatingMutability
import org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.kannotator.index.DeclarationIndex

fun buildMutabilityAnnotations(
        graph: ControlFlowGraph,
        positions: Positions,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<MutabilityAnnotation>
) : Annotations<MutabilityAnnotation> {

    val annotationsInference = MutabilityAnnotationsInference(graph)
    annotationsInference.process()
    return annotationsInference.getResult().toAnnotations(positions)
}

class MutabilityAnnotationsInference(private val graph: ControlFlowGraph) {
    private val annotationsManager = MutabilityAnnotationsManager()

    val instructionCache = hashMap<AbstractInsnNode, Instruction>()
    fun createInstructionCache(graph: ControlFlowGraph) {
        for (instruction in graph.instructions) {
            val metadata = instruction.metadata
            if (metadata is AsmInstructionMetadata) {
                instructionCache[metadata.asmInstruction] = instruction
            }
        }
    }

    fun process() {
        createInstructionCache(graph)
        for (instruction in graph.instructions) {
            analyzeInstruction(instruction, annotationsManager)
        }
    }

    fun getResult(): MutabilityAnnotationsManager = annotationsManager

    private fun analyzeInstruction(instruction: Instruction, annotation: MutabilityAnnotationsManager) {
        if (instruction[STATE_BEFORE] == null) return // dead instructions

        val asserts = generateAsserts(instruction)
        for (assert in asserts) {
            if (isAnnotationNecessary(assert)) {
                annotation.addAssert(assert)
            }
        }
    }

    private fun generateAsserts(instruction: Instruction) : Collection<MutabilityAssert> {
        val state = instruction[STATE_BEFORE]!!
        val result = hashSet<MutabilityAssert>()
        val asmInstruction = (instruction.metadata as? AsmInstructionMetadata)?.asmInstruction
        if (!(asmInstruction is MethodInsnNode)) return Collections.emptyList()
        when (instruction.getOpcode()) {
            INVOKEINTERFACE -> {
                val methodName = asmInstruction.name!!
                val valueSet = state.stack[0]
                for (value in valueSet) {
                    if (!(value is TypedValue) || value._type == null) continue;
                    if (isInvocationRequiredMutability(value._type.getClassName()!!, methodName)) {
                        result.add(MutabilityAssert(value))
                        if (value.createdAtInsn is MethodInsnNode) {
                            result.addAll(generatePropagatingMutabilityAsserts(value.createdAtInsn))
                        }
                    }
                }
            }
            else -> {}
        }

        return result
    }

    private fun generatePropagatingMutabilityAsserts(createdAtInsn: MethodInsnNode) : Collection<MutabilityAssert> {
        val result = hashSet<MutabilityAssert>()
        if (isPropagatingMutability(createdAtInsn.owner!!.replace("/", "."),createdAtInsn.name!!)) {
            val insn = instructionCache[createdAtInsn]
            val valueSet = insn?.get(STATE_BEFORE)?.stack?.get(0)
            if (valueSet != null) {
                for (value in valueSet) {
                    result.add(MutabilityAssert(value))
                }
            }
        }
        return result
    }

    private fun isAnnotationNecessary(assert: MutabilityAssert): Boolean {
        return true
    }

    private fun isInvocationRequiredMutability(className: String, methodName: String) : Boolean =
        mutableInterfaces[className]?.contains(methodName) ?: false

    private fun isPropagatingMutability(className: String, methodName: String) : Boolean =
        propagatingMutability[className]?.contains(methodName) ?: false
}

private class MutabilityAnnotationsManager {
    val parameterAnnotations = hashMap<Value, MutabilityAnnotation>()

    fun addParameterAnnotation(value: Value, annotation: MutabilityAnnotation) {
        parameterAnnotations[value] = annotation
    }
}

private fun MutabilityAnnotationsManager.addAssert(assert: MutabilityAssert) {
    addParameterAnnotation(assert.shouldBeMutable, MutabilityAnnotation.MUTABLE)
}

private fun MutabilityAnnotationsManager.toAnnotations(positions: Positions): Annotations<MutabilityAnnotation> {
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
package org.jetbrains.kannotator.annotationsInference.mutability

import java.util.Collections
import kotlinlib.emptyList
import org.jetbrains.kannotator.annotationsInference.AnnotationsInference
import org.jetbrains.kannotator.annotationsInference.AnnotationsManager
import org.jetbrains.kannotator.annotationsInference.Assert
import org.jetbrains.kannotator.annotationsInference.ValueInfo
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

class MutabilityAnnotationsInference(graph: ControlFlowGraph,
                                     annotations: Annotations<MutabilityAnnotation>,
                                     positions: PositionsWithinMember,
                                     declarationIndex: DeclarationIndex
): AnnotationsInference<MutabilityAnnotation, ValueInfo>(graph, annotations, positions, declarationIndex, MutabilityAnnotationsManager(positions)) {
    private val asm2GraphInstructionMap = createInstructionMap()

    private fun createInstructionMap() : Map<AbstractInsnNode, Instruction> {
        val map = hashMap<AbstractInsnNode, Instruction>()
        traverseInstructions { instruction ->
            val metadata = instruction.metadata
            if (metadata is AsmInstructionMetadata) {
                map[metadata.asmInstruction] = instruction
            }
        }
        return map
    }

    override fun generateAsserts(instruction: Instruction) : Collection<Assert> {
        val state = instruction[STATE_BEFORE]!!
        val result = hashSet<Assert>()
        val asmInstruction = (instruction.metadata as? AsmInstructionMetadata)?.asmInstruction
        if (!(asmInstruction is MethodInsnNode)) return emptyList()
        if (instruction.getOpcode() == INVOKEINTERFACE) {
            val methodId = getMethodIdByInstruction(instruction)
            val valueSet = state.stack[methodId!!.getArgumentCount()]
            for (value in valueSet) {
                if (!(value is TypedValue) || value._type == null) continue;
                if (isInvocationRequiredMutability(asmInstruction)) {
                    result.add(Assert(value))
                    if (value.createdAtInsn is MethodInsnNode) {
                        result.addAll(generatePropagatingMutabilityAsserts(value.createdAtInsn))
                    }
                }
            }
        }
        generateAssertsForCallArguments(instruction,
                { indexFromTop ->
                    state.stack[indexFromTop].forEach { value -> result.add(Assert(value)) }
                },
                false,  { paramAnnotation -> paramAnnotation == MutabilityAnnotation.MUTABLE } )

        return result
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

    private fun generatePropagatingMutabilityAsserts(createdAtInsn: MethodInsnNode) : Collection<Assert> {
        val result = hashSet<Assert>()
        if (isPropagatingMutability(createdAtInsn)) {
            val instruction = asm2GraphInstructionMap[createdAtInsn]!!
            val valueSet = instruction[STATE_BEFORE]!!.stack[0]
            for (value in valueSet) {
                result.add(Assert(value))
            }
        }
        return result
    }

    override fun isAnnotationNecessary(assert: Assert, valueInfos: Map<Value, ValueInfo>): Boolean {
        return true
    }

    protected override fun computeValueInfos(instruction: Instruction): Map<Value, ValueInfo> = Collections.emptyMap()
}

private class MutabilityAnnotationsManager(val positions: PositionsWithinMember) : AnnotationsManager<MutabilityAnnotation>() {
    val parameterAnnotations = hashMap<Value, MutabilityAnnotation>()

    private fun addParameterAnnotation(value: Value, annotation: MutabilityAnnotation) {
        parameterAnnotations[value] = annotation
    }

    override fun addAssert(assert: Assert) {
        addParameterAnnotation(assert.value, MutabilityAnnotation.MUTABLE)
    }

    override fun toAnnotations(): Annotations<MutabilityAnnotation> {
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
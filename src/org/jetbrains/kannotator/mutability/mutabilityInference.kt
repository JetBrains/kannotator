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
import org.jetbrains.kannotator.mutability.Mutability
import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.declarations.getArgumentCount

class MutabilityAnnotationsInference(graph: ControlFlowGraph,
                                     annotations: Annotations<MutabilityAnnotation>,
                                     positions: Positions,
                                     declarationIndex: DeclarationIndex
): AnnotationsInference<Mutability>(graph, annotations, positions, declarationIndex, MutabilityAnnotationsManager(positions)) {
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

    override fun generateAsserts(instruction: Instruction) : Collection<Assert<Mutability>> {
        val state = instruction[STATE_BEFORE]!!
        val result = hashSet<Assert<Mutability>>()
        val asmInstruction = (instruction.metadata as? AsmInstructionMetadata)?.asmInstruction
        if (!(asmInstruction is MethodInsnNode)) return Collections.emptyList()
        when (instruction.getOpcode()) {
            INVOKEINTERFACE -> {
                val methodId = getMethodIdByInstruction(instruction)
                val valueSet = state.stack[methodId!!.getArgumentCount()]
                for (value in valueSet) {
                    if (!(value is TypedValue) || value._type == null) continue;
                    if (isInvocationRequiredMutability(asmInstruction)) {
                        result.add(Assert<Mutability>(value))
                        if (value.createdAtInsn is MethodInsnNode) {
                            result.addAll(generatePropagatingMutabilityAsserts(value.createdAtInsn))
                        }
                    }
                }
            }
            //todo check function arguments mutability annotations
            //foo(collection) if 'foo' expects @Mutable
            else -> {}
        }

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

    private fun generatePropagatingMutabilityAsserts(createdAtInsn: MethodInsnNode) : Collection<Assert<Mutability>> {
        val result = hashSet<Assert<Mutability>>()
        if (isPropagatingMutability(createdAtInsn)) {
            val instruction = asm2GraphInstructionMap[createdAtInsn]!!
            val valueSet = instruction[STATE_BEFORE]!!.stack[0]
            for (value in valueSet) {
                result.add(Assert(value))
            }
        }
        return result
    }

    override fun isAnnotationNecessary(assert: Assert<Mutability>, valueInfos: Map<Value, ValueInfo<Mutability>>): Boolean {
        return true
    }

    protected override fun computeValueInfos(instruction: Instruction): Map<Value, ValueInfo<Mutability>> = Collections.emptyMap()
}

private class MutabilityAnnotationsManager(val positions: Positions) : AnnotationsManager<Mutability>() {
    val parameterAnnotations = hashMap<Value, MutabilityAnnotation>()

    private fun addParameterAnnotation(value: Value, annotation: MutabilityAnnotation) {
        parameterAnnotations[value] = annotation
    }

    override fun addAssert(assert: Assert<Mutability>) {
        addParameterAnnotation(assert.value, MutabilityAnnotation.MUTABLE)
    }

    override fun toAnnotations(): Annotations<Annotation<Mutability>> {
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
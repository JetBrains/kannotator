package org.jetbrains.kannotator.annotationsInference

import org.objectweb.asm.Opcodes.*
import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlow.builder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.jetbrains.kannotator.declarations.getArgumentCount
import org.jetbrains.kannotator.index.DeclarationIndex

trait Annotation
trait ValueInfo
data class Assert(val value: Value)

abstract class AbstractAnnotationInferrer<A: Annotation, I: ValueInfo>(
        private val graph: ControlFlowGraph,
        protected val annotations: Annotations<A>,
        positions: PositionsWithinMember,
        protected val declarationIndex: DeclarationIndex,
        protected val annotationManager: AnnotationManager<A>) {

    fun buildAnnotations() : Annotations<A> {
        process()
        postProcess()
        return annotationManager.toAnnotations()
    }

    private fun process() = traverseInstructions { insn -> analyzeInstruction(insn) }

    protected fun traverseInstructions(f: (Instruction) -> Unit) {
        for (instruction in graph.instructions) {
            if (instruction[STATE_BEFORE] == null) continue // dead instructions
            f(instruction)
        }
    }

    protected open fun postProcess() {}

    private fun analyzeInstruction(instruction: Instruction) {
        val valueInfos = computeValueInfos(instruction)

        val asserts = generateAsserts(instruction)
        for (assert in asserts) {
            if (isAnnotationNecessary(assert, valueInfos)) {
                annotationManager.addAssert(assert)
            }
        }
    }

    protected abstract fun computeValueInfos(instruction: Instruction) : Map<Value, I>

    protected abstract fun isAnnotationNecessary(
            assert: Assert,
            valueInfos: Map<Value, I>
    ): Boolean

    protected abstract fun generateAsserts(instruction: Instruction) : Collection<Assert>
}

public fun <A: Annotation> generateAssertsForCallArguments(
        instruction: Instruction,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<A>,
        addAssertForStackValue: (Int) -> Unit,
        needGenerateAssertForThis: Boolean,
        needGenerateAssertForArgument: (A) -> Boolean
) {
    val methodId = getMethodIdByInstruction(instruction)
    val hasThis = instruction.getOpcode() != INVOKESTATIC
    val thisSlots = if (hasThis) 1 else 0
    val parametersCount = methodId.getArgumentCount() + thisSlots

    fun addAssertForArgumentOnStack(index: Int) {
        addAssertForStackValue(parametersCount - index - 1)
    }

    if (hasThis && needGenerateAssertForThis) {
        addAssertForArgumentOnStack(0)
    }
    if (instruction.getOpcode() != INVOKEDYNAMIC) {
        val method = declarationIndex.findMethodByInstruction(instruction)
        if (method != null) {
            val positions = PositionsWithinMember(method)
            for (paramIndex in thisSlots..parametersCount - 1) {
                val paramAnnotation = annotations[positions.forParameter(paramIndex).position]
                if (paramAnnotation != null && needGenerateAssertForArgument(paramAnnotation)) {
                    addAssertForArgumentOnStack(paramIndex)
                }
            }
        }
    }
}

abstract class AnnotationManager<A: Annotation> {
    abstract fun addAssert(assert: Assert)
    abstract fun toAnnotations(): Annotations<A>
}
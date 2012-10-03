package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.Positions
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.declarations.AnnotationsImpl

trait AnnotationKind
trait Annotation<T: AnnotationKind>
trait ValueInfo<T: AnnotationKind>
data class Assert<T: AnnotationKind>(val value: Value)

abstract class AnnotationsInference<T: AnnotationKind>(
        private val graph: ControlFlowGraph,
        annotations: Annotations<Annotation<T>>,
        positions: Positions,
        declarationIndex: DeclarationIndex,
        protected val annotationsManager: AnnotationsManager<T>) {

    fun buildAnnotations() : Annotations<Annotation<T>> {
        process()
        postProcess()
        return getResult().toAnnotations()
    }

    private fun process() = traverseInstructions { insn -> analyzeInstruction(insn )}

    protected fun traverseInstructions(f: (Instruction) -> Unit) {
        for (instruction in graph.instructions) {
            if (instruction[STATE_BEFORE] == null) continue // dead instructions
            f(instruction)
        }
    }

    protected open fun postProcess() {}

    private fun getResult(): AnnotationsManager<T> = annotationsManager

    private fun analyzeInstruction(instruction: Instruction) {
        val valueInfos = computeValueInfos(instruction)

        val asserts = generateAsserts(instruction)
        for (assert in asserts) {
            if (isAnnotationNecessary(assert, valueInfos)) {
                annotationsManager.addAssert(assert)
            }
        }
    }

    protected abstract fun computeValueInfos(instruction: Instruction) : Map<Value, ValueInfo<T>>

    protected abstract fun isAnnotationNecessary(
            assert: Assert<T>,
            valueInfos: Map<Value, ValueInfo<T>>
    ): Boolean

    protected abstract fun generateAsserts(instruction: Instruction) : Collection<Assert<T>>
}

abstract class AnnotationsManager<T: AnnotationKind> {
    abstract fun addAssert(assert: Assert<T>)
    abstract fun toAnnotations(): Annotations<Annotation<T>>
}
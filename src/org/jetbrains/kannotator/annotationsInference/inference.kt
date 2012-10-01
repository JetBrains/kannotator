package org.jetbrains.kannotator.annotationsInference

import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.Positions
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.controlFlow.Value

trait AnnotationKind
trait Annotation<T: AnnotationKind>
trait ValueInfo<T: AnnotationKind>
data class Assert<T: AnnotationKind>(val value: Value)

abstract class AnnotationsInference<T: AnnotationKind>(
        private val graph: ControlFlowGraph,
        protected val annotationsManager: AnnotationsManager<T>) {

    fun buildAnnotations(
            positions: Positions,
            declarationIndex: DeclarationIndex,
            annotations: Annotations<Annotation<T>>
    ) : Annotations<Annotation<T>> {
        process()
        return getResult().toAnnotations(positions)
    }

    private fun process() {
        for (instruction in graph.instructions) {
            analyzeInstruction(instruction)
        }
    }

    private fun getResult(): AnnotationsManager<T> = annotationsManager

    private fun analyzeInstruction(instruction: Instruction) {
        if (instruction[STATE_BEFORE] == null) return // dead instructions

        val valueInfos = computeValueInfos(instruction)

        val asserts = generateAsserts(instruction)
        for (assert in asserts) {
            if (isAnnotationNecessary(assert, valueInfos)) {
                annotationsManager.addAssert(assert)
            }
        }
        postProcess(instruction, valueInfos)
    }

    protected abstract fun computeValueInfos(instruction: Instruction) : Map<Value, ValueInfo<T>>

    protected abstract fun isAnnotationNecessary(
            assert: Assert<T>,
            valueInfos: Map<Value, ValueInfo<T>>
    ): Boolean

    protected abstract fun generateAsserts(instruction: Instruction) : Collection<Assert<T>>

    protected abstract fun postProcess(instruction: Instruction, valueInfos: Map<Value, ValueInfo<T>>)
}

abstract class AnnotationsManager<T: AnnotationKind> {
    abstract fun addAssert(assert: Assert<T>)
    abstract fun toAnnotations(positions: Positions): Annotations<Annotation<T>>
}
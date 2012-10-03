package inference

import junit.framework.TestCase
import kotlin.test.assertEquals
import org.jetbrains.kannotator.annotationsInference.AnnotationsInference
import org.jetbrains.kannotator.controlFlowBuilder.buildControlFlowGraph
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.Positions
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import java.util.HashMap
import kotlinlib.*
import org.jetbrains.kannotator.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.mutability.Mutability
import org.jetbrains.kannotator.annotationsInference.MutabilityAnnotationsInference

class MutabilityInferenceTest: AbstractInferenceTest<Mutability>(
        javaClass<inferenceData.MutabilityTest>()) {

    override fun Array<jet.Annotation>.toAnnotation(): MutabilityAnnotation? {
        for (ann in this) {
            if (ann.annotationType().getSimpleName() == "ExpectMutable") return MutabilityAnnotation.MUTABLE
            if (ann.annotationType().getSimpleName() == "ExpectNotNull") return MutabilityAnnotation.IMMUTABLE
        }
        return null
    }

    override protected fun buildAnnotations(graph: ControlFlowGraph, positions: Positions, declarationIndex: DeclarationIndex,
                                            annotations: Annotations<Annotation<Mutability>>) : Annotations<Annotation<Mutability>> {
        return MutabilityAnnotationsInference(graph, annotations as Annotations<MutabilityAnnotation>, positions, declarationIndex).buildAnnotations()
    }

    fun testMutableCollection() = doTest()

    fun testIterateOverMutableCollection() = doTest()

    fun testImmutableCollection() = doTest()

    fun testMapEntry() = doTest()

    fun testChangeKeySetInMap() = doTest()
}

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
import org.jetbrains.kannotator.annotationsInference.buildAnnotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import java.util.HashMap
import kotlinlib.*
import org.jetbrains.kannotator.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.buildMutabilityAnnotations
import org.jetbrains.kannotator.annotationsInference.DerivedAnnotation
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.index.DeclarationIndex

class MutabilityInferenceTest: InferenceTest<MutabilityAnnotation>(
        javaClass<inferenceData.MutabilityTest>(),
        {(graph: ControlFlowGraph, positions: Positions, declarationIndex: DeclarationIndex,
          annotations: Annotations<MutabilityAnnotation>) : Annotations<MutabilityAnnotation> ->
            buildMutabilityAnnotations(graph, positions, declarationIndex, annotations)}) {


    override fun Array<Annotation>.toAnnotation(): MutabilityAnnotation? {
        for (ann in this) {
            if (ann.annotationType().getSimpleName() == "ExpectMutable") return MutabilityAnnotation.MUTABLE
            if (ann.annotationType().getSimpleName() == "ExpectNotNull") return MutabilityAnnotation.IMMUTABLE
        }
        return null
    }

    fun testMutableCollection() = doTest()

    fun testIterateOverMutableCollection() = doTest()

    fun testImmutableCollection() = doTest()
}

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
import org.jetbrains.kannotator.annotationsInference.MutabilityAnnotationsInference
import org.jetbrains.kannotator.index.FileBasedClassSource
import java.io.File
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles
import org.jetbrains.kannotator.nullability.classNameToNullabilityAnnotation

class MutabilityInferenceTest: AbstractInferenceTest<MutabilityAnnotation>(
        javaClass<inferenceData.MutabilityInferenceTestClass>()) {

    override fun Array<jet.Annotation>.toAnnotation(): MutabilityAnnotation? {
        for (ann in this) {
            if (ann.annotationType().getSimpleName() == "ExpectMutable") return MutabilityAnnotation.MUTABLE
            if (ann.annotationType().getSimpleName() == "ExpectNotNull") return MutabilityAnnotation.IMMUTABLE
        }
        return null
    }

    protected override fun getInitialAnnotations(): Annotations<MutabilityAnnotation> {
        val utilClass = "out/production/kannotator/inferenceData/MutabilityInferenceTestLib.class"
        val classSource = FileBasedClassSource(arrayList(File(utilClass)))
        val existingNullabilityAnnotations = getAnnotationsFromClassFiles(classSource) {
            annotationName -> if (annotationName == "inferenceData.annotations.Mutable")
                                   MutabilityAnnotation.MUTABLE
                              else MutabilityAnnotation.IMMUTABLE
        }
        return existingNullabilityAnnotations
    }

    override protected fun buildAnnotations(graph: ControlFlowGraph, positions: Positions, declarationIndex: DeclarationIndex,
                                            annotations: Annotations<MutabilityAnnotation>) : Annotations<MutabilityAnnotation> {
        return MutabilityAnnotationsInference(graph, annotations, positions, declarationIndex).buildAnnotations()
    }

    fun testMutableCollection() = doTest()

    fun testIterateOverMutableCollection() = doTest()

    fun testImmutableCollection() = doTest()

    fun testMapEntry() = doTest()

    fun testChangeKeySetInMap() = doTest()

    fun testInvokeProcessMutable() = doTest()

    fun testInvokeProcessReadableAndMutable() = doTest()
}

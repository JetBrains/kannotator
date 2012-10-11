package inference

import java.io.File
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles
import org.jetbrains.kannotator.annotationsInference.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.annotationsInference.mutability.buildMutabilityAnnotations

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
            annotationNames -> if ("inferenceData.annotations.Mutable" in annotationNames)
                                   MutabilityAnnotation.MUTABLE
                              else MutabilityAnnotation.IMMUTABLE
        }
        return existingNullabilityAnnotations
    }

    override protected fun buildAnnotations(graph: ControlFlowGraph, positions: PositionsWithinMember, declarationIndex: DeclarationIndex,
                                            annotations: Annotations<MutabilityAnnotation>) : Annotations<MutabilityAnnotation> {
        return buildMutabilityAnnotations(graph, positions, declarationIndex, annotations)
    }

    fun testMutableCollection() = doTest()

    fun testIterateOverMutableCollection() = doTest()

    fun testImmutableCollection() = doTest()

    fun testMapEntry() = doTest()

    fun testChangeKeySetInMap() = doTest()

    fun testEntrySetInMap() = doTest()

    fun testEntrySetInMap2() = doTest()

    fun testInvokeProcessMutable() = doTest()

    fun testInvokeProcessReadableAndMutable() = doTest()
}

package inference

import java.io.File
import org.jetbrains.kannotator.annotationsInference.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.MUTABILITY_INFERRER
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles

class MutabilityInferenceTest: AbstractInferenceTest<MutabilityAnnotation>(
        javaClass<inferenceData.MutabilityInferenceTestClass>()) {
    protected override fun getClassFiles(): Collection<File> {
        return arrayList(
                "out/production/kannotator/inferenceData/MutabilityInferenceTestLib.class",
                "out/production/kannotator/inferenceData/MutabilityInferenceTestClass.class").map { File(it) }
    }

    protected override fun getInferrer(): AnnotationInferrer<MutabilityAnnotation, *> {
        return MUTABILITY_INFERRER
    }

    override fun Array<out jet.Annotation>.toAnnotation(): MutabilityAnnotation? {
        for (ann in this) {
            if (ann.annotationType().getSimpleName() == "ExpectMutable") return MutabilityAnnotation.MUTABLE
            if (ann.annotationType().getSimpleName() == "ExpectReadOnly") return MutabilityAnnotation.READ_ONLY
        }
        return null
    }

    protected override fun getInitialAnnotations(): Annotations<MutabilityAnnotation> {
        val utilClass = "out/production/kannotator/inferenceData/MutabilityInferenceTestLib.class"
        val classSource = FileBasedClassSource(arrayList(File(utilClass)))
        val existingNullabilityAnnotations = getAnnotationsFromClassFiles(classSource) { annotationNames ->
            if ("org.jetbrains.kannotator.runtime.annotations.Mutable" in annotationNames)
                MutabilityAnnotation.MUTABLE
            else
                MutabilityAnnotation.READ_ONLY
        }
        return existingNullabilityAnnotations
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

    fun testWalk() = doTest()

    fun testScopeExit() = doTest()
}

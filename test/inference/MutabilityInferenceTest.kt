package inference

import java.io.File
import org.junit.Test
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.MUTABILITY_INFERRER
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.controlFlow.builder.analysis.MUTABILITY_KEY
import org.jetbrains.kannotator.runtime.annotations.AnalysisType

/** checks inference for MutabilityInferenceTestLib */
class MutabilityInferenceTest: AbstractInferenceTest<MutabilityAnnotation>(
        javaClass<inferenceData.MutabilityInferenceTestClass>()) {
    protected override val analysisType: AnalysisType = MUTABILITY_KEY

    protected override fun getClassFiles(): Collection<File> {
        return arrayListOf(
                "out/test/kannotator/inferenceData/MutabilityInferenceTestLib.class",
                "out/test/kannotator/inferenceData/MutabilityInferenceTestClass.class").map { File(it) }
    }

    protected override fun getInferrer(): AnnotationInferrer<MutabilityAnnotation, *> {
        return MUTABILITY_INFERRER
    }

    override fun Array<out kotlin.Annotation>.toAnnotation(): MutabilityAnnotation? {
        for (ann in this) {
            if (ann.annotationType().getSimpleName() == "ExpectMutable") return MutabilityAnnotation.MUTABLE
            if (ann.annotationType().getSimpleName() == "ExpectReadOnly") return MutabilityAnnotation.READ_ONLY
        }
        return null
    }

    protected override fun getInitialAnnotations(): Annotations<MutabilityAnnotation> {
        val utilClass = "out/test/kannotator/inferenceData/MutabilityInferenceTestLib.class"
        val classSource = FileBasedClassSource(arrayListOf(File(utilClass)))
        val existingNullabilityAnnotations = getAnnotationsFromClassFiles(classSource) { annotationNames ->
            if ("org.jetbrains.kannotator.runtime.annotations.Mutable" in annotationNames)
                MutabilityAnnotation.MUTABLE
            else
                MutabilityAnnotation.READ_ONLY
        }
        return existingNullabilityAnnotations
    }

    Test fun mutableCollection() = doTest()

    Test fun iterateOverMutableCollection() = doTest()

    Test fun immutableCollection() = doTest()

    Test fun mapEntry() = doTest()

    Test fun changeKeySetInMap() = doTest()

    Test fun entrySetInMap() = doTest()

    Test fun entrySetInMap2() = doTest()

    Test fun invokeProcessMutable() = doTest()

    Test fun invokeProcessReadableAndMutable() = doTest()

    Test fun walk() = doTest()

    Test fun scopeExit() = doTest()
}

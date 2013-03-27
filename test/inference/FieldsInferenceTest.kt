package inference

import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.FieldDependencyInfo
import java.util.ArrayList
import java.io.File
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles
import org.jetbrains.kannotator.annotationsInference.nullability.classNamesToNullabilityAnnotation
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.controlFlow.builder.analysis.AnalysisType
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY

class FieldsInferenceTest: AbstractInferenceTest<NullabilityAnnotation>(
        javaClass<inferenceData.NullabilityFieldsInferenceTestClass>()) {
    protected override val analysisType: AnalysisType = NULLABILITY_KEY

    protected override fun getInitialAnnotations(): Annotations<NullabilityAnnotation> {
        val utilClass = "out/test/kannotator/inferenceData/NullabilityFieldsInferenceTestClass.class"
        val classSource = FileBasedClassSource(arrayListOf(File(utilClass)))
        val existingNullabilityAnnotations = getAnnotationsFromClassFiles(classSource) {
            annotationNames -> classNamesToNullabilityAnnotation(annotationNames)
        }
        return existingNullabilityAnnotations
    }

    protected override fun getClassFiles(): Collection<File> {
        return arrayListOf(File("out/test/kannotator/inferenceData/NullabilityFieldsInferenceTestClass.class"))
    }

    protected override fun getInferrer(): AnnotationInferrer<NullabilityAnnotation, *> {
        return NullabilityInferrer()
    }

    protected override fun Array<out Annotation>.toAnnotation(): NullabilityAnnotation? {
        for (ann in this) {
            if (ann.annotationType().getSimpleName() == "ExpectNullable") return NullabilityAnnotation.NULLABLE
            if (ann.annotationType().getSimpleName() == "ExpectNotNull") return NullabilityAnnotation.NOT_NULL
        }
        return null
    }

    fun testSTRING_NOT_NULL_FIELD() = doFieldTest()

    fun testSTRING_NULL_FIELD() = doFieldTest()

    fun testFROM_PREVIOUS_FIELD() = doFieldTest()

    fun testNEW_OBJECT_FIELD() = doFieldTest()

    fun testINTEGER_FIELD() = doFieldTest()

    fun testDOUBLE_FIELD() = doFieldTest()

    fun testStringClass() = doFieldTest()

    fun testNullFinalField() = doFieldTest()

    fun testNewObjectFinalField() = doFieldTest()

    fun testConstantStringFinalField() = doFieldTest()

    fun testConstantIntegerFinalField() = doFieldTest()

    // fun testMethodInitFinalField() = doFieldTest()

    fun testFromConstructorParameterFinalField() = doFieldTest()

    fun testDifferentAnnotationsFromDifferentConstructors() = doFieldTest()

    fun testNullableInConstructorInitFinalField() = doFieldTest()

    // fun testFromMethodInConstructorFinalField() = doFieldTest()

    // fun testNullableFromCallingMethodOnValue() = doFieldTest()

    // fun testNotUsedNonFinalField() = doFieldTest()

    private class EmptyFieldInfo(override val field : Field) : FieldDependencyInfo {
        override val writers: Collection<Method> = ArrayList()
        override val readers: Collection<Method> = ArrayList()
    }
}
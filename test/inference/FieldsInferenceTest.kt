package inference

import org.junit.Test

import java.util.ArrayList
import java.io.File

import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles
import org.jetbrains.kannotator.annotationsInference.nullability.classNamesToNullabilityAnnotation
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.junit.Ignore

/** Tests inference of fields for NullabilityFieldsInferenceTestClass */
class FieldsInferenceTest: AbstractInferenceTest<NullabilityAnnotation>(
        inferenceData.NullabilityFieldsInferenceTestClass::class.java) {
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
            if (ann.annotationType().simpleName == "ExpectNullable") return NullabilityAnnotation.NULLABLE
            if (ann.annotationType().simpleName == "ExpectNotNull") return NullabilityAnnotation.NOT_NULL
        }
        return null
    }

    @Test fun STRING_NOT_NULL_FIELD() = doFieldTest()

    @Test fun STRING_NULL_FIELD() = doFieldTest()

    @Test fun FROM_PREVIOUS_FIELD() = doFieldTest()

    @Test fun NEW_OBJECT_FIELD() = doFieldTest()

    @Test fun INTEGER_FIELD() = doFieldTest()

    @Test fun DOUBLE_FIELD() = doFieldTest()

    @Test fun stringClass() = doFieldTest()

    @Test fun nullFinalField() = doFieldTest()

    @Test fun newObjectFinalField() = doFieldTest()

    @Test fun constantStringFinalField() = doFieldTest()

    @Test fun constantIntegerFinalField() = doFieldTest()

    @Test fun methodInitFinalField() = doFieldTest()

    @Test fun fromConstructorParameterFinalField() = doFieldTest()

    @Test fun differentAnnotationsFromDifferentConstructors() = doFieldTest()

    @Test fun nullableInConstructorInitFinalField() = doFieldTest()

    @Test fun fromMethodInConstructorFinalField() = doFieldTest()

    @Ignore
    @Test fun nullableFromCallingMethodOnValue() = doFieldTest()

    @Ignore
    @Test fun testNotUsedNonFinalField() = doFieldTest()

    private class EmptyFieldInfo(override val field : Field) : FieldDependencyInfo {
        override val writers: Collection<Method> = ArrayList()
        override val readers: Collection<Method> = ArrayList()
    }
}
package inference

import kotlin.test.assertTrue
import kotlinlib.minus
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.ClassReader
import util.junit.getTestName
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.index.ClassSource
import util.ClassPathDeclarationIndex
import org.jetbrains.kannotator.index.buildFieldsDependencyInfos
import java.util.ArrayList
import java.io.File
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles
import org.jetbrains.kannotator.annotationsInference.nullability.classNamesToNullabilityAnnotation
import org.jetbrains.kannotator.main.inferAnnotations
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.ProgressMonitor
import org.jetbrains.kannotator.declarations.getFieldTypePosition
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.setIfNotNull
import org.jetbrains.kannotator.main.NullabilityInferrer

class FieldsInferenceTest: AbstractInferenceTest<NullabilityAnnotation>(
        javaClass<inferenceData.NullabilityFieldsInferenceTestClass>()) {
    protected override fun getInitialAnnotations(): Annotations<NullabilityAnnotation> {
        val utilClass = "out/production/kannotator/inferenceData/NullabilityFieldsInferenceTestClass.class"
        val classSource = FileBasedClassSource(arrayList(File(utilClass)))
        val existingNullabilityAnnotations = getAnnotationsFromClassFiles(classSource) {
            annotationNames -> classNamesToNullabilityAnnotation(annotationNames)
        }
        return existingNullabilityAnnotations
    }

    protected override fun getClassFiles(): Collection<File> {
        return arrayList(File("out/production/kannotator/inferenceData/NullabilityFieldsInferenceTestClass.class"))
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
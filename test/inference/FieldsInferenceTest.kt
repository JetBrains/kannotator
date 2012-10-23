package inference

import inferenceData.annotations.Ignore
import kotlin.test.assertTrue
import kotlinlib.minus
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.buildFieldNullabilityAnnotations
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.ClassReader
import util.junit.getTestName
import org.jetbrains.kannotator.index.FieldDependencyInfo
import util.controlFlow.buildControlFlowGraph

class FieldsInferenceTest: AbstractInferenceTest<NullabilityAnnotation>(javaClass<inferenceData.NullabilityFieldsInferenceTestClass>()) {

    protected override fun Array<Annotation>.toAnnotation(): NullabilityAnnotation? {
        for (ann in this) {
            if (ann.annotationType().getSimpleName() == "ExpectNullable") return NullabilityAnnotation.NULLABLE
            if (ann.annotationType().getSimpleName() == "ExpectNotNull") return NullabilityAnnotation.NOT_NULL
        }
        return null
    }

    protected override fun buildFieldAnnotations(
            field: Field,
            classReader: ClassReader,
            declarationIndex: DeclarationIndex,
            annotations: Annotations<NullabilityAnnotation>) : Annotations<NullabilityAnnotation> {
        return buildFieldNullabilityAnnotations(
                FakeFieldInfo(field),
                { method -> buildControlFlowGraph(classReader, method) },
                declarationIndex,
                annotations)
    }

    fun testSTRING_NOT_NULL_FIELD() = doFieldTest()

    fun testSTRING_NULL_FIELD() = doFieldTest()

    fun testFROM_PREVIOUS_FIELD() = doFieldTest()

    fun testNEW_OBJECT_FIELD() = doFieldTest()

    fun testINTEGER_FIELD() = doFieldTest()

    fun testDOUBLE_FIELD() = doFieldTest()

//    fun testNullFinalField() = doFieldTest()
//
//    fun testNewObjectFinalField() = doFieldTest()
//
//    fun testConstantStringFinalField() = doFieldTest()
//
//    fun testConstantIntegerFinalField() = doFieldTest()
//
//    fun testMethodInitFinalField() = doFieldTest()
//
//    fun testFromConstructorParameterFinalField() = doFieldTest()
//
//    fun testDifferentAnnotationsFromDifferentConstructors() = doFieldTest()
//
//    fun testNullableInConstructorInitFinalField() = doFieldTest()
//
//    fun testFromMethodInConstructorFinalField() = doFieldTest()

    fun testAllFieldsTested() {
        val fieldsInTestClass = testClass.getFields()
                .filter { field -> field.getAnnotation(javaClass<Ignore>()) == null }
                .map { field -> field.getName() }

        val testedFields = this.getClass().getMethods().iterator()
                .map { method -> method.getName()!! }
                .filter { methodName -> methodName.startsWith("test") }
                .map { methodName -> getTestName(methodName, true) }
                .filter { testName -> testName != getTestName(true) }
                .toList()

        val nonTestedFields = fieldsInTestClass - testedFields

        assertTrue(nonTestedFields.isEmpty(), "Fields in '$nonTestedFields' are not tested")
    }

    private class FakeFieldInfo(override val field : Field) : FieldDependencyInfo {
        override val setters: Collection<Method> get() { throw UnsupportedOperationException() }
        override val getters: Collection<Method> get() { throw UnsupportedOperationException() }
    }
}
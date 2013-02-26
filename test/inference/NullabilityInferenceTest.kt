package inference

import java.io.File
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.classNamesToNullabilityAnnotation
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.NullabilityInferrer

class NullabilityInferenceTest : AbstractInferenceTest<NullabilityAnnotation>(javaClass<inferenceData.NullabilityInferenceTestClass>()) {
    protected override fun getInferrer(): AnnotationInferrer<NullabilityAnnotation, *> {
        return NullabilityInferrer()
    }

    protected override fun Array<out jet.Annotation>.toAnnotation(): NullabilityAnnotation? {
        for (ann in this) {
            if (ann.annotationType().getSimpleName() == "ExpectNullable") return NullabilityAnnotation.NULLABLE
            if (ann.annotationType().getSimpleName() == "ExpectNotNull") return NullabilityAnnotation.NOT_NULL
        }
        return null
    }

    protected override fun getClassFiles(): Collection<File> {
        return arrayList(
                "out/production/kannotator/inferenceData/NullabilityInferenceTestLib.class",
                "out/production/kannotator/inferenceData/NullabilityInferenceTestClass.class").map { File(it) }
    }

    protected override fun getInitialAnnotations(): Annotations<NullabilityAnnotation> {
        val utilClass = "out/production/kannotator/inferenceData/NullabilityInferenceTestLib.class"
        val classSource = FileBasedClassSource(arrayList(File(utilClass)))
        val existingNullabilityAnnotations = getAnnotationsFromClassFiles(classSource) {
            annotationNames -> classNamesToNullabilityAnnotation(annotationNames)
        }
        return existingNullabilityAnnotations
    }

    fun testNull() = doTest()

    fun testNullOrObject() = doTest()

    fun testNotNullParameter() = doTest()

    fun testInvocationOnCheckedParameter() = doTest()

    //todo test CONFLICT
    fun testIncompatibleChecks() = doTest()

    fun testInvocationOnNullParameter() = doTest()

    fun testNullableParameter() = doTest()

    fun testSenselessIsNullCheck() = doTest()

    fun testInvocationAfterReturn() = doTest()

    fun testReturnInvokeSpecial() = doTest()

    fun testReturnInvokeVirtual() = doTest()

    fun testReturnInvokeStatic() = doTest()

    fun testInvokeInterface() = doTest()

    fun testReturnArrayLoad() = doTest()

    fun testReturnNewIntArray() = doTest()

    fun testReturnNewObjectArray() = doTest()

    fun testReturnNewMultiArray() = doTest()

    // TODO
    fun todotestReturnField() = doTest()

    fun testReturnNotNullField() = doTest()

    fun testReturnStaticField() = doTest()

    fun testReturnNullableStaticField() = doTest()

    fun testReturnStringConstant() = doTest()

    fun testReturnThis() = doTest()

    fun testReturnCaughtException() = doTest()

    fun testInstanceofAndReturn() = doTest()

    fun testClassLiteral() = doTest()

    fun testNotNullIfNullCheckThrowsException() = doTest()

    fun testAssertAfterReturn() = doTest()

    fun testGetField() = doTest()

    fun testPutField() = doTest()

    fun testArrayLength() = doTest()

//todo support merge termination with exceptions only
//    fun testThrowParameter() = doTest()

    fun testMonitor() = doTest()

    fun testArrayLoad() = doTest()

    fun testArrayStore() = doTest()

    fun testUnboxingToPrimitive() = doTest()

    fun testInvokeStaticAssertNotNull() = doTest()

    fun testInvokeAssertNotNull() = doTest()

    fun testInvokeAssertSecondNotNull() = doTest()

    fun testInvokeStaticAssertSecondNotNull() = doTest()

    fun testInvokeNullableParameter() = doTest()

    fun testInvokeReturnNotNull() = doTest()

    fun testInvokeReturnNullable() = doTest()

    fun testReturnAfterInvocation() = doTest()

    fun testReturnAfterPassingAsNotNullArgument() = doTest()

    fun testConflict() = doTest()

    fun testConflict2() = doTest()

    // TODO
    fun todotestAutoboxing() = doTest()

    fun testNullableAfterInstanceOf() = doTest()

    // TODO
    fun todotestMonitorValueThroughLocalVariable() = doTest()

    // TODO
    fun todotestMonitorValueThroughField() = doTest()

    fun testNotInstanceOf() = doTest()
    fun testNotInstanceOfWithAssignment() = doTest()

    fun testMultipleInstanceOf() = doTest()

    fun testTrimStringList() = doTest()

    fun testUnmodifiableCollectionSubclass() = doTest()
}
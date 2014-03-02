package inference

import java.io.File
import org.junit.Test
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.classNamesToNullabilityAnnotation
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.junit.Ignore

class NullabilityInferenceTest : AbstractInferenceTest<NullabilityAnnotation>(javaClass<inferenceData.NullabilityInferenceTestClass>()) {
    protected override val analysisType: AnalysisType = NULLABILITY_KEY

    protected override fun getInferrer(): AnnotationInferrer<NullabilityAnnotation, *> {
        return NullabilityInferrer()
    }

    protected override fun Array<out kotlin.Annotation>.toAnnotation(): NullabilityAnnotation? {
        for (ann in this) {
            if (ann.annotationType().getSimpleName() == "ExpectNullable") return NullabilityAnnotation.NULLABLE
            if (ann.annotationType().getSimpleName() == "ExpectNotNull") return NullabilityAnnotation.NOT_NULL
        }
        return null
    }

    protected override fun getClassFiles(): Collection<File> {
        return arrayList(
                "out/test/kannotator/inferenceData/NullabilityInferenceTestLib.class",
                "out/test/kannotator/inferenceData/NullabilityInferenceTestClass.class").map { File(it) }
    }

    protected override fun getInitialAnnotations(): Annotations<NullabilityAnnotation> {
        val utilClass = "out/test/kannotator/inferenceData/NullabilityInferenceTestLib.class"
        val classSource = FileBasedClassSource(arrayList(File(utilClass)))
        val existingNullabilityAnnotations = getAnnotationsFromClassFiles(classSource) {
            annotationNames -> classNamesToNullabilityAnnotation(annotationNames)
        }
        return existingNullabilityAnnotations
    }

    Test fun testNull() = doTest()

    Test fun testNullOrObject() = doTest()

    Test fun testNotNullParameter() = doTest()

    Test fun testInvocationOnCheckedParameter() = doTest()

    //todo test CONFLICT
    Test fun testIncompatibleChecks() = doTest()

    Test fun testInvocationOnNullParameter() = doTest()

    Test fun testNullableParameter() = doTest()

    Test fun testSenselessIsNullCheck() = doTest()

    Test fun testInvocationAfterReturn() = doTest()

    Test fun testReturnInvokeSpecial() = doTest()

    Test fun testReturnInvokeVirtual() = doTest()

    Test fun testReturnInvokeStatic() = doTest()

    Test fun testInvokeInterface() = doTest()

    Test fun testReturnArrayLoad() = doTest()

    Test fun testReturnNewIntArray() = doTest()

    Test fun testReturnNewObjectArray() = doTest()

    Test fun testReturnNewMultiArray() = doTest()

    Ignore
    Test fun testReturnField() = doTest()

    Test fun testReturnNotNullField() = doTest()

    Test fun testReturnStaticField() = doTest()

    Test fun testReturnNullableStaticField() = doTest()

    Test fun testReturnStringConstant() = doTest()

    Test fun testReturnThis() = doTest()

    Test fun testReturnCaughtException() = doTest()

    Test fun testInstanceofAndReturn() = doTest()

    Test fun testClassLiteral() = doTest()

    Test fun testNotNullIfNullCheckThrowsException() = doTest()

    Test fun testAssertAfterReturn() = doTest()

    Test fun testGetField() = doTest()

    Test fun testPutField() = doTest()

    Test fun testArrayLength() = doTest()

    //todo support merge termination with exceptions only
    Ignore
    Test fun testThrowParameter() = doTest()

    Test fun testMonitor() = doTest()

    Test fun testArrayLoad() = doTest()

    Test fun testArrayStore() = doTest()

    Test fun testUnboxingToPrimitive() = doTest()

    Test fun testInvokeStaticAssertNotNull() = doTest()

    Test fun testInvokeAssertNotNull() = doTest()

    Test fun testInvokeAssertSecondNotNull() = doTest()

    Test fun testInvokeStaticAssertSecondNotNull() = doTest()

    Test fun testInvokeNullableParameter() = doTest()

    Test fun testInvokeReturnNotNull() = doTest()

    Test fun testInvokeReturnNullable() = doTest()

    Test fun testReturnAfterInvocation() = doTest()

    Test fun testReturnAfterPassingAsNotNullArgument() = doTest()

    Test fun testConflict() = doTest()

    Test fun testConflict2() = doTest()

    Ignore
    Test fun testAutoboxing() = doTest()

    Test fun testNullableAfterInstanceOf() = doTest()

    // TODO
    Test fun testMonitorValueThroughLocalVariable() = doTest()

    Ignore
    Test fun testMonitorValueThroughField() = doTest()

    Test fun testNotInstanceOf() = doTest()
    Test fun testNotInstanceOfWithAssignment() = doTest()

    Test fun testMultipleInstanceOf() = doTest()

    Test fun testTrimStringList() = doTest()

    Test fun testUnmodifiableCollectionSubclass() = doTest()

    Test fun testArgOfStaticMethod() = doTest()

    Ignore
    Test fun testArgAssign() = doTest()

    Ignore
    Test fun testErrorCall() = doTest()

    Ignore
    Test fun methodCallInsideTry() = doTest()
}
package inference

import java.io.File
import org.jetbrains.kannotator.annotations.io.getAnnotationsFromClassFiles
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.classNamesToNullabilityAnnotation
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.annotationsInference.nullability.buildNullabilityAnnotations

class NullabilityInferenceTest : AbstractInferenceTest<NullabilityAnnotation>(javaClass<inferenceData.NullabilityInferenceTestClass>()) {

    protected override fun Array<jet.Annotation>.toAnnotation(): NullabilityAnnotation? {
        for (ann in this) {
            if (ann.annotationType().getSimpleName() == "ExpectNullable") return NullabilityAnnotation.NULLABLE
            if (ann.annotationType().getSimpleName() == "ExpectNotNull") return NullabilityAnnotation.NOT_NULL
        }
        return null
    }

    protected override fun getInitialAnnotations(): Annotations<NullabilityAnnotation> {
        val utilClass = "out/production/kannotator/inferenceData/NullabilityInferenceTestLib.class"
        val classSource = FileBasedClassSource(arrayList(File(utilClass)))
        val existingNullabilityAnnotations = getAnnotationsFromClassFiles(classSource) {
            annotationNames -> classNamesToNullabilityAnnotation(annotationNames)
        }
        return existingNullabilityAnnotations
    }

    override protected fun buildAnnotations(graph: ControlFlowGraph, positions: PositionsWithinMember, declarationIndex: DeclarationIndex,
                                            annotations: Annotations<NullabilityAnnotation>) : Annotations<NullabilityAnnotation> {
        return buildNullabilityAnnotations(graph, positions, declarationIndex, annotations)
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

    fun testReturnField() = doTest()

    fun testReturnStaticField() = doTest()

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
}
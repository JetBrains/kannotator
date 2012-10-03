package inference

import inference.AbstractInferenceTest
import org.jetbrains.kannotator.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.declarations.Positions
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.index.DeclarationIndex
import interpreter.doTest
import org.jetbrains.kannotator.nullability.Nullability
import org.jetbrains.kannotator.annotationsInference.NullabilityAnnotationsInference

class NullabilityInferenceTest : AbstractInferenceTest<Nullability>(javaClass<inferenceData.NullabilityTest>()) {

    protected override fun Array<jet.Annotation>.toAnnotation(): NullabilityAnnotation? {
        for (ann in this) {
            if (ann.annotationType().getSimpleName() == "ExpectNullable") return NullabilityAnnotation.NULLABLE
            if (ann.annotationType().getSimpleName() == "ExpectNotNull") return NullabilityAnnotation.NOT_NULL
        }
        return null
    }

    override protected fun buildAnnotations(graph: ControlFlowGraph, positions: Positions, declarationIndex: DeclarationIndex,
                                            annotations: Annotations<Annotation<Nullability>>) : Annotations<Annotation<Nullability>> {
        return NullabilityAnnotationsInference(graph, annotations as Annotations<NullabilityAnnotation>, positions, declarationIndex).buildAnnotations()
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
}
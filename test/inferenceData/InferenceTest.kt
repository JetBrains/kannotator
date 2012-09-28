package inference

import junit.framework.TestCase
import kotlin.test.assertEquals
import org.jetbrains.kannotator.annotationsInference.AnnotationsInference
import org.jetbrains.kannotator.controlFlowBuilder.buildControlFlowGraph
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.Positions
import org.jetbrains.kannotator.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.nullability.NullabilityAnnotation.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class InferenceTest: TestCase() {
    fun doTest(expectedReturnInfo: NullabilityAnnotation?, vararg pairs: Pair<Int, NullabilityAnnotation>) {
        val theClass = javaClass<inferenceData.Test>()
        val className = theClass.getName()
        val methodName = getName()!!
        val reflectMethod = theClass.getMethods().find { m -> m.getName() == methodName }!!
        val methodDescriptor = Type.getMethodDescriptor(reflectMethod)

        val parametersMap = hashMap(*pairs)
        val classReader = ClassReader(className)

        val graph = buildControlFlowGraph(classReader, methodName, methodDescriptor)

        val method = Method(ClassName.fromInternalName(className), Opcodes.ACC_PUBLIC, methodName, methodDescriptor, null)
        val positions = Positions(method)
        val annotationsInference = AnnotationsInference(graph)
        annotationsInference.process()
        val result = annotationsInference.getResultAnnotations(positions)
        assertEquals(parametersMap, expectedReturnInfo, result, pairs.size, positions)
    }

    fun assertEquals(expectedParametersInfo: Map<Int, NullabilityAnnotation>, expectedReturnInfo: NullabilityAnnotation?,
                     actual: Annotations<NullabilityAnnotation>, parametersNumber: Int, positions: Positions) {
        assertEquals(expectedReturnInfo, actual.get(positions.forReturnType().position))

        for (index in 1..parametersNumber) {
            assertEquals(expectedParametersInfo.get(index), actual.get(positions.forParameter(index).position))
        }
    }

    fun testNull() = doTest(NULLABLE)

    fun testNullOrObject() = doTest(NULLABLE)

    fun testNotNullParameter() = doTest(null, 1 to NOT_NULL);

    fun testInvocationOnCheckedParameter() = doTest(null);

    //todo test CONFLICT
    fun testIncompatibleChecks() = doTest(null);

    fun testInvocationOnNullParameter() = doTest(null, 1 to NOT_NULL);

    fun testNullableParameter() = doTest(null, 1 to NULLABLE)

//    fun testSenselessNotNullCheck() = doTest("testSenselessNotNullCheck", "(Ljava/lang/String;)V", arrayList(null, NOT_NULL))

    fun testInvocationAfterReturn() = doTest(null, 1 to NULLABLE)

    fun testReturnInvokeSpecial() = doTest(null)

    fun testReturnInvokeVirtual() = doTest(null)

    fun testReturnInvokeStatic() = doTest(null)

    fun testInvokeInterface() = doTest(null, 1 to NOT_NULL)

    fun testReturnArrayLoad() = doTest(null, 1 to NOT_NULL)

    fun testReturnNewIntArray() = doTest(NOT_NULL)

    fun testReturnNewObjectArray() = doTest(NOT_NULL)

    fun testReturnNewMultiArray() = doTest(NOT_NULL)

    fun testReturnField() = doTest(null)

    fun testReturnStaticField() = doTest(null)

    fun testReturnStringConstant() = doTest(NOT_NULL)

    fun testReturnThis() = doTest(NOT_NULL)

    fun testReturnCaughtException() = doTest(NOT_NULL)

    fun testInstanceofAndReturn() = doTest(NOT_NULL)
}

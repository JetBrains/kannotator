package inference

import junit.framework.TestCase
import org.objectweb.asm.ClassReader
import org.jetbrains.kannotator.annotationsInference.inferAnnotations
import org.jetbrains.kannotator.controlFlowBuilder.buildControlFlowGraph
import org.jetbrains.kannotator.nullability.NullabilityValueInfo
import org.jetbrains.kannotator.nullability.NullabilityValueInfo.*
import kotlin.test.assertEquals

class InferenceTest: TestCase() {
    fun doTest(theClass: Class<out Any>, methodName: String, methodDescriptor: String, expected: List<NullabilityValueInfo>) {
        val classReader = ClassReader(theClass.getCanonicalName())
        val graph = buildControlFlowGraph(classReader, methodName, methodDescriptor)
        val result = inferAnnotations(graph)
        assertEquals(expected, result)
    }

    fun testNull() = doTest(javaClass<inferenceData.Test>(), "testNull", "()Ljava/lang/Object;", arrayList(NULL))

    fun testNullOrObject() = doTest(javaClass<inferenceData.Test>(), "testNullOrObject", "()Ljava/lang/Object;", arrayList(NULLABLE))

    fun testNotNullParameter() = doTest(javaClass<inferenceData.Test>(), "testNotNullParameter", "(Ljava/lang/String;)V", arrayList(UNKNOWN, NOT_NULL));

    fun testInvocationOnCheckedParameter() = doTest(javaClass<inferenceData.Test>(), "testInvocationOnCheckedParameter", "(Ljava/lang/String;)V", arrayList(UNKNOWN));

    fun testIncompatibleChecks() = doTest(javaClass<inferenceData.Test>(), "testIncompatibleChecks", "(Ljava/lang/String;)V", arrayList(UNKNOWN, CONFLICT));

    fun testInvocationOnNullParameter() = doTest(javaClass<inferenceData.Test>(), "testInvocationOnNullParameter", "(Ljava/lang/String;)V", arrayList(UNKNOWN, CONFLICT));
}
package inference

import junit.framework.TestCase
import org.objectweb.asm.ClassReader
import org.jetbrains.kannotator.controlFlowBuilder.buildControlFlowGraph
import org.jetbrains.kannotator.nullability.NullabilityValueInfo
import kotlin.test.assertEquals
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.ClassName
import org.objectweb.asm.Opcodes
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.Positions
import java.util.Collections
import org.jetbrains.kannotator.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.nullability.NullabilityAnnotation.*
import org.jetbrains.kannotator.annotationsInference.AnnotationsInference

class InferenceTest: TestCase() {
    fun doTest(theClass: Class<out Any>, methodName: String, methodDescriptor: String,
               expectedReturnInfo: NullabilityAnnotation?, vararg pairs: Pair<Int, NullabilityAnnotation>) {
        val parametersMap = hashMap(*pairs)
        val className = theClass.getName()
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
        for (index in parametersNumber.indices) {
            assertEquals(expectedParametersInfo.get(index), actual.get(positions.forReturnType().position))
        }
    }

    fun testNull() = doTest(javaClass<inferenceData.Test>(), "testNull", "()Ljava/lang/Object;", NULLABLE)

    fun testNullOrObject() = doTest(javaClass<inferenceData.Test>(), "testNullOrObject", "()Ljava/lang/Object;", NULLABLE)

    fun testNotNullParameter() = doTest(javaClass<inferenceData.Test>(), "testNotNullParameter", "(Ljava/lang/String;)V", null, 1 to NOT_NULL);

    fun testInvocationOnCheckedParameter() = doTest(javaClass<inferenceData.Test>(), "testInvocationOnCheckedParameter", "(Ljava/lang/String;)V", null);

    //todo test CONFLICT
    fun testIncompatibleChecks() = doTest(javaClass<inferenceData.Test>(), "testIncompatibleChecks", "(Ljava/lang/String;)V", null);

    fun testInvocationOnNullParameter() = doTest(javaClass<inferenceData.Test>(), "testInvocationOnNullParameter", "(Ljava/lang/String;)V", null, 1 to NOT_NULL);

    fun testNullableParameter() = doTest(javaClass<inferenceData.Test>(), "testNullableParameter", "(Ljava/lang/String;)V", null, 1 to NULLABLE)

//    fun testSenselessNotNullCheck() = doTest(javaClass<inferenceData.Test>(), "testSenselessNotNullCheck", "(Ljava/lang/String;)V", arrayList(null, NOT_NULL))

    fun testInvocationAfterReturn() = doTest(javaClass<inferenceData.Test>(), "testInvocationAfterReturn", "(Ljava/lang/String;)V", null, 1 to NULLABLE)
}

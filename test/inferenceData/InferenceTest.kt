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
import org.jetbrains.kannotator.annotationsInference.buildAnnotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.funDependecy.GlobalMethodSearcher
import java.util.HashMap
import kotlinlib.*

class InferenceTest: TestCase() {
    private fun Array<Annotation?>.toNullabilityAnnotation(): NullabilityAnnotation? {
        for (ann in this) {
            if (ann!!.annotationType().getSimpleName() == "ExpectNullable") return NullabilityAnnotation.NULLABLE
            if (ann!!.annotationType().getSimpleName() == "ExpectNotNull") return NullabilityAnnotation.NOT_NULL
        }
        return null
    }

    private fun doTest() {
        val theClass = javaClass<inferenceData.Test>()
        val className = theClass.getName()
        val methodName = getName()!!
        val reflectMethod = theClass.getMethods().find { m -> m.getName() == methodName }!!
        val methodDescriptor = Type.getMethodDescriptor(reflectMethod)

        val classReader = ClassReader(className)

        val graph = buildControlFlowGraph(classReader, methodName, methodDescriptor)

        val method = Method(ClassName.fromInternalName(className), Opcodes.ACC_PUBLIC, methodName, methodDescriptor, null)
        val positions = Positions(method)
        val result = buildAnnotations(graph, positions, GlobalMethodSearcher(), AnnotationsImpl())

        val expectedReturnInfo = reflectMethod.getAnnotations().toNullabilityAnnotation()

        val parametersMap = HashMap<Int, NullabilityAnnotation>()
        for ((paramIndex, paramAnnotations) in reflectMethod.getParameterAnnotations().indexed) {
            val annotation = paramAnnotations!!.toNullabilityAnnotation()
            if (annotation != null) {
                parametersMap[paramIndex + 1] = annotation!! // Kotlin compiler bug
            }
        }

        assertEquals(parametersMap, expectedReturnInfo, result, reflectMethod.getParameterTypes()!!.size, positions)
    }

    fun assertEquals(expectedParametersAnnotations: Map<Int, NullabilityAnnotation>, expectedReturnAnnotation: NullabilityAnnotation?,
                     actual: Annotations<NullabilityAnnotation>, parametersNumber: Int, positions: Positions) {
        assertEquals(expectedReturnAnnotation, actual.get(positions.forReturnType().position))

        for (index in 1..parametersNumber) {
            assertEquals(expectedParametersAnnotations.get(index), actual.get(positions.forParameter(index).position))
        }
    }

    fun testNull() = doTest()

    fun testNullOrObject() = doTest()

    fun testNotNullParameter() = doTest()

    fun testInvocationOnCheckedParameter() = doTest()

    //todo test CONFLICT
    fun testIncompatibleChecks() = doTest()

    fun testInvocationOnNullParameter() = doTest()

    fun testNullableParameter() = doTest()

//    fun testSenselessNotNullCheck() = doTest("testSenselessNotNullCheck", "(Ljava/lang/String;)V", arrayList(null, NOT_NULL))

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

    //todo
//    fun testInvocationAfterException() = doTest()
}

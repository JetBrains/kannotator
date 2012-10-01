package inference

import junit.framework.TestCase
import kotlin.test.assertEquals
import org.jetbrains.kannotator.annotationsInference.AnnotationsInference
import org.jetbrains.kannotator.controlFlowBuilder.buildControlFlowGraph
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.Positions
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.jetbrains.kannotator.annotationsInference.buildAnnotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import java.util.HashMap
import kotlinlib.*
import org.jetbrains.kannotator.annotationsInference.DerivedAnnotation
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.index.DeclarationIndex
import util.ClassPathDeclarationIndex

abstract class InferenceTest<A: DerivedAnnotation>(
        val testClass: Class<*>,
        val buildAnnotationsMethod: (ControlFlowGraph, Positions, DeclarationIndex, Annotations<A>) -> Annotations<A>)
: TestCase() {

    protected abstract fun Array<Annotation>.toAnnotation(): A?

    protected fun doTest() {
        val className = testClass.getName()
        val methodName = getName()!!
        val reflectMethod = testClass.getMethods().find { m -> m.getName() == methodName }!!
        val methodDescriptor = Type.getMethodDescriptor(reflectMethod)

        val classReader = ClassReader(className)

        val graph = buildControlFlowGraph(classReader, methodName, methodDescriptor)

        val method = Method(ClassName.fromInternalName(className), Opcodes.ACC_PUBLIC, methodName, methodDescriptor, null)
        val positions = Positions(method)
        val result = buildAnnotationsMethod(graph, positions, ClassPathDeclarationIndex, AnnotationsImpl())

        val expectedReturnInfo = reflectMethod.getAnnotations().toAnnotation()

        val parametersMap = HashMap<Int, DerivedAnnotation>()
        for ((paramIndex, paramAnnotations) in reflectMethod.getParameterAnnotations().indexed) {
            val annotation = paramAnnotations.toAnnotation()
            if (annotation != null) {
                parametersMap[paramIndex + 1] = annotation!! // Kotlin compiler bug
            }
        }

        assertEquals(parametersMap, expectedReturnInfo, result, reflectMethod.getParameterTypes()!!.size, positions)
    }

    fun assertEquals(expectedParametersAnnotations: Map<Int, DerivedAnnotation>, expectedReturnAnnotation: DerivedAnnotation?,
                     actual: Annotations<DerivedAnnotation>, parametersNumber: Int, positions: Positions) {
        assertEquals(expectedReturnAnnotation, actual.get(positions.forReturnType().position))

        for (index in 1..parametersNumber) {
            assertEquals(expectedParametersAnnotations.get(index), actual.get(positions.forParameter(index).position))
        }
    }
}

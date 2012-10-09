package inference

import junit.framework.TestCase
import kotlin.test.assertEquals
import org.jetbrains.kannotator.annotationsInference.AbstractAnnotationInferrer
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import java.util.HashMap
import kotlinlib.*
import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.index.DeclarationIndex
import util.ClassPathDeclarationIndex
import util.controlFlow.buildControlFlowGraph

abstract class AbstractInferenceTest<A: Annotation>(val testClass: Class<*>) : TestCase() {

    protected abstract fun Array<jet.Annotation>.toAnnotation(): A?

    protected abstract fun buildAnnotations(graph: ControlFlowGraph, positions: PositionsWithinMember, declarationIndex: DeclarationIndex,
                                            annotations: Annotations<A>) : Annotations<A>

    protected open fun getInitialAnnotations(): Annotations<A> = AnnotationsImpl()

    protected fun doTest() {
        val className = testClass.getName()
        val methodName = getName()!!
        val reflectMethod = testClass.getMethods().find { m -> m.getName() == methodName }!!
        val methodDescriptor = Type.getMethodDescriptor(reflectMethod)

        val classReader = ClassReader(className)

        val graph = buildControlFlowGraph(classReader, methodName, methodDescriptor)

        val method = Method(ClassName.fromInternalName(className), Opcodes.ACC_PUBLIC, methodName, methodDescriptor, null)
        val positions = PositionsWithinMember(method)
        val result = buildAnnotations(graph, positions, ClassPathDeclarationIndex, getInitialAnnotations())

        val expectedReturnInfo = reflectMethod.getAnnotations().toAnnotation()

        val parametersMap = HashMap<Int, A>()
        for ((paramIndex, paramAnnotations) in reflectMethod.getParameterAnnotations().indexed) {
            val annotation = paramAnnotations.toAnnotation()
            if (annotation != null) {
                parametersMap[paramIndex + 1] = annotation!! // Kotlin compiler bug
            }
        }

        checkInferredAnnotations(parametersMap, expectedReturnInfo, result, reflectMethod.getParameterTypes()!!.size, positions)
    }

    fun checkInferredAnnotations(expectedParametersAnnotations: Map<Int, A>, expectedReturnAnnotation: A?,
                     actual: Annotations<A>, parametersNumber: Int, positions: PositionsWithinMember) {
        assertEquals(expectedReturnAnnotation, actual.get(positions.forReturnType().position),
                "Return annotations error")

        for (index in 1..parametersNumber) {
            assertEquals(expectedParametersAnnotations.get(index), actual.get(positions.forParameter(index).position),
                    "Annotations for parameters ($index) error")
        }
    }
}

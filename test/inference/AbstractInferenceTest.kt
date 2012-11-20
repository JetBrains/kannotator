package inference

import java.util.HashMap
import junit.framework.TestCase
import kotlin.test.assertEquals
import kotlinlib.*
import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import util.ClassPathDeclarationIndex
import util.controlFlow.buildControlFlowGraph
import util.junit.getTestName
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.declarations.getFieldAnnotatedType
import org.objectweb.asm.ClassVisitor
import org.jetbrains.kannotator.asm.util.forEachField
import kotlin.test.assertFalse
import kotlin.test.fail
import util.getClassReader

abstract class AbstractInferenceTest<A: Annotation>(val testClass: Class<*>) : TestCase() {

    protected abstract fun Array<out jet.Annotation>.toAnnotation(): A?

    protected open fun buildAnnotations(
            graph: ControlFlowGraph, positions: PositionsForMethod, declarationIndex: DeclarationIndex,
            annotations: Annotations<A>) : Annotations<A> = AnnotationsImpl()

    protected open fun buildFieldAnnotations(
            field: Field,
            classReader: ClassReader,
            declarationIndex: DeclarationIndex,
            annotations: Annotations<A>) : Annotations<A> = AnnotationsImpl()

    protected open fun getInitialAnnotations(): Annotations<A> = AnnotationsImpl()

    protected fun doFieldTest() {
        val fieldName = getTestName(true)

        val reflectedField = testClass.getField(fieldName)
        val classReader = getClassReader(testClass)

        var foundField: Field? = null
        classReader.forEachField {
            className, access, name, desc, signature, value ->
            if (name == fieldName) {
                foundField = Field(ClassName.fromInternalName(className), access, name, desc, signature, value)
            }
        }

        val field = foundField ?: throw AssertionError("Field $fieldName wasn't found")

        val result = buildFieldAnnotations(field, classReader, ClassPathDeclarationIndex, getInitialAnnotations())

        val expectedReturnInfo = reflectedField.getAnnotations().toAnnotation()

        checkFieldInferredAnnotations(expectedReturnInfo, result, field)
    }

    protected fun doTest() {
        val className = testClass.getName()
        val methodName = getName()!!
        val reflectMethod = (testClass.getMethods() as Array<java.lang.reflect.Method>).find { m -> m.getName() == methodName }!!
        val methodDescriptor = Type.getMethodDescriptor(reflectMethod)

        val classReader = getClassReader(className)

        val graph = buildControlFlowGraph(classReader, methodName, methodDescriptor)

        val method = Method(ClassName.fromInternalName(className), Opcodes.ACC_PUBLIC, methodName, methodDescriptor, null)
        val positions = PositionsForMethod(method)
        val result = buildAnnotations(graph, positions, ClassPathDeclarationIndex, getInitialAnnotations())

        val expectedReturnInfo = reflectMethod.getAnnotations().toAnnotation()

        val parametersMap = HashMap<Int, A>()
        for ((paramIndex, paramAnnotations) in (reflectMethod.getParameterAnnotations() as Array<Array<jet.Annotation>>).indexed) {
            val annotation = paramAnnotations.toAnnotation()
            if (annotation != null) {
                parametersMap[paramIndex + 1] = annotation!! // Kotlin compiler bug
            }
        }

        checkInferredAnnotations(parametersMap, expectedReturnInfo, result, reflectMethod.getParameterTypes()!!.size, positions)
    }

    fun checkInferredAnnotations(expectedParametersAnnotations: Map<Int, A>, expectedReturnAnnotation: A?,
                     actual: Annotations<A>, parametersNumber: Int, positions: PositionsForMethod) {
        assertEquals(expectedReturnAnnotation, actual.get(positions.forReturnType().position),
                "Return annotations error")

        for (index in 1..parametersNumber) {
            assertEquals(expectedParametersAnnotations.get(index), actual.get(positions.forParameter(index).position),
                    "Annotations for parameter $index error")
        }
    }

    fun checkFieldInferredAnnotations(
            expectedReturnAnnotation: A?,
            actual: Annotations<A>,
            field: Field) {
        val fieldTypePosition = getFieldAnnotatedType(field).position
        assertEquals(expectedReturnAnnotation, actual[fieldTypePosition], "Return annotations error")
    }
}

package inference

import java.io.File
import java.util.ArrayList
import java.util.HashMap
import junit.framework.TestCase
import kotlin.test.assertEquals
import kotlinlib.*
import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.asm.util.forEachField
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.ProgressMonitor
import org.jetbrains.kannotator.main.inferAnnotations
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import util.getClassReader
import util.junit.getTestName

abstract class AbstractInferenceTest<A: Annotation>(val testClass: Class<*>) : TestCase() {

    protected abstract fun Array<out jet.Annotation>.toAnnotation(): A?

    protected abstract fun getInitialAnnotations(): Annotations<A>
    protected abstract fun getInferrer(): AnnotationInferrer<A>
    protected abstract fun getClassFiles(): Collection<File>

    private fun doInferAnnotations(annotations: Annotations<A>) : Annotations<Any> {
        return inferAnnotations<String>(
                FileBasedClassSource(getClassFiles()),
                ArrayList<File>(),
                hashMap(Pair("inferrer", getInferrer() as AnnotationInferrer<Any>)),
                ProgressMonitor(),
                false,
                true,
                hashMap(Pair("inferrer", annotations))).inferredAnnotationsMap["inferrer"]!!
    }

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
        val resultFieldAnnotation = doInferAnnotations(getInitialAnnotations())[getFieldTypePosition(field)]

        val expectedReturnInfo = reflectedField.getAnnotations().toAnnotation()

        checkFieldInferredAnnotations(expectedReturnInfo, resultFieldAnnotation as Annotation?, field)
    }

    protected fun doTest() {
        val methodName = getName()!!
        val reflectMethod = (testClass.getMethods() as Array<java.lang.reflect.Method>).find { m -> m.getName() == methodName }!!
        val methodDescriptor = Type.getMethodDescriptor(reflectMethod)

        val classReader = getClassReader(testClass.getName())

        // Bad: Reading class source twice
        val declarationIndex = DeclarationIndexImpl(object : ClassSource { override fun forEach(body: (ClassReader) -> Unit) = body(classReader) })
        val method = declarationIndex.findMethod(ClassName.fromInternalName(Type.getInternalName(testClass)), methodName, methodDescriptor)
        assert(method != null, "Tested method $methodName wasn't found in index")

        val resultAnnotations = doInferAnnotations(getInitialAnnotations())

        val positions = PositionsForMethod(method!!)
        val expectedReturnInfo = reflectMethod.getAnnotations().toAnnotation()

        val parametersMap = HashMap<Int, A>()
        for ((paramIndex, paramAnnotations) in (reflectMethod.getParameterAnnotations() as Array<Array<jet.Annotation>>).indexed) {
            val annotation = paramAnnotations.toAnnotation()
            if (annotation != null) {
                parametersMap[paramIndex + 1] = annotation!! // Kotlin compiler bug
            }
        }

        checkInferredAnnotations(parametersMap, expectedReturnInfo, resultAnnotations, reflectMethod.getParameterTypes()!!.size, positions)
    }

    fun checkInferredAnnotations(expectedParametersAnnotations: Map<Int, A>, expectedReturnAnnotation: A?,
                     actual: Annotations<Any>, parametersNumber: Int, positions: PositionsForMethod) {
        assertEquals(expectedReturnAnnotation, actual[positions.forReturnType().position],
                "Return annotations error")

        for (index in 1..parametersNumber) {
            assertEquals(expectedParametersAnnotations[index], actual[positions.forParameter(index).position],
                    "Annotations for parameter $index error")
        }
    }

    fun checkFieldInferredAnnotations(expectedReturnAnnotation: A?, actualAnnotation: Annotation?, field: Field) {
        assertEquals(expectedReturnAnnotation, actualAnnotation, "Return annotations error for field $field")
    }
}

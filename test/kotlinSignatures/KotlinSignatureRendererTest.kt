package kotlinSignatures

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import kotlinlib.recurseFiltered
import kotlinlib.replaceSuffix
import kotlinlib.replaceExtension
import kotlin.template.append
import kotlinlib.getParents
import util.getClassReader
import org.jetbrains.kannotator.asm.util.*
import org.jetbrains.kannotator.declarations.*
import java.util.ArrayList
import org.jetbrains.kannotator.kotlinSignatures.kotlinSignatureToAnnotationData
import org.jetbrains.kannotator.kotlinSignatures.renderMethodSignature
import org.jetbrains.kannotator.kotlinSignatures.renderFieldSignature
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.AnnotationVisitor
import junit.framework.ComparisonFailure
import org.objectweb.asm.tree.MethodNode
import java.util.HashMap
import org.objectweb.asm.tree.FieldNode
import org.jetbrains.kannotator.main.loadMethodAnnotationsFromByteCode
import util.*
import org.jetbrains.kannotator.main.loadFieldAnnotationsFromByteCode
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.MUTABILITY_INFERRER
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.index.DeclarationIndex
import java.util.LinkedHashMap
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.objectweb.asm.ClassReader
import org.jetbrains.kannotator.index.loadMethodParameterNames
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.controlFlow.builder.analysis.MUTABILITY_KEY
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY

/** Tests rendering of Nullability/Mutability annotations into KotlinSignature.
 *  Input: classes in KotlinSignatureTestData with annotations.
 *  Output: Kotlin signatures based on found annotations
 *  */
class KotlinSignatureRendererTest {

    fun checkSignature(
            expectedSignature: String,
            member: ClassMember,
            nullability: Annotations<NullabilityAnnotation> = AnnotationsImpl(),
            mutability: Annotations<MutabilityAnnotation> = AnnotationsImpl()
    ): ComparisonFailure? {
        val signature = when (member) {
                            is Method -> renderMethodSignature(member, nullability, mutability)
                            is Field -> renderFieldSignature(member, nullability, mutability)
                            else -> throw IllegalArgumentException("Unknown member kind: $member")
                        }
        if (expectedSignature != signature) {
            return ComparisonFailure(null, expectedSignature, signature)
        }
        return null
    }

    fun doMultipleDeclarationsTest(classReader: ClassReader) {
        val methodNodes = LinkedHashMap<Method, MethodNode>()
        val fieldNodes = LinkedHashMap<Field, FieldNode>()

        val errors = ArrayList<ComparisonFailure?>()

        val className = ClassName.fromInternalName(classReader.getClassName())
        classReader.accept(object : ClassVisitor(Opcodes.ASM4) {
            public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                val method = Method(className, access, name, desc, signature)
                val node = MethodNode(access, name, desc, signature, exceptions)
                methodNodes[method] = node
                return node
            }

            public override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                val field = Field(className, access, name, desc, signature, value)
                val node = FieldNode(access, name, desc, signature, value)
                fieldNodes[field] = node
                return node
            }
        }, 0)

        val methodAnnotations = loadMethodAnnotationsFromByteCode(methodNodes, INFERRERS)
        val fieldAnnotations = loadFieldAnnotationsFromByteCode(fieldNodes, INFERRERS)

        for ((field, node) in fieldNodes) {
            val annotations = node.invisibleAnnotations
            if (annotations != null) {
                for (annotation in annotations) {
                    if (annotation.desc == "Ljet/runtime/typeinfo/KotlinSignature;") {
                        val nullability = fieldAnnotations[NULLABILITY_KEY] as Annotations<NullabilityAnnotation>
                        val mutability = fieldAnnotations[MUTABILITY_KEY] as Annotations<MutabilityAnnotation>
                        errors.add(checkSignature(annotation.values!!.get(1)!!.toString(), field, nullability, mutability))
                    }
                }
            }
        }

        for ((method, node) in methodNodes) {
            loadMethodParameterNames(method, node)

            val annotations = node.invisibleAnnotations
            if (annotations != null) {
                for (annotation in annotations) {
                    if (annotation.desc == "Ljet/runtime/typeinfo/KotlinSignature;") {
                        val nullability = methodAnnotations[NULLABILITY_KEY] as Annotations<NullabilityAnnotation>
                        val mutability = methodAnnotations[MUTABILITY_KEY] as Annotations<MutabilityAnnotation>
                        errors.add(checkSignature(annotation.values!!.get(1)!!.toString(), method, nullability, mutability))
                    }
                }
            }
        }

        val actualErrors = errors.filterNotNull()

        if (!actualErrors.isEmpty()) {
            System.err.println("${actualErrors.size()} Errors:")
            System.err.println()
        }

        for (error in actualErrors) {
            System.err.println("Expected: ${error.getExpected()}")
            System.err.println("Actual  : ${error.getActual()}")
            error.printStackTrace()
            System.err.flush()
        }

        if (!actualErrors.isEmpty()) {
            fail("See errors above: $actualErrors")
        }

    }

    Test fun noAnnotations() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.NoAnnotations>())
        doMultipleDeclarationsTest(classReader)
    }

    Test fun nullability() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.Nullability>())
        doMultipleDeclarationsTest(classReader)
    }

    Test fun generics() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.NoAnnotationsGeneric<*>>())
        doMultipleDeclarationsTest(classReader)
    }

    Test fun mutabilityNoAnnotations() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.MutabilityNoAnnotations>())
        doMultipleDeclarationsTest(classReader)
    }

    Test fun genericInner() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.WithGenericInner<*>>())
        doMultipleDeclarationsTest(classReader)
    }

    Test fun mutability() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.Mutability>())
        doMultipleDeclarationsTest(classReader)
    }

    Test fun constructorOfInner() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.Inner>())
        doMultipleDeclarationsTest(classReader)
    }

    Test fun namedParametersLongTypes() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.NamedParametersLongTypes>())
        doMultipleDeclarationsTest(classReader)
    }

    Test fun `enum`() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.Enum>())
        doMultipleDeclarationsTest(classReader)
    }
}

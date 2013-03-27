package kotlinSignatures

import junit.framework.TestCase
import junit.framework.Assert.*
import java.io.File
import kotlinlib.recurseFiltered
import kotlinlib.replaceSuffix
import kotlinlib.replaceExtension
import kotlinlib.buildString
import kotlin.template.append
import kotlinlib.getParents
import kotlinlib.join
import kotlinlib.removeSuffix
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

class KotlinSignatureRendererTest : TestCase() {

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
            System.err.println("${actualErrors.size} Errors:")
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

    fun testNoAnnotations() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.NoAnnotations>())
        doMultipleDeclarationsTest(classReader)
    }

    fun testNullability() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.Nullability>())
        doMultipleDeclarationsTest(classReader)
    }

    fun testGenerics() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.NoAnnotationsGeneric<*>>())
        doMultipleDeclarationsTest(classReader)
    }

    fun testMutabilityNoAnnotations() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.MutabilityNoAnnotations>())
        doMultipleDeclarationsTest(classReader)
    }

    fun testGenericInner() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.WithGenericInner<*>>())
        doMultipleDeclarationsTest(classReader)
    }

    fun testMutability() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.Mutability>())
        doMultipleDeclarationsTest(classReader)
    }

    fun testConstructorOfInner() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.Inner>())
        doMultipleDeclarationsTest(classReader)
    }

    fun testNamedParametersLongTypes() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.NamedParametersLongTypes>())
        doMultipleDeclarationsTest(classReader)
    }

    fun testEnum() {
        val classReader = getClassReader(javaClass<KotlinSignatureTestData.Enum>())
        doMultipleDeclarationsTest(classReader)
    }
}

fun main(args: Array<String>) {


    val dir = File("/Users/abreslav/work/kannotator/testData/kotlinSignatures")
    val str = buildString {
        sb ->
        sb.append("class KotlinSignatureGeneratorTest : TestCase() {\n")
        dir.recurseFiltered({file -> file.getName().endsWith(".java")}) {
            javaFile ->
            val sourceRoot = dir.getParentFile()!!
            val relativeJavaFile = File(javaFile.getPath().substring(sourceRoot.getPath().size + 1))
            val packageFqn = relativeJavaFile.getParents().join(".")
            val className = javaFile.getName().removeSuffix(".java")
            val ktTxt = javaFile.replaceExtension("kt.txt")
            val relativeKtTxt = relativeJavaFile.replaceExtension("kt.txt")
            if (ktTxt.exists()) {
                sb.append("    fun test$className() { doTest(javaClass<$packageFqn.$className>(), \"$relativeKtTxt\") }\n\n")
            }
        }
        sb.append("}")
    }
    println(str)
}
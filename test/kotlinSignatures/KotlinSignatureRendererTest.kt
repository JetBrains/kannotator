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
import org.jetbrains.kannotator.kotlinSignatures.renderKotlinSignature
import org.jetbrains.kannotator.kotlinSignatures.renderMethodSignature
import org.jetbrains.kannotator.kotlinSignatures.renderFieldSignature
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.AnnotationVisitor
import junit.framework.ComparisonFailure

class KotlinSignatureRendererTest: TestCase() {
    fun doTest(javaClass: Class<*>, expectedPath: String) {
        val members = ArrayList<ClassMember>()
        getClassReader(javaClass).forEachMember {
            member ->
            members.add(member)
        }

        assertFalse(javaClass.toString(), members.isEmpty())

        val member = if (members.size == 1)
                        members[0]
                     else
                        members.filter { m -> m !is Method || m.id.methodName != "<init>" }.first()

        val expectedText = File("testData/" + expectedPath).readText()
        assertSignature(member, expectedText)
    }

    fun assertSignature(member: ClassMember, expectedSignature: String) {
        val check = checkSignature(member, expectedSignature)
        if (check != null) {
            throw check
        }
    }

    fun checkSignature(member: ClassMember, expectedSignature: String): ComparisonFailure? {
        val signature = when (member) {
            is Method -> renderMethodSignature(member, AnnotationsImpl(), AnnotationsImpl())
            is Field -> renderFieldSignature(member, AnnotationsImpl(), AnnotationsImpl())
            else -> throw AssertionError("Unknown member type: $member")
        }
        if (expectedSignature != signature) {
            return ComparisonFailure(null, expectedSignature, signature)
        }
        return null
    }

    fun testMultipleDeclarationsInOneClass() {
        val errors = ArrayList<ComparisonFailure?>()

        val classReader = getClassReader(javaClass<KotlinSignatureTestData<*>>())
        val className = ClassName.fromInternalName(classReader.getClassName())
        classReader.accept(object : ClassVisitor(Opcodes.ASM4) {
            public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                val method = Method(className, access, name, desc, signature)
                return object : MethodVisitor(Opcodes.ASM4) {
                    public override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                        if (desc != "Ljet/runtime/typeinfo/KotlinSignature;") return null
                        return object : AnnotationVisitor(Opcodes.ASM4) {
                            public override fun visit(name: String?, value: Any?) {
                                errors.add(checkSignature(method, value.toString()))
                            }
                        }
                    }
                }
            }

            public override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                val field = Field(className, access, name, desc, signature, value)
                return object : FieldVisitor(Opcodes.ASM4) {

                    public override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                        if (desc != "Ljet/runtime/typeinfo/KotlinSignature;") return null
                        return object : AnnotationVisitor(Opcodes.ASM4) {
                            public override fun visit(name: String?, value: Any?) {
                                errors.add(checkSignature(field, value.toString()))
                            }
                        }
                    }
                }
            }
        }, 0)

        val actualErrors = errors.filterNotNull()
        for (error in actualErrors) {
            System.err.println("Expected: ${error.getExpected()}")
            System.err.println("Actual  : ${error.getActual()}")
            error.printStackTrace()
        }

        if (!actualErrors.isEmpty()) {
            fail("See errors above")
        }

    }

    fun testAnnotatedMethod() { doTest(javaClass<kotlinSignatures.annotation.AnnotatedMethod>(), "kotlinSignatures/annotation/AnnotatedMethod.kt.txt") }

    fun testArrayTypeVariance() { doTest(javaClass<kotlinSignatures.ArrayTypeVariance>(), "kotlinSignatures/ArrayTypeVariance.kt.txt") }

    fun testClassDoesNotOverrideMethod() { doTest(javaClass<kotlinSignatures.ClassDoesNotOverrideMethod>(), "kotlinSignatures/ClassDoesNotOverrideMethod.kt.txt") }

    fun testClassWithTypeP() { doTest(javaClass<kotlinSignatures.ClassWithTypeP<*>>(), "kotlinSignatures/ClassWithTypeP.kt.txt") }

    fun testClassWithTypePExtendsIterableP() { doTest(javaClass<kotlinSignatures.ClassWithTypePExtendsIterableP<*>>(), "kotlinSignatures/ClassWithTypePExtendsIterableP.kt.txt") }

    fun testClassWithTypePP() { doTest(javaClass<kotlinSignatures.ClassWithTypePP<*, *>>(), "kotlinSignatures/ClassWithTypePP.kt.txt") }

    fun testClassWithTypePRefNext() { doTest(javaClass<kotlinSignatures.ClassWithTypePRefNext<*, *>>(), "kotlinSignatures/ClassWithTypePRefNext.kt.txt") }

    fun testClassWithTypePRefSelf() { doTest(javaClass<kotlinSignatures.ClassWithTypePRefSelf<*>>(), "kotlinSignatures/ClassWithTypePRefSelf.kt.txt") }

    fun testConstructorGenericDeep() { doTest(javaClass<kotlinSignatures.constructor.ConstructorGenericDeep>(), "kotlinSignatures/constructor/ConstructorGenericDeep.kt.txt") }

    fun testConstructorGenericSimple() { doTest(javaClass<kotlinSignatures.constructor.ConstructorGenericSimple>(), "kotlinSignatures/constructor/ConstructorGenericSimple.kt.txt") }

    fun testConstructorGenericUpperBound() { doTest(javaClass<kotlinSignatures.constructor.ConstructorGenericUpperBound>(), "kotlinSignatures/constructor/ConstructorGenericUpperBound.kt.txt") }

    fun testFieldAsVar() { doTest(javaClass<kotlinSignatures.FieldAsVar>(), "kotlinSignatures/FieldAsVar.kt.txt") }

    fun testFieldOfArrayType() { doTest(javaClass<kotlinSignatures.FieldOfArrayType>(), "kotlinSignatures/FieldOfArrayType.kt.txt") }

    fun testFinalFieldAsVal() { doTest(javaClass<kotlinSignatures.FinalFieldAsVal>(), "kotlinSignatures/FinalFieldAsVal.kt.txt") }

    fun testInnerClass() { doTest(javaClass<kotlinSignatures.InnerClass>(), "kotlinSignatures/InnerClass.kt.txt") }

    fun testDifferentGetterAndSetter() { doTest(javaClass<kotlinSignatures.javaBean.DifferentGetterAndSetter>(), "kotlinSignatures/javaBean/DifferentGetterAndSetter.kt.txt") }

    fun testJavaBeanAbstractGetter() { doTest(javaClass<kotlinSignatures.javaBean.JavaBeanAbstractGetter>(), "kotlinSignatures/javaBean/JavaBeanAbstractGetter.kt.txt") }

    fun testJavaBeanVal() { doTest(javaClass<kotlinSignatures.javaBean.JavaBeanVal>(), "kotlinSignatures/javaBean/JavaBeanVal.kt.txt") }

    fun testJavaBeanVar() { doTest(javaClass<kotlinSignatures.javaBean.JavaBeanVar>(), "kotlinSignatures/javaBean/JavaBeanVar.kt.txt") }

    fun testJavaBeanVarOfGenericType() { doTest(javaClass<kotlinSignatures.javaBean.JavaBeanVarOfGenericType<*>>(), "kotlinSignatures/javaBean/JavaBeanVarOfGenericType.kt.txt") }

    fun testTwoSetters() { doTest(javaClass<kotlinSignatures.javaBean.TwoSetters>(), "kotlinSignatures/javaBean/TwoSetters.kt.txt") }

    fun testLoadIterable() { doTest(javaClass<kotlinSignatures.library.LoadIterable<*>>(), "kotlinSignatures/library/LoadIterable.kt.txt") }

    fun testLoadIterator() { doTest(javaClass<kotlinSignatures.library.LoadIterator<*>>(), "kotlinSignatures/library/LoadIterator.kt.txt") }

    fun testMethodTypePOneUpperBound() { doTest(javaClass<kotlinSignatures.MethodTypePOneUpperBound>(), "kotlinSignatures/MethodTypePOneUpperBound.kt.txt") }

    fun testMethodTypePTwoUpperBounds() { doTest(javaClass<kotlinSignatures.MethodTypePTwoUpperBounds>(), "kotlinSignatures/MethodTypePTwoUpperBounds.kt.txt") }

    fun testMethodWithTypeP() { doTest(javaClass<kotlinSignatures.MethodWithTypeP>(), "kotlinSignatures/MethodWithTypeP.kt.txt") }

    fun testMethodWithTypePP() { doTest(javaClass<kotlinSignatures.MethodWithTypePP>(), "kotlinSignatures/MethodWithTypePP.kt.txt") }

    fun testMethodWithTypePRefClassP() { doTest(javaClass<kotlinSignatures.MethodWithTypePRefClassP<*>>(), "kotlinSignatures/MethodWithTypePRefClassP.kt.txt") }

    fun testMethosWithPRefTP() { doTest(javaClass<kotlinSignatures.MethosWithPRefTP>(), "kotlinSignatures/MethosWithPRefTP.kt.txt") }

    fun testModalityOfFakeOverrides() { doTest(javaClass<kotlinSignatures.modality.ModalityOfFakeOverrides>(), "kotlinSignatures/modality/ModalityOfFakeOverrides.kt.txt") }

    fun testMyException() { doTest(javaClass<kotlinSignatures.MyException>(), "kotlinSignatures/MyException.kt.txt") }

    fun testNotNullField() { doTest(javaClass<kotlinSignatures.notNull.NotNullField>(), "kotlinSignatures/notNull/NotNullField.kt.txt") }

    fun testNotNullMethod() { doTest(javaClass<kotlinSignatures.notNull.NotNullMethod>(), "kotlinSignatures/notNull/NotNullMethod.kt.txt") }

    fun testNotNullParameter() { doTest(javaClass<kotlinSignatures.notNull.NotNullParameter>(), "kotlinSignatures/notNull/NotNullParameter.kt.txt") }

    fun testSimple() { doTest(javaClass<kotlinSignatures.Simple>(), "kotlinSignatures/Simple.kt.txt") }

    fun testVarargInt() { doTest(javaClass<kotlinSignatures.vararg.VarargInt>(), "kotlinSignatures/vararg/VarargInt.kt.txt") }

    fun testVarargString() { doTest(javaClass<kotlinSignatures.vararg.VarargString>(), "kotlinSignatures/vararg/VarargString.kt.txt") }
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
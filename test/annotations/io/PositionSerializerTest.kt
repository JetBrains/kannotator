package annotations.io

import junit.framework.TestCase
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.PositionWithinDeclaration
import org.jetbrains.kannotator.declarations.AnnotationPosition
import org.jetbrains.kannotator.declarations.RETURN_TYPE
import org.jetbrains.kannotator.declarations.Access
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import junit.framework.Assert.*
import org.objectweb.asm.Opcodes
import org.jetbrains.kannotator.declarations.ParameterPosition
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.util.TraceSignatureVisitor
import org.jetbrains.kannotator.declarations.MethodTypePosition

class PositionSerializerTest : TestCase() {
    fun doTest(
            expected: String,
            owner: String, methodName: String, desc: String,
            position: PositionWithinDeclaration,
            signature: String? = null,
            static: Boolean = false, varargs: Boolean = false) {
        val staticFlag = if (static) Opcodes.ACC_STATIC else 0
        val varargsFlag = if (varargs) Opcodes.ACC_VARARGS else 0
        val method = Method(ClassName.fromInternalName(owner), staticFlag or varargsFlag, methodName, desc, signature)
        val pos = MockTypePosition(method, position)
        assertEquals(expected, pos.toAnnotationKey())
    }

    fun testVoid() {
        doTest("a.b.C void foo()",
               "a/b/C", "foo", "()V", RETURN_TYPE)
    }

    fun testClass() {
        doTest("a.b.C a.b.C foo()",
                "a/b/C", "foo", "()La/b/C;", RETURN_TYPE)
    }

    fun testInnerClass() {
        doTest("a.b.C a.b.C.D foo()",
                "a/b/C", "foo", "()La/b/C\$D;", RETURN_TYPE)
    }

    fun testConstructor() {
        doTest("a.b.C C()",
                "a/b/C", "<init>", "()V", RETURN_TYPE, null, true)
    }

    fun testStaticFirstParam() {
        doTest("A void foo(int) 0",
                "A", "foo", "(I)V", ParameterPosition(0), null, true)
    }

    fun testNonStaticFirstParam() {
        doTest("A void foo(int) 0",
                "A", "foo", "(I)V", ParameterPosition(1), null, false)
    }

    fun testNonStaticTwoParams() {
        doTest("A void foo(int, int) 0",
                "A", "foo", "(II)V", ParameterPosition(1), null, false)
    }

    fun testSpacesInTypeArguments() {
        doTest("A void foo(java.util.Map<java.lang.String,java.lang.String>)",
                "A", "foo", "(Ljava/util/Map;)V", RETURN_TYPE, "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;)V", false)
    }

    fun testGenericSignatureNoParams() {
        doTest("A java.util.List<java.lang.String> foo()",
                "A", "foo", "()Ljava/util/List;", RETURN_TYPE, "()Ljava/util/List<Ljava/lang/String;>;")
    }

    fun testGenericSignatureNonGenericParams() {
        doTest("A java.util.Map.Entry<java.lang.Integer,java.lang.String> foo(int, a.b.C)",
                "A", "foo", "(ILa/b/C;)Ljava/util/Map\$Entry;", RETURN_TYPE,
                "(ILa/b/C;)Ljava/util/Map\$Entry<Ljava/lang/Integer;Ljava/lang/String;>;")
    }

    fun testGenericSignatureGenericParams() {
        doTest("A java.util.Map.Entry<java.lang.Integer,java.util.Map.Entry<java.lang.Integer,java.lang.String>> " +
                "foo(" +
                "java.util.List<java.util.List<java.lang.String>>, " +
                "java.util.List<java.util.Map.Entry<java.lang.Integer,java.lang.String>>)",
                "A", "foo", "(Ljava/util/List;Ljava/util/List;)Ljava/util/Map\$Entry;", RETURN_TYPE,
                "(Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;" +
                "Ljava/util/List<Ljava/util/Map\$Entry<Ljava/lang/Integer;Ljava/lang/String;>;>;)" +
                "Ljava/util/Map\$Entry<Ljava/lang/Integer;Ljava/util/Map\$Entry<Ljava/lang/Integer;Ljava/lang/String;>;>;")
    }

    fun testGenericInnerInGenericOuter() {
        doTest("A t.Test<java.lang.String> test(t.Test<java.lang.String>.Inner<java.lang.Integer>)",
                "A", "test", "(Lt/Test\$Inner;)Lt/Test;", RETURN_TYPE,
                "(Lt/Test<Ljava/lang/String;>.Inner<Ljava/lang/Integer;>;)Lt/Test<Ljava/lang/String;>;")
    }

    fun testGenericMethod() {
        doTest("A void foo(java.util.Map<A,B>)",
                "A", "foo", "(Ljava/util/Map;)V", RETURN_TYPE, "<A:Ljava/lang/Object;B:Ljava/lang/Object;>(Ljava/util/Map<TA;TB;>;)V")
    }

    fun testVarargsNonGeneric() {
        doTest("A java.lang.String[] foo(java.lang.String[], int, java.lang.String...)",
                "A", "foo", "([Ljava/lang/String;I[Ljava/lang/String;)[Ljava/lang/String;", RETURN_TYPE, null, false, true)
    }

    fun testVarargsGeneric() {
        doTest("A java.lang.String[] foo(java.util.List<java.lang.String>[], int, java.util.List<java.lang.String>...)",
                "A", "foo", "([Ljava/util/List;I[Ljava/util/List;)[Ljava/lang/String;", RETURN_TYPE,
                "([Ljava/util/List<Ljava/lang/String;>;I[Ljava/util/List<Ljava/lang/String;>;)[Ljava/lang/String;",
                false, true)
    }

    fun testGenericReturningObject() {
        doTest("A java.lang.Object foo(java.lang.Class<?>, int...)",
                "A", "foo", "(Ljava/lang/Class;[I)Ljava/lang/Object;", RETURN_TYPE,
                "(Ljava/lang/Class<*>;[I)Ljava/lang/Object;", false, true)
    }

    fun testBoundTypeParameters() {
        doTest("A A foo(java.lang.Class<A>)",
                "A", "foo", "(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;", RETURN_TYPE,
                "<A::Ljava/lang/annotation/Annotation;>(Ljava/lang/Class<TA;>;)TA;", false)
    }

    fun testInnerClassConstructor() {
        doTest("java.util.zip.ZipOutputStream.XEntry XEntry(java.util.zip.ZipEntry, long) 0",
                "java/util/zip/ZipOutputStream\$XEntry", "<init>", "(Ljava/util/zip/ZipEntry;J)V", ParameterPosition(1),
                null, false)
    }
}

data class MockTypePosition(
        override val method: Method,
        override val relativePosition: PositionWithinDeclaration
) : MethodTypePosition
package annotations.io

import java.io.File
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import org.jetbrains.kannotator.annotations.io.getMethodNameAccountingForConstructor
import org.jetbrains.kannotator.annotations.io.parseFieldAnnotationKey
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.annotations.io.parseMethodAnnotationKey
import org.jetbrains.kannotator.util.processJar
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.FieldVisitor
import util.recurseIntoJars

/**
 * tests that parse(position.toAnnotationKey()) = position,
 * sample positions are taken from bytecode in jars in lib folder
 * */
class AnnotationKeyParserTest {
    private fun doMethodTest(className: String, access: Int, name: String, desc: String, signature: String?) {
        val method = Method(ClassName.fromInternalName(className), access, name, desc, signature)
        doTest(method)
    }

    private fun doFieldTest(className: String, access: Int, name: String, desc: String, signature: String?, value : Any?) {
        val field = Field(ClassName.fromInternalName(className), access, name, desc, signature, value)
        doTest(field)
    }

    fun doTest(method: Method) {
        val pos = PositionsForMethod(method).forReturnType()
        val key = pos.position.toAnnotationKey()
        try {
            val (parsedClassName, parsedReturnType, parsedMethodName) = parseMethodAnnotationKey(key)
            assertEquals(method.getMethodNameAccountingForConstructor(), parsedMethodName)
            assertEquals(method.declaringClass.canonicalName, parsedClassName)
        } catch (e: IllegalArgumentException) {
            System.err.println(e.message)
        }

        PositionsForMethod(method).forEachValidPosition { position ->
            val key = position.toAnnotationKey()
            val (parsedClassName, parsedReturnType, parsedMethodName) = parseMethodAnnotationKey(key)
            assertEquals(method.getMethodNameAccountingForConstructor(), parsedMethodName)
            assertEquals(method.declaringClass.canonicalName, parsedClassName)
        }
    }

    fun doTest(field: Field) {
        val fieldKey = getFieldAnnotatedType(field).position.toAnnotationKey()
        try {
            val (parsedClassName, parsedFieldName) = parseFieldAnnotationKey(fieldKey)
            assertEquals(field.id.fieldName, parsedFieldName)
            assertEquals(field.declaringClass.canonicalName, parsedClassName)
        } catch (e: IllegalArgumentException) {
            System.err.println(e.message)
        }
    }

    inner class TestVisitor(val reader: ClassReader) : ClassVisitor(Opcodes.ASM4) {
        override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            doMethodTest(reader.className, access, name, desc, signature)
            return null
        }
        public override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
            doFieldTest(reader.className, access, name, desc, signature, value)
            return null
        }
    }

    @Test
    fun javaByteCode() {
        recurseIntoJars(File("lib")) {
            file, owner, reader ->
            if (file.name != "kotlin-runtime.jar" && file.name != "kotlin-reflect.jar") {
                reader.accept(TestVisitor(reader), 0)
            }
        }
    }

    @Ignore("KT-4510")
    @Test
    fun kotlinByteCode() {
        recurseIntoJars(File("lib/kotlin-runtime.jar")) {
            file, owner, reader ->
            reader.accept(TestVisitor(reader), 0)
        }
    }
}

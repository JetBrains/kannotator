package annotations.io

import java.io.File
import junit.framework.Assert.*
import junit.framework.TestCase
import org.jetbrains.kannotator.annotations.io.getMethodNameAccountingForConstructor
import org.jetbrains.kannotator.annotations.io.parseFieldAnnotationKey
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.declarations.*
import util.recurseIntoJars
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.FieldVisitor
import org.jetbrains.kannotator.annotations.io.parseMethodAnnotationKey

class AnnotationKeyParserTest : TestCase() {
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
            System.err.println(e.getMessage())
        }
    }

    fun doTest(field: Field) {
        val fieldKey = getFieldAnnotatedType(field).position.toAnnotationKey()

        try {
            val (parsedClassName, parsedFieldName) = parseFieldAnnotationKey(fieldKey)

            assertEquals(field.id.fieldName, parsedFieldName)
            assertEquals(field.declaringClass.canonicalName, parsedClassName)
        } catch (e: IllegalArgumentException) {
            System.err.println(e.getMessage())
        }
    }

    fun test() {
        val dirs = arrayList(
                java.io.File("/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Classes"),
                java.io.File("/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib"),
                File("lib")
        )
        for (dir in dirs) {
            recurseIntoJars(dir) {
                file, owner, reader ->
                if (file.getName() != "kotlin-runtime.jar") {
                    reader.accept(object : ClassVisitor(Opcodes.ASM4) {
                        override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                            doMethodTest(reader.getClassName(), access, name, desc, signature)
                            return null
                        }
                        public override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                            doFieldTest(reader.getClassName(), access, name, desc, signature, value)
                            return null
                        }
                    }, 0)
                }
            }
        }
    }
}
package annotations.io

import junit.framework.TestCase
import junit.framework.Assert.*
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.annotations.io.parseMethodAnnotationKey
import org.jetbrains.kannotator.declarations.internalNameToCanonical
import util.recurseIntoJars
import java.io.File
import org.jetbrains.kannotator.asm.util.forEachMethod
import org.jetbrains.kannotator.annotations.io.getMethodNameAccountingForConstructor
import org.jetbrains.kannotator.annotations.io.MethodAnnotationKeyData

class AnnotationKeyParserTest : TestCase() {
    fun doTest(className: String, name: String, desc: String, signature: String? = null) {
        val method = Method(ClassName.fromInternalName(className), 0, name, desc, signature)
        doTest(method)
    }

    fun doTest(method: Method) {
        val pos = PositionsWithinMember(method).forReturnType()

        val key = pos.position.toAnnotationKey()
        try {
            val (parsedClassName, parsedReturnType, parsedMethodName) = parseMethodAnnotationKey(key)

            assertEquals(method.declaringClass.canonicalName, parsedClassName)
            assertEquals(method.getMethodNameAccountingForConstructor(), parsedMethodName)
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
                    reader.forEachMethod {
                        owner, access, name, desc, signature ->
                        doTest(owner, name, desc, signature)
                    }
                }
            }
        }
    }
}
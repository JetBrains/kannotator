package index

import java.io.File
import java.util.ArrayList
import org.junit.Test
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.util.processJar
import kotlin.test.fail
import kotlin.util.measureTimeMillis
import org.jetbrains.kannotator.asm.util.forEachMethod
import util.findJarsInLibFolder
import util.traceExecutionTime

/** Compares times of traversing methods vs indexing methods.
 *  Checks that each traversed method is found in index */
class MethodIndexTest {

    fun doTest(jar: File) {
        val methods = ArrayList<Method>()

        traceExecutionTime("Loading methods: ") {
            processJar(jar) {
                file, owner, reader ->
                val className = ClassName.fromInternalName(reader.getClassName())
                reader.forEachMethod {
                    owner, access, name, desc, signature ->
                    val method = Method(className, access, name, desc, signature)
                    methods.add(method)
                }
            }
        }

        println("${methods.size()} methods found")

        val index = traceExecutionTime("Building index: ") {
            val source = FileBasedClassSource(listOf(jar))
            DeclarationIndexImpl(source)
        }

        for (method in methods) {
            val found = index.findMethod(method.declaringClass, method.id.methodName, method.id.methodDesc)
            if (found == null) {
                fail("Method not found: ${method.toFullString()}")
            }
            else if (method != found) {
                fail("Wrong method found. Expected\n" + method.toFullString() + "\nbut was\n" + found.toFullString())
            }
        }
    }

    @Test fun libFolder() {
        for (jar in findJarsInLibFolder()) {
            println(jar)
            doTest(jar)
            println()
        }
    }
}

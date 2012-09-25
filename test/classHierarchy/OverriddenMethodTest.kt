package classHierarchy

import java.io.File
import junit.framework.TestCase
import kotlinlib.buildString
import kotlinlib.println
import kotlinlib.sortByToString
import org.jetbrains.kannotator.classHierarchy.getOverriddenMethods

class OverriddenMethodTest : TestCase() {
    val BASE_DIR = "testData/classHierarchy/overriddenMethods/"

//    fun testAll() {
//        doTest("", "all.txt")
//    }

    fun testJava() {
        doTest("java/", "java.txt")
    }

    fun testJung() {
        doTest("edu/uci/ics/jung/", "jung.txt")
    }


    fun doTest(prefix: String, filename: String) {
        val classes = getClassesHierarchy(prefix)

        val result = buildString {
            sb ->
            for (node in classes) {
                val methods = node.methods.sortBy { it -> it.asmMethod.toString()!! }
                for (method in methods) {
                    sb.println(method)
                    val overridden = node.getOverriddenMethods(method).sortByToString()
                    overridden.forEach {
                        // Do not output method itself to results
                        if (it != method) {
                            sb.println("  $it")
                        }
                    }
                }
            }
        }

        assertEqualsOrCreate(File(BASE_DIR + filename), result)
    }
}

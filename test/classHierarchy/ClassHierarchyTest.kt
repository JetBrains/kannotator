package classHierarchy

import java.io.File
import junit.framework.TestCase
import kotlinlib.buildString
import kotlinlib.println
import kotlinlib.sortByToString

class ClassHierarchyTest : TestCase() {
    val BASE_DIR = "testData/classHierarchy/"

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
        val builder = ClassHierarchyGraphBuilder()
        for (clazz in getAllClassesWithPrefix(prefix)) {
            builder.addClass(clazz)
        }

        val graph = builder.buildGraph()

        val classes = graph.classes.filter {
            it.name.internal.startsWith(prefix)
        }.sortBy {
            it.name.toString()
        }

        val result = buildString {
            sb ->
            for (node in classes) {
                sb.println(node)
                sb.println("  SubClasses")
                val subClasses = node.subClasses.map { it.derived }.sortByToString()
                subClasses.forEach { sb.println("    $it") }
                sb.println("  SuperClasses")
                val superClasses = node.superClasses.map { it.base }.sortByToString()
                superClasses.forEach { sb.println("    $it") }
                sb.println("  Methods")
                val methods = node.methods.map { it.asmMethod }.sortByToString()
                methods.forEach { sb.println("    $it") }
            }
        }

        val expectedFile = File(BASE_DIR + filename)

        if (!expectedFile.exists()) {
            expectedFile.getParentFile()!!.mkdirs()
            expectedFile.writeText(result)
            fail("Expected data file file does not exist: ${expectedFile}. It is created from actual data")
        }

        val expected = expectedFile.readText()

        assertEquals(expected, result)
    }
}

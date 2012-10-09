package classHierarchy

import java.io.File
import junit.framework.TestCase
import kotlinlib.buildString
import kotlinlib.println
import kotlinlib.sortByToString
import org.jetbrains.kannotator.classHierarchy.*
import util.assertEqualsOrCreate

class ClassHierarchyTest : TestCase() {
    val BASE_DIR = "testData/classHierarchy/"

//    fun testAll() {
//        doTest("", "all.txt")
//    }

    fun testGsCollections() {
        doTest("com/gs/collections/", "gs-collections.txt")
    }

    fun testJung() {
        doTest("edu/uci/ics/jung/", "jung.txt")
    }

    fun doTest(prefix: String, filename: String) {
        val classes = getClassesHierarchy(prefix)

        val result = buildString {
            sb ->
            for (node in classes) {
                sb.println(node)
                sb.println("  SubClasses")
                val subClasses = node.children.map { it.child }.sortByToString()
                subClasses.forEach { sb.println("    $it") }
                sb.println("  SuperClasses")
                val superClasses = node.parents.map { it.parent }.sortByToString()
                superClasses.forEach { sb.println("    $it") }
                sb.println("  Methods")
                val methods = node.methods.map { it.id }.sortByToString()
                methods.forEach { sb.println("    $it") }
            }
        }

        assertEqualsOrCreate(File(BASE_DIR + filename), result)
    }
}

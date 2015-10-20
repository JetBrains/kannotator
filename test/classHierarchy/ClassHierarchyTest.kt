package classHierarchy

import java.io.File
import org.junit.Test
import kotlinlib.sortByToString
import org.jetbrains.kannotator.classHierarchy.*
import util.assertEqualsOrCreate
import util.getClassesHierarchy

/**
 * Builds class hierarchy for some libs in `lib` folder and compares
 * to reference hierarchy (in text files).
 */
class ClassHierarchyTest {
    val BASE_DIR = "testData/classHierarchy"

    @Test
    fun gsCollections() {
        doTest("com/gs/collections/", "gs-collections.txt")
    }

    @Test
    fun jung() {
        doTest("edu/uci/ics/jung/", "jung.txt")
    }

    fun doTest(prefix: String, filename: String) {
        val classes = getClassesHierarchy(prefix)

        val result = StringBuilder().apply {
            for (node in classes) {
                appendln(node)
                appendln("  SubClasses")
                val subClasses = node.children.map { it.child }.sortByToString()
                subClasses.forEach { appendln("    $it") }
                appendln("  SuperClasses")
                val superClasses = node.parents.map { it.parent }.sortByToString()
                superClasses.forEach { appendln("    $it") }
                appendln("  Methods")
                val methods = node.methods.map { it.id }.sortByToString()
                methods.forEach { appendln("    $it") }
            }
        }.toString()

        assertEqualsOrCreate(File("$BASE_DIR/$filename"), result)
    }
}

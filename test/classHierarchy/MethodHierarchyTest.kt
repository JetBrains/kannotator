package classHierarchy

import org.junit.Test
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import org.jetbrains.kannotator.classHierarchy.buildMethodHierarchy
import util.getAllClassesWithPrefix
import java.io.File
import kotlinlib.*
import org.jetbrains.kannotator.classHierarchy.*
import util.assertEqualsOrCreate
import org.jetbrains.kannotator.declarations.Method

/**
 * Builds method hierarchy for some libs in `lib` folder and compares
 * to reference hierarchy (in text files).
 */
class MethodHierarchyTest {

    val BASE_DIR = "testData/classHierarchy/methodHierarchy"

    fun doTest(classInternalNamePrefix: String, expectedFileName: String) {
        val classHierarchy = buildClassHierarchyGraph(getAllClassesWithPrefix(classInternalNamePrefix))
        val methodHierarchy = buildMethodHierarchy(classHierarchy)
        val expectedFile = File("$BASE_DIR/$expectedFileName")

        fun Collection<HierarchyNode<Method>>.sortForTest(): List<HierarchyNode<Method>> {
            return this.toSortedList {
                a, b -> a.data.toString().compareTo(b.data.toString())
            }
        }

        val actual = StringBuilder {
            for (methodNode in methodHierarchy.hierarchyNodes.sortForTest()) {
                appendln(methodNode.data)

                fun appendNodes(title: String, nodes: Collection<HierarchyNode<Method>>) {
                    if (nodes.isEmpty()) return

                    appendln("  $title")
                    for (node in nodes.sortForTest()) {
                        appendln("    ${node.data}")
                    }
                }

                appendNodes("It overrides", methodNode.parentNodes)
                appendNodes("It is overridden by", methodNode.childNodes)
                appendln()
            }
        }.toString()

        assertEqualsOrCreate(expectedFile, actual)
    }

    @Test
    fun jung() {
        doTest("edu/uci/ics/jung/", "jung.txt")
    }

    @Test
    fun gsCollections() {
        doTest("com/gs/collections/", "gs-collections.txt")
    }
}
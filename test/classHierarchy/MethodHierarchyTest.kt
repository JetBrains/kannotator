package classHierarchy

import junit.framework.TestCase
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import util.getAllClassesWithPrefix
import org.jetbrains.kannotator.classHierarchy.buildMethodHierarchy
import java.io.File
import kotlinlib.*
import org.jetbrains.kannotator.classHierarchy.*
import util.assertEqualsOrCreate
import org.jetbrains.kannotator.declarations.Method

class MethodHierarchyTest : TestCase() {

    fun doTest(classInternalNamePrefix: String, expectedFileName: String) {
        val classHierarchy = buildClassHierarchyGraph(getAllClassesWithPrefix(classInternalNamePrefix))
        val methodHierarchy = buildMethodHierarchy(classHierarchy)
        val expectedFile = File("testData/classHierarchy/methodHierarchy/$expectedFileName")

        fun Collection<HierarchyNode<Method>>.sortForTest(): List<HierarchyNode<Method>> {
            return this.toSortedList {
                a, b -> a.data.toString().compareTo(b.data.toString())
            }
        }

        val actual = buildString {
            sb ->
            for (methodNode in methodHierarchy.nodes.sortForTest()) {
                sb.append(methodNode.data).append("\n")

                fun appendNodes(title: String, nodes: Collection<HierarchyNode<Method>>) {
                    if (nodes.isEmpty()) return

                    sb.append("  $title\n")
                    for (node in nodes.sortForTest()) {
                        sb.append("    ${node.data}\n")
                    }
                }

                appendNodes("It overrides", methodNode.parentNodes())
                appendNodes("It is overridden by", methodNode.childNodes())
                sb.append("\n")
            }
        }

        assertEqualsOrCreate(expectedFile, actual)
    }

    fun testJung() {
        doTest("edu/uci/ics/jung/", "jung.txt")
    }

    fun testGsCollections() {
        doTest("com/gs/collections/", "gs-collections.txt")
    }
}
package classHierarchy

import java.io.File
import junit.framework.TestCase
import kotlinlib.buildString
import kotlinlib.println
import kotlinlib.sortByToString
import org.jetbrains.kannotator.classHierarchy.HierarchyNode
import org.jetbrains.kannotator.classHierarchy.getOverridingMethods
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import util.ClassesFromClassPath
import org.jetbrains.kannotator.classHierarchy.*
import util.assertEqualsOrCreate

class OverriddenMethodTest : TestCase() {
    val BASE_DIR = "testData/classHierarchy/overriddenMethods/"

//    fun testAll() {
//        doTest("", "all.txt")
//    }

    fun testGsCollections() {
        doTest(getClassesHierarchy("com/gs/collections/"), "gs-collections.txt")
    }

    fun testJung() {
        doTest(getClassesHierarchy("edu/uci/ics/jung/"), "jung.txt")
    }

    fun testOverridesVisibility() {
        val graph = buildClassHierarchyGraph(ClassesFromClassPath(
                arrayList("Base", "Derived", "subpackage/DerivedInSubpackage").map {
                    ClassName.fromInternalName("classHierarchy/overriddenMethods/overridesVisibility/$it")
                }
        ))

        doTest(graph.nodes.sortByToString(), "overridesVisibility/result.txt")
    }


    fun doTest(classes: Collection<HierarchyNode<ClassData>>, filename: String) {
        val result = buildString {
            sb ->
            for (node in classes) {
                val methods = node.methods.sortBy { it.id.toString() }
                for (method in methods) {
                    sb.println(method)
                    val overridden = node.getOverridingMethods(method).sortByToString()
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

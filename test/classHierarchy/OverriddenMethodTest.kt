package classHierarchy

import java.io.File
import junit.framework.TestCase
import kotlinlib.buildString
import kotlinlib.println
import kotlinlib.sortByToString
import org.jetbrains.kannotator.classHierarchy.ClassNode
import org.jetbrains.kannotator.classHierarchy.getOverriddenMethods
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import util.ClassesFromClassPath

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

        doTest(graph.classes.sortByToString(), "overridesVisibility/result.txt")
    }


    fun doTest(classes: Collection<ClassNode>, filename: String) {
        val result = buildString {
            sb ->
            for (node in classes) {
                val methods = node.methods.sortBy { it.id.toString() }
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

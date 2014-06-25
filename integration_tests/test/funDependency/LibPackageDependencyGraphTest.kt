package funDependency

import java.io.File
import java.util.Comparator
import kotlinlib.*
import org.jetbrains.kannotator.funDependecy.*
import org.junit.Test
import util.assertEqualsOrCreate
import org.jetbrains.kannotator.declarations.Package
import org.jetbrains.kannotator.graphs.Node as GraphNode
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.classHierarchy.buildMethodHierarchy
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import org.jetbrains.kannotator.graphs.dependencyGraphs.PackageDependencyGraphBuilder

/** Checks construction of package dependencies for jdk against expected. */
class LibPackageDependencyGraphTest {
    fun doTest(expectedResultPath: String, jarFile: File) {
        val classSource = FileBasedClassSource(arrayListOf(jarFile))

        val funGraph = buildFunctionDependencyGraph(DeclarationIndexImpl(classSource), classSource)
        val packageGraphBuilder = PackageDependencyGraphBuilder(funGraph)

        val classHierarchy = buildClassHierarchyGraph(classSource)
        val methodHierarchy = buildMethodHierarchy(classHierarchy)

        val graph = packageGraphBuilder.build()
        packageGraphBuilder.extendWithHierarchy(methodHierarchy)

        val functionNodeComparator = object : Comparator<GraphNode<Package, Nothing?>> {
            public override fun compare(o1: GraphNode<Package, Nothing?>, o2: GraphNode<Package, Nothing?>): Int {
                return o1.data.toString().compareTo(o2.data.toString())
            }

            public override fun equals(other: Any?): Boolean {
                throw IllegalStateException()
            }
        }

        val actual = StringBuilder {
            appendln("== All Nodes == ")
            for (node in graph.nodes.sort(functionNodeComparator)) {
                printFunctionNode(this, node)
            }

            appendln()
            appendln("== No Outgoing Nodes == ")

            for (node in graph.sinkNodes.sort(functionNodeComparator)) {
                printFunctionNode(this, node)
            }
        }.toString().trim()

        val expectedFile = File(PATH + expectedResultPath)
        assertEqualsOrCreate(expectedFile, actual)
    }

    fun printFunctionNode(sb: StringBuilder, node: GraphNode<Package, *>) {
        sb.appendln(node.data)
        if (node.outgoingEdges.size() > 0) sb.appendln("    outgoing edges:")
        for (edge in node.outgoingEdges.sortByToString()) {
            sb.appendln("        $edge")
        }

        if (node.incomingEdges.size() > 0) sb.appendln("    incoming edges:")
        for (edge in node.incomingEdges.sortByToString()) {
            sb.appendln("        $edge")
        }
    }

    Test fun testJDK_1_7_12_win() {
        doTest("jdkPackages/jre-7u12-windows-rt.txt", File("lib/jdk/jre-7u12-windows-rt.jar"))
    }
}


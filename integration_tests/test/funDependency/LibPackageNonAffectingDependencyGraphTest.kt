package funDependency

import java.io.File
import java.util.Comparator
import kotlinlib.*
import org.jetbrains.kannotator.funDependecy.*
import org.junit.Test
import util.ClassPathDeclarationIndex
import util.ClassesFromClassPath
import util.assertEqualsOrCreate
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.Package
import org.jetbrains.kannotator.graphs.Node as GraphNode
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.graphs.dependencyGraphs.buildPackageDependencyGraph
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.classHierarchy.buildMethodHierarchy
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import org.jetbrains.kannotator.graphs.dependencyGraphs.PackageDependencyGraphBuilder
import org.jetbrains.kannotator.graphs.removeGraphNodes

/** Tests how filtering of "non-interesting" nodes works for jre jar */
class LibPackageNonAffectingDependencyGraphTest {
    fun doTest(expectedResultPath: String, jarFile: File) {
        val classSource = FileBasedClassSource(arrayListOf(jarFile))

        val funGraph = buildFunctionDependencyGraph(DeclarationIndexImpl(classSource), classSource)
        val packageGraphBuilder = PackageDependencyGraphBuilder(funGraph)

        val graph = packageGraphBuilder.build()
        val packageCount = graph.nodes.size

        val interestingNodes = graph.getTransitivelyInterestingNodes {
            val name = it.data.name
            name.startsWith("java") || name.startsWith("javax") || name.startsWith("org")
        }
        val nonInterestingNodes = graph.nodes.subtract(interestingNodes)
        packageGraphBuilder.removeGraphNodes {it !in interestingNodes}

        val classHierarchy = buildClassHierarchyGraph(classSource)
        val methodHierarchy = buildMethodHierarchy(classHierarchy)
        packageGraphBuilder.extendWithHierarchy(methodHierarchy)

        val functionNodeComparator = object : Comparator<GraphNode<Package, Nothing?>> {
            public override fun compare(o1: GraphNode<Package, Nothing?>, o2: GraphNode<Package, Nothing?>): Int {
                return o1.data.toString().compareTo(o2.data.toString())
            }

            public override fun equals(obj: Any?): Boolean {
                throw IllegalStateException()
            }
        }

        val actual = buildString { sb ->
            sb.println()
            sb.println("== Non-Affecting Nodes == ")
            sb.println("Found ${nonInterestingNodes.size} out of total $packageCount")
            for (node in nonInterestingNodes.sort(functionNodeComparator)) {
                printFunctionNode(sb, node)
            }
        }.trim()

        val expectedFile = File(PATH + expectedResultPath)
        assertEqualsOrCreate(expectedFile, actual)
    }

    fun printFunctionNode(sb: StringBuilder, node: GraphNode<Package, *>) {
        sb.println(node.data)
        if (node.outgoingEdges.size() > 0) sb.println("    outgoing edges:")
        for (edge in node.outgoingEdges.sortByToString()) {
            sb.println("        $edge")
        }

        if (node.incomingEdges.size() > 0) sb.println("    incoming edges:")
        for (edge in node.incomingEdges.sortByToString()) {
            sb.println("        $edge")
        }
    }

    Test fun jre_1_7_12_win() {
        doTest("jdkNonAffectingPackages/jre-7u12-windows-rt.txt", File("lib/jdk/jre-7u12-windows-rt.jar"))
    }
}


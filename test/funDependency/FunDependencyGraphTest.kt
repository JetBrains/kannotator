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
import org.jetbrains.kannotator.graphs.Node as GraphNode
import org.jetbrains.kannotator.index.buildFieldsDependencyInfos
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.declarations.ClassMember

private val PATH = "testData/funDependency/"

/** builds function dependencies graph for classes in `testData/funDependency/`
 *  and checks their textual representation against expected ones
 */
class FunDependencyGraphTest {
    Test fun callDependencyForNonAnnotativeMethod() {
        doTest("callDependencyForNonAnnotativeMethod/callDependencyForNonAnnotativeMethod.txt",
                "funDependency.callDependencyForNonAnnotativeMethod.CallDependencyForNonAnnotativeMethod")
    }

    Test fun funInDifferentClassesTest() {
        doTest("funInDifferentClasses/funInDifferentClasses.txt", "funDependency.funInDifferentClasses.First", "funDependency.funInDifferentClasses.Second")
    }

    Test fun multiplyInvokeOfMethod() {
        doTest("multiplyInvokeOfMethod/multiplyInvokeOfMethod.txt", "funDependency.multiplyInvokeOfMethod.First", "funDependency.multiplyInvokeOfMethod.Second")
    }

    Test fun noAnnotatedMethods() {
        doTest("noAnnotatedMethods/noAnnotatedMethods.txt", "funDependency.noAnnotatedMethods.First")
    }

    Test fun recursiveFunTest() {
        doTest("recursiveFun/recursiveFun.txt", "funDependency.recursiveFun.First", "funDependency.recursiveFun.Second")
    }

    Test fun simpleTest() {
        doTest("simple/simple.txt", "funDependency.simple.Simple")
    }

    Test fun dependOnConstructorBecauseOfFields() {
        doTest("dependOnConstructorBecauseOfFields/dependOnConstructorBecauseOfFields.txt", "funDependency.dependOnConstructorBecauseOfFields.DependOnConstructorBecauseOfFields")
    }

    Test fun missingDependencies() {
        val classSource = ClassesFromClassPath("funDependency.simple.Simple")
        val di = DeclarationIndexImpl(FileBasedClassSource(listOf(File("out/test/kannotator/funDependency/simple/Simple.class"))))
        val missing = arrayListOf<ClassMember>()
        FunDependencyGraphBuilder(
            di,
            classSource,
            buildFieldsDependencyInfos(di, classSource),
            {
                m ->
                missing.add(m)
                null
            }
        ).build()
        val missingStr = missing.join("\n")
        assertEqualsOrCreate(File(PATH + "simple/missing.txt"), missingStr)
    }

    fun doTest(expectedResultPath: String, vararg canonicalNames: String) {
        val classSource = ClassesFromClassPath(*canonicalNames)
        val graph = buildFunctionDependencyGraph(ClassPathDeclarationIndex, classSource)

        val functionNodeComparator = object : Comparator<GraphNode<Method, String>> {
            public override fun compare(o1: GraphNode<Method, String>, o2: GraphNode<Method, String>): Int {
                return o1.data.toString().compareTo(o2.data.toString())
            }

            public override fun equals(obj: Any?): Boolean {
                throw IllegalStateException()
            }
        }

        val actual = buildString { sb ->
            sb.println("== All Nodes == ")
            for (node in graph.nodes.sort(functionNodeComparator)) {
                printFunctionNode(sb, node)
            }

            sb.println()
            sb.println("== No Outgoing Nodes == ")

            for (node in graph.sinkNodes.sort(functionNodeComparator)) {
                printFunctionNode(sb, node)
            }
        }.trim()

        val expectedFile = File(PATH + expectedResultPath)
        assertEqualsOrCreate(expectedFile, actual)
    }

    fun printFunctionNode(sb: StringBuilder, node: GraphNode<Method, String>) {
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
}


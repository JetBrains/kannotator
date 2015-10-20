package funDependency

import org.junit.Assert.*
import org.junit.Test
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph
import util.ClassesFromClassPath
import util.ClassPathDeclarationIndex
import org.jetbrains.kannotator.funDependecy.getTopologicallySortedStronglyConnectedComponents
import java.io.File
import kotlinlib.*
import util.assertEqualsOrCreate

/** Tests detecting of SCC for classes in testData/funDependency/ */
class TopologicalSortTest {
    fun doTest(expectedFileName: String, vararg canonicalNames: String) {
        val methodGraph = buildFunctionDependencyGraph(ClassPathDeclarationIndex, ClassesFromClassPath(*canonicalNames))
        val components = methodGraph.getTopologicallySortedStronglyConnectedComponents()
        val actual = components.map { it.map { n -> n.data.toString()}.sorted().joinToString("\n", "", "\n===========\n") }.joinToString("\n")
        val expectedFile = File("testData/funDependency/" + expectedFileName)

        assertEqualsOrCreate(expectedFile, actual)
    }

    @Test fun callDependencyForNonAnnotativeMethod() {
        doTest("callDependencyForNonAnnotativeMethod/callDependencyForNonAnnotativeMethod.sorted.txt",
                "funDependency.callDependencyForNonAnnotativeMethod.CallDependencyForNonAnnotativeMethod")
    }

    @Test fun dependOnConstructorBecauseOfFields() {
        doTest("dependOnConstructorBecauseOfFields/DependOnConstructorBecauseOfFields.sorted.txt",
                "funDependency.dependOnConstructorBecauseOfFields.DependOnConstructorBecauseOfFields")
    }

    @Test fun funInDifferentClassesTest() {
        doTest("funInDifferentClasses/funInDifferentClasses.sorted.txt", "funDependency.funInDifferentClasses.First", "funDependency.funInDifferentClasses.Second")
    }

    @Test fun multiplyInvokeOfMethod() {
        doTest("multiplyInvokeOfMethod/multiplyInvokeOfMethod.sorted.txt", "funDependency.multiplyInvokeOfMethod.First", "funDependency.multiplyInvokeOfMethod.Second")
    }

    @Test fun noAnnotatedMethods() {
        doTest("noAnnotatedMethods/noAnnotatedMethods.sorted.txt", "funDependency.noAnnotatedMethods.First")
    }

    @Test fun recursiveFunTest() {
        doTest("recursiveFun/recursiveFun.sorted.txt", "funDependency.recursiveFun.First", "funDependency.recursiveFun.Second")
    }

    @Test fun simpleTest() {
        doTest("simple/simple.sorted.txt", "funDependency.simple.Simple")
    }

    @Test fun multipleComponents() {
        doTest("multipleComponents/multipleComponents.sorted.txt", "funDependency.multipleComponents.First")
    }
}
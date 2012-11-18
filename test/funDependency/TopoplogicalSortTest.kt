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

class TopologicalSortTest {
    fun doTest(expectedFileName: String, vararg canonicalNames: String) {
        val methodGraph = buildFunctionDependencyGraph(ClassPathDeclarationIndex, ClassesFromClassPath(*canonicalNames))
        val components = methodGraph.getTopologicallySortedStronglyConnectedComponents()
        val actual = components.map { it.map {n -> n.method.toString()}.toSortedList().join("\n", "", "\n===========\n") }.join("\n")
        val expectedFile = File("testData/funDependency/" + expectedFileName)

        assertEqualsOrCreate(expectedFile, actual)
    }

    Test fun callDependencyForNonAnnotativeMethod() {
        doTest("callDependencyForNonAnnotativeMethod/callDependencyForNonAnnotativeMethod.sorted.txt",
                "fundependency.callDependencyForNonAnnotativeMethod.CallDependencyForNonAnnotativeMethod")
    }

    Test fun dependOnConstructorBecauseOfFields() {
        doTest("dependOnConstructorBecauseOfFields/DependOnConstructorBecauseOfFields.sorted.txt",
                "fundependency.dependOnConstructorBecauseOfFields.DependOnConstructorBecauseOfFields")
    }

    Test fun funInDifferentClassesTest() {
        doTest("funInDifferentClasses/funInDifferentClasses.sorted.txt", "fundependency.funInDifferentClasses.First", "fundependency.funInDifferentClasses.Second")
    }

    Test fun multiplyInvokeOfMethod() {
        doTest("multiplyInvokeOfMethod/multiplyInvokeOfMethod.sorted.txt", "fundependency.multiplyInvokeOfMethod.First", "fundependency.multiplyInvokeOfMethod.Second")
    }

    Test fun noAnnotatedMethods() {
        doTest("noAnnotatedMethods/noAnnotatedMethods.sorted.txt", "fundependency.noAnnotatedMethods.First")
    }

    Test fun recursiveFunTest() {
        doTest("recursiveFun/recursiveFun.sorted.txt", "fundependency.recursiveFun.First", "fundependency.recursiveFun.Second")
    }

    Test fun simpleTest() {
        doTest("simple/simple.sorted.txt", "fundependency.simple.Simple")
    }

    Test fun multipleComponents() {
        doTest("multipleComponents/multipleComponents.sorted.txt", "funDependency.multipleComponents.First")
    }
}
package funDependency

/**
 * User: Natalia.Ukhorskaya
 */

import org.junit.Test as test
import org.objectweb.asm.ClassReader
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph
import org.jetbrains.kannotator.funDependecy.FunctionNode
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.io.File
import kotlinlib.buildString
import kotlin.template.append
import org.junit.Assert
import kotlin.test.fail


private val PATH = "testData/funDependency/"

class FunDependencyGraphTest {

    test fun funInDifferentClassesTest() {
        doTest("funInDifferentClasses/funInDifferentClasses.txt", "fundependency.funInDifferentClasses.First", "fundependency.funInDifferentClasses.Second")
    }

    test fun noAnnotatedMethods() {
        doTest("noAnnotatedMethods/noAnnotatedMethods.txt", "fundependency.noAnnotatedMethods.First")
    }

    test fun recursiveFunTest() {
        doTest("recursiveFun/recursiveFun.txt", "fundependency.recursiveFun.First", "fundependency.recursiveFun.Second")
    }

    test fun simpleTest() {
        doTest("simple/simple.txt", "fundependency.simple.Simple")
    }

    fun doTest(expectedResultPath: String, vararg listOfFiles: String) {
        val graph = buildFunctionDependencyGraph(listOfFiles)

        val mutableList: MutableList<FunctionNode> = ArrayList<FunctionNode>()
        mutableList.addAll(graph.functions)
//     todo   val functions = graph.functions
        Collections.sort(mutableList, object : Comparator<FunctionNode> {
            public override fun compare(o1: FunctionNode?, o2: FunctionNode?): Int {
                return o1?.toString()?.compareTo(o2?.toString() ?: "") ?: -1
            }

            public override fun equals(obj: Any?): Boolean {
                return this.toString().equals(obj.toString())
            }
        })

        val actual = buildString {
            sb ->
            for (f in mutableList) {
                sb.append(f)
                sb.append(System.getProperty("line.separator"))
            }

        }.trim()

        val expectedFile = File(PATH + expectedResultPath)
        if (!expectedFile.exists()) {
            expectedFile.writeText(actual)
            fail("Expected data file file does not exist: ${expectedFile}. It is created from actual data")
        }
        val expected = expectedFile.readText().trim()
        println(actual)

        Assert.assertEquals(expected, actual)
    }
}


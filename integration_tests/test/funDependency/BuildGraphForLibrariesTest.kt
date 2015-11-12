package funDependency

import java.io.File
import org.jetbrains.kannotator.funDependecy.*
import org.junit.Test
import org.jetbrains.kannotator.index.FileBasedClassSource
import util.ClassPathDeclarationIndex

/** Smoke testing that function dependency graph is built for each jar
 * without any errors
 */
class BuildGraphForLibrariesTest() {
    @Test fun allLibsTest() {
        File("lib").walkTopDown().forEach {
            file ->
            if (file.isFile && file.name.endsWith(".jar")) {
                doTest(file)
            }
        }
    }

    /*test fun rtJarTest() {
       doTest(File("c:/Program Files/Java/jdk1.6.0_30/jre/lib/rt.jar"))
    }*/

    fun doTest(file: File) {
        println("Processing: $file")

        val classSource = FileBasedClassSource(listOf(file))
        val graph = buildFunctionDependencyGraph(ClassPathDeclarationIndex, classSource)
        val finder = SCCFinder(graph, { graph.nodes }, { it.outgoingEdges.map { edge -> edge.to } })
        val allComponents = finder.getAllComponents()
        println("Graph size: " + graph.nodes.size + " " + graph.sinkNodes.size)
        println("Number of component (size > 1): ${allComponents.filter { it.size > 1 }.size}")
    }

}

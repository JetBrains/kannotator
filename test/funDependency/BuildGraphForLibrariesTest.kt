package funDependency

import java.io.File
import java.util.ArrayList
import java.util.jar.JarFile
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph
import org.junit.Test as test
import org.objectweb.asm.ClassReader
import org.jetbrains.kannotator.util.processJar

class BuildGraphForLibrariesTest() {
    test fun allLibsTest() {
        File("lib").recurse {
            file ->
            if (file.isFile() && file.getName().endsWith(".jar")) {
                doTest(file)
            }
        }
    }

    /*test fun rtJarTest() {
       doTest(File("c:/Program Files/Java/jdk1.6.0_30/jre/lib/rt.jar"))
    }*/

    fun doTest(file: File) {
        println("Processing: $file")
        val classReaders = ArrayList<ClassReader>()
        processJar(file, {
            jarFile, classType, classReader ->
            classReaders.add(classReader)
        })

        val graph = buildFunctionDependencyGraph(classReaders)
        val finder = SCCFinder(graph, { graph.functions }, { it.outgoingEdges.map { edge -> edge.to } })
        val allComponents = finder.getAllComponents()
        println("Graph size: " + graph.functions.size() + " " + graph.noOutgoingNodes.size())
        println("Number of component (size > 1): ${allComponents.filter { it.size() > 1 }.size()}")
    }

}

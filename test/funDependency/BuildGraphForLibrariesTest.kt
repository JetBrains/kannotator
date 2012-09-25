package funDependency

import java.io.File
import java.util.ArrayList
import java.util.jar.JarFile
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph
import org.junit.Test as test
import org.objectweb.asm.ClassReader
import util.processJar

class BuildGraphForLibrariesTest() {
    test fun test() {
        File("lib").recurse {
            file ->
            if (file.isFile() && file.getName().endsWith(".jar")) {
                println("Processing: $file")

                val classReaders = ArrayList<ClassReader>()
                processJar(file,  {
                    jarFile, classType, classReader ->
                    classReaders.add(classReader)
                })

                val graph = buildFunctionDependencyGraph(classReaders)
                println("Graph size: " + graph.functions.size() + " " + graph.noOutgoingNodes.size())
            }
        }
    }
}

package interpreter

import java.io.File
import java.util.jar.JarFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import kotlinlib.removeSuffix
import org.jetbrains.kannotator.controlFlowBuilder.buildGraphsForAllMethods
import junit.framework.TestCase

class WholeLibInterpreterTest : TestCase() {

    fun test() {
        val libDir = File("lib")
        libDir.recurse {
            file ->
            if (file.isFile() && file.getName().endsWith(".jar")) {
                println("Processing: $file")

                val jar = JarFile(file)

                for (entry in jar.entries()) {
                    val name = entry!!.getName()!!
                    if (!name.endsWith(".class")) continue

                    println("  " + entry)

                    val internalName = name.removeSuffix(".class")
                    val classType = Type.getType("L$internalName;")

                    val inputStream = jar.getInputStream(entry)
                    val classReader = ClassReader(inputStream)

                    doTest(File("testData/wholeLib"), classType, classReader, false)
                }
            }
        }
    }
}
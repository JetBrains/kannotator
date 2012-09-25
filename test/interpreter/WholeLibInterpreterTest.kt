package interpreter

import java.io.File
import junit.framework.TestCase
import util.recurseIntoJars
import java.util.HashSet

class WholeLibInterpreterTest : TestCase() {

    fun testKAnnotatorLibs() {
        doTestJars(File("lib"), File("lib.testData"))
    }

    fun testJDK() {
        val libDir = File("/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents")
        val dataDir = File("jdk.testData")
        doTestJars(libDir, dataDir)
    }
}

fun doTestJars(libDir: File, dataDir: File) {
    val jars = HashSet<File>()
    recurseIntoJars(libDir) {
        jarFile, classType, classReader ->

        if (jars.add(jarFile)) {
            println(jarFile)
        }

        println("  " + classType.getInternalName())

        val dir = File(dataDir, jarFile.getName())
        dir.mkdir()
        doTest(dir, classType, classReader, false)
    }
}
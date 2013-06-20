package annotations.io

import java.io.File
import java.io.FileWriter
import junit.framework.Assert
import org.jetbrains.kannotator.annotations.io.writeAnnotations
import org.junit.Test
import kotlinlib.toUnixSeparators

public class WriteAnnotationToXmlTest : AbstractWriteAnnotationTest() {
    Test fun testAll() {
        doTest(File("lib/asm-debug-all-4.0.jar"), File("testData/annotations/write/annotations.xml"))
    }

    /*Test fun testJdk() {
        doTest(array(File("c:/Program Files/Java/jdk1.6.0_30/jre/lib/rt.jar")),
                array(File("testData/annotations/jdk-annotations")))
    }*/

    fun doTest(jarFile: File, annotationFile: File) {
        val typePositionAndAnnotationData = readAnnotationsForAllPositionsInJarFile(jarFile, annotationFile)
        val actualFile = File.createTempFile("writeAnnotations", annotationFile.getName())
        actualFile.createNewFile()
        println("Saved file: ${actualFile.getAbsolutePath()}")
        writeAnnotations(FileWriter(actualFile), typePositionAndAnnotationData)
        Assert.assertEquals(annotationFile.readText().trim().toUnixSeparators(), actualFile.readText().trim().toUnixSeparators())
    }
}



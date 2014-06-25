package annotations

import java.io.File
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import org.junit.Test
import org.junit.Assert.fail
import org.junit.Assert.assertEquals
import util.assertEqualsOrCreate
import org.jetbrains.kannotator.simpleErrorHandler

/** Reads xml annotations from `testData/annotations/read` and checks against expected reading (including error handling) */
class ReadAnnotationsTest {

    private var testResult = true

    fun doTest(file: File) {
        val actualSB = StringBuilder()
        parseAnnotations(file.reader(), { position, annotationData ->
            actualSB.appendln("$position")
            for (annotation in annotationData) {
                actualSB.appendln("    annotationClassFqn=${annotation.annotationClassFqn}, attributes=${annotation.attributes}")

            }
            actualSB.appendln()
        }, simpleErrorHandler{
            kind, message ->
            actualSB.appendln(message)
            actualSB.appendln()
        })

        println(actualSB)
        val expectedFile = File(file.getAbsolutePath().replaceAll(".xml", ".txt"))

        val success = assertEqualsOrCreate(expectedFile, actualSB.toString(), false)
        if (!success) {
            println("Expected data file does not exist: ${expectedFile}. It is created from actual data")
            testResult = false
        }
    }

    Test fun readAnnotationsTest() {
        File("testData/annotations/read").recurse {
            file ->
            if (file.isFile() && file.getName().endsWith(".xml")) {
                println("Processing: $file")
                doTest(file)
            }
        }

        if (!testResult) {
            fail("Missed expected data file(s)")
        }
    }
}


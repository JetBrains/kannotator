package interpreter

import java.io.File
import junit.framework.TestCase
import util.recurseIntoJars

class WholeLibInterpreterTest : TestCase() {

    fun test() {
        recurseIntoJars(File("lib")) {
            classType, classReader ->
            println("  " + classType.getInternalName())
            doTest(File("testData/wholeLib"), classType, classReader, false)
        }
    }
}

package interpreter

import junit.framework.TestCase
import org.objectweb.asm.commons.Method as AsmMethod

class InterpreterTest : TestCase() {

    fun testSimpleIf() {
        doTest(javaClass<interpreter.simpleIf.Test>())
    }

    fun testOneParam() {
        doTest(javaClass<interpreter.oneParam.Test>())
    }

    fun testStatic() {
        doTest(javaClass<interpreter._static.Test>())
    }

    fun testLoops() {
        doTest(javaClass<interpreter.loops.Test>())
    }
}